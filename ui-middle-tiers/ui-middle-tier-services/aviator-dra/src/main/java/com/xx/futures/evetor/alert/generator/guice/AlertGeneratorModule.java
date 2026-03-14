package com.xx.futures.evetor.alert.generator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.xx.futures.evetor.alert.alertengineconsumer.AlertEngineConsumer;
import com.xx.futures.evetor.alert.alertengineconsumer.CustomAlertEngineConsumer;
import com.xx.futures.evetor.alert.alertengineconsumer.LimitUsageAlertEngineConsumer;
import com.xx.futures.evetor.alert.generator.AlertEngine;
import com.xx.futures.evetor.alert.generator.AlertPublisher;
import com.xx.futures.evetor.alert.generator.BootstrapPendingPublisher;
import com.xx.futures.evetor.alert.generator.FastAlertEngine;
import com.xx.futures.evetor.alert.generator.PendingPublisher;
import com.xx.futures.evetor.alert.generator.sources.AlertGenerator;
import com.xx.futures.evetor.alert.generator.sources.AlertGeneratorSource;
import com.xx.futures.evetor.alert.generator.sources.CustomAlertSource;
import com.xx.futures.evetor.alert.generator.sources.LimitUsageAlertSource;
import com.xx.futures.evetor.alert.generator.sources.SystemAlertSource;

public class AlertGeneratorModule extends AbstractModule {

    private final NamedProperties namedProperties;
    private final boolean customAlertDisabled;
    private final boolean systemDisabled;
    private final boolean bootstrapPublisherDisabled;
    private final boolean processLimitUsageAlert;

    public AlertGeneratorModule(NamedProperties namedProperties) {
        this.namedProperties = namedProperties;

        customAlertDisabled =
            namedProperties.getBooleanProperty(AviatorProperty.alertSourceCustomAlertDisabled);
        systemDisabled =
            namedProperties.getBooleanProperty(AviatorProperty.alertSourceSystemDisabled);
        bootstrapPublisherDisabled =
            namedProperties.getBooleanProperty(AviatorProperty.alertBootstrapPublisherDisabled);
        processLimitUsageAlert = Boolean.parseBoolean(
            namedProperties.getProperty(AviatorProperty.processLimitUsageAlert, "false")
        );
    }

    @Override
    protected void configure() {
        install(new AlertCommonModule(namedProperties));
        install(new SystemAlertRulesModule(namedProperties));

        Multibinder<AlertGeneratorSource> alertGeneratorSourceSet =
            Multibinder.newSetBinder(binder(), AlertGeneratorSource.class);

        if (!systemDisabled) {
            alertGeneratorSourceSet.addBinding().to(SystemAlertSource.class).in(Singleton.class);
        }
        if (!customAlertDisabled) {
            alertGeneratorSourceSet.addBinding().to(CustomAlertSource.class).in(Singleton.class);
        }
        if (processLimitUsageAlert) {
            alertGeneratorSourceSet
                .addBinding()
                .to(LimitUsageAlertSource.class)
                .in(Singleton.class);
        }

        bind(AlertGeneratorSource.class).to(AlertGenerator.class).in(Singleton.class);
        bind(AlertPublisher.class).in(Singleton.class);
        bind(AlertEngine.class).to(FastAlertEngine.class).in(Singleton.class);
        if (processLimitUsageAlert) {
            bind(AlertEngineConsumer.class)
                .to(LimitUsageAlertEngineConsumer.class)
                .in(Singleton.class);
        } else {
            bind(AlertEngineConsumer.class).to(CustomAlertEngineConsumer.class).in(Singleton.class);
        }

        OptionalBinder.newOptionalBinder(binder(), PendingPublisher.class);
        if (!bootstrapPublisherDisabled) {
            bind(PendingPublisher.class).to(BootstrapPendingPublisher.class);
        }
    }
}
