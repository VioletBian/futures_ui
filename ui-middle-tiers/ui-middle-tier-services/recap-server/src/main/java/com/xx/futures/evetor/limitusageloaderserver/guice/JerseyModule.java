package com.xx.futures.evetor.limitusageloaderserver.guice;

public class JerseyModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<String> packageBinder =
            Multibinder.newSetBinder(binder(), String.class, Names.named(JerseyScanner.PACKAGES));
        packageBinder.addBinding().toInstance(LimitUsageApi.class.getPackage().getName());
    }
}
