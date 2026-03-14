package com.xx.futures.evetor.limitusageloaderserver.server;

// This is a server that loads the limit usage data from a Kafka topic
public class LimitUsageLoaderServer extends JetstreamVertxIoServer {

    private final NamedProperties properties;

    LimitUsageLoaderServer(NamedProperties properties) {
        this.properties = properties;
    }

    @Override
    public Injector newInjector() {
        return Guice.createInjector(
            new JetstreamVertxioModule(),
            new ApplicationModule(properties),
            new ConfigLifecycleModule(properties),
            new EntitlementModule(),
            new JerseyModule()
        );
    }

    @Override
    public int getWebThreads() {
        return properties.getIntegerProperty(JetstreamVertxIoServerProperty.serverThreads);
    }

    @Override
    public int getStatusThreads() {
        return 1;
    }

    public void initializeLoader() throws Exception {
        super.initialize();
        LimitUsageLoader loader = injector.getInstance(LimitUsageLoader.class);
        loader.configureAndStartKafkaSubscription();
    }
}
