package com.xx.futures.evetor.limitusageserver.alerts;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.xx.futures.evetor.shared.util.AlertUtils;
import com.xx.futures.jetstream.utils.config.NamedProperties;
import com.xx.jetstream.model.alert.AlertRule;
import com.xx.octane.datamodel.business.futures.generated.Coverage;

import static com.xx.futures.evetor.limitusageserver.alerts.LimitUsageAlertConstants.ENABLE_SYMPHONY_ALERTS;
import static com.xx.futures.evetor.shared.util.EmailUtil.isValidInternalXXEmail;

public class LimitUsageAlertRuleValidation {

    private final boolean enableSymphonyAlerts;

    @Inject
    public LimitUsageAlertRuleValidation(NamedProperties properties) {
        this.enableSymphonyAlerts = Boolean.parseBoolean(
            properties.getProperty(ENABLE_SYNPHONY_ALERTS, String.valueOf(false))
        );
    }

    public boolean validateShouldSendGenericEmail(Coverage.Alert alertToSend, AlertRule alertRule) {
        return alertRule.getGenericEmail()
            && alertRule.getEmailAddress() != null
            && !alertRule.getEmailAddress().isEmpty()
            && isValidInternalXXEmail(alertRule.getEmailAddress())
            && validateAlertIdToRuleId(alertToSend, alertRule)
            && validateAlertMessageToRuleMessage(alertToSend, alertRule)
            && alertToSend.getActivity(alertToSend.getActivityCount() - 1)
                .getActorId()
                .equals(alertRule.getKerberos());
    }

    public boolean validateShouldSendSymphonyMessage(Coverage.Alert alertToSend, AlertRule alertRule) {
        return this.enableSymphonyAlerts
            && alertRule.getSymphonyEnabled()
            && validateAlertIdToRuleId(alertToSend, alertRule)
            && validateAlertMessageToRuleMessage(alertToSend, alertRule)
            && alertToSend.getActivity(alertToSend.getActivityCount() - 1)
                .getActorId()
                .equals(alertRule.getKerberos());
    }

    public boolean validateShouldSendSymphonyRoomMessage(Coverage.Alert alertToSend, AlertRule alertRule) {
        return this.enableSymphonyAlerts
            && alertRule.getSymphonyEnabled()
            && !Strings.isNullOrEmpty(alertRule.getSymphonyRoomName())
            && validateAlertIdToRuleId(alertToSend, alertRule)
            && validateAlertMessageToRuleMessage(alertToSend, alertRule)
            && alertToSend.getActivity(alertToSend.getActivityCount() - 1)
                .getActorId()
                .equals(alertRule.getKerberos());
    }

    public boolean validateAlertIdToRuleId(Coverage.Alert alertToSend, AlertRule alertRule) {
        return alertToSend.getAlertId().endsWith(alertRule.getId());
    }

    public boolean validateAlertMessageToRuleMessage(Coverage.Alert alertToSend, AlertRule alertRule) {
        return alertRule.getMessage().equals(AlertUtils.extractActionInformation(alertToSend).getMessage());
    }
}
