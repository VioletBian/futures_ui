package com.xx.futures.evetor.alert.generator.guice;

import com.xx.futures.evetor.alert.generator.sources.AlertGenerator;
import com.xx.futures.evetor.alert.generator.sources.AlertGeneratorSource;
import com.xx.futures.evetor.alert.generator.sources.LimitUsageAlertSource;

public class AlertGeneratorModule extends AbstractModule {

    private final NamedProperties properties;
    private final float mockProperties;
    private final boolean customAlertSourceEnabled;
    private final boolean bestThresholdPublisherEnabled;
    private final boolean processLimitUsageAlerts;

    public AlertGeneratorModule(NamedProperties namedProperties) {
        this.properties = namedProperties;
        this.mockProperties = 0.0f;
        this.customAlertSourceEnabled = properties.getBooleanProperty(
            AviatorProperty.alertSourceEnabled,
            false
        );
        this.bestThresholdPublisherEnabled = properties.getBooleanProperty(
            AviatorProperty.alertSourceBestThresholdPublisherEnabled,
            false
        );
        this.processLimitUsageAlerts = properties.getBooleanProperty(
            AviatorProperty.processLimitUsageAlerts,
            false
        );
    }

    @Override
    protected void configure() {
        bind(AlertGenerator.class).in(Singleton.class);

        Multibinder<AlertGeneratorSource> alertGeneratorSources =
            Multibinder.newSetBinder(binder(), AlertGeneratorSource.class);

        if (customAlertSourceEnabled) {
            alertGeneratorSources.addBinding().to(CustomAlertSource.class).in(Singleton.class);
        }
        if (processLimitUsageAlerts) {
            alertGeneratorSources.addBinding().to(LimitUsageAlertSource.class).in(Singleton.class);
        }

        Multibinder<AlertEngineConsumer> alertEngineConsumers =
            Multibinder.newSetBinder(binder(), AlertEngineConsumer.class);
        alertEngineConsumers.addBinding().to(AlertEngineConsumer.class).in(Singleton.class);
        alertEngineConsumers.addBinding().to(SerializedOrderEngineConsumer.class).in(Singleton.class);
        if (processLimitUsageAlerts) {
            alertEngineConsumers.addBinding().to(LimitUsageAlertEngineConsumer.class).in(Singleton.class);
        }

        bind(PendingPublisher.class).to(BootstrapPendingPublisher.class);
    }
}
