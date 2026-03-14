package com.xx.futures.evetor.alert.alertengineconsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.xx.futures.evetor.alert.generator.AlertEngine;
import com.xx.octane.datamodel.business.futures.generated.ClearingData;

/**
 * Combines a filter and a processer into a single class. Does so because the consumer accepts quite different data
 * types and so it's easier to mix filtering with processing and not have to maintain cast checking logic in multiple
 * places.
 */
@Singleton
public class LimitUsageAlertEngineConsumer implements AlertEngineConsumer {

    private final AlertEngine alertEngine;

    @Inject
    public LimitUsageAlertEngineConsumer(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }

    @Override
    public void consume(Object dataToConsume) throws DataConsumptionException {
        if (dataToConsume instanceof ClearingData.LimitUsage) {
            ClearingData.LimitUsage limitUsage = (ClearingData.LimitUsage) dataToConsume;
            consumeLimitUsage(limitUsage);
        }
    }

    private void consumeLimitUsage(ClearingData.LimitUsage limitUsage) {
        alertEngine.process(limitUsage);
    }

    @Override
    public void setLiveMode() {
        alertEngine.setLiveMode();
    }
}
