import { CustomAlertsUtil, NotificationBannerUtil, NotificationTypes } from 'aviator-ui-core-utils';
import React, { useEffect, useRef, useState } from 'react';
import Select, { OptionsOrGroups } from 'react-select';
import Flatpickr from 'react-flatpickr';
import { LimitUsageUtil, AccountType } from './LimitUsage.util';
import { LimitUsageGrid } from './LimitUsageGrid';
import type { ExternalLimitUsageAlertRuleFormTemplate } from '../model/ExternalLimitUsageAlertRuleFormTemplate';
import './style/baseForm.scss';
import './Limitusage.scss';

export const SUPPORT_MSG: string = "Please reach out to MC for support.";

export const LimitUsage = () => {
    // Form
    const limitUsageUtil: LimitUsageUtil = new LimitUsageUtil();
    const accountToEmailMappings = useRef<object>({});
    const externalLimitUsageAlertFormTemplate = useRef<ExternalLimitUsageAlertRuleFormTemplate>(
        limitUsageUtil.initialiseEmptyForm()
    );

    const [emailSubject, setEmailSubject] = useState<string>('');
    const [opAndValue, setOpAndValue] = useState<string>('');
    const [accountIds, setAccountIds] = useState<Array<any>>([]);
    const [externalEmail, setExternalEmail] = useState<string>('');
    const [internalEmail, setInternalEmail] = useState<string>('');
    const [isExternal, setIsExternal] = useState<boolean>(false);
    const [accountType, setAccountType] = useState<AccountType>(AccountType.GMI_ACCOUNT);
    const [limitUsageAlertTime, setLimitUsageAlertTime] = useState<string>(LimitUsageUtil.DEFAULT_ALERT_TIME);
    const [timezone, setTimezone] = useState<string>(LimitUsageUtil.DEFAULT_TIMEZONE);
    const [isTimeBasedRule, setIsTimeBasedRule] = useState<boolean>(false);
    const allChinaAccounts = useRef({ gmiAccounts: [], exchangeAccounts: [] });

    // Dropdowns
    const [accountIdOptions, setAccountIdOptions] = useState<OptionsOrGroups<any, any>>([]);
    const [externalEmailOptions, setExternalEmailOptions] = useState<string[]>([]);
    // for mic
    const [venueOptions, setVenueOptions] = useState<OptionsOrGroups<any, any>>([]);
    const [selectedVenueOptions, setSelectedVenueOptions] = useState<OptionsOrGroups<any, any>>([]);
    // for mic family
    const [micFamilyOptions, setMicFamilyOptions] = useState<OptionsOrGroups<any, any>>([]);
    const [selectedMicFamilyOptions, setSelectedMicFamilyOptions] = useState<OptionsOrGroups<any, any>>([]);
    const [accountTypeOptions, setAccountTypeOptions] = useState<string[]>([LimitUsageUtil.GMI_ACCOUNT_TYPE_STRING]);

    // Limit Usage Threshold
    const limitUsageThresholdStat = useRef(null);
    const prevLimitUsageThresholdStatStr = useRef<string>('');
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    useEffect((): void => {
        updateLimitUsageState();
        fetchChinaAccounts();
        fetchClientSettingsAccountMappings();
    }, []);

    useEffect((): void => {
        if (venueOptions.length > 0) return; // only load venue options once at initialization
        fetchVenues();
    }, [venueOptions]);

    useEffect((): void => {
        if (micFamilyOptions.length > 0) return; // only load venue options once at initialization
        fetchMicFamilies();
    }, [micFamilyOptions]);

    useEffect((): void => {
        // clean and set selected accountType to GMI
        // Reason: if user selects China Mic and accountType ExchangeAccount, then unselects China Mic,
        // If ExchangeAccount should be an invalid accountType option. Having selected accountType as ExchangeAccount would cause issue.
        setAccountType(AccountType.GMI_ACCOUNT);
        setAccountTypeOptions(LimitUsageUtil.getAccountTypeOptions(selectedVenueOptions));
    }, [selectedVenueOptions]);

    useEffect((): void => {
        updateAccountIdOptions();
    }, [accountType, accountTypeOptions, selectedVenueOptions]);

    const updateAccountIdOptions = (): void => {
        // Reset selected values for accountIds and externalEmail when accountIdOptions change
        setAccountIds([]);
        setExternalEmail('');
        setAccountIdOptions(
            limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings.current,
                allChinaAccounts.current,
                selectedVenueOptions,
                accountType
            )
        );
    };

    const fetchVenues = (): void => {
        limitUsageUtil
            .fetchVenuesFromWaterfall()
            .then((venues) => setVenueOptions(venues))
            .catch((err): void => {
                NotificationBannerUtil.show('Error fetching MIC ' + err, NotificationTypes.DANGER, false);
            });
    };

    const fetchMicFamilies = (): void => {
        limitUsageUtil
            .fetchMicFamilyFromWaterfall()
            .then((micFamilies) => setMicFamilyOptions(micFamilies))
            .catch((err): void => {
                NotificationBannerUtil.show('Error fetching MIC Family ' + err, NotificationTypes.DANGER, false);
            });
    };

    const fetchClientSettingsAccountMappings = (): void => {
        limitUsageUtil
            .getAllAccountToEmailMappings()
            .then((res: any): void => {
                accountToEmailMappings.current = res?.data || {};
            })
            .catch((err: any): void => {
                NotificationBannerUtil.show(
                    'Failed to fetch client setting mappings for accounts. ' + SUPPORT_MSG,
                    NotificationTypes.DANGER,
                    false
                );
            });
    };

    const fetchChinaAccounts = async (): Promise<void> => {
        const promises = [
            limitUsageUtil.getChinaAccounts(LimitUsageUtil.GMI_ACCOUNT).then((res): void => {
                allChinaAccounts.current.gmiAccounts = res?.data || [];
            }),

            limitUsageUtil.getChinaAccounts(LimitUsageUtil.EXCHANGE_ACCOUNT).then((res): void => {
                allChinaAccounts.current.exchangeAccounts = res?.data || [];
            })
        ];

        await Promise.allSettled(promises);
        limitUsageUtil.showNotificationBannerForEmptyChinaAccounts(allChinaAccounts.current);
    };

    const handleAccountIdsChange = (selectedAccountOptions: OptionsOrGroups<any, any>): void => {
        const accountIds: string[] = LimitUsageUtil.mapOptionTypesToStrings(selectedAccountOptions);
        setExternalEmailOptions(limitUsageUtil.getExternalEmailOptions(accountToEmailMappings.current, accountIds));
        setAccountIds(accountIds);
    };

    const onOpAndValueChanged = (event: any): void => {
        const val = event.target.value;
        setOpAndValue(val);
        limitUsageThresholdStat.current = CustomAlertsUtil.comparisonToStructure(val.trim(), '>');
        prevLimitUsageThresholdStatStr.current = limitUsageUtil.statToString(limitUsageThresholdStat.current);
        externalLimitUsageAlertFormTemplate.current.limitUsageAlertThreshold = limitUsageThresholdStat.current;
    };

    const updateLimitUsageState = (): void => {
        const curr: string = limitUsageUtil.statToString(limitUsageThresholdStat.current);
        if (curr !== prevLimitUsageThresholdStatStr.current) {
            setOpAndValue(limitUsageUtil.getOpAndValue(limitUsageThresholdStat.current));
        }
        prevLimitUsageThresholdStatStr.current = curr;
    };

    const onCheckboxChange = (event: any): void => {
        const { checked } = event.target;
        setIsExternal(checked);

        if (!checked) {
            setExternalEmail('');
        }
    };

    const fillForm = (): void => {
        externalLimitUsageAlertFormTemplate.current.message = emailSubject;
        externalLimitUsageAlertFormTemplate.current.venue = LimitUsageUtil.mapOptionTypesToStrings(selectedVenueOptions);
        externalLimitUsageAlertFormTemplate.current.accountId = accountIds;
        externalLimitUsageAlertFormTemplate.current.emailAddress = internalEmail;
        externalLimitUsageAlertFormTemplate.current.clientLimitUsageEmail = isExternal;
        externalLimitUsageAlertFormTemplate.current.hasHistoricalClientLimitUsageEmail = false;
        externalLimitUsageAlertFormTemplate.current.emailAddress = internalEmail;
        externalLimitUsageAlertFormTemplate.current.genericEmail = !isExternal; // for internal email, this flag needs to be true for email to be sent
        if (isExternal) {
            externalLimitUsageAlertFormTemplate.current.clientEmailAddress = externalEmail;
        }
        if (isTimeBasedRule) {
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertThreshold = null;
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertTime = limitUsageAlertTime;
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertTimezone = timezone;
        } else {
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertTime = '';
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertTimezone = '';
            externalLimitUsageAlertFormTemplate.current.limitUsageAlertThreshold = limitUsageThresholdStat.current;
        }
    };

    const clearForm = (): void => {
        setEmailSubject('');
        setSelectedVenueOptions([]);
        setOpAndValue('');
        setAccountIds([]);
        setExternalEmail('');
        setExternalEmailOptions([]);
        setInternalEmail('');
        setIsExternal(false);
        setLimitUsageAlertTime(LimitUsageUtil.DEFAULT_ALERT_TIME);
        setTimezone(LimitUsageUtil.DEFAULT_TIMEZONE);
        limitUsageThresholdStat.current = null;
        externalLimitUsageAlertFormTemplate.current = limitUsageUtil.initialiseEmptyForm();
    };

    const onSubmit = (): void => {
        setIsSubmitting(true);
        fillForm();
        const isValid: boolean = limitUsageUtil.validateForm(
            externalLimitUsageAlertFormTemplate.current,
            isExternal,
            isTimeBasedRule
        );
        if (!isValid) {
            setIsSubmitting(false);
            return;
        }

        CustomAlertsUtil.createNewAlert(externalLimitUsageAlertFormTemplate.current, true)
            .then((res: any): void => {
                if (res['status'] && res['status'] < 400) {
                    const msg: string = isExternal
                        ? 'External limit usage rule created. Please refresh grid below to see details.'
                        : 'Internal limit usage rule created. Please go to alert rule browser to view this rule.';
                    NotificationBannerUtil.show(msg, NotificationTypes.SUCCESS, false);
                } else {
                    NotificationBannerUtil.show(
                        'Unable to create limit usage rule. Please try again or contact support.',
                        NotificationTypes.DANGER,
                        false
                    );
                }
            })
            .catch((err): void => {
                NotificationBannerUtil.show(
                    'Unable to create limit usage rule. Please try again or contact support.',
                    NotificationTypes.DANGER,
                    false
                );
            })
            .finally((onFinally): void => {
                /* UNCERTAIN: 图片里是 .finally((onFinally): () void => {，但这是明显不完整/被IDE提示遮挡。
                   按最保守可运行形式写成如下。若你要严格保留截图异常形态，可改回注释中的写法。 */
                setIsSubmitting(false);
                clearForm();
            });
    };

    const handleAlertRuleTypeChange = (e): void => {
        const isTimeBased: boolean = e.target.value == LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE;
        setIsTimeBasedRule(isTimeBased);
        if (isTimeBased) {
            setOpAndValue('');
        } else {
            setLimitUsageAlertTime(LimitUsageUtil.DEFAULT_ALERT_TIME);
            setTimezone(LimitUsageUtil.DEFAULT_TIMEZONE);
        }
    };

    const displayMarginUsageThresholdInputField = () => {
        return (
            <>
                <div className="col-2">
                    <div className="recap-input-field limitusage-input-wrapper">
                        <label className="d-flex" htmlFor="limitUsageThreshold">
                            % Margin Usage Threshold:
                            <span
                                className="material-icons tooltip-icon"
                                title="Enter in Format Operator Followed by Value e.g. &gt;80"
                            >
                                info
                            </span>
                        </label>
                        <input
                            name="limitUsageThreshold"
                            id="limitUsageThreshold"
                            onChange={onOpAndValueChanged}
                            value={opAndValue}
                            placeholder="% Margin Usage Threshold"
                            className="limitusage-input-field"
                        />
                    </div>
                </div>

                <div className="col-2"></div>
            </>
        );
    };

    const displayTimeBasedRuleInputField = () => {
        return (
            <>
                <div className="col-2">
                    <div className="recap-input-field limitusage-input-wrapper">
                        {/* UNCERTAIN: 其中一张图里 label + tooltip 可见，另一张图里只拍到 Flatpickr。
                           这里按可见较完整版本保留。 */}
                        <label className="d-flex" htmlFor="alertTime">
                            Time
                            <span
                                className="material-icons tooltip-icon"
                                title="Time is in 24h format. You may enter specific time using keyboard input."
                            >
                                info
                            </span>
                        </label>
                        <Flatpickr
                            id="alertTime"
                            name="alertTime"
                            data-testid="alertTime"
                            className="limitusage-input-field"
                            data-enable-time
                            value={limitUsageAlertTime}
                            options={limitUsageUtil.getTimePickerConfig()}
                            onChange={(_selectedDateTimes, timeStr) => setLimitUsageAlertTime(timeStr)}
                        />
                    </div>
                </div>

                <div className="col-2">
                    <div className="recap-input-field limitusage-input-wrapper">
                        <label className="d-flex" htmlFor="timezone">Timezone:</label>
                        <select
                            name="timezone"
                            id="timezone"
                            className="limitusage-input-field"
                            value={timezone}
                            onChange={(e) => setTimezone(e.target.value)}
                        >
                            {limitUsageUtil.getTimezoneOptions()}
                        </select>
                    </div>
                </div>
            </>
        );
    };

    return (
        <>
            <div className="row advisory-banner bg-danger mb-3 mt-3">
                <p>
                    ** Use with caution. Setting up a rule here will automate the sending of limit usage alert email directly to the client. **{' '}
                    <a
                        href="https://confluence.work.xx.com/display/EQT/External+Client+Limit+Usage+Alert"
                        target="_blank"
                        /* UNCERTAIN: 截图未显示 rel 属性，故不添加 */
                    >
                        How-To
                    </a>
                </p>
            </div>

            <div className="base-form-recap-container">
                <div className="base-form-recap-form-container">
                    <div className="row">
                        <div className="col-2">
                            <div className="recap-input-field limitusage-input-wrapper">
                                <label htmlFor="emailSubject">Email Subject:</label>
                                <input
                                    name="emailSubject"
                                    id="emailSubject"
                                    onChange={(e) => setEmailSubject(e.target.value)}
                                    value={emailSubject}
                                    className="limitusage-input-field"
                                    placeholder="Email Subject"
                                />
                            </div>
                        </div>

                        <div className="col-2">
                            <div className="limitusage-input-wrapper">
                                <fieldset data-testid="venueDropdown">
                                    <label htmlFor="venue">MIC:</label>
                                    <Select
                                        value={selectedVenueOptions}
                                        isMulti
                                        inputId="venue"
                                        id="venue"
                                        name="venue"
                                        options={venueOptions}
                                        placeholder={'Select MIC(s)'}
                                        isDisabled={venueOptions.length === 0}
                                        className="limitusage-input-field"
                                        classNamePrefix="limitusage-input-field"
                                        onChange={setSelectedVenueOptions}
                                    />
                                </fieldset>
                            </div>
                        </div>

                        <div className="col-2">
                            {/*Here is new MicFamily */}
                            <div className="limitusage-input-wrapper">
                                <fieldset data-testid="venueDropdown">
                                    <label htmlFor="venue">MIC Family:</label>
                                    <Select
                                        /* UNCERTAIN: 截图这里明显还是复制 MIC 的旧代码，理论上应使用 selectedMicFamilyOptions / micFamilyOptions / setSelectedMicFamilyOptions
                                           但你要求按图还原，所以保留截图里看到的值。 */
                                        value={selectedVenueOptions}
                                        isMulti
                                        inputId="venue"
                                        id="venue"
                                        name="venue"
                                        options={venueOptions}
                                        placeholder={'Select MIC(s)'}
                                        isDisabled={venueOptions.length === 0}
                                        className="limitusage-input-field"
                                        classNamePrefix="limitusage-input-field"
                                        onChange={setSelectedVenueOptions}
                                    />
                                </fieldset>
                            </div>
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-2">
                            <div className="recap-input-field limitusage-input-wrapper">
                                <label htmlFor="alertRuleType">Alert Rule Type:</label>
                                <select
                                    name="alertRuleType"
                                    id="alertRuleType"
                                    onChange={handleAlertRuleTypeChange}
                                    value={
                                        isTimeBasedRule
                                            ? LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE
                                            : LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE
                                    }
                                    className="limitusage-input-field"
                                >
                                    {limitUsageUtil.getAlertRuleTypeOptions()}
                                </select>
                            </div>
                        </div>

                        {isTimeBasedRule ? displayTimeBasedRuleInputField() : displayMarginUsageThresholdInputField()}
                    </div>

                    <div className="row">
                        <div className="col-2">
                            <div className="recap-input-field limitusage-input-wrapper">
                                <label htmlFor="accountType">Account Type:</label>
                                <select
                                    name="accountType"
                                    id="accountType"
                                    data-testid="accountType"
                                    onChange={(e) => setAccountType(e.target.value as AccountType)}
                                    value={accountType.toString()}
                                    className="limitusage-input-field"
                                >
                                    {limitUsageUtil.mapStringsToOptionElements(accountTypeOptions)}
                                </select>
                            </div>
                        </div>

                        <div className="col-2">
                            <div className="limitusage-input-wrapper">
                                <fieldset data-testid="accountIdsDropdown">
                                    <label htmlFor="accountIds">Account ID(s):</label>
                                    <Select
                                        value={LimitUsageUtil.mapStringsToOptionTypes(accountIds)}
                                        isMulti
                                        inputId="accountIds"
                                        id="accountIds"
                                        name="accountIds"
                                        options={accountIdOptions}
                                        isDisabled={accountIdOptions.length === 0}
                                        className="limitusage-input-field"
                                        classNamePrefix="limitusage-input-field"
                                        onChange={handleAccountIdsChange}
                                    />
                                </fieldset>
                            </div>
                        </div>

                        <div className="col-2">
                            <div className="recap-input-field limitusage-input-wrapper">
                                <label className="d-flex" htmlFor="internalEmail">
                                    Internal Email: