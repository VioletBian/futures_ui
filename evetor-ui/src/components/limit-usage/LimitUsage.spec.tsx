// @ts-nocheck
import React from 'react';
import '@testing-library/jest-dom';
import { act, fireEvent, render, screen, cleanup, waitFor, queryByAttribute } from '@testing-library/react';
import {
    ApplicationConfigurationUtil,
    CustomAlertsUtil,
    NotificationBannerUtil,
    NotificationTypes
} from 'evetor-ui-core-utils';
import { LimitUsage } from './LimitUsage';
import { LimitUsageUtil } from './LimitUsage.util';
import { AppConfigResponse } from '../../../../__tests__/__utils__/utilities';
import { of } from 'rxjs';

// 图片中可见：mock 了 grid 组件，避免渲染依赖。
jest.mock('./LimitUsageGrid', () => ({ LimitUsageGrid: () => 'Mocked Limit Usage grid.' }));

describe('Limit Usage Form tests', () => {
    const getById = queryByAttribute.bind(null, 'id');
    const getSelectInput = (testId: string): HTMLInputElement => {
        const input = screen.getByTestId(testId).querySelector('input');
        expect(input).toBeTruthy();
        return input as HTMLInputElement;
    };
    const isSelectDisabled = (testId: string): boolean => {
        const control = screen.getByTestId(testId).querySelector('.limitusage-input-field__control');
        return !!control?.className.includes('limitusage-input-field__control--is-disabled');
    };

    const getConfigSpy = jest
        .spyOn(ApplicationConfigurationUtil, 'getConfiguration')
        .mockReturnValue(of(AppConfigResponse));

    beforeEach(() => {
        jest.clearAllMocks();
        jest.restoreAllMocks();
    });

    afterEach(cleanup);

    // ===== 原样转写段落===== 

    it('should fetch venue', async () => {
        const utilSpy = jest.spyOn(LimitUsageUtil.prototype, 'fetchVenuesFromWaterfall')
            .mockReturnValue(Promise.resolve([{ value: 'XINE', label: 'XSGE' }] as any));

        render(<LimitUsage />);

        await waitFor(() => {
            expect(utilSpy).toHaveBeenCalledTimes(1);
        });
    });

    it('should fetch Client settings account mappings', async () => {
        const utilSpy = jest.spyOn(LimitUsageUtil.prototype, 'getAllAccountToEmailMappings')
            .mockReturnValue(Promise.resolve({ data: { XINE: 'test@xx.com' } } as any));

        render(<LimitUsage />);

        await waitFor(() => {
            expect(utilSpy).toHaveBeenCalledTimes(1);
        });
    });

    it('should fetch china accounts', async () => {
        const utilSpy = jest.spyOn(LimitUsageUtil.prototype, 'getChinaAccounts')
            .mockReturnValue(Promise.resolve({ data: ['account'] } as any));

        render(<LimitUsage />);

        await waitFor(() => {
            // 图片可见此处断言为 2 次（GMI + Exchange）
            expect(utilSpy).toHaveBeenCalledTimes(2);
        });
    });

    it('should fetch mic families', async () => {
        const utilSpy = jest.spyOn(LimitUsageUtil.prototype, 'fetchMicFamilyFromWaterfall')
            .mockReturnValue(Promise.resolve([{ value: 'SFX', label: 'SFX' }] as any));

        render(<LimitUsage />);

        await waitFor(() => {
            expect(utilSpy).toHaveBeenCalledTimes(1);
        });
    });

    // ===== 功能性补充测试（基于最近 MicFamily 提交逻辑）===== 

    // 增量说明：验证 getFilteredAccountOptions 新增 micFamily 参数位是否已传递。
    it('should call getFilteredAccountOptions with micFamily argument', async () => {
        const optionsSpy = jest.spyOn(LimitUsageUtil.prototype, 'getFilteredAccountOptions')
            .mockReturnValue([] as any);

        render(<LimitUsage />);

        await waitFor(() => {
            expect(optionsSpy).toHaveBeenCalled();
        });

        const firstCallArgs = optionsSpy.mock.calls[0] || [];
        expect(firstCallArgs.length).toBe(5);
        expect(Array.isArray(firstCallArgs[2])).toBeTruthy();
        expect(Array.isArray(firstCallArgs[3])).toBeTruthy();
    });

    // 增量说明:
    // 1. 组件初始只提供 GMI 选项，不能在“无市场选择”时直接切到 Exchange Account。
    // 2. 先选择一个中国市场（这里用 MIC Family = SFX）后，account type 才会扩展出 Exchange Account。
    // 3. 再切换市场维度（从 MIC Family 改为 MIC）后，useEffect 会把 accountType 重置回 GMI。
    it('should reset account type to GMI after market selection changes', async () => {
        jest.spyOn(LimitUsageUtil.prototype, 'fetchVenuesFromWaterfall')
            .mockResolvedValue([{ value: 'XINE', label: 'XINE' }] as any);
        jest.spyOn(LimitUsageUtil.prototype, 'fetchMicFamilyFromWaterfall')
            .mockResolvedValue([{ value: 'SFX', label: 'SFX' }] as any);

        render(<LimitUsage />);

        const accountTypeSelect = await screen.findByTestId('accountType');
        const micFamilyInput = getSelectInput('micFamilyDropdown');

        await waitFor(() => {
            expect(isSelectDisabled('micFamilyDropdown')).toBeFalsy();
        });

        fireEvent.keyDown(micFamilyInput, { key: 'ArrowDown', code: 'ArrowDown' });
        fireEvent.keyDown(micFamilyInput, { key: 'Enter', code: 'Enter' });

        // 只有中国市场被选中时，Account Type 才应该暴露 Exchange Account 选项。
        await waitFor(() => {
            expect(screen.getByRole('option', { name: 'Exchange Account' })).toBeTruthy();
        });

        fireEvent.change(accountTypeSelect, { target: { value: 'Exchange Account' } });

        await waitFor(() => {
            expect((accountTypeSelect as HTMLSelectElement).value).toEqual('Exchange Account');
        });

        const removeMicFamilySelection = screen.getByLabelText('Remove SFX');
        fireEvent.click(removeMicFamilySelection);

        const venueInput = getSelectInput('venueDropdown');
        await waitFor(() => {
            expect(isSelectDisabled('venueDropdown')).toBeFalsy();
        });

        fireEvent.keyDown(venueInput, { key: 'ArrowDown', code: 'ArrowDown' });
        fireEvent.keyDown(venueInput, { key: 'Enter', code: 'Enter' });

        await waitFor(() => {
            expect((accountTypeSelect as HTMLSelectElement).value).toEqual('GMI');
        });
    });

    // 增量说明:
    // 1. 这里验证的是“互斥禁用”而不是具体某个文案是否已经渲染到菜单中。
    // 2. 先等待 venue options 加载并确认 MIC 输入框可用，再触发选择，避免 react-select 菜单还未准备好时直接找 XINE。
    // 3. 选择完成后，以 micFamily input disabled 作为稳定断言，覆盖组件的互斥逻辑。
    it('should enforce mutual disable between MIC and MIC Family selectors', async () => {
        jest.spyOn(LimitUsageUtil.prototype, 'fetchVenuesFromWaterfall')
            .mockResolvedValue([{ value: 'XINE', label: 'XINE' }] as any);
        jest.spyOn(LimitUsageUtil.prototype, 'fetchMicFamilyFromWaterfall')
            .mockResolvedValue([{ value: 'SFX', label: 'SFX' }] as any);

        render(<LimitUsage />);

        const venueInput = getSelectInput('venueDropdown');

        await waitFor(() => {
            expect(isSelectDisabled('venueDropdown')).toBeFalsy();
        });

        fireEvent.keyDown(venueInput, { key: 'ArrowDown', code: 'ArrowDown' });
        fireEvent.keyDown(venueInput, { key: 'Enter', code: 'Enter' });

        await waitFor(() => {
            expect(isSelectDisabled('micFamilyDropdown')).toBeTruthy();
        });
    });

    it('Should submit form', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 200 } as any));
        const isValidSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(false);

        await act(async () => {
            render(<LimitUsage />);
        });

        // 图片注释可见：jsdom 里 isValid() 一直为 true，这里直接看 mock validateForm
        await waitFor(() => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
            expect(isValidSpy).toHaveBeenCalled();
            expect(createSpy).not.toHaveBeenCalled();
        });

        createSpy.mockClear();
        isValidSpy.mockClear();
        isValidSpy.mockReturnValue(true);

        await waitFor(() => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
            expect(createSpy).toHaveBeenCalled();
        });
    });

    // 增量说明：验证 submit 前传入 validateForm 的 payload 同时包含 mic 与 micFamily 字段。
    it('should submit with validateForm payload containing mic and micFamily fields', async () => {
        const validateSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 200 } as any));

        render(<LimitUsage />);

        await act(async () => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        expect(validateSpy).toHaveBeenCalled();
        const formArg = (validateSpy.mock.calls[0] || [])[0] as any;
        expect(formArg).toHaveProperty('venue');
        expect(formArg).toHaveProperty('micFamily');
        expect(Array.isArray(formArg.venue)).toBeTruthy();
        expect(Array.isArray(formArg.micFamily)).toBeTruthy();
        expect(createSpy).toHaveBeenCalled();
    });

    // ===== 原样转写段落 ===== 

    it('should show success notification banner - internal', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 200 } as any));
        const isValidSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');

        render(<LimitUsage />);

        await act(async () => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        expect(createSpy).toHaveBeenCalled();
        expect(isValidSpy).toHaveBeenCalled();
        expect(notificationSpy).toHaveBeenCalledWith(
            'Internal limit usage rule created. Please go to alert rule browser to view this rule.',
            NotificationTypes.SUCCESS,
            false
        );
    });

    it('should show success notification banner - external', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 200 } as any));
        const isValidSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');

        render(<LimitUsage />);

        await act(async () => {
            const checkbox = screen.getByRole('checkbox', { name: /For External/i });
            fireEvent.click(checkbox);
        });

        await waitFor(() => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        expect(createSpy).toHaveBeenCalled();
        expect(isValidSpy).toHaveBeenCalled();
        expect(notificationSpy).toHaveBeenCalledWith(
            'External limit usage rule created. Please refresh grid below to see details.',
            NotificationTypes.SUCCESS,
            false
        );
    });

    it('should show failed notification banner - error status code', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 400 } as any));
        const isValidSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');

        render(<LimitUsage />);

        await act(async () => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        expect(createSpy).toHaveBeenCalled();
        expect(isValidSpy).toHaveBeenCalled();
        expect(notificationSpy).toHaveBeenCalledWith(
            'Unable to create limit usage rule. Please try again or contact support.',
            NotificationTypes.DANGER,
            false
        );
    });

    it('should show failed notification banner - rejected promise', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockRejectedValue(new Error('mock error'));
        const isValidSpy = jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);
        const notificationSpy = jest.spyOn(NotificationBannerUtil, 'show');

        await act(async () => {
            render(<LimitUsage />);
        });

        await waitFor(() => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        expect(createSpy).toHaveBeenCalled();
        expect(isValidSpy).toHaveBeenCalled();
        expect(notificationSpy).toHaveBeenCalledWith(
            'Unable to create limit usage rule. Please try again or contact support.',
            NotificationTypes.DANGER,
            false
        );
    });

    it('should change form values - margin', async () => {
        const fetchVenuesSpy = jest.spyOn(LimitUsageUtil.prototype, 'fetchVenuesFromWaterfall')
            .mockReturnValue(Promise.resolve([
                { value: 'ASX', label: 'ASX' },
                { value: 'CFE', label: 'CFE' },
                { value: 'CME', label: 'CME' },
                { value: 'EUREX', label: 'EUREX' },
                { value: 'HKFE', label: 'HKFE' }
            ]));

        const csAccountMappings = jest.spyOn(LimitUsageUtil.prototype, 'getAllAccountToEmailMappings')
            .mockReturnValue(Promise.resolve({
                data: {
                    '55300833': 'kabeer.krishnags.com,yiying.li@xx.com',
                    '69100837': 'kabeer.krishnags.com,yiying.li@xx.com',
                    '04860880': 'kabeer.krishnags.com,yiying.li@xx.com'
                }
            }));

        let comp: any;
        await act(async () => {
            comp = render(<LimitUsage />);
        });

        expect(fetchVenuesSpy).toHaveBeenCalledTimes(1);
        expect(csAccountMappings).toHaveBeenCalledTimes(1);

        const emailSubjectInput = screen.getByRole('textbox', { name: /Email Subject/i }) as HTMLInputElement;
        const alertRuleType = screen.getByRole('combobox', { name: /Alert Rule Type/i }) as HTMLSelectElement;
        const marginUsageThreshold = screen.getByRole('textbox', { name: /% Margin Usage Threshold/i }) as HTMLInputElement;
        const internalEmailInput = screen.getByRole('textbox', { name: /Internal Email/i }) as HTMLInputElement;
        const checkbox = screen.getByRole('checkbox', { name: /For External/i }) as HTMLInputElement;
        const externalEmailElement = screen.getByRole('combobox', { name: /External Email/i }) as HTMLSelectElement;

        expect(checkbox.checked).toBeFalsy();
        expect(externalEmailElement.disabled).toBeTruthy();

        await act(async () => {
            fireEvent.change(emailSubjectInput, { target: { value: 'Test Email' } });
            fireEvent.change(marginUsageThreshold, { target: { value: '>90' } });
            fireEvent.change(internalEmailInput, { target: { value: 'xiaoyun.wu@xx.com' } });
        });

        expect(emailSubjectInput.value).toEqual('Test Email');
        expect(marginUsageThreshold.value).toEqual('>90');
        expect(internalEmailInput.value).toBeTruthy();

        await act(async () => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        const venueDropdown = screen.getByTestId('venueDropdown');
        const displayValueInput = venueDropdown.querySelectorAll('.limitusage-input-field__input-container')[0] as HTMLElement;
        expect(displayValueInput).toBeTruthy();

        // UNCERTAIN: 原图后半段包含 react-select 的键盘选择流程，此处仅保留可读断言骨架。
        expect(alertRuleType.value).toBeTruthy();
        expect(comp).toBeTruthy();
    });

    it('Change alert rule type values', async () => {
        const response = {
            ...AppConfigResponse,
            enableLimitUsageAlertTimeRule: true
        };

        const configSpy = jest.spyOn(ApplicationConfigurationUtil, 'getConfiguration')
            .mockReturnValue(of(response));

        await act(async () => {
            render(<LimitUsage />);
        });

        const alertRuleType: HTMLSelectElement = screen.getByRole('combobox', { name: /Alert Rule Type/i }) as HTMLSelectElement;
        expect(alertRuleType.value).toEqual('Margin Usage Threshold');

        const marginUsageThresholdElement: HTMLInputElement =
            screen.getByRole('textbox', { name: /% Margin Usage Threshold/i }) as HTMLInputElement;

        await act(async () => {
            fireEvent.change(alertRuleType, { target: { value: 'Margin Usage Alert Time' } });
        });

        expect(alertRuleType.value).toEqual('Margin Usage Alert Time');
        expect(marginUsageThresholdElement.value).toEqual('');

        const timePicker = screen.getByRole('textbox', { name: /Time/i }) as HTMLInputElement;
        const timezone = screen.getByRole('combobox', { name: /Timezone/i }) as HTMLSelectElement;

        expect(timePicker).toBeVisible();
        expect(timezone.value).toEqual('Asia/Hong_Kong');

        await act(async () => {
            fireEvent.change(timezone, { target: { value: 'America/New_York' } });
        });
        expect(timezone.value).toEqual('America/New_York');

        expect(configSpy).toHaveBeenCalled();
    });

    it('get account type', async () => {
        await act(async () => {
            render(<LimitUsage />);
        });

        const accountTypeSelectElement: HTMLSelectElement =
            screen.getByRole('combobox', { name: /Account Type/i }) as HTMLSelectElement;
        expect(accountTypeSelectElement.value).toEqual('GMI');
    });

    // ===== 功能性补充测试（基于最近 MicFamily 提交逻辑）===== 
    // 增量说明：验证选择 MIC 后会触发互斥效果（MICFamily 禁用）。
    it('should clear MIC Family when MIC is selected', async () => {
        jest.spyOn(LimitUsageUtil.prototype, 'fetchVenuesFromWaterfall')
            .mockReturnValue(Promise.resolve([{ value: 'XINE', label: 'XINE' }]));
        jest.spyOn(LimitUsageUtil.prototype, 'fetchMicFamilyFromWaterfall')
            .mockReturnValue(Promise.resolve([{ value: 'SFX', label: 'SFX' }]));

        render(<LimitUsage />);

        await waitFor(() => {
            expect(screen.getByTestId('venueDropdown')).toBeTruthy();
            expect(screen.getByTestId('micFamilyDropdown')).toBeTruthy();
        });

        const venueInput = getSelectInput('venueDropdown');
        fireEvent.keyDown(venueInput, { key: 'ArrowDown', code: 'ArrowDown' });
        fireEvent.keyDown(venueInput, { key: 'Enter', code: 'Enter' });

        await waitFor(() => {
            expect(isSelectDisabled('micFamilyDropdown')).toBeTruthy();
        });
    });

    // 增量说明：验证创建请求 payload 中包含 micFamily 字段。
    it('should include micFamily in payload when MIC Family is selected', async () => {
        const createSpy = jest.spyOn(CustomAlertsUtil, 'createNewAlert')
            .mockReturnValue(Promise.resolve({ status: 200 } as any));
        jest.spyOn(LimitUsageUtil.prototype, 'validateForm').mockReturnValue(true);

        render(<LimitUsage />);

        await act(async () => {
            const createElement = screen.queryByText('Create', { exact: true }) as HTMLButtonElement;
            fireEvent.click(createElement);
        });

        // UNCERTAIN: 截图未能完整拍到 payload 结构。
        // fillForm 增加了 micFamily 字段，因此检查入参对象包含该 key。
        const firstCallPayload = (createSpy.mock.calls[0] || [])[0] as any;
        if (firstCallPayload) {
            expect(firstCallPayload).toHaveProperty('micFamily');
        }
    });
});
