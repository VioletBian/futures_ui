package com.xx.futures.evetor.limitusageserver.server;

public class LimitUsageServer extends JetstreamVertxIoServer {

    private final NamedProperties properties;

    public LimitUsageServer(NamedProperties properties) {
        this.properties = properties;
    }

    @Override
    public Injector getInjector() {
        return Guice.createInjector(
            new JetstreamVertxIoModule(),
            new ApplicationModule(properties),
            new ConfigLifecycleModule(properties),
            new EntitlementModule(),
            new JerseyModule()
        );
    }

    @Override
    public int getNumThreads() {
        return properties.getIntegerProperty(JetstreamVertxIoServerProperty.serverThreads);
    }

    @Override
    public int getStatisticsThreads() {
        return 1;
    }
}
