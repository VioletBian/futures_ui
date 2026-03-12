import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { NotificationBanner } from '../common/NotificationBanner';
import { createLimitUsageRule, fetchLimitUsageEnumOptions } from '../../services/limitUsageConfigService';
import type { AccountType, EnumOptionGroups, LimitUsageFormState, OptionType } from '../../types/limitUsage';
import { LimitUsageUtil } from './LimitUsage.util';
import './LimitUsage.css';

type NotificationState = {
  kind: 'success' | 'danger' | 'warning';
  message: string;
} | null;

const TAB_NAMES = ['Active Limit Usage Alerts', 'Historical Limit Usage Alerts', 'Audit'] as const;

export const LimitUsage = () => {
  const [enumOptions, setEnumOptions] = useState<EnumOptionGroups>({});
  const [formState, setFormState] = useState<LimitUsageFormState>(LimitUsageUtil.createEmptyForm());
  const [notification, setNotification] = useState<NotificationState>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState<(typeof TAB_NAMES)[number]>('Active Limit Usage Alerts');

  useEffect(() => {
    fetchLimitUsageEnumOptions()
      .then((result) => {
        setEnumOptions(result);
      })
      .catch(() => {
        setNotification({
          kind: 'danger',
          message: 'Failed to load enum options for LimitUsage.',
        });
      });
  }, []);

  const micOptions = useMemo(() => LimitUsageUtil.getMicOptions(enumOptions), [enumOptions]);
  const micFamilyOptions = useMemo(() => LimitUsageUtil.getMicFamilyOptions(enumOptions), [enumOptions]);
  const accountTypeOptions = useMemo(() => LimitUsageUtil.getAccountTypeOptions(enumOptions), [enumOptions]);
  const timezoneOptions = useMemo(() => LimitUsageUtil.getTimezoneOptions(enumOptions), [enumOptions]);
  const accountIdOptions = useMemo(() => LimitUsageUtil.getAccountIdOptions(enumOptions), [enumOptions]);
  const internalEmailOptions = useMemo(() => LimitUsageUtil.getInternalEmailOptions(enumOptions), [enumOptions]);
  const externalEmailOptions = useMemo(() => LimitUsageUtil.getExternalEmailOptions(enumOptions), [enumOptions]);
  const mockRows = useMemo(() => LimitUsageUtil.getMockRows(), []);

  const isTimeBasedRule = formState.alertRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE;
  const isMicDisabled = formState.selectedMicFamilyOptions.length > 0;
  const isMicFamilyDisabled = formState.selectedMicOptions.length > 0;

  const updateForm = <K extends keyof LimitUsageFormState>(key: K, value: LimitUsageFormState[K]) => {
    setFormState((current) => ({
      ...current,
      [key]: value,
    }));
  };

  const handleAlertRuleTypeChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextRuleType = event.target.value as LimitUsageFormState['alertRuleType'];

    setFormState((current) => ({
      ...current,
      alertRuleType: nextRuleType,
      limitUsageAlertTime:
        nextRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE
          ? current.limitUsageAlertTime || LimitUsageUtil.DEFAULT_ALERT_TIME
          : current.limitUsageAlertTime,
      limitUsageAlertTimezone:
        nextRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE
          ? current.limitUsageAlertTimezone || LimitUsageUtil.DEFAULT_TIMEZONE
          : current.limitUsageAlertTimezone,
    }));
  };

  const handleMicChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const selected = LimitUsageUtil.sanitizeSingleSelection(LimitUsageUtil.toOptionValues(event.target.options));

    setFormState((current) => ({
      ...current,
      selectedMicOptions: selected,
      selectedMicFamilyOptions: selected.length > 0 ? [] : current.selectedMicFamilyOptions,
    }));
  };

  const handleMicFamilyChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const selected = LimitUsageUtil.sanitizeSingleSelection(LimitUsageUtil.toOptionValues(event.target.options));

    setFormState((current) => ({
      ...current,
      selectedMicFamilyOptions: selected,
      selectedMicOptions: selected.length > 0 ? [] : current.selectedMicOptions,
    }));
  };

  const handleAccountIdsChange = (event: ChangeEvent<HTMLSelectElement>) => {
    updateForm('accountIds', LimitUsageUtil.toOptionValues(event.target.options));
  };

  const clearForm = () => {
    setFormState(LimitUsageUtil.createEmptyForm(formState.accountType));
  };

  const onSubmit = async () => {
    setNotification(null);
    const validationError = LimitUsageUtil.validateForm(formState);

    if (validationError) {
      setNotification({ kind: 'warning', message: validationError });
      return;
    }

    setIsSubmitting(true);

    try {
      await createLimitUsageRule(LimitUsageUtil.buildPayload(formState));
      setNotification({
        kind: 'success',
        message: 'Internal limit usage rule created. Please refresh grid below to see details.',
      });
      clearForm();
    } catch {
      setNotification({
        kind: 'danger',
        message: 'Unable to create limit usage rule. Please try again or contact support.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const displayMarginUsageThresholdInputField = () => {
    return (
      <div className="col-2">
        <div className="recap-input-field limitusage-input-wrapper">
          <label className="d-flex" htmlFor="limitUsageThreshold">
            % Margin Usage Threshold:
            <span
              className="material-icons tooltip-icon"
              title="Enter in format operator followed by value, for example >80"
            >
              info
            </span>
          </label>
          <input
            name="limitUsageThreshold"
            id="limitUsageThreshold"
            onChange={(event) => updateForm('limitUsageThreshold', event.target.value)}
            value={formState.limitUsageThreshold}
            placeholder="% Margin Usage Threshold"
            className="limitusage-input-field"
          />
        </div>
      </div>
    );
  };

  const displayTimeBasedRuleInputField = () => {
    return (
      <>
        <div className="col-2">
          <div className="recap-input-field limitusage-input-wrapper">
            <label className="d-flex" htmlFor="alertTime">
              Time:
              <span
                className="material-icons tooltip-icon"
                title="Time is in 24h format. You may enter specific time using keyboard input."
              >
                info
              </span>
            </label>
            <input
              id="alertTime"
              name="alertTime"
              data-testid="alertTime"
              className="limitusage-input-field"
              value={formState.limitUsageAlertTime}
              onChange={(event) => updateForm('limitUsageAlertTime', event.target.value)}
              placeholder={LimitUsageUtil.getTimePickerConfig().dateFormat}
            />
          </div>
        </div>

        <div className="col-2">
          <div className="recap-input-field limitusage-input-wrapper">
            <label htmlFor="timezone">Timezone:</label>
            <select
              name="timezone"
              id="timezone"
              className="limitusage-input-field"
              value={formState.limitUsageAlertTimezone}
              onChange={(event) => updateForm('limitUsageAlertTimezone', event.target.value)}
            >
              {LimitUsageUtil.mapOptionsToOptionElements(timezoneOptions)}
            </select>
          </div>
        </div>
      </>
    );
  };

  const renderMultiSelect = (
    id: string,
    label: string,
    options: OptionType[],
    selectedValues: string[],
    onChange: (event: ChangeEvent<HTMLSelectElement>) => void,
    disabled = false,
    testId?: string,
  ) => {
    return (
      <div className="recap-input-field limitusage-input-wrapper">
        <fieldset data-testid={testId}>
          <label htmlFor={id}>{label}</label>
          <select
            multiple
            id={id}
            name={id}
            className="limitusage-input-field"
            value={selectedValues}
            disabled={disabled || options.length === 0}
            onChange={onChange}
          >
            {LimitUsageUtil.mapOptionsToOptionElements(options)}
          </select>
        </fieldset>
      </div>
    );
  };

  return (
    <div className="limitusage-page">
      <div className="advisory-banner">
        <span>
          ** Use with caution. Setting up a rule here will automate the sending of limit usage alert email directly to the client. **{' '}
        </span>
        <a href="https://confluence.xx.com/display/EI/External+Client+Limit+Usage+Alerts" target="_blank" rel="noreferrer">
          How-To
        </a>
      </div>

      {notification ? <NotificationBanner kind={notification.kind} message={notification.message} /> : null}

      <div className="base-form-recap-container">
        <div className="base-form-recap-form-container">
          <div className="row">
            <div className="col-2">
              <div className="recap-input-field limitusage-input-wrapper">
                <label htmlFor="emailSubject">Email Subject:</label>
                <input
                  type="text"
                  id="emailSubject"
                  name="emailSubject"
                  onChange={(event) => updateForm('emailSubject', event.target.value)}
                  value={formState.emailSubject}
                  className="limitusage-input-field"
                  placeholder="Email Subject"
                />
              </div>
            </div>

            <div className="col-2">
              {renderMultiSelect(
                'venueMic',
                'MIC:',
                micOptions,
                formState.selectedMicOptions,
                handleMicChange,
                isMicDisabled,
                'venueDropdown',
              )}
              <div className="muted-note">MIC and MICFamily are mutually exclusive.</div>
            </div>

            <div className="col-2">
              <div className="recap-input-field limitusage-input-wrapper">
                <label className="d-flex" htmlFor="internalEmail">
                  Internal Email:
                  <span
                    className="material-icons tooltip-icon"
                    title="Internal email address must end with @com.xx"
                  >
                    info
                  </span>
                </label>
                <input
                  type="email"
                  name="internalEmail"
                  id="internalEmail"
                  onChange={(event) => updateForm('internalEmail', event.target.value)}
                  value={formState.internalEmail}
                  list="internalEmailOptions"
                  placeholder="Internal Email"
                  className="limitusage-input-field"
                />
                <datalist id="internalEmailOptions">
                  {LimitUsageUtil.mapOptionsToOptionElements(internalEmailOptions)}
                </datalist>
              </div>
            </div>
          </div>

          <div className="row">
            <div className="col-2">
              {renderMultiSelect(
                'venueMicFamily',
                'MICFamily:',
                micFamilyOptions,
                formState.selectedMicFamilyOptions,
                handleMicFamilyChange,
                isMicFamilyDisabled,
                'venueMicFamilyDropdown',
              )}
            </div>

            <div className="col-2">
              <div className="recap-input-field limitusage-input-wrapper">
                <label htmlFor="alertRuleType">Alert Rule Type:</label>
                <select
                  name="alertRuleType"
                  id="alertRuleType"
                  onChange={handleAlertRuleTypeChange}
                  value={formState.alertRuleType}
                  className="limitusage-input-field"
                >
                  {LimitUsageUtil.mapOptionsToOptionElements(LimitUsageUtil.getAlertRuleTypeOptions())}
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
                  onChange={(event) => updateForm('accountType', event.target.value as AccountType)}
                  value={formState.accountType.toString()}
                  className="limitusage-input-field"
                >
                  {LimitUsageUtil.mapOptionsToOptionElements(accountTypeOptions)}
                </select>
              </div>
            </div>

            <div className="col-2">
              {renderMultiSelect(
                'accountIds',
                'Account ID(s):',
                accountIdOptions,
                formState.accountIds,
                handleAccountIdsChange,
                false,
                'accountIdsDropdown',
              )}
            </div>

            <div className="col-2">
              <div className="recap-input-field limitusage-input-wrapper">
                <label htmlFor="externalEmail">External Email:</label>
                <select
                  name="externalEmail"
                  id="externalEmail"
                  disabled={!formState.isExternal || externalEmailOptions.length === 0}
                  onChange={(event) => updateForm('externalEmail', event.target.value)}
                  value={formState.externalEmail}
                  className="limitusage-input-field"
                >
                  <option value="">Select external email</option>
                  {LimitUsageUtil.mapOptionsToOptionElements(externalEmailOptions)}
                </select>
              </div>
            </div>
          </div>

          <div className="row">
            <div className="col-2">
              <div className="recap-input-field limitusage-input-wrapper external-checkbox-wrapper">
                <input
                  type="checkbox"
                  id="isExternal"
                  name="isExternal"
                  onChange={(event) => {
                    const checked = event.target.checked;
                    setFormState((current) => ({
                      ...current,
                      isExternal: checked,
                      externalEmail: checked ? current.externalEmail : '',
                    }));
                  }}
                  checked={formState.isExternal}
                />
                <label htmlFor="isExternal">For External</label>
              </div>
            </div>

            <div className="col-1 pull-right">
              <button className="btn btn-primary" onClick={onSubmit} disabled={isSubmitting}>
                Create
              </button>
            </div>
          </div>
        </div>

        <div className="tab-strip">
          {TAB_NAMES.map((tabName) => (
            <button
              key={tabName}
              type="button"
              className={activeTab === tabName ? 'active' : ''}
              onClick={() => setActiveTab(tabName)}
            >
              {tabName}
            </button>
          ))}
        </div>

        <div className="grid-panel">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Message</th>
                <th>MIC</th>
                <th>MICFamily</th>
                <th>Limit Usage Alert Threshold</th>
                <th>Limit Usage Alert Time</th>
                <th>Limit Usage Alert Timezone</th>
                <th>Account ID</th>
                <th>External Email</th>
                <th>Internal Email</th>
                <th>Created By</th>
                <th>Created On</th>
              </tr>
            </thead>
            <tbody>
              {mockRows.map((row, index) => (
                <tr key={`${row.message}-${index + 1}`}>
                  <td>{index + 1}</td>
                  <td>{row.message}</td>
                  <td>{row.mic}</td>
                  <td>{row.micFamily}</td>
                  <td>{row.threshold}</td>
                  <td>{row.time}</td>
                  <td>{row.timezone}</td>
                  <td>{row.accountId}</td>
                  <td>{row.externalEmail}</td>
                  <td>{row.internalEmail}</td>
                  <td>{row.createdBy}</td>
                  <td>{row.createdOn}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default LimitUsage;