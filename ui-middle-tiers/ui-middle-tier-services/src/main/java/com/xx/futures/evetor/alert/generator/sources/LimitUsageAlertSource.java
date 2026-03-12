package com.xx.futures.evetor.alert.generator.sources;

import com.xx.futures.evetor.alert.generator.filters.LimitUsageAlertFilter;
import java.util.ArrayList;
import java.util.List;

public class LimitUsageAlertSource {
    private final List<LimitUsageAlertFilter> activeFilters = new ArrayList<>();

    public void loadActiveFilters(List<LimitUsageAlertFilter> filtersFromConfigStore) {
        activeFilters.clear();
        activeFilters.addAll(filtersFromConfigStore);
    }

    public List<LimitUsageAlertFilter> getActiveFilters() {
        return new ArrayList<>(activeFilters);
    }
}
