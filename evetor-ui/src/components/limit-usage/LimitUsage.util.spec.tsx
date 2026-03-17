// @ts-nocheck
import { cleanup, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import {
    NotificationTypes,
    NotificationBannerUtil,
    ApplicationConfigurationUtil
} from 'evetor-ui-core-utils';
import { AppConfigResponse } from '../../../../__tests__/__utils__/utilities';
import { of } from 'rxjs';
import React from 'react';
import { AccountType, LimitUsageUtil } from './LimitUsage.util';

describe('Limit usage util tests', () => {
    const configSpy = jest.spyOn(ApplicationConfigurationUtil, 'getConfiguration').mockReturnValue(of(AppConfigResponse));
    const limitUsageUtil: LimitUsageUtil = new LimitUsageUtil();

    const emailMap: Object = {
        account1: 'email1, email2',
        account2: 'email1, email2'
    };
    const account1: Set<string> = new Set(['email1', 'email2']);
    const account2: Set<string> = new Set(['email1']);
    const emailMapAsSet: Map<string, Set<string>> = new Map(Object.entries({ account1, account2 }));

    beforeEach(() => {
        jest.clearAllMocks();
        jest.restoreAllMocks();
    });

    afterEach(cleanup);

    // ===== 原样转写段落 =====

    it('should validate init state of form', () => {
        const limitUsageForm: any = limitUsageUtil.initialiseEmptyForm();
        expect(limitUsageForm.message).toEqual('');
        expect(limitUsageForm.venue).toEqual([]);
        expect(limitUsageForm.micFamily).toEqual('');
        expect(limitUsageForm.limitUsageAlertThreshold).toEqual(null);
        expect(limitUsageForm.limitUsageAlertTime).toEqual('');
        expect(limitUsageForm.limitUsageAlertTimezone).toEqual('');
        expect(limitUsageForm.accountId).toEqual([]);
        expect(limitUsageForm.clientEmailAddress).toEqual('');
        expect(limitUsageForm.emailAddress).toEqual('');
        expect(limitUsageForm.clientLimitUsageEmail).toBeTruthy();
        expect(limitUsageForm.hasHistoricalClientLimitUsageEmail).toBeFalsy();
    });

    it('test hasOnlyChinaMics', () => {
        let venue: any = [];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeFalsy();

        venue = ['XINE'];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeTruthy();

        venue = ['XINE', 'XSGE'];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeTruthy();

        venue = ['XINE', 'XNSE'];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeFalsy();

        // 回补图片原样断言：非中国 MIC
        venue = ['XNSE'];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeFalsy();

        // 回补图片原样断言：纯非中国 MIC 组合
        venue = ['XNSE', 'XKFE'];
        expect(limitUsageUtil.hasOnlyChinaMics(venue)).toBeFalsy();
    });

    it('test hasOnlyNonChinaMics', () => {
        let venue: any = [];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeFalsy();

        venue = ['XINE'];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeFalsy();

        venue = ['XNSE', 'XSGE'];

        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeFalsy();

        venue = ['XNSE', 'XKFE'];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeTruthy();

        venue = ['XNSE'];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeTruthy();

        venue = ['XNSE', 'XKFE'];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeTruthy();

        // 回补图片原样断言：混合中国与非中国 MIC 不是 only non-china
        venue = ['XINE', 'XNSE'];
        expect(limitUsageUtil.hasOnlyNonChinaMics(venue)).toBeFalsy();
    });

    it('should reject when waterfall uri is missing - venues', async () => {
        limitUsageUtil.waterfallUri = '';
        await expect(limitUsageUtil.fetchVenuesFromWaterfall()).rejects.toEqual('No Waterfall uri found in config.');
    });

    it('should reject when waterfall uri is missing - mic families', async () => {
        limitUsageUtil.waterfallUri = '';
        await expect(limitUsageUtil.fetchMicFamilyFromWaterfall()).rejects.toEqual('No Waterfall uri found in config.');
        expect(limitUsageUtil.hasOnlyChinaMicFamilies('SFX')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaMicFamilies('SFX')).toBeFalsy();
        expect(limitUsageUtil.hasOnlyNonChinaMicFamilies('NON_CHINA')).toBeTruthy();
    });

    it('test hasOnlyChinaSelections/hasOnlyNonChinaSelections/hasMixedSelections', () => {
        expect(limitUsageUtil.hasOnlyChinaSelections(['XINE'], '')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyChinaSelections([], 'SFX')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaSelections(['XNSE'], '')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaSelections([], 'NON_CHINA')).toBeTruthy();
        expect(limitUsageUtil.hasMixedSelections(['XINE'], 'SFX')).toBeTruthy();
    });

    // ===== 原样转写段落 ===== 

    it('test extractChinaAccountsFromClientSettings', () => {
        const chinaAccountList: string[] = ['IN03456', 'DCI03456'];
        const clientSettingsAccounts: string[] = ['IN03456', 'DCI03456', 'C0796453'];
        expect(limitUsageUtil.extractChinaAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts))
            .toEqual(['IN03456', 'DCI03456']);
    });

    it('test extractNonChinaAccountsFromClientSettings', () => {
        const chinaAccountList: string[] = ['IN03456', 'DCI03456'];
        const clientSettingsAccounts: string[] = ['IN03456', 'DCI03456', 'C0796453'];
        expect(limitUsageUtil.extractNonChinaAccountsFromClientSettings(chinaAccountList, clientSettingsAccounts))
            .toEqual(['C0796453']);
    });

    const goodForm: any = {
        message: 'Test',
        venue: ['XINE'],
        limitUsageAlertThreshold: { operator: '>', value: 88 },
        accountId: ['666TEST'],
        clientEmailAddress: 'test@external.xx.com',
        emailAddress: 'test@xx.com',
        clientLimitUsageEmail: true,
        hasHistoricalClientLimitUsageEmail: false
    };

    it('should validate form - return true', async () => {
        expect(limitUsageUtil.validateForm(goodForm, true, false)).toBeTruthy();
    });

    it('should validate form - mixture of China and non China MICs', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const formWithMixedVenue = {
            ...goodForm,
            venue: ['XINE', 'XNSE']
        };

        limitUsageUtil.validateForm(formWithMixedVenue, true, false);
        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'Please do not enter a mixture of China and Non-China MICs.',
                NotificationTypes.WARNING,
                false
            );
        });

        expect(limitUsageUtil.validateForm(formWithMixedVenue, true, false)).toBeFalsy();
    });

    // 回补图片原样测试：阈值格式错误
    it('should validate form - invalid limitUsageAlertThreshold', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const formWithInvalidThreshold = {
            ...goodForm,
            limitUsageAlertThreshold: {}
        };

        limitUsageUtil.validateForm(formWithInvalidThreshold, true, false);
        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'Incorrect input format for % Margin Usage Threshold, please key in operator followed by value e.g. >80',
                NotificationTypes.WARNING,
                false
            );
        });
    });

    // 增量回补：覆盖图片中的阈值边界校验意图（1 <= value <= 100）
    it('should validate form - limitUsageAlertThreshold range boundary', () => {
        const formAtLowerBound = {
            ...goodForm,
            limitUsageAlertThreshold: { operator: '>', value: 1 }
        };
        const formAtUpperBound = {
            ...goodForm,
            limitUsageAlertThreshold: { operator: '>', value: 100 }
        };

        expect(limitUsageUtil.validateForm(formAtLowerBound, true, false)).toBeTruthy();
        expect(limitUsageUtil.validateForm(formAtUpperBound, true, false)).toBeTruthy();
    });

    it('should validate form - time based - invalid alert time', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const formWithInvalidThreshold = {
            ...goodForm,
            limitUsageAlertTime: null
        };

        limitUsageUtil.validateForm(formWithInvalidThreshold, true, true);
        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'Following fields need to be populated: Margin Usage Alert Time',
                NotificationTypes.WARNING,
                false
            );
        });

        expect(limitUsageUtil.validateForm(formWithInvalidThreshold, true, true)).toBeFalsy();
    });
    // 回补图片原样测试：time-based 缺少 timezone
    it('should validate form - time based - invalid alert timezone', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const formWithInvalidThreshold = {
            ...goodForm,
            limitUsageAlertTimezone: null
        };

        limitUsageUtil.validateForm(formWithInvalidThreshold, true, true);
        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'Following fields need to be populated: Timezone',
                NotificationTypes.WARNING,
                false
            );
        });

        expect(limitUsageUtil.validateForm(formWithInvalidThreshold, true, true)).toBeFalsy();
    });

    // 回补图片原样测试：non-time-based 下 alert time 可为空
    it('should validate form - non-time based - invalid alert time', () => {
        const formWithInvalidThreshold = {
            ...goodForm,
            limitUsageAlertTime: null
        };
        expect(limitUsageUtil.validateForm(formWithInvalidThreshold, true, false)).toBeTruthy();
    });

    // 回补图片原样测试：non-time-based 下 timezone 可为空
    it('should validate form - non-time based - invalid alert timezone', () => {
        const formWithInvalidThreshold = {
            ...goodForm,
            limitUsageAlertTimezone: null
        };
        expect(limitUsageUtil.validateForm(formWithInvalidThreshold, true, false)).toBeTruthy();
    });
    

    // 回补图片原样测试：空表单缺失字段提示    
    it('should validate form - non-time based and empty fields', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const formWithEmptyFields = {
            ...limitUsageUtil.initialiseEmptyForm(),
            limitUsageAlertThreshold: {}
        };

        limitUsageUtil.validateForm(formWithEmptyFields, true, false);
        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'Following fields need to be populated: Email Subject, MIC or MIC Family, % Margin Usage Threshold, Account ID(s), Internal Email Address ending with @xx.com, External Email Address',
                NotificationTypes.WARNING,
                false
            );
        });
    });

    // ===== 功能性补充测试（基于最近 MicFamily 提交逻辑）===== 

    // 增量说明：覆盖新增的 MICFamily selector 相关工具函数分支

    it('test micFamily china/non-china/mixed selectors', () => {
        expect(limitUsageUtil.hasOnlyChinaMicFamilies('')).toBeFalsy();
        expect(limitUsageUtil.hasOnlyChinaMicFamilies('SFX')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaMicFamilies('SFX')).toBeFalsy();
        expect(limitUsageUtil.hasOnlyNonChinaMicFamilies('NON_CHINA')).toBeTruthy();
    });

    it('test hasOnlyChinaSelections/hasOnlyNonChinaSelections/hasMixedSelections', () => {
        expect(limitUsageUtil.hasOnlyChinaSelections(['XINE'], '')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyChinaSelections([], 'SFX')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaSelections(['XNSE'], '')).toBeTruthy();
        expect(limitUsageUtil.hasOnlyNonChinaSelections([], 'NON_CHINA')).toBeTruthy();
        expect(limitUsageUtil.hasMixedSelections(['XINE'], 'SFX')).toBeTruthy();
    });

    it('should validate form - both MIC and MIC Family selected (mutual exclusion)', () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const form = {
            ...goodForm,
            venue: ['XINE'],
            micFamily: 'SFX'
        };

        expect(limitUsageUtil.validateForm(form, true, false)).toBeFalsy();
        expect(notificationSpy).toHaveBeenCalledWith(
            'MIC and MIC Family are mutually exclusive. Please select only one.',
            NotificationTypes.WARNING,
            false
        );
    });

    it('should validate form - legacy multi-value micFamily payload is invalid', () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const form = {
            ...goodForm,
            venue: [],
            micFamily: ['SFX', 'NON_CHINA_FAMILY']
        };

        expect(limitUsageUtil.validateForm(form, true, false)).toBeFalsy();
        expect(notificationSpy).toHaveBeenCalledWith(
            'MIC Family only supports a single selection.',
            NotificationTypes.WARNING,
            false
        );
    });

    it('should validate form - invalid micFamily selected', () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const form = {
            ...goodForm,
            venue: [],
            micFamily: ''
        };

        expect(limitUsageUtil.validateForm(form, true, false)).toBeFalsy();
        expect(notificationSpy).toHaveBeenCalledWith(
            'Invalid MIC Family selected.',
            NotificationTypes.WARNING,
            false
        );
    });


    it('should get china exchange accounts', async () => {
        jest.mock('axios', () => ({ get: jest.fn() }));
        const getSpy = jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: [] }));

        // 增量说明：修复假通过，使用 await 确保断言在异步完成后执行
        await limitUsageUtil.getChinaAccounts(LimitUsageUtil.EXCHANGE_ACCOUNT);
        expect(getSpy).toHaveBeenCalledWith(
            'http://margin.mock.api.cn.qa.futures.nimbus.xx.com/rest/margincontrol/cn_offshore/accountList/exchangeaccount',
            { withCredentials: true, responseType: 'json' as 'json' }
        );
    });

    it('should get client setting email to account mappings', async () => {
        jest.mock('axios', () => ({ get: jest.fn() }));
        const getSpy = jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: [] }));

        // 增量说明：修复假通过，使用 await 确保断言有效
        await limitUsageUtil.getAllAccountToEmailMappings();
        expect(getSpy).toHaveBeenCalledWith(
            'www.recap-service.com/rest/clientsettings/cache/limitusage/accounts/all',
            { withCredentials: true, responseType: 'json' as 'json' }
        );
    });

    it('should get MIC enums from waterfall', async () => {
        jest.mock('axios', () => ({ get: jest.fn() }));
        const getSpy = jest.spyOn(axios, 'get').mockResolvedValueOnce({ data: { mic: ['XINE'] } });

        // 增量说明：修复假通过，改为 await 获取返回结果
        const res = await limitUsageUtil.fetchVenuesFromWaterfall();
        expect(getSpy).toHaveBeenCalledWith('www.waterfallservice.xx.com/rest/search/enums', {
            withCredentials: true,
            responseType: 'json' as 'json'
        });
        expect(res).toEqual(expect.arrayContaining([{ value: 'XINE', label: 'XINE' }]));
    });

    it('should get MIC Family enums from waterfall', async () => {
        jest.mock('axios', () => ({ get: jest.fn() }));
        const getSpy = jest.spyOn(axios, 'get').mockResolvedValueOnce({ data: { micFamily: ['SFX', 'NON_CHINA'] } });

        // 增量说明：修复假通过，改为 await 获取返回结果
        const res = await limitUsageUtil.fetchMicFamilyFromWaterfall();
        expect(getSpy).toHaveBeenCalledWith('www.waterfallservice.xx.com/rest/search/enums', {
            withCredentials: true,
            responseType: 'json' as 'json'
        });
        expect(res).toEqual([
            { value: 'NON_CHINA', label: 'NON_CHINA' },
            { value: 'SFX', label: 'SFX' }
        ]);
    });

     it('should reject when waterfall uri is missing - venues', async () => {
        // 增量说明：覆盖配置缺失时的 reject 分支
        limitUsageUtil.waterfallUri = '';
        await expect(limitUsageUtil.fetchVenuesFromWaterfall()).rejects.toEqual('No Waterfall uri found in config.');
    });

    it('should reject when waterfall uri is missing - mic families', async () => {
        // 增量说明：覆盖 MICFamily 枚举接口在配置缺失时的 reject 分支
        limitUsageUtil.waterfallUri = '';
        await expect(limitUsageUtil.fetchMicFamilyFromWaterfall()).rejects.toEqual('No Waterfall uri found in config.');
    });

    // ===== 原样转写段落 ===== 

    it('should convert comma separated str to list', async () => {
        let accountList: string[] = limitUsageUtil.convertCommaSeparatedStrToList('account1,account2');
        expect(accountList).toEqual(['account1', 'account2']);

        accountList = limitUsageUtil.convertCommaSeparatedStrToList('account1 , account2');
        expect(accountList).toEqual(['account1', 'account2']);
    });

    it('should compare email address sets', async () => {
        const emailSet1: Set<string> = new Set(['email1', 'email2', 'email3']);
        const emailSet2: Set<string> = new Set(['email1', 'email3', 'email2']);
        const emailSet3: Set<string> = new Set(['email1', 'email2']);

        expect(limitUsageUtil.areEqualSets(emailSet1, emailSet2)).toBeTruthy();
        expect(limitUsageUtil.areEqualSets(emailSet1, emailSet3)).toBeFalsy();
    });

    it('should convert map to string', async () => {
        const emailStr: string = limitUsageUtil.convertAccountToEmailsMapToString(emailMapAsSet);
        expect(emailStr).toEqual('[account1: email1, email2]; [account2: email1]; ');
    });

    it('should validate external email map', async () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const emailUnion: Set<string> = new Set(['email1']);
        expect(limitUsageUtil.isValidExternalEmail(emailMapAsSet, emailUnion)).toBeFalsy();

        await waitFor(() => {
            expect(notificationSpy).toHaveBeenCalledWith(
                'This limit rule is not allowed as current account ID(s) has different external email addresses: [account1: email1, email2]; [account2: email1]; ',
                NotificationTypes.WARNING,
                false
            );
        });
    });

    // 回补图片原样测试：账户筛选列表场景
    describe('getAccountOptionsFromAccountList', () => {
        const accountToEmailMappings: any = {
            '55300833': 'kabeer.krishnags.com,yiying.li@xx.com',
            '69100837': 'kabeer.krishnags.com,yiying.li@xx.com',
            '04860880': 'kabeer.krishnags.com,yiying.li@xx.com',
            'IN03456': 'yiying.li@xx.com',
            'DCI03456': 'kabeer.krishnags.com,yiying.li@xx.com',
            'ZCI03456': 'yiying.li@xx.com',
            'C0796453': 'yiying.li@xx.com'
        };

        const allChinaAccounts = {
            gmiAccounts: ['ZCI90219', 'IN03456', 'DCI03456', 'ZCI03456'],
            exchangeAccounts: ['55300870', '55300833', '69100837', '04860880']
        };

        it('China only MICs - GMI Account', async () => {
            const selectedVenues = [{ value: 'XINE', label: 'XINE' }, { value: 'XSGE', label: 'XSGE' }];
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                selectedVenues,
                [],
                AccountType.GMI_ACCOUNT
            );
            const expected = [
                { value: 'IN03456', label: 'IN03456' },
                { value: 'DCI03456', label: 'DCI03456' },
                { value: 'ZCI03456', label: 'ZCI03456' }
            ];
            expect(actual).toEqual(expected);
        });

        it('China only MICs - Exchange Account', async () => {
            const selectedVenues = [{ value: 'XINE', label: 'XINE' }, { value: 'XSGE', label: 'XSGE' }];
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                selectedVenues,
                [],
                AccountType.EXCHANGE_ACCOUNT
            );
            const expected = [
                { value: '55300833', label: '55300833' },
                { value: '69100837', label: '69100837' },
                { value: '04860880', label: '04860880' }
            ];
            expect(actual).toEqual(expected);
        });

        it('Non-China MICs', async () => {
            const selectedVenues = [{ value: 'XNSE', label: 'XNSE' }, { value: 'XKFE', label: 'XKFE' }];
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                selectedVenues,
                [],
                AccountType.GMI_ACCOUNT
            );
            const expected = [{ value: 'C0796453', label: 'C0796453' }];
            expect(actual).toEqual(expected);
        });

        it('Mixing China and non-China MICs', async () => {
            const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
            const selectedVenues = [{ value: 'XNSE', label: 'XNSE' }, { value: 'XINE', label: 'XINE' }];
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                selectedVenues,
                [],
                AccountType.GMI_ACCOUNT
            );
            expect(actual).toEqual([]);
            expect(notificationSpy).toHaveBeenCalled();
        });
    });

        describe('showNotificationBannerForEmptyChinaAccounts', () => {
        it('empty GMI and exchange accounts', () => {
            const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
            const chinaAccounts = { gmiAccounts: [], exchangeAccounts: [] };
            limitUsageUtil.showNotificationBannerForEmptyChinaAccounts(chinaAccounts);
            expect(notificationSpy).toHaveBeenCalledWith(
                'Fetched empty list of GMI Accounts & Exchange Accounts for China markets. Please reach out to MC for support.',
                NotificationTypes.DANGER,
                false
            );
        });

        it('non-empty GMI and exchange accounts', () => {
            const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
            const chinaAccounts = { gmiAccounts: ['123'], exchangeAccounts: ['456'] };
            limitUsageUtil.showNotificationBannerForEmptyChinaAccounts(chinaAccounts);
            expect(notificationSpy).not.toHaveBeenCalled();

        // 增量回补：无市场选择时不应返回账号（与图片原样行为保持一致）
        it('should get all account options when no market selections', () => {
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                [],
                [],
                AccountType.GMI_ACCOUNT
            );

            expect(actual).toEqual([]);
        });

        // 增量回补：有市场选择时应返回对应过滤后的账号
        it('should get account options by market selections', () => {
            const actual = limitUsageUtil.getFilteredAccountOptions(
                accountToEmailMappings,
                allChinaAccounts,
                [{ value: 'XINE', label: 'XINE' }],
                [],
                AccountType.GMI_ACCOUNT
            );

            expect(actual).toEqual([
                { value: 'IN03456', label: 'IN03456' },
                { value: 'DCI03456', label: 'DCI03456' },
                { value: 'ZCI03456', label: 'ZCI03456' }
            ]);
        });
    });

    // ===== 功能性补充测试（基于最近 MicFamily 提交逻辑）===== 
    it('should validate form - both MIC and MIC Family selected (mutual exclusion)', () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const form = {
            ...goodForm,
            venue: ['XINE'],
            micFamily: 'SFX'
        };

        expect(limitUsageUtil.validateForm(form, true, false)).toBeFalsy();
        expect(notificationSpy).toHaveBeenCalledWith(
            'MIC and MIC Family are mutually exclusive. Please select only one.',
            NotificationTypes.WARNING,
            false
        );
    });

    it('should validate form - legacy multi-value micFamily payload is invalid', () => {
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');
        const form = {
            ...goodForm,
            venue: [],
            micFamily: ['SFX', 'NON_CHINA_FAMILY']
        };

        expect(limitUsageUtil.validateForm(form, true, false)).toBeFalsy();
        expect(notificationSpy).toHaveBeenCalledWith(
            'MIC Family only supports a single selection.',
            NotificationTypes.WARNING,
            false
        );
    });

    it('should get account type options by micFamily selections', () => {
        const options = limitUsageUtil.getAccountTypeOptions([], { value: 'SFX', label: 'SFX' } as any);
        expect(options).toEqual([AccountType.GMI_ACCOUNT.toString(), AccountType.EXCHANGE_ACCOUNT.toString()]);
    });

    it('should filter account options by micFamily selections', () => {
        const accountToEmailMappings: any = {
            china1: 'a@xx.com',
            china2: 'a@xx.com',
            nonchina1: 'a@xx.com'
        };
        const allChinaAccounts = {
            gmiAccounts: ['china1', 'china2'],
            exchangeAccounts: []
        };

        const actual = limitUsageUtil.getFilteredAccountOptions(
            accountToEmailMappings,
            allChinaAccounts,
            [],
            { value: 'SFX', label: 'SFX' } as any,
            AccountType.GMI_ACCOUNT
        );

        expect(actual).toEqual([
            { value: 'china1', label: 'china1' },
            { value: 'china2', label: 'china2' }
        ]);
    });
});
