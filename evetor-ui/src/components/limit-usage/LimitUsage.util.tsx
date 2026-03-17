/* TODO:
   Keep/adjust imports to match your real project.
   The screenshot had folded imports, so below is a reasonable reconstruction.
*/
import axios from 'axios';
import get from 'lodash/get';
import React from 'react';
import { OptionsOrGroups } from 'react-select';
import { NotificationBannerUtil, NotificationTypes } from 'aviator-ui-core-utils';
// TODO: replace these imports with real paths in your repo
// import { ApplicationConfigurationUtil } from '...';
// import type { IAppConfig } from '...';
import { SUPPORT_MSG } from './LimitUsage';
import type { ExternalLimitUsageAlertRuleFormTemplate } from '../model/ExternalLimitUsageAlertRuleFormTemplate';

declare const ApplicationConfigurationUtil: any;
declare const Common: any;

export enum AccountType {
    GMI_ACCOUNT = 'GMI',
    EXCHANGE_ACCOUNT = 'Exchange Account'
}

export class LimitUsageUtil {
    public static MARGIN_USAGE_ALERT_TIME_RULE: string = 'Margin Usage Alert Time';
    public static MARGIN_USAGE_THRESHOLD_RULE: string = 'Margin Usage Threshold';
    public static CHINA_MIC_LIST: string[] = ['XINE', 'XZCE', 'XDCE', 'GFEX', 'XSGE', 'CCFX'];
    public static GMI_ACCOUNT: string = 'gmi';
    public static EXCHANGE_ACCOUNT: string = 'exchangeaccount';
    public static ALL_ACCOUNT_TYPE_STRINGS: string[] = [AccountType.GMI_ACCOUNT.toString(), AccountType.EXCHANGE_ACCOUNT.toString()];
    public static GMI_ACCOUNT_TYPE_STRING: string[] = [AccountType.GMI_ACCOUNT.toString()];
    public static TIMEZONES: string[] = [
        'America/New_York',
        'America/Chicago',
        'Asia/Hong_Kong',
        'Asia/Tokyo',
        'Asia/Singapore',
        'Australia/Melbourne',
        'Australia/Sydney',
        'Europe/London'
    ];
    public static DEFAULT_ALERT_TIME: string = '15:00';
    public static DEFAULT_TIMEZONE: string = 'Asia/Hong_Kong';

    public limitUsageServerUrl: string = '';
    public emailAddress: string = '';
    public waterfallUri: string = '';
    public chinaMarginApiUrl: string = '';
    public enableLimitUsageAlertTimeRule: boolean = false;

    constructor() {
        ApplicationConfigurationUtil.getConfiguration().subscribe(({ config }: any): void => {
            this.limitUsageServerUrl = get(config, 'recapsServerUrl', '');
            this.emailAddress = get(config, 'userEmail', '');
            this.waterfallUri = get(config, 'waterfallUri', '');
            this.chinaMarginApiUrl = get(config, 'chinaMarginApiUrl', '');
            this.enableLimitUsageAlertTimeRule = get(config, 'enableLimitUsageAlertTimeRule', false);
        });
    }

    public getSimpleExchangeNames(): any[] {
        return [
            { value: 'ASX', label: 'ASX' },
            { value: 'ATDE', label: 'ATDE' },
            { value: 'BDPF', label: 'BDPF' },
            { value: 'BIST', label: 'BISTF' },
            { value: 'BMF', label: 'BMF' },
            { value: 'CBOF', label: 'CBOF' },
            { value: 'CCFX', label: 'CCFX' },
            { value: 'CFE', label: 'CFE' },
            { value: 'CME', label: 'CME' },
            { value: 'COMEX', label: 'COMEX' },
            { value: 'EEX', label: 'EEX' },
            { value: 'ELX', label: 'ELX' },
            { value: 'EUR AMS', label: 'EUR AMS' },
            { value: 'EUR BRU', label: 'EUR BRU' },
            { value: 'EUR LIS', label: 'EUR LIS' },
            { value: 'EUR PAR', label: 'EUR PAR' },
            { value: 'EUREX', label: 'EUREX' },
            { value: 'HKFE', label: 'HKFE' },
            { value: 'ICE EU', label: 'ICE EU' },
            { value: 'ICE US', label: 'ICE US' },
            { value: 'IDEM', label: 'IDEM' },
            { value: 'INDE', label: 'INDE' },
            { value: 'INDF', label: 'INDF' },
            { value: 'KCBT', label: 'KCBT' },
            { value: 'KFE', label: 'KFE' },
            { value: 'KLOF', label: 'KLOF' },
            { value: 'LME', label: 'LME' },
            { value: 'LSED', label: 'LSED' },
            { value: 'MEFF', label: 'MEFF' },
            { value: 'MFE', label: 'MFE' },
            { value: 'MGE', label: 'MGE' },
            { value: 'MXDR', label: 'MXDR' },
            { value: 'NOMX', label: 'NOMX' },
            { value: 'NYMEX', label: 'NYMEX' },
            { value: 'NZFE', label: 'NZFE' },
            { value: 'OSE', label: 'OSE' },
            { value: 'OTOB', label: 'OTOB' },
            { value: 'RTSX', label: 'RTSX' },
            { value: 'SAFEX', label: 'SAFEX' },
            { value: 'SFE', label: 'SFE' },
            { value: 'SGX', label: 'SGX' },
            { value: 'TFEX', label: 'TFEX' },
            { value: 'TIFF', label: 'TIFF' },
            { value: 'TKYO', label: 'TKYO' },
            { value: 'TLVE', label: 'TLVE' },
            { value: 'TWFX', label: 'TWFX' },
            { value: 'UNKNOWN', label: 'UNKNOWN' },
            { value: 'WPG', label: 'WPG' },
            { value: 'WSE', label: 'WSE' },
            { value: 'XSIM', label: 'XSIM' }
        ];
    }

    public initialiseEmptyForm(): ExternalLimitUsageAlertRuleFormTemplate {
        return {
            message: '',
            venue: [],
            // 中文注释：MicFamily 在单选语义下以单个字符串承载，空值统一用空字符串表示。
            micFamily: '',
            limitUsageAlertThreshold: null,
            limitUsageAlertTime: '',
            limitUsageAlertTimezone: '',
            accountId: [],
            clientEmailAddress: '',
            emailAddress: '',
            clientLimitUsageEmail: true,
            hasHistoricalClientLimitUsageEmail: false,
            genericEmail: false
        } as any;
    }

    public hasOnlyChinaMics(venues: string[]): boolean {
        if (venues.length === 0) {
            return false;
        }
        for (const venue of venues) {
            if (!LimitUsageUtil.CHINA_MIC_LIST.includes(venue)) {
                return false;
            }
        }
        return true;
    }

    public hasOnlyNonChinaMics(venues: string[]): boolean {
        if (venues.length === 0) {
            return false;
        }
        for (const venue of venues) {
            if (LimitUsageUtil.CHINA_MIC_LIST.includes(venue)) {
                return false;
            }
        }
        return true;
    }

    public hasMixedMics(venues: string[]): boolean {
        return venues.length > 0 && !this.hasOnlyChinaMics(venues) && !this.hasOnlyNonChinaMics(venues);
    }

    public hasOnlyChinaMicFamilies(micFamily: string): boolean {
        if (!micFamily) {
            return false;
        }
        return micFamily === Common.MicFamily.SFX;
    }

    public hasOnlyNonChinaMicFamilies(micFamily: string): boolean {
        if (!micFamily) {
            return false;
        }
        return micFamily !== Common.MicFamily.SFX;
    }

    public hasOnlyChinaSelections(venues: string[], micFamily: string): boolean {
        if (venues.length > 0 && !!micFamily) {
            return false;
        }
        if (venues.length > 0) {
            return this.hasOnlyChinaMics(venues);
        }
        if (micFamily) {
            return this.hasOnlyChinaMicFamilies(micFamily);
        }
        return false;
    }

    public hasOnlyNonChinaSelections(venues: string[], micFamily: string): boolean {
        if (venues.length > 0 && !!micFamily) {
            return false;
        }
        if (venues.length > 0) {
            return this.hasOnlyNonChinaMics(venues);
        }
        if (micFamily) {
            return this.hasOnlyNonChinaMicFamilies(micFamily);
        }
        return false;
    }

    public hasMixedSelections(venues: string[], micFamily: string): boolean {
        if (venues.length > 0 && !!micFamily) {
            return true;
        }
        if (venues.length > 0) {
            return this.hasMixedMics(venues);
        }
        return false;
    }

    public isValidMicFamily(micFamily: string): boolean {
        return typeof micFamily === 'string' && micFamily.length > 0;
    }

    public getChinaAccounts(accountType: string): Promise<any> {
        return axios.get(`${this.chinaMarginApiUrl}/rest/margincontrol/cn_offshore/accountList/${accountType}`, {
            withCredentials: true,
            responseType: 'json'
        });
    }

    public getAllAccountToEmailMappings(): Promise<any> {
        return axios.get(`${this.limitUsageServerUrl}/rest/clientsettings/cache/limitusage/accounts/all`, {
            withCredentials: true,
            responseType: 'json'
        });
    }

    public extractChinaAccountsFromClientSettings(chinaAccountList: string[], clientSettingsAccounts: string[]): string[] {
        return this.extractAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts, true);
    }

    public extractNonChinaAccountsFromClientSettings(chinaAccountList: string[], clientSettingsAccounts: string[]): string[] {
        return this.extractAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts, false);
    }

    public extractAccountsFromClientSettings(
        chinaAccountList: string[],
        clientSettingsAccounts: string[],
        isForChinaMic: boolean
    ): string[] {
        const filteredList: string[] = [];
        for (const account of clientSettingsAccounts) {
            const shouldIncludeAccount: boolean = isForChinaMic
                ? chinaAccountList.includes(account)
                : !chinaAccountList.includes(account);
            if (shouldIncludeAccount) {
                filteredList.push(account);
            }
        }
        return filteredList;
    }

    public convertCommaSeparatedStrToList = (str: string): string[] => {
        const stripped: string = (str || '').replace(/\s+/g, '');
        return stripped ? stripped.split(',') : [];
    };

    public areEqualSets = (emailAddressSet: Set<string>, combinedEmailAddressSet: Set<string>): boolean => {
        return emailAddressSet.size === combinedEmailAddressSet.size
            && [...emailAddressSet].every((value: string) => combinedEmailAddressSet.has(value));
    };

    public convertAccountToEmailsMapToString = (map: Map<string, Set<string>>): string => {
        let res: string = '';
        for (const [account, emails] of map) {
            const msg: string = `[${account}: ${Array.from(emails).join(', ')}]; `;
            res = res.concat(msg);
        }
        return res;
    };

    public isValidExternalEmail = (
        accountToEmailsMap: Map<string, Set<string>>,
        emailUnion: Set<string>
    ): boolean => {
        if (emailUnion.size === 0) {
            return false;
        }

        for (const [, emails] of accountToEmailsMap) {
            if (!this.areEqualSets(emails, emailUnion)) {
                const errMsg: string =
                    'This limit rule is not allowed as current account ID(s) has different external email addresses: '
                    + this.convertAccountToEmailsMapToString(accountToEmailsMap);
                NotificationBannerUtil.show(errMsg, NotificationTypes.WARNING, false);
                return false;
            }
        }
        return true;
    };

    public validateForm(form: any, isExternal: boolean, isTimeBased: boolean): boolean {
        const missingFields: string[] = [];
        const emailSubject = get(form, 'message', '');
        const venue = get(form, 'venue', []);
        const micFamily = get(form, 'micFamily', '');
        const limitUsageThreshold = get(form, 'limitUsageAlertThreshold', null);
        const accountIds = get(form, 'accountId', []);
        const internalEmail = get(form, 'emailAddress', '');
        const externalEmail = get(form, 'clientEmailAddress', '');
        const alertTime = get(form, 'limitUsageAlertTime', '');
        const alertTimezone = get(form, 'limitUsageAlertTimezone', '');

        const hasMic = Array.isArray(venue) && venue.length > 0;
        const hasMicFamily = typeof micFamily === 'string' && micFamily.length > 0;

        if (hasMic && hasMicFamily) {
            NotificationBannerUtil.show(
                'MIC and MIC Family are mutually exclusive. Please select only one.',
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        if (!hasMic && !hasMicFamily) {
            missingFields.push('MIC or MIC Family');
        }

        if (hasMic && this.hasMixedMics(venue)) {
            NotificationBannerUtil.show(
                'Please do not enter a mixture of China and Non-China MICs.',
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        // 中文注释：当前分支把 MicFamily 收口为单选，旧的数组输入直接判为非法，避免新旧契约混用。
        if (Array.isArray(micFamily)) {
            NotificationBannerUtil.show(
                'MIC Family only supports a single selection.',
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        if (hasMicFamily && !this.isValidMicFamily(micFamily)) {
            NotificationBannerUtil.show(
                'Invalid MIC Family selected.',
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        if (!isTimeBased && !limitUsageThreshold) {
            NotificationBannerUtil.show(
                'Incorrect input format for % Margin Usage Threshold, please key in operator followed by value e.g. >80',
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        if (!emailSubject) {
            missingFields.push('Email Subject');
        }

        if (isTimeBased) {
            if (!alertTime) {
                missingFields.push('Margin Usage Alert Time');
            }
            if (!alertTimezone) {
                missingFields.push('Timezone');
            }
        } else if (limitUsageThreshold?.value == null) {
            missingFields.push('% Margin Usage Threshold');
        }

        if (accountIds.length === 0) {
            missingFields.push('Account ID(s)');
        }

        if (!internalEmail || !internalEmail.toString().toLocaleLowerCase().includes('@xx.com')) {
            missingFields.push('Internal Email Address ending with @xx.com');
        }

        if (isExternal && !externalEmail) {
            missingFields.push('External Email Address');
        }

        if (missingFields.length > 0) {
            NotificationBannerUtil.show(
                'Following fields need to be populated: ' + missingFields.join(', '),
                NotificationTypes.WARNING,
                false
            );
            return false;
        }

        return true;
    }

    public fetchVenuesFromWaterfall(): Promise<any> {
        if (!this.waterfallUri) {
            return Promise.reject('No Waterfall uri found in config.');
        }

        return axios.get(`${this.waterfallUri}/rest/search/enums`, {
            withCredentials: true,
            responseType: 'json'
        }).then((res: any) => {
            return [
                ...((res?.data?.mic || []).map((item: any) => ({ value: item, label: item }))),
                ...this.getSimpleExchangeNames()
            ].sort((a: any, b: any) => a.value.localeCompare(b.value));
        });
    }

    public fetchMicFamilyFromWaterfall(): Promise<any> {
        if (!this.waterfallUri) {
            return Promise.reject('No Waterfall uri found in config.');
        }

        return axios.get(`${this.waterfallUri}/rest/search/enums`, {
            withCredentials: true,
            responseType: 'json'
        }).then((res: any) => {
            return ((res?.data?.micFamily || []).map((item: any) => ({ value: item, label: item })))
                .sort((a: any, b: any) => a.value.localeCompare(b.value));
        });
    }

    public getEmailOptionsFromEmailSet(emailSet: Set<string>): string[] {
        return Array.from(emailSet);
    }

    public getFilteredAccountsList(
        accountToEmailMappings: Object,
        allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] },
        venues: string[],
        micFamilies: string[],
        accountType: string
    ): string[] {
        const clientSettingsAccountList: string[] = Object.keys(accountToEmailMappings);
        const combinedChinaAccountList: string[] = [
            ...allChinaAccounts.gmiAccounts,
            ...allChinaAccounts.exchangeAccounts
        ];

        let filteredAccountList: string[] = [];

        if (this.hasOnlyNonChinaSelections(venues, micFamilies)) {
            filteredAccountList = this.extractNonChinaAccountsFromClientSettings(
                combinedChinaAccountList,
                clientSettingsAccountList
            );
        } else if (this.hasOnlyChinaSelections(venues, micFamilies)) {
            if (accountType === AccountType.GMI_ACCOUNT) {
                filteredAccountList = this.extractChinaAccountsFromClientSettings(
                    allChinaAccounts.gmiAccounts,
                    clientSettingsAccountList
                );
            } else if (accountType === AccountType.EXCHANGE_ACCOUNT) {
                filteredAccountList = this.extractChinaAccountsFromClientSettings(
                    allChinaAccounts.exchangeAccounts,
                    clientSettingsAccountList
                );
            }
        }

        return filteredAccountList;
    }

    public getFilteredAccountOptions(
        accountToEmailMappings: Object,
        allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] },
        venueOptions: OptionsOrGroups<any, any>,
        micFamilyOption: any,
        accountType: string
    ): OptionsOrGroups<any, any> {
        const venues: string[] = LimitUsageUtil.mapOptionTypesToStrings(venueOptions);
        const micFamily: string = LimitUsageUtil.mapOptionTypeToString(micFamilyOption);

        const accountList: string[] = this.getFilteredAccountsList(
            accountToEmailMappings,
            allChinaAccounts,
            venues,
            micFamily ? [micFamily] : [],
            accountType
        );

        return accountList.map((filteredAccount: string) => ({
            value: filteredAccount,
            label: filteredAccount
        }));
    }

    public showNotificationBannerForEmptyChinaAccounts(
        allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] }
    ): void {
        const emptyFieldList: string[] = [];

        if (allChinaAccounts.gmiAccounts.length === 0) {
            emptyFieldList.push('GMI Accounts');
        }
        if (allChinaAccounts.exchangeAccounts.length === 0) {
            emptyFieldList.push('Exchange Accounts');
        }

        if (emptyFieldList.length > 0) {
            const emptyFieldStr: string = emptyFieldList.join(' & ');
            const errMsg: string = `Fetched empty list of ${emptyFieldStr} for China markets. ${SUPPORT_MSG}`;
            NotificationBannerUtil.show(errMsg, NotificationTypes.DANGER, false);
        }
    }

    public getExternalEmailOptions(accountToEmailMappings: Object, accountIds: string[]): string[] {
        const filteredAccountToEmailsMap: Map<string, Set<string>> = new Map<string, Set<string>>();
        const emailUnion: Set<string> = new Set<string>();

        for (const accountId of accountIds) {
            const externalEmailStr = get(accountToEmailMappings, accountId, '');
            const externalEmailList: string[] = this.convertCommaSeparatedStrToList(externalEmailStr);

            externalEmailList.forEach((email: string): void => {
                if (!filteredAccountToEmailsMap.has(accountId)) {
                    filteredAccountToEmailsMap.set(accountId, new Set<string>());
                }
                filteredAccountToEmailsMap.get(accountId)!.add(email);
                emailUnion.add(email);
            });
        }

        if (!this.isValidExternalEmail(filteredAccountToEmailsMap, emailUnion)) {
            return [];
        }

        return this.getEmailOptionsFromEmailSet(emailUnion);
    }

    public getAccountTypeOptions(
        venueOptions: OptionsOrGroups<any, any>,
        micFamilyOption: any
    ): string[] {
        const venues: string[] = LimitUsageUtil.mapOptionTypesToStrings(venueOptions);
        const micFamily: string = LimitUsageUtil.mapOptionTypeToString(micFamilyOption);

        if (this.hasOnlyNonChinaSelections(venues, micFamily)) {
            return [...LimitUsageUtil.GMI_ACCOUNT_TYPE_STRING];
        } else if (this.hasOnlyChinaSelections(venues, micFamily)) {
            return [...LimitUsageUtil.ALL_ACCOUNT_TYPE_STRINGS];
        } else if (this.hasMixedSelections(venues, micFamily)) {
            NotificationBannerUtil.show(
                'Please do not enter a mixture of China and Non-China market selections.',
                NotificationTypes.WARNING,
                false
            );
        }

        return [...LimitUsageUtil.GMI_ACCOUNT_TYPE_STRING];
    }

    public statToString(limitUsageThresholdStat: any): string {
        return limitUsageThresholdStat ? JSON.stringify(limitUsageThresholdStat) : 'EMPTY';
    }

    public getOpAndValue(limitUsageThresholdStat: any): string {
        if (limitUsageThresholdStat) {
            if ('operator' in limitUsageThresholdStat && 'value' in limitUsageThresholdStat) {
                return `${limitUsageThresholdStat.operator}${limitUsageThresholdStat.value}`;
            }
            return limitUsageThresholdStat.value || '';
        }
        return '';
    }

    public getEmailOptions(isExternal: boolean, emails: string[]): JSX.Element[] {
        if (!isExternal || !emails || emails.length === 0) {
            return [];
        }

        const res: JSX.Element[] = [
            <option key="external-email-placeholder" value=""></option>
        ];

        res.push(
            ...emails.map((email: string, index: number) => (
                <option key={`${email}-${index}`} value={email}>
                    {email}
                </option>
            ))
        );

        return res;
    }

    public static mapOptionTypesToStrings(options: any): string[] {
        return options?.map((option: any) => option.value) || [];
    }

    public static mapOptionTypeToString(option: any): string {
        return option?.value || '';
    }

    public static mapStringsToOptionElements(stringArray: string[]): JSX.Element[] {
        return stringArray?.map((item: string, index: number) => (
            <option key={index} value={item}>
                {item}
            </option>
        )) || [];
    }

    public static mapStringsToOptionTypes(stringArray: string[]): OptionsOrGroups<any, any> {
        return stringArray?.map((s: string) => ({ value: s, label: s })) || [];
    }

    public static getTimezoneOptions(): JSX.Element[] {
        return LimitUsageUtil.TIMEZONES.map((item: string, index: number) => (
            <option key={`${item}-${index}`} value={item}>
                {item}
            </option>
        ));
    }

    public getTimePickerConfig(): any {
        return {
            enableTime: true,
            noCalendar: true,
            dateFormat: 'H:i',
            time_24hr: true,
            defaultDate: '00:00'
        };
    }

    public getAlertRuleTypeOptions(): JSX.Element {
        return (
            <>
                <option
                    key="alert-rule-type-margin"
                    value={LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE}
                >
                    {LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE}
                </option>
                {this.enableLimitUsageAlertTimeRule && (
                    <option
                        key="alert-rule-type-time"
                        value={LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE}
                    >
                        {LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE}
                    </option>
                )}
            </>
        );
    }
}
