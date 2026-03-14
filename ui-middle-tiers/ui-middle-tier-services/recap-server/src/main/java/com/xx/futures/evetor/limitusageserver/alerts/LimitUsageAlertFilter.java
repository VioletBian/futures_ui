package com.xx.futures.evetor.limitusageserver.alerts;

import java.util.HashSet;
import java.util.Set;

public class LimitUsageAlertFilter implements AviatorAlertFilter {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    Set<String> alertIdSet = new HashSet<>();

    @Override
    public synchronized boolean shouldBeProcessed(GeneratedMessage data) {
        if (data instanceof Coverage.Alert) {
            Coverage.Alert alert = (Coverage.Alert) data;
            if (LimitUsageAlert != alert.getType()) {
                return false;
            }

            String alertId = alert.getAlertId();
            if (isExistingId(alertId)) {
                LOG.warn("Not processing alert [{}] as it has been added to alertIdSet before.", alertId);
                return false;
            }
            alertIdSet.add(alertId);
            LOG.info("Added alert [{}] to alertIdSet.", alertId);
            return true;
        }

        return false;
    }

    public boolean isExistingId(String id) {
        return alertIdSet.contains(id);
    }
}
