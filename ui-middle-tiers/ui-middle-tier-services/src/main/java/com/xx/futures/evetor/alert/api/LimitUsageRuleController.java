package com.xx.futures.evetor.alert.api;

import com.xx.futures.evetor.alert.dto.LimitUsageRuleRequest;
import com.xx.futures.evetor.alert.generator.filters.LimitUsageAlertFilter;
import com.xx.futures.evetor.alert.service.LimitUsageRuleService;

public class LimitUsageRuleController {
    private final LimitUsageRuleService ruleService = new LimitUsageRuleService();

    public LimitUsageAlertFilter createLimitUsageRule(LimitUsageRuleRequest request) {
        return ruleService.createRule(request);
    }
}
