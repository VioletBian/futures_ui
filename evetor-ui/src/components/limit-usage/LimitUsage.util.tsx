import type {
  AccountType,
  AlertRuleType,
  EnumOptionGroups,
  LimitUsageFormState,
  LimitUsagePayload,
  OptionType,
} from '../../types/limitUsage';

export class LimitUsageUtil {
  public static readonly MARGIN_USAGE_ALERT_TIME_RULE: AlertRuleType = 'Margin Usage Alert Time';

  public static readonly MARGIN_USAGE_THRESHOLD_RULE: AlertRuleType = 'Margin Usage Threshold';

  public static readonly DEFAULT_ALERT_TIME = '15:00';

  public static readonly DEFAULT_TIMEZONE = 'Asia/Hong_Kong';

  public static readonly INTERNAL_EMAIL_SUFFIX = '@com.xx';

  public static readonly ALERT_RULE_TYPES: AlertRuleType[] = [
    LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE,
    LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE,
  ];

  public static readonly EMPTY_FORM: LimitUsageFormState = {
    emailSubject: '',
    selectedMicOptions: [],
    selectedMicFamilyOptions: [],
    internalEmail: '',
    externalEmail: '',
    alertRuleType: LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE,
    limitUsageThreshold: '',
    limitUsageAlertTime: LimitUsageUtil.DEFAULT_ALERT_TIME,
    limitUsageAlertTimezone: LimitUsageUtil.DEFAULT_TIMEZONE,
    accountType: 'GMI',
    accountIds: [],
    isExternal: false,
  };

  public static getMicOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'MIC');
  }

  public static getMicFamilyOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'MICFamily');
  }

  public static getAccountTypeOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'AccountType');
  }

  public static getTimezoneOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'Timezone');
  }

  public static getAccountIdOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'AccountIds');
  }

  public static getInternalEmailOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'InternalEmail');
  }

  public static getExternalEmailOptions(enumOptions: EnumOptionGroups): OptionType[] {
    return LimitUsageUtil.getOptions(enumOptions, 'ExternalEmail');
  }

  public static getAlertRuleTypeOptions(): OptionType[] {
    return LimitUsageUtil.ALERT_RULE_TYPES.map((item) => ({ value: item, label: item }));
  }

  public static mapStringsToOptionElements(options: string[]) {
    return options.map((option) => (
      <option key={option} value={option}>
        {option}
      </option>
    ));
  }

  public static mapOptionsToOptionElements(options: OptionType[]) {
    return options.map((option) => (
      <option key={option.value} value={option.value}>
        {option.label}
      </option>
    ));
  }

  public static getTimePickerConfig() {
    return {
      enableTime: true,
      noCalendar: true,
      dateFormat: 'H:i',
      time_24hr: true,
      defaultHour: 15,
      defaultMinute: 0,
    };
  }

  public static toOptionValues(options: HTMLOptionsCollection): string[] {
    return Array.from(options)
      .filter((option) => option.selected)
      .map((option) => option.value);
  }

  public static sanitizeSingleSelection(values: string[]): string[] {
    if (values.length === 0) {
      return [];
    }

    return [values[values.length - 1]];
  }

  public static buildPayload(state: LimitUsageFormState): LimitUsagePayload {
    return {
      emailSubject: state.emailSubject.trim(),
      mic: state.selectedMicOptions,
      micFamily: state.selectedMicFamilyOptions,
      internalEmail: state.internalEmail.trim(),
      externalEmail: state.externalEmail.trim(),
      alertRuleType: state.alertRuleType,
      limitUsageThreshold:
        state.alertRuleType === LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE
          ? state.limitUsageThreshold.trim()
          : undefined,
      limitUsageAlertTime:
        state.alertRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE
          ? state.limitUsageAlertTime.trim()
          : undefined,
      limitUsageAlertTimezone:
        state.alertRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE
          ? state.limitUsageAlertTimezone.trim()
          : undefined,
      accountType: state.accountType,
      accountIds: state.accountIds,
      isExternal: state.isExternal,
    };
  }

  public static validateForm(state: LimitUsageFormState): string | null {
    if (!state.emailSubject.trim()) {
      return 'Email Subject is required.';
    }

    if (state.selectedMicOptions.length > 0 && state.selectedMicFamilyOptions.length > 0) {
      return 'MIC and MICFamily cannot both contain values.';
    }

    if (state.selectedMicOptions.length === 0 && state.selectedMicFamilyOptions.length === 0) {
      return 'Select either MIC or MICFamily.';
    }

    if (!state.internalEmail.trim()) {
      return 'Internal Email is required.';
    }

    if (!state.internalEmail.trim().endsWith(LimitUsageUtil.INTERNAL_EMAIL_SUFFIX)) {
      return `Internal email address must end with ${LimitUsageUtil.INTERNAL_EMAIL_SUFFIX}.`;
    }

    if (state.alertRuleType === LimitUsageUtil.MARGIN_USAGE_THRESHOLD_RULE && !state.limitUsageThreshold.trim()) {
      return 'Margin Usage Threshold is required.';
    }

    if (state.alertRuleType === LimitUsageUtil.MARGIN_USAGE_ALERT_TIME_RULE) {
      if (!state.limitUsageAlertTime.trim()) {
        return 'Time is required.';
      }

      if (!state.limitUsageAlertTimezone.trim()) {
        return 'Timezone is required.';
      }
    }

    if (state.accountIds.length === 0) {
      return 'Account ID is required.';
    }

    if (state.isExternal && !state.externalEmail.trim()) {
      return 'External Email is required when For External is checked.';
    }

    return null;
  }

  public static createEmptyForm(accountType: AccountType = 'GMI'): LimitUsageFormState {
    return {
      ...LimitUsageUtil.EMPTY_FORM,
      accountType,
    };
  }

  public static getMockRows() {
    return [
      {
        message: 'China Oil Margin Balance - MWAM',
        mic: 'XINE, XZCE, XDCE',
        micFamily: '',
        threshold: '',
        time: '22:00',
        timezone: 'Asia/Hong_Kong',
        accountId: 'DC161522, DC107',
        externalEmail: 'Treasury@mwam.com',
        internalEmail: 'gset-futures@com.xx',
        createdBy: 'banaaa',
        createdOn: '11-Apr-25',
      },
      {
        message: 'Korea Margin Alert - MLP',
        mic: 'XKFE',
        micFamily: '',
        threshold: '>90',
        time: '',
        timezone: '',
        accountId: 'CO795186',
        externalEmail: 'clarence.yeo@xx.com',
        internalEmail: 'gset-futures@com.xx',
        createdBy: 'banaaa',
        createdOn: '12-Aug-25',
      },
      {
        message: 'China Energy Margin Alert',
        mic: '',
        micFamily: 'Energy Futures',
        threshold: '>75',
        time: '',
        timezone: '',
        accountId: '55300162, 0694',
        externalEmail: 'ops@pharo.com',
        internalEmail: 'ops@com.xx',
        createdBy: 'yufer',
        createdOn: '26-Nov-24',
      },
    ];
  }

  private static getOptions(enumOptions: EnumOptionGroups, key: string): OptionType[] {
    const values = enumOptions[key] ?? [];

    return values.map((value) => ({
      value,
      label: value,
    }));
  }
}