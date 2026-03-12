package com.xx.futures.evetor.alert.generator.filters;

import java.util.Map;

public interface AlertRuleFilter {
    boolean matches(Map<String, Object> marketData);
}
