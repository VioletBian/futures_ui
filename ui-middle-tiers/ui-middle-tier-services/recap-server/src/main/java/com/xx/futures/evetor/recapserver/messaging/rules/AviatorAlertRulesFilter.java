package com.xx.futures.evetor.recapserver.messaging.rules;

public class AviatorAlertRulesFilter implements DataFilter<GeneratedMessage> {

    @Inject
    AviatorAlertRulesFilter() {}

    @Override
    public boolean shouldBeProcessed(GeneratedMessage data) {
        if (data instanceof Coverage.NotificationAlert) {
            Coverage.NotificationAlert alert = (Coverage.NotificationAlert) data;
            return checkValidAlertRule(alert);
        }

        return false;
    }

    public boolean checkValidAlertRule(Coverage.NotificationAlert alert) {
        return alert.hasNotificationType()
            & alert.getNotificationType() == Coverage.NotificationAlert.NotificationType.AlertRule;
    }
}
