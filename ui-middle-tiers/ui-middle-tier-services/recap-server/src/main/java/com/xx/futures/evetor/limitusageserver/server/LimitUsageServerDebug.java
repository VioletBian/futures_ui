package com.xx.futures.evetor.limitusageserver.server;

import java.util.ArrayList;
import java.util.List;

public class LimitUsageServerDebug {

    public static void main(String[] args) throws Throwable {
        Main.main(args);
    }

    static String[] getDebugArgs() {
        List<String> args = new ArrayList<>();
        args.add("--site");
        args.add("aws");
        args.add("--env");
        args.add("uat");
        args.add("--data-centre-site");
        args.add("DAL7");
        args.add("-D");
        args.add("region=us");

        if (SystemUtils.IS_OS_MAC_OSX) {
            String nonProdWaterfallKeyTab = "/Users/xx/jetstream_config/xx_conf/nonprod/nonprod-waterfall-keytab";
            args.add("--");
            args.add("waterfallKeyTab");
            args.add(nonProdWaterfallKeyTab);
        }

        args.add("-D");
        args.add(JetstreamVertxIoServerProperty.serverThreads.name() + "=128000");
        args.add("-D");
        args.add("archiveRoom=true");
        args.add("-D");
        args.add("enableLimitUsageAlert=true");
        return args.toArray(String[]::new);
    }
}
