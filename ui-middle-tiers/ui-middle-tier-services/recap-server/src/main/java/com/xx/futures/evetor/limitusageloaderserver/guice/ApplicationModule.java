package com.xx.futures.evetor.limitusageloaderserver.guice;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.util.Properties;
import java.util.Set;

public class ApplicationModule extends AbstractModule {

    private final NamedProperties properties;

    public ApplicationModule(NamedProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(this.binder(), properties);
        bind(NamedProperties.class).toInstance(properties);
        bind(Properties.class).toInstance(properties);
        bind(ConfigLifecycle.class).to(DefaultConfigLifecycle.class);
        bind(PermitEngine.class).toProvider(CachedPermitProvider.class).in(Singleton.class);
        bind(FileLocator.class).to(ConfigUtilsFileLocator.class);

        String envStr = properties.getProperty("environment", "DEV").toUpperCase();
        Messaging.MessagingExchange.EnvironmentNamespace environment =
            Messaging.MessagingExchange.EnvironmentNamespace.valueOf(envStr);
        bind(Messaging.MessagingExchange.EnvironmentNamespace.class).toInstance(environment);

        Multibinder<Object> initSetBinder = Multibinder.newSetBinder(
            binder(),
            Object.class,
            Names.named(DefaultConfigLifecycle.INIT_SET)
        );
        initSetBinder.addBinding().to(VisibilityProvider.class);

        bind(LimitUsageLoader.class).in(Singleton.class);
        bind(new TypeLiteral<Set<String>>() {})
            .annotatedWith(Names.named(JetstreamRouteHandlers.EXPOSED_HEADERS))
            .toInstance(Sets.newHashSet("X-JSI-GSSSO-TOKEN"));
    }
}
