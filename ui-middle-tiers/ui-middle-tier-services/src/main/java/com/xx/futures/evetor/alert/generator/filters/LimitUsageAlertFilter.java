package com.xx.futures.evetor.alert.generator.filters;

import java.util.List;
import java.util.Map;

public class LimitUsageAlertFilter implements AlertRuleFilter {
    private final List<String> mics;
    private final List<String> micFamilies;

    public LimitUsageAlertFilter(List<String> mics, List<String> micFamilies) {
        this.mics = mics;
        this.micFamilies = micFamilies;
    }

    @Override
    public boolean matches(Map<String, Object> marketData) {
        String eventMic = String.valueOf(marketData.getOrDefault("mic", ""));
        String eventMicFamily = String.valueOf(marketData.getOrDefault("micFamily", ""));

        if (!mics.isEmpty() && !micFamilies.isEmpty()) {
            return false;
        }

        if (!mics.isEmpty()) {
            return mics.contains(eventMic);
        }

        if (!micFamilies.isEmpty()) {
            return micFamilies.contains(eventMicFamily);
        }

        return false;
    }
}
