package com.xx.futures.evetor.alert.generator;

import java.util.List;

public interface AlertEngine {

    void checkTimePendingAlerts();

    void process(Alert alert);

    void process(AlertRecord alertRecord);

    void process(ClearingData.LimitUsage limitUsage);

    void process(List<UnpublishedAlert> unpublishedAlerts);

    void setLiveMode();
}
