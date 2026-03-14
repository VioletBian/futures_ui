package com.xx.futures.evetor.alert.generator.sources;

import java.util.List;

public interface AlertGeneratorSource extends AlertClosingRule {

    List<UnpublishedAlert> generateNewAlerts(AlertRecord alertRecord);

    default List<UnpublishedAlert> generateNewAlerts(ClearingData.LimitUsage limitUsage) {
        return null;
    }

    default List<UnpublishedAlert> checkoutOfBandAlerts() {
        return null;
    }
}
