package com.xx.futures.evetor.limitusageloaderserver.server;

public class Main {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    public static void main(String[] args) throws Throwable {
        LOG.info("Starting limit usage loader server");

        try {
            LOG.info("Class path: {}", System.getProperty("java.class.path"));
            LOG.info("JAVA_OPTS: {}", getJvmArguments());

            NamedProperties properties = ConfigUtils.getProperties("limit-usage-loader", args);
            checkProperties(properties);

            LimitUsageLoaderServer limitUsageLoaderServer = new LimitUsageLoaderServer(properties);
            limitUsageLoaderServer.initialize();
            limitUsageLoaderServer.initToLiveMode();
        } catch (Throwable e) {
            LOG.error("Failed to start LimitUsageLoaderServer, shutting down process.", e);
            throw e;
        } finally {
            LOG.info("Sleeping 5s before quitting application termination.");
            Thread.sleep(5000);
            System.exit(1);
        }
    }
}
