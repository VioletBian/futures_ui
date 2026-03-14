package com.xx.futures.evetor.alert.generator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AlertEngineTicker {

    private static final OctaneLogger LOG = LogUtility.getLogger();
    private final AlertEngine alertEngine;

    @Inject
    private AlertEngineTicker(AlertEngine alertEngine) {
        LOG.info("Instantiating Alert Engine Ticker");
        this.alertEngine = alertEngine;
    }

    @LifeCycleStart
    public void startTicking() {
        LOG.info("Starting the alert engine ticket executor to tick the time every 1 second");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                alertEngine.checkTimePendingAlerts();
            } catch (Exception e) {
                LOG.warn(
                    "Alert engine timer - uncaught exception handler"
                        + " - swallowing exception to prevent thread death",
                    e
                );
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
