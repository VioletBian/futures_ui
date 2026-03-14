package com.xx.futures.evetor.limitusageserver.alerts;

import java.util.Collections;
import java.util.List;

public class LimitUsageAlertAckingConsumer implements AviatorAlertAckingConsumer {

    private final LimitUsageAlertConsumer consumer;

    @Inject
    public LimitUsageAlertAckingConsumer(LimitUsageAlertConsumer limitUsageAlertConsumer) {
        this.consumer = limitUsageAlertConsumer;
    }

    @Override
    public List<Long> consume(Coverage.Alert dataToConsume, long deliveryTag)
        throws DataConsumptionException {
        consumer.consume(dataToConsume);
        return Collections.singletonList(deliveryTag);
    }
}
