/* UNCERTAIN IMPORTS:
import axios from 'axios';
import get from 'lodash/get';
import React from 'react';
import { NotificationBannerUtil, NotificationTypes } from 'aviator-ui-core-utils';
import { ApplicationConfigurationUtil } from '...';
import { IAppConfig } from '...';
import { OptionsOrGroups } from 'react-select';
import { ExternalLimitUsageAlertRuleFormTemplate } from '../model/ExternalLimitUsageAlertRuleFormTemplate';
import { SUPPORT_MSG } from './LimitUsage';
以上 import 区域在截图里被折叠，无法严格确认，只能标注。
*/

export enum AccountType {
    GMI_ACCOUNT = 'GMI',
    EXCHANGE_ACCOUNT = 'Exchange Account'
}

export class LimitUsageUtil {
    public static MARGIN_USAGE_ALERT_TIME_RULE : string = 'Margin Usage Alert Time';
    public static MARGIN_USAGE_THRESHOLD_RULE : string = 'Margin Usage Threshold';
    public static CHINA_MIC_LIST : string[] = ['XINE', 'XZCE', 'XDCE', 'GFEX', 'XSGE', 'CCFX'];
    public static GMI_ACCOUNT : string = 'gmi';
    public static EXCHANGE_ACCOUNT : string = 'exchangeaccount';
    public static ALL_ACCOUNT_TYPE_STRINGS : string[] = [AccountType.GMI_ACCOUNT.toString(), AccountType.EXCHANGE_ACCOUNT.toString()];
    public static GMI_ACCOUNT_TYPE_STRING : string[] = [AccountType.GMI_ACCOUNT.toString()];
    public static TIMEZONES : string[] = [
        "America/New_York",
        "America/Chicago",
        "Asia/Hong_Kong",
        "Asia/Tokyo",
        "Asia/Singapore",
        "Australia/Melbourne",
        "Australia/Sydney",
        "Europe/London"
    ];
    public static DEFAULT_ALERT_TIME : string = "15:00";
    public static DEFAULT_TIMEZONE : string = "Asia/Hong_Kong";

    public limitUsageServerUrl: string;
    public emailAddress: string;
    public waterfallUri: string;
    public chinaMarginApiUrl: string;
    public enableLimitUsageAlertTimeRule: boolean;

    constructor() {
        ApplicationConfigurationUtil.getConfiguration().subscribe((options: {config: IAppConfig}): void => {
            this.limitUsageServerUrl = get(config, ['recapsServerUrl', '']); // use recapsServerUrl to load account to limit usage email mapping from Client Settings
            this.emailAddress = get(config, 'userEmail', '');
            this.waterfallUri = get(config, 'waterfallUri', '');
            this.chinaMarginApiUrl = get(config, 'chinaMarginApiUrl', '');
            this.enableLimitUsageAlertTimeRule = get(config, 'enableLimitUsageAlertTimeRule', false);

            /* UNCERTAIN:
               截图里 subscribe 参数写成 (options: {config: IAppConfig})，但函数体里使用的是 config，
               很可能真实代码是 ({ config }: { config: IAppConfig }) 或者 (options) 后用 options.config。
               这里按截图主体保留，但这段大概率需要你本地修一下。 */
        });
    }

    public getSimpleExchangeNames(): any[] {
        return [
            {value: 'ASX', label: 'ASX'},
            {value: 'ATDE', label: 'ATDE'},
            {value: 'BDPF', label: 'BDPF'},
            {value: 'BIST', label: 'BISTF'},
            {value: 'BMF', label: 'BMF'},
            {value: 'CBOF', label: 'CBOF'},
            {value: 'CCFX', label: 'CCFX'},
            {value: 'CFE', label: 'CFE'},
            {value: 'CME', label: 'CME'},
            {value: 'COMEX', label: 'COMEX'},
            {value: 'EEX', label: 'EEX'},
            {value: 'ELX', label: 'ELX'},
            {value: 'EUR AMS', label: 'EUR AMS'},
            {value: 'EUR BRU', label: 'EUR BRU'},
            {value: 'EUR LIS', label: 'EUR LIS'},
            {value: 'EUR PAR', label: 'EUR PAR'},
            {value: 'EUREX', label: 'EUREX'},
            {value: 'HKFE', label: 'HKFE'},
            {value: 'ICE EU', label: 'ICE EU'},
            {value: 'ICE US', label: 'ICE US'},
            {value: 'IDEM', label: 'IDEM'},
            {value: 'INDE', label: 'INDE'},
            {value: 'INDF', label: 'INDF'},
            {value: 'KCBT', label: 'KCBT'},
            {value: 'KFE', label: 'KFE'},
            {value: 'KLOF', label: 'KLOF'},
            {value: 'LME', label: 'LME'},
            {value: 'LSED', label: 'LSED'},
            {value: 'MEFF', label: 'MEFF'},
            {value: 'MFE', label: 'MFE'},
            {value: 'MGE', label: 'MGE'},
            {value: 'MXDR', label: 'MXDR'},
            {value: 'NOMX', label: 'NOMX'},
            {value: 'NYMEX', label: 'NYMEX'},
            {value: 'NZFE', label: 'NZFE'},
            {value: 'OSE', label: 'OSE'},
            {value: 'OTOB', label: 'OTOB'},
            {value: 'RTSX', label: 'RTSX'},
            {value: 'SAFEX', label: 'SAFEX'},
            {value: 'SFE', label: 'SFE'},
            {value: 'SGX', label: 'SGX'},
            {value: 'TFEX', label: 'TFEX'},
            {value: 'TIFF', label: 'TIFF'},
            {value: 'TKYO', label: 'TKYO'},
            {value: 'TLVE', label: 'TLVE'},
            {value: 'TWFX', label: 'TWFX'},
            {value: 'UNKNOWN', label: 'UNKNOWN'},
            {value: 'WPG', label: 'WPG'},
            {value: 'WSE', label: 'WSE'},
            {value: 'XSIM', label: 'XSIM'}
        ];
    }

    public initialiseEmptyForm(): ExternalLimitUsageAlertRuleFormTemplate {
        return {
            message: '',
            venue: [],
            limitUsageAlertThreshold: null,
            limitUsageAlertTime: '',
            limitUsageAlertTimezone: '',
            accountId: [],
            clientEmailAddress: '',
            emailAddress: '',
            clientLimitUsageEmail: true,
            hasHistoricalClientLimitUsageEmail: false,
            genericEmail: false
        };
    }

    public hasOnlyChinaMics(venues: string[]): boolean {
        if (venues.length === 0) {
            return false;
        }

        for (const venue: string of venues) {
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

        for (const venue: string of venues) {
            if (LimitUsageUtil.CHINA_MIC_LIST.includes(venue)) {
                return false;
            }
        }
        return true;
    }

    public hasMixedMics(venue: string[]): boolean {
        return venue.length > 0 && !this.hasOnlyChinaMics(venue) && !this.hasOnlyNonChinaMics(venue);
    }

    public getChinaAccounts(accountType: string): Promise<any> {
        return axios.get(this.chinaMarginApiUrl + `/rest/margincontrol/cn_offshore/accountList/${accountType}`, {
            withCredentials: true,
            responseType: 'json' as 'json'
        });
    }

    public getAllAccountToEmailMappings(): Promise<any> {
        return axios.get(this.limitUsageServerUrl + '/rest/clientsettings/cache/limitusage/accounts/all', {
            withCredentials: true,
            responseType: 'json' as 'json'
        });
    }

    public extractChinaAccountsFromClientSettings(chinaAccountList: string[], clientSettingsAccounts: string[]): string[] {
        return this.extractAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts, true);
    }

    public extractNonChinaAccountsFromClientSettings(chinaAccountList: string[], clientSettingsAccounts: string[]): string[] {
        return this.extractAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts, false);
    }

    public extractAccountsFromClientSettings(chinaAccountList: string[], clientSettingsAccounts: string[], isForChinaMic: boolean): string[] {
        const filteredList: any[] = [];
        for (const account: string of clientSettingsAccounts) {
            const shouldIncludeAccount: boolean = isForChinaMic ? chinaAccountList.includes(account) : !chinaAccountList.includes(account);
            if (shouldIncludeAccount) {
                filteredList.push(account);
            }
        }
        return filteredList;
    }

    public convertCommaSeparatedStrToList = (str: string): string[] => {
        /* UNCERTAIN: 截图里 IDE 参数提示覆盖了 replace / split 调用，能确认语义，不能确认源码是否真带命名参数提示文本。
           这里保留为可读的截图式还原。 */
        const stripped : string = str.replace(searchValue: /\s+/g, replaceValue: '');
        return stripped ? stripped.split(separator: ',') : [];
    };

    public areEqualSets = (emailAddressSet: Set<string>, combinedEmailAddressSet: Set<string>) => {
        return emailAddressSet.size == combinedEmailAddressSet.size &&
            [...emailAddressSet].every((value: string) => combinedEmailAddressSet.has(value));
    };

    public convertAccountToEmailsMapToString = (map: Map<string, Set<string>>): string => {
        let res: string = '';
        for (const [account: string, emails: Set<string>] of map) {
            const msg : string = `[${account}: ${Array.from(emails).join(', ')}]; `;
            res = res.concat(msg);
        }
        return res;
    };

    public isValidExternalEmail = (accountToEmailsMap: Map<string, Set<string>>, emailUnion: Set<string>) : boolean => {
        if (emailUnion.size == 0) {
            return false;
        }

        /* UNCERTAIN: 截图这里 for-of 被 IDE 类型提示遮挡，
           看起来接近：for (const [, : string , emails: Set<string>] of accountToEmailsMap) { ... }
           这是不可编译的截图态。下面保留为接近截图的形态注释，并给出可运行写法。 */
        // for (const [, : string , emails: Set<string>] of accountToEmailsMap) {
        for (const [, emails] of accountToEmailsMap) {
            if (!this.areEqualSets(emails, emailUnion)) {
                const errMsg : string =
                    'This limit rule is not allowed as current account ID(s) has different external email addresses: ' +
                    this.convertAccountToEmailsMapToString(accountToEmailsMap);
                NotificationBannerUtil.show(errMsg, NotificationTypes.WARNING, false);
                return false;
            }
        }
        return true;
    };

    public validateForm(form: any, isExternal: boolean, isTimeBased: boolean): boolean {
        const missingFields : string[] = [];
        const emailSubject = get(form, 'message', '');
        const venue = get(form, 'venue', []);
        const limitUsageThreshold = get(form, 'limitUsageAlertThreshold', null);
        const accountIds = get(form, 'accountId', []);
        const internalEmail = get(form, 'emailAddress', '');
        const externalEmail = get(form, 'clientEmailAddress', '');
        const alertTime = get(form, 'limitUsageAlertTime', '');
        const alertTimezone = get(form, 'limitUsageAlertTimezone', '');

        if (this.hasMixedMics(venue)) {
            NotificationBannerUtil.show(
                'Please do not enter a mixture of China and Non-China MICs.',
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

        if (venue.length === 0) {
            missingFields.push('MIC');
        }

        if (isTimeBased) {
            if (!alertTime) {
                missingFields.push('Margin Usage Alert Time');
            }
            if (!alertTimezone) {
                missingFields.push('Timezone');
            }
        } else if (limitUsageThreshold.value == null) {
            // disallow null or undefined, == null checks for both undefined and null in JS
            // allow 0 value
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

    public fetchVenuesFromWaterfall(): Promise<any> | any {
        if (!this.waterfallUri) {
            /* UNCERTAIN: 截图为 Promise<any>(executor: (_, err) => err(reason: '...'))
               这是 IDE 参数提示态，不是合法源码。 */
            return new Promise<any>((_, err) => err('No Waterfall uri found in config.'));
        }
        return axios.get(this.waterfallUri + '/rest/search/enums', {
            withCredentials: true,
            responseType: 'json' as 'json'
        }).then((res) => {
            return [
                ...res.data.mic.map((item: any): {label: any, value: any} => ({value: item, label: item})),
                ...this.getSimpleExchangeNames()
            ].sort((a, b) => a.value.localeCompare(b.value));
        });
    }

    public fetchMicFamilyFromWaterfall(): Promise<any> | any {
        if (!this.waterfallUri) {
            return new Promise<any>((_, err) => err('No Waterfall uri found in config.'));
        }
        return axios.get(this.waterfallUri + '/rest/search/enums', {
            withCredentials: true,
            responseType: 'json' as 'json'
        }).then((res) => {
            return [
                ...res.data.micFamily.map((item: any) => ({value: item, label: item})),
                ...this.getSimpleExchangeNames() // what is it
            ].sort((a, b) => a.value.localeCompare(b.value));
        });

        /* UNCERTAIN:
           截图里这里确实有注释 `// what is it`
           所以保留。 */
    }

    public getEmailOptionsFromEmailSet(emailSet: Set<string>): any {
        /* UNCERTAIN: 截图这一行看起来像：
           return [Array.from(emailSet).join(',')];
           也可能不是最终真实实现，因为和下游 getEmailOptions 的用法不太一致。
           这里先按截图语义最接近保留。 */
        return [Array.from(emailSet).join(',')];
    }

    public getFilteredAccountsList(
        accountToEmailMappings: Object,
        allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] },
        venues: string[],
        accountType: string
    ): string[] {
        const clientSettingsAccountList : string[] = Object.keys(accountToEmailMappings);
        const combinedChinaAccountList: string[] = [...allChinaAccounts.gmiAccounts, ...allChinaAccounts.exchangeAccounts];
        let filteredAccountList: string[] = [];

        if (this.hasOnlyNonChinaMics(venues)) {
            filteredAccountList = this.extractNonChinaAccountsFromClientSettings(combinedChinaAccountList, clientSettingsAccountList);
        } else if (this.hasOnlyChinaMics(venues)) {
            if (accountType == AccountType.GMI_ACCOUNT) {
                filteredAccountList = this.extractChinaAccountsFromClientSettings(allChinaAccounts.gmiAccounts, clientSettingsAccountList);
            } else if (accountType == AccountType.EXCHANGE_ACCOUNT) {
                filteredAccountList = this.extractChinaAccountsFromClientSettings(allChinaAccounts.exchangeAccounts, clientSettingsAccountList);
            }
        }

        return filteredAccountList;
    }

    public getFilteredAccountOptions(
        accountToEmailMappings: Object,
        allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] },
        venueOptions: OptionsOrGroups<any, any>,
        accountType: string
    ): OptionsOrGroups<any, any> {
        const venues: string[] = LimitUsageUtil.mapOptionTypesToStrings(venueOptions);
        const accountList : string[] = this.getFilteredAccountsList(accountToEmailMappings, allChinaAccounts, venues, accountType);
        const accountOptionList : any[] = [];
        for (const filteredAccount : string of accountList) {
            accountOptionList.push({value: filteredAccount, label: filteredAccount});
        }
        return accountOptionList;
    }

    public showNotificationBannerForEmptyChinaAccounts(allChinaAccounts: { gmiAccounts: string[], exchangeAccounts: string[] }): void {
        const emptyFieldList : any[] = [];
        if (allChinaAccounts.gmiAccounts.length == 0) {
            emptyFieldList.push("GMI Accounts");
        }
        if (allChinaAccounts.exchangeAccounts.length == 0) {
            emptyFieldList.push("Exchange Accounts");
        }
        if (emptyFieldList.length > 0) {
            const emptyFieldStr : string = emptyFieldList.join('&');
            const errMsg : string = `Fetched empty list of ${emptyFieldStr} for China markets. ${SUPPORT_MSG}`;
            NotificationBannerUtil.show(errMsg, NotificationTypes.DANGER, false);
        }
    }

    public getExternalEmailOptions(accountToEmailMappings: Object, accountIds: string[]): string[] {
        const filteredAccountToEmailsMap: Map<string, Set<string>> = new Map<string, Set<string>>();
        const emailUnion: Set<string> = new Set();

        for (const accountId: string of accountIds) {
            const externalEmailStr = get(accountToEmailMappings, accountId, '');
            const externalEmailList : string[] = this.convertCommaSeparatedStrToList(externalEmailStr);
            externalEmailList.forEach((email : string ) : void => {
                if (!filteredAccountToEmailsMap.has(accountId)) {
                    filteredAccountToEmailsMap.set(accountId, new Set<string>());
                }
                filteredAccountToEmailsMap.get(accountId)!.add(email);
                emailUnion.add(email); // combined set of all external emails for accountIds that have been looped through
            });
        }

        if (!this.isValidExternalEmail(filteredAccountToEmailsMap, emailUnion)) {
            return [];
        } else {
            return this.getEmailOptionsFromEmailSet(emailUnion);
        }
    }

    public getAccountTypeOptions(venueOptions: OptionsOrGroups<any, any>): string[] {
        const venues : string[] = LimitUsageUtil.mapOptionTypesToStrings(venueOptions);
        if (this.hasOnlyNonChinaMics(venues)) {
            return LimitUsageUtil.GMI_ACCOUNT_TYPE_STRING;
        } else if (this.hasOnlyChinaMics(venues)) {
            return LimitUsageUtil.ALL_ACCOUNT_TYPE_STRINGS;
        } else if (this.hasMixedMics(venues)) {
            // Mixing China and non-China mics in one rule is not allowed, as China MICs have specific account rendering logic
            // Checking mixMics here to ensure we don't render Notification Banner when Mic is not yet selected
            NotificationBannerUtil.show(
                'Please do not enter a mixture of China and Non-China MICs.',
                NotificationTypes.WARNING,
                false
            );
        }
        return LimitUsageUtil.GMI_ACCOUNT_TYPE_STRING;
    }

    public statToString(limitUsageThresholdStat: any) : string {
        return limitUsageThresholdStat ? JSON.stringify(limitUsageThresholdStat) : 'EMPTY';
    }

    public getOpAndValue(limitUsageThresholdStat: any): any | string {
        if (limitUsageThresholdStat) {
            if ('operator' in limitUsageThresholdStat && 'value' in limitUsageThresholdStat) {
                return limitUsageThresholdStat.operator + limitUsageThresholdStat.value;
            } else {
                return limitUsageThresholdStat.value || '';
            }
        }
        return '';
    }

    public getEmailOptions(isExternal: boolean, emails: string[]) {
        if (!isExternal || !emails || emails.length === 0) {
            return [];
        }
        const res : JSX.Element[] = [<option key="external-email-placeholder" value=""></option>];
        res.push(...emails.map((email : string, index : number) => <option key={email + index} value={email}>{email}</option>));
        return res;

        /* UNCERTAIN: 截图里的函数返回类型被截断，只能确认返回 JSX option 列表。 */
    }

    public static mapOptionTypesToStrings(options: OptionsOrGroups<any, any>): string[] {
        return options?.map((option) => option.value) || [];
    }

    public static mapStringsToOptionElements(stringArray: string[]): JSX.Element[] {
        return stringArray?.map((item : string, index : number) => <option key={index} value={item}>{item}</option>) || [];
    }

    public static mapStringsToOptionTypes(stringArray: string[]): OptionsOrGroups<any, any> {
        return stringArray?.map((s: string) => ({ value: s, label: s })) || [];
    }

    public static getTimezoneOptions() {
        return LimitUsageUtil.TIMEZONES.map((item : string, index : number) => <option key={item + index} value={item}>{item}</option>);
    }

    public getTimePickerConfig() {
        return {
            enableTime: true,
            noCalendar: true,
            dateFormat: 'H:i',
            time_24hr: true,
            defaultDate: '00:00'
        };

        /* UNCERTAIN: 截图里返回类型显示成 getTimePickerConfig(): {...}
           被 IDE 折叠，无法知道精确类型注解。 */
    }

    public getAlertRuleTypeOptions() {
        return (
            <>
                <option key="alert-rule-type-margin" value={LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE}>
                    {LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE}
                </option>
                { this.enableLimitUsageAlertTimeRule && (
                    <option key="alert-rule-type-time" value={LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE}>
                        {LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE}
                    </option>
                ) }
            </>
        );
    }
}