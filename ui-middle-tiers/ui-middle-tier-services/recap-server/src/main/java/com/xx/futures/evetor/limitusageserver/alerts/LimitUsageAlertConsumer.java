package com.xx.futures.evetor.limitusageserver.alerts;

public class LimitUsageAlertConsumer implements AviatorAlertConsumer {

    private final OctaneLogger LOG = LogUtility.getLogger();
    private final AlertRuleCache alertRuleCache;
    private final ConfigServerDao configServerDao;
    private final SymphonyNotifier symphonyNotifier;
    private final GenericEmailNotifier genericEmailNotifier;
    private final ExternalRecapNotifier externalRecapNotifier;
    private final boolean enableGenericEmail;
    private final boolean enableSymphonyNotifications;
    private final boolean enableExternalLimitUsageEmail;

    @Inject
    public LimitUsageAlertConsumer(
        AlertRuleCache alertRuleCache,
        ConfigServerDao configServerDao,
        SymphonyNotifier symphonyNotifier,
        GenericEmailNotifier genericEmailNotifier,
        ExternalRecapNotifier externalRecapNotifier,
        NamedProperties properties
    ) {
        this.alertRuleCache = alertRuleCache;
        this.configServerDao = configServerDao;
        this.symphonyNotifier = symphonyNotifier;
        this.genericEmailNotifier = genericEmailNotifier;
        this.externalRecapNotifier = externalRecapNotifier;
        this.enableGenericEmail = Boolean.parseBoolean(
            properties.getProperty(ENABLE_GENERIC_EMAIL, String.valueOf(false))
        );
        this.enableSymphonyNotifications = Boolean.parseBoolean(
            properties.getProperty(ENABLE_SYMPHONY_NOTIFICATIONS, String.valueOf(false))
        );
        this.enableExternalLimitUsageEmail = Boolean.parseBoolean(
            properties.getProperty(ENABLE_EXTERNAL_LIMIT_USAGE_EMAIL, String.valueOf(false))
        );
    }

    @Override
    public void consume(Coverage.Alert alert) throws DataConsumptionException {
        LOG.info(
            "Received alert - Alert Id [{}], Type [{}], Actor [{}], Action [{}], ActionInformation [{}]",
            alert.getAlertId(),
            alert.getType(),
            alert.getActivity(alert.getActivityCount() - 1).getActorId(),
            alert.getLatestAlertAction(),
            AlertUtils.extractActionInformation(alert)
        );

        AlertRule alertRule = getAlertRule(alert);
        if (alertRule == null) {
            throw new DataConsumptionException(
                String.format("Failed to obtain alert rule for alertId [%s]", alert.getAlertId())
            );
        }
        LOG.info("Retrieved alert rule [{}]", alertRule);

        if (alertRule.getEnabled()) {
            try {
                triggerNotification(alert, alertRule);
            } catch (Exception e) {
                throw new DataConsumptionException(
                    String.format(
                        "Failed to trigger notification for limit usage alert Id [%s] based on rule [%s]",
                        alert.getAlertId(),
                        alertRule.getId()
                    )
                );
            }
        } else {
            LOG.warn("Alert rule [{}] is not enabled", alertRule.getId());
        }
    }

    public AlertRule getAlertRule(Coverage.Alert alert) {
        String alertId = alert.hasAlertId() ? alert.getAlertId() : null;
        if (StringUtils.isEmpty(alertId)) {
            return null;
        }

        String alertRuleId = AlertUtils.getAlertRuleIdForLimitUsageAlertId(alertId);
        if (StringUtils.isEmpty(alertRuleId)) {
            return null;
        }

        return ElasticUtils.getElasticDocument(alertRuleId, alertRuleCache, configServerDao);
    }

    public void triggerNotification(Coverage.Alert alert, AlertRule alertRule) {
        AlertAdditionalInfo info = AlertUtils.extractActionInformation(alert);
        String message = StringUtils.isBlank(info.getMessage())
            ? alertRule.getMessage()
            : info.getMessage();
        String explanation = StringUtils.isBlank(info.getExplanation())
            ? null
            : info.getExplanation();
        String ruleActorId = alert.getActivity(alert.getActivityCount() - 1).getActorId();
        String alertId = alert.hasAlertId() ? alert.getAlertId() : "";

        if (alertRule.getClientLimitUsageEmail()) {
            if (this.enableExternalLimitUsageEmail) {
                LOG.info(
                    "Sending external email for alert [{}] using alertRule [{}]",
                    alert.getAlertId(),
                    alertRule.getId()
                );
                externalRecapNotifier.sendExternalEmailForLimitUsage(
                    alertRule,
                    alertId,
                    message,
                    explanation
                );
            } else {
                LOG.info(
                    "enableExternalLimitUsageEmail is set to false. Not sending client limit usage email."
                );
            }
            return;
        }

        if (this.enableGenericEmail && alertRule.getGenericEmail()) {
            LOG.info(
                "Sending internal generic email for alert [{}] using alertRule [{}]",
                alert.getAlertId(),
                alertRule.getId()
            );
            genericEmailNotifier.sendGenericEmailForLimitUsage(
                alert,
                alertRule,
                alertId,
                ruleActorId,
                message,
                explanation
            );
        } else {
            LOG.info(
                "enableGenericEmail is [{}] and alertRule.getGenericEmail is [{}]. Not sending generic email.",
                enableGenericEmail,
                alertRule.getGenericEmail()
            );
        }

        if (this.enableSymphonyNotifications && alertRule.getSymphonyEnabled()) {
            String symphonyMessage = java.net.URLEncoder.encode(
                message + " || " + explanation,
                java.nio.charset.StandardCharsets.UTF_8
            );

            if (!Strings.isNullOrEmpty(alertRule.getSymphonyRoomName())) {
                LOG.info(
                    "Sending internal symphony message for alert [{}] using alertRule [{}]",
                    alert.getAlertId(),
                    alertRule.getId()
                );
                symphonyNotifier.sendSymphonyMsgForLimitUsage(
                    alert,
                    alertRule,
                    alertId,
                    ruleActorId,
                    symphonyMessage
                );
            }

            if (!Strings.isNullOrEmpty(alertRule.getSymphonyTeamRoomName())) {
                LOG.info(
                    "Sending internal symphony team room message for alert [{}] using alertRule [{}]",
                    alert.getAlertId(),
                    alertRule.getId()
                );
                symphonyNotifier.sendSymphonyTeamRoomMsgForLimitUsage(
                    alert,
                    alertRule,
                    alertId,
                    ruleActorId,
                    symphonyMessage
                );
            }
        } else {
            LOG.info(
                "enableSymphonyNotifications is [{}] and alertRule.getSymphonyEnabled is [{}]. Not sending symphony notifications.",
                enableSymphonyNotifications,
                alertRule.getSymphonyEnabled()
            );
        }
    }
}
