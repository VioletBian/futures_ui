package com.xx.futures.evetor.alert.generator.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class AlertGenerator implements AlertGeneratorSource {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    private final AlertGeneratorSource[] generatorSources;

    @Inject
    public AlertGenerator(Set<AlertGeneratorSource> generatorSources) {
        this.generatorSources = generatorSources.toArray(new AlertGeneratorSource[0]);
    }

    @Override
    public List<UnpublishedAlert> generateNewAlerts(ClearingData.LimitUsage limitUsage) {
        return concatenateLimitUsageAlerts(
            source -> source.generateNewAlerts(limitUsage),
            limitUsage
        );
    }

    @Override
    public List<UnpublishedAlert> generateNewAlerts(AlertRecord alertRecord) {
        return concatenateAlerts(
            source -> source.generateNewAlerts(alertRecord),
            alertRecord
        );
    }

    @Override
    public List<UnpublishedAlert> checkoutOfBandAlerts() {
        return concatenateAlerts(
            source -> source.checkoutOfBandAlerts(),
            null
        );
    }

    private List<UnpublishedAlert> concatenateLimitUsageAlerts(
        Function<AlertGeneratorSource, List<UnpublishedAlert>> unpublishedAlertSupplier,
        ClearingData.LimitUsage limitUsage
    ) {
        List<UnpublishedAlert> newAlerts = new ArrayList<>();
        for (AlertGeneratorSource source : generatorSources) {
            try {
                List<UnpublishedAlert> unpublishedAlerts = unpublishedAlertSupplier.apply(source);
                if (unpublishedAlerts != null) {
                    newAlerts.addAll(unpublishedAlerts);
                }
            } catch (Exception e) {
                LOG.warn("Error generating alerts for limit usage [{}]", limitUsage.getId(), e);
            }
        }
        return newAlerts.isEmpty() ? null : newAlerts;
    }

    private List<UnpublishedAlert> concatenateAlerts(
        Function<AlertGeneratorSource, List<UnpublishedAlert>> unpublishedAlertSupplier,
        AlertRecord alertRecord
    ) {
        List<UnpublishedAlert> newAlerts = null;
        for (AlertGeneratorSource source : generatorSources) {
            try {
                List<UnpublishedAlert> unpublishedAlerts = unpublishedAlertSupplier.apply(source);
                if (unpublishedAlerts != null) {
                    if (newAlerts == null) {
                        newAlerts = unpublishedAlerts;
                    } else {
                        newAlerts.addAll(unpublishedAlerts);
                    }
                }
            } catch (Exception e) {
                String debugMessage;
                if (alertRecord != null) {
                    debugMessage = String.format(
                        "for order [%s v %s] whilst generating new alerts",
                        alertRecord.getOrderId(),
                        alertRecord.getOrderVersion()
                    );
                } else {
                    debugMessage = "No alert record supplied.";
                }
                LOG.warn(
                    "Alert generator source [{}] threw an exception: {}",
                    source.getClass().getSimpleName(),
                    debugMessage,
                    e
                );
            }
        }
        return newAlerts;
    }

    @Override
    public boolean canAlertRecordCloseAlert(AlertRecord alertRecord, SerializedOrderAlert alert) {
        for (AlertGeneratorSource source : generatorSources) {
            try {
                if (source.canAlertRecordCloseAlert(alertRecord, alert)) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn(
                    "Alert generator source [{}] threw an exception for order [{} v {}]"
                        + " whilst checking for closeable alerts",
                    source.getClass().getSimpleName(),
                    alertRecord.getOrderId(),
                    alertRecord.getOrderVersion(),
                    e
                );
            }
        }
        return false;
    }
}
