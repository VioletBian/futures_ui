export type AccountType = 'GMI' | 'EXCHANGE_ACCOUNT' | 'SMX_ACCOUNT';

export type AlertRuleType = 'Margin Usage Alert Time' | 'Margin Usage Threshold';

export type OptionType = {
  value: string;
  label: string;
};

export type EnumOptionGroups = Record<string, string[]>;

export type LimitUsageFormState = {
  emailSubject: string;
  selectedMicOptions: string[];
  selectedMicFamilyOptions: string[];
  internalEmail: string;
  externalEmail: string;
  alertRuleType: AlertRuleType;
  limitUsageThreshold: string;
  limitUsageAlertTime: string;
  limitUsageAlertTimezone: string;
  accountType: AccountType;
  accountIds: string[];
  isExternal: boolean;
};

export type LimitUsagePayload = {
  emailSubject: string;
  mic: string[];
  micFamily: string[];
  internalEmail: string;
  externalEmail: string;
  alertRuleType: AlertRuleType;
  limitUsageThreshold?: string;
  limitUsageAlertTime?: string;
  limitUsageAlertTimezone?: string;
  accountType: AccountType;
  accountIds: string[];
  isExternal: boolean;
};