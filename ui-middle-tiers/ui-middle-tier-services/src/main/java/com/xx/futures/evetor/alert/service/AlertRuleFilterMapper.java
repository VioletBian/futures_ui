package com.xx.futures.evetor.alert.service;

import com.xx.futures.evetor.alert.dto.LimitUsageRuleRequest;
import com.xx.futures.evetor.alert.generator.filters.LimitUsageAlertFilter;

public class AlertRuleFilterMapper {
    public LimitUsageAlertFilter toLimitUsageFilter(LimitUsageRuleRequest request) {
        return new LimitUsageAlertFilter(request.getMic(), request.getMicFamily());
    }
}
