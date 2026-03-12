package com.xx.futures.evetor.alert.service;

import com.xx.futures.evetor.alert.dto.LimitUsageRuleRequest;
import com.xx.futures.evetor.alert.generator.filters.LimitUsageAlertFilter;

public class LimitUsageRuleService {
    private final AlertRuleFilterMapper mapper = new AlertRuleFilterMapper();

    public LimitUsageAlertFilter createRule(LimitUsageRuleRequest request) {
        if (!request.hasExclusiveVenueSelector()) {
            throw new IllegalArgumentException("Exactly one of mic or micFamily must be selected.");
        }

        return mapper.toLimitUsageFilter(request);
    }
}
