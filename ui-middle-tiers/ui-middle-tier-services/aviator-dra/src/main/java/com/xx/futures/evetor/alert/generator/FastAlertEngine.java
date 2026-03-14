package com.xx.futures.evetor.alert.generator;

import com.xx.futures.evetor.alert.generator.sources.AlertGeneratorSource;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public class FastAlertEngine implements AlertEngine {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    private final Object lock = new Object();
    private final Application application;
    private final Clock clock;
    private final TimePendingAlerts pendingAlerts;
    private final ActiveAlerts activeAlerts;
    private final AlertGeneratorSource alertGenerator;
    private final AlertPublisher alertPublisher;
    private PendingPublisher bootstrapPendingPublisher;
    private boolean isLive;

    @Inject
    FastAlertEngine(
        @AlertApplication Application application,
        Clock clock,
        TimePendingAlerts pendingAlerts,
        ActiveAlerts activeAlerts,
        AlertGeneratorSource alertGenerator,
        AlertPublisher alertPublisher,
        Optional<PendingPublisher> bootstrapPendingPublisher
    ) {
        this.application = application;
        this.clock = clock;
        this.pendingAlerts = pendingAlerts;
        this.activeAlerts = activeAlerts;
        this.alertGenerator = alertGenerator;
        this.alertPublisher = alertPublisher;
        this.bootstrapPendingPublisher = bootstrapPendingPublisher.orElse(null);
        this.isLive = !bootstrapPendingPublisher.isPresent();
        LOG.info("Alert engine is constructed in [live={}] mode.", isLive);
    }

    @Override
    public void checkTimePendingAlerts() {
        synchronized (lock) {
            for (TimePendingAlert pendingAlert : pendingAlerts.removeBefore(clock.millis() * 1000)) {
                raiseActiveAlert(pendingAlert);
            }
            processUnpublishedAlerts(alertGenerator.checkoutOfBandAlerts());
        }
    }

    @Override
    public void process(Alert alert) {
        AlertAction latestAlertAction = alert.getLatestAlertAction();
        synchronized (lock) {
            try {
                if (!isLive) {
                    bootstrapPendingPublisher.seen(alert);
                }

                if (LimitUsageAlert == alert.getType()) {
                    LOG.info("Not processing limit usage alert [{}].", alert.getAlertId());
                    return;
                }

                Alert.OrderAlertExtension.OrderAlertType orderAlertType =
                    alert.getOrderAlert().getAlert();
                if (!(orderAlertType == Alert.OrderAlertExtension.OrderAlertType.DanglingCfo
                    || orderAlertType == Alert.OrderAlertExtension.OrderAlertType.UnacceptedCfo
                    || orderAlertType == Alert.OrderAlertExtension.OrderAlertType.Custom
                    || orderAlertType == Alert.OrderAlertExtension.OrderAlertType.CustomAlgo
                    || orderAlertType == Alert.OrderAlertExtension.OrderAlertType.RTLSoftRestriction
                    || orderAlertType == Alert.OrderAlertExtension.OrderAlertType.RfqRegistrationAlert)) {
                    LOG.info(
                        "Not processing order alert [AlertID={}, AlertVersion={}, OrderAlertType={}] as "
                            + "it's not a known resolvable type.",
                        alert.getAlertId(),
                        alert.getAlertVersion(),
                        orderAlertType
                    );
                    return;
                }

                if (latestAlertAction == Resolved
                    || latestAlertAction == Suppressed
                    || latestAlertAction == Ignored) {
                    LOG.info(
                        "Processing terminal alert update with ID [{}] and version [{}] and state [{}]",
                        alert.getAlertId(),
                        alert.getAlertVersion(),
                        latestAlertAction
                    );
                    activeAlerts.removeTerminalAlert(alert.getAlertId());
                } else {
                    LOG.info(
                        "Processing alert update with ID [{}] and version [{}] and state [{}]",
                        alert.getAlertId(),
                        alert.getAlertVersion(),
                        latestAlertAction
                    );
                    activeAlerts.addUpdateAlert(alert);
                }
            } catch (RuntimeException e) {
                LOG.warn(
                    "Detected an unexpected exception in alerting processing. Ignoring it as to "
                        + "not fail process.",
                    e
                );
            }
        }
    }

    @Override
    public void process(AlertRecord alertRecord) {
        synchronized (lock) {
            try {
                pendingAlerts.cancel(alertRecord);
                resolveActiveAlerts(alertRecord);
                generateNewAlerts(alertRecord);
            } catch (RuntimeException e) {
                LOG.warn(
                    "Detected an unexpected exception in order processing. Ignoring it as to not "
                        + "fail process.",
                    e
                );
            }
        }
    }

    @Override
    public void process(ClearingData.LimitUsage limitUsage) {
        synchronized (lock) {
            try {
                generateNewAlerts(limitUsage);
            } catch (RuntimeException e) {
                LOG.warn(
                    "Detected an unexpected exception in limit usage processing. Ignoring it as to not fail process.",
                    e
                );
            }
        }
    }

    @Override
    public void process(List<UnpublishedAlert> unpublishedAlerts) {
        synchronized (lock) {
            try {
                processUnpublishedAlerts(unpublishedAlerts);
            } catch (RuntimeException e) {
                LOG.warn(
                    "Detected an unexpected exception in time based rule alert processing. Ignoring it as to not fail process.",
                    e
                );
            }
        }
    }

    @Override
    public void setLiveMode() {
        synchronized (lock) {
            try {
                if (!isLive) {
                    LOG.info("Alert Engine is now setting into LIVE mode.");
                    isLive = true;
                    bootstrapPendingPublisher.flush().forEach(alert -> {
                        LOG.info("Flushing alert [{}] from bootstrap queue.", alert.getAlertId());
                        alertPublisher.publishAlert(alert);
                    });
                    bootstrapPendingPublisher = null;
                }
            } catch (RuntimeException e) {
                LOG.warn(
                    "Detected an unexpected exception in marking process as live. Ignoring it as to not "
                        + "fail process.",
                    e
                );
            }
        }
    }

    private void resolveActiveAlerts(AlertRecord alertRecord) {
        List<SerializedOrderAlert> alerts = activeAlerts.forAlertRecord(alertRecord);
        if (alerts != null) {
            for (SerializedOrderAlert alert : alerts) {
                if (alertGenerator.canAlertRecordCloseAlert(alertRecord, alert)) {
                    activeAlerts.removeTerminalAlert(alert.getAlertId());
                    resolveActiveAlert(alert, alertRecord);
                }
            }
        }
    }

    private void generateNewAlerts(AlertRecord alertRecord) {
        processUnpublishedAlerts(alertGenerator.generateNewAlerts(alertRecord));
    }

    private void generateNewAlerts(ClearingData.LimitUsage limitUsage) {
        processUnpublishedAlerts(alertGenerator.generateNewAlerts(limitUsage));
    }

    private void processUnpublishedAlerts(List<UnpublishedAlert> unpublishedAlerts) {
        if (unpublishedAlerts != null) {
            for (UnpublishedAlert unpublishedAlert : unpublishedAlerts) {
                if (unpublishedAlert instanceof ImmediateAlert) {
                    raiseActiveAlert(unpublishedAlert);
                } else if (unpublishedAlert instanceof TimePendingAlert) {
                    pendingAlerts.add((TimePendingAlert) unpublishedAlert);
                } else {
                    logUnsupportedAlert(unpublishedAlert);
                }
            }
        }
    }

    private void raiseActiveAlert(UnpublishedAlert pendingAlert) {
        Alert alert = pendingAlert.getAlert();
        if (LimitUsageAlert == alert.getType()) {
            activeAlerts.addLimitUsageAlert(alert);
        } else {
            activeAlerts.addUpdateAlert(alert);
        }

        if (isLive) {
            alertPublisher.publishAlert(alert);
        } else {
            bootstrapPendingPublisher.queue(alert);
        }
    }

    private void resolveActiveAlert(SerializedOrderAlert serializedAlert, AlertRecord alertRecord) {
        Long overrideAlertVersion = null;
        if (serializedAlert.getOrderAlertType()
            == Alert.OrderAlertExtension.OrderAlertType.DanglingCfo) {
            overrideAlertVersion = alertRecord.getUpdatedTimeMillis();
        }

        Alert resolvedAlert = AlertUtils.generateOrderAlertEvent(
            serializedAlert.getAlert(),
            alertRecord.getOrder(),
            application,
            Resolved,
            Priority.Low,
            "Alert resolved by subsequent order version",
            clock
        );

        if (overrideAlertVersion != null) {
            int activityIndex = resolvedAlert.getActivityCount() - 1;
            resolvedAlert = resolvedAlert
                .toBuilder()
                .setActivity(
                    activityIndex,
                    resolvedAlert.getActivity(activityIndex)
                        .toBuilder()
                        .setAlertVersion(overrideAlertVersion)
                )
                .setAlertVersion(overrideAlertVersion)
                .build();
        }

        if (isLive) {
            alertPublisher.publishAlert(resolvedAlert);
        } else {
            bootstrapPendingPublisher.queue(resolvedAlert);
        }
    }

    private void logUnsupportedAlert(UnpublishedAlert alert) {
        // The exact log line is cut off in the source image.
        LOG.warn("Ignoring unsupported unpublished alert [{}]", alert.getClass().getSimpleName());
    }
}
