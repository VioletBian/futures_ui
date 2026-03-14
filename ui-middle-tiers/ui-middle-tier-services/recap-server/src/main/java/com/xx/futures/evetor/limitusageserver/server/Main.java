package com.xx.futures.evetor.limitusageserver.server;

@ExcludeFromJacocoGeneratedReport
public class Main {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    public static void main(String[] args) throws Throwable {
        LOG.info("Starting limit usage server");

        try {
            LOG.info("Class path: {}", System.getProperty("java.class.path"));
            LOG.info("JAVA_OPTS: {}", getJvmArguments());

            NamedProperties properties = ConfigUtils.getProperties("recap", args);
            checkProperties(properties);

            LimitUsageServer limitUsageServer = new LimitUsageServer(properties);
            limitUsageServer.initialize();
            limitUsageServer.initShutdown();
        } catch (Throwable e) {
            LOG.error("Failed to start LimitUsageServer, shutting down process.", e);
            throw e;
        } finally {
            LOG.info("Sleeping 5s before quitting application termination.");
            Thread.sleep(5000);
            System.exit(1);
        }
    }
}
