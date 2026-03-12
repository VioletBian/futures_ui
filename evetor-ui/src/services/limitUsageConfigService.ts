import appConfigResponse from '../data/app-config-response.json';
import type { EnumOptionGroups } from '../types/limitUsage';

export async function fetchLimitUsageEnumOptions(): Promise<EnumOptionGroups> {
  return appConfigResponse.enumOptions as EnumOptionGroups;
}

export async function createLimitUsageRule(payload: unknown): Promise<{ status: number; payload: unknown }> {
  return {
    status: 200,
    payload,
  };
}