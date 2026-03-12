package com.xx.futures.evetor.alert.engine;

import com.xx.futures.evetor.alert.generator.filters.LimitUsageAlertFilter;
import com.xx.futures.evetor.alert.generator.sources.LimitUsageAlertSource;
import java.util.List;

public class AlertEngineBootstrap {
    private final LimitUsageAlertSource limitUsageAlertSource = new LimitUsageAlertSource();

    public void start(List<LimitUsageAlertFilter> filtersFromConfigStore) {
        limitUsageAlertSource.loadActiveFilters(filtersFromConfigStore);
    }

    public LimitUsageAlertSource getLimitUsageAlertSource() {
        return limitUsageAlertSource;
    }
}
