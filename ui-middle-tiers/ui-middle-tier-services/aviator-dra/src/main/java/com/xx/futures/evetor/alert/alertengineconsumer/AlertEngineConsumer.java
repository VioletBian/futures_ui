package com.xx.futures.evetor.alert.alertengineconsumer;

public interface AlertEngineConsumer extends DataConsumer<Object> {

    void setLiveMode();

    @Override
    default CallbackPriority getCallbackPriority() {
        return CallbackPriority.NORMAL;
    }
}
