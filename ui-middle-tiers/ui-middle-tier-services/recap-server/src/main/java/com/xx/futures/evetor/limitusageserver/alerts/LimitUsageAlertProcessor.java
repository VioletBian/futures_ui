package com.xx.futures.evetor.limitusageserver.alerts;

public class LimitUsageAlertProcessor implements AviatorAlertProcessor {

    @Override
    public Coverage.Alert process(GeneratedMessage dataToProcess) throws DataProcessException {
        return (Coverage.Alert) dataToProcess;
    }
}
