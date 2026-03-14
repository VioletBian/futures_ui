package com.xx.futures.evetor.alert.generator.rules;

public class LimitUsageRule {

    private static final OctaneLogger LOG = LogUtility.getLogger();

    private final AlertRule alertRule;
    private final Common.Application application;
    private final ActiveAlerts activeAlerts;

    @Inject
    public LimitUsageRule(
        AlertRule alertRule,
        Common.Application application,
        ActiveAlerts activeAlerts
    ) {
        this.alertRule = alertRule;
        this.application = application;
        this.activeAlerts = activeAlerts;
    }

    public AlertRule getAlertRule() {
        return alertRule;
    }

    public String getLimitUsageAlertRuleId() {
        return alertRule.getId();
    }

    public UnpublishedAlert process(ClearingData.LimitUsage limitUsage) {
        if (shouldAlert(limitUsage)) {
            UnpublishedAlert alert = new ImmediateAlert(getLimitUsageAlert(getTimestamp(), limitUsage));
            LOG.info("Raising alert for limit usage and rule pair.");
            LOG.info(
                "Limit usage is - Id:[{}] | Mic:[{}] | ClientRefId:[{}] | GMI:[{}] | Usage:[{}]",
                limitUsage.getId(),
                limitUsage.getMic(),
                limitUsage.getClientRefId(),
                getGmiSynonymForLimitUsage(limitUsage),
                calculatePercentage(limitUsage)
            );
            LOG.info(
                "Rule is - RuleId:[{}] | Venue:{} | AccountId:{} | Threshold:[{}]",
                alertRule.getId(),
                alertRule.getVenue(),
                alertRule.getAccountId(),
                getThresholdForRule()
            );
            return alert;
        }
        return null;
    }

    public boolean shouldAlert(ClearingData.LimitUsage limitUsage) {
        if (activeAlerts.hasActiveLimitUsageAlert(getAlertId(limitUsage))) {
            return false;
        }

        StatComparison<Double> statComparison = alertRule.getLimitUsageAlertThreshold();
        boolean isMatchingThreshold =
            statComparison != null && compareStat(statComparison, calculatePercentage(limitUsage), 0d);
        boolean containsVenue = alertRule.getVenue().contains(limitUsage.getMic());
        return isMatchingThreshold && containsVenue && containsAccount(limitUsage);
    }

    public Long getTimestamp() {
        return System.currentTimeMillis();
    }

    public boolean containsAccount(ClearingData.LimitUsage limitUsage) {
        HashSet<String> accountIdSet = alertRule.getAccountId();
        if (accountIdSet == null || accountIdSet.isEmpty()) {
            return true;
        }

        boolean containsGmiSynonym = accountIdSet.contains(getGmiSynonymForLimitUsage(limitUsage));
        boolean containsClientRefId = accountIdSet.contains(limitUsage.getClientRefId());
        return containsGmiSynonym || containsClientRefId;
    }

    public String getAlertId(ClearingData.LimitUsage limitUsage) {
        return String.format(
            "%s-%s-%s-RuleId%s",
            limitUsage.getClientRefId(),
            getGmiSynonymForLimitUsage(limitUsage),
            alertRule.getVersion(),
            alertRule.getId()
        );
    }

    public String getThresholdForRule() {
        return alertRule.getLimitUsageAlertThreshold().getOperator()
            + alertRule.getLimitUsageAlertThreshold().getValue();
    }

    public String getAlertRuleBreachingMessage(ClearingData.LimitUsage limitUsage) {
        String clientRefId = StringUtils.isNotEmpty(limitUsage.getClientRefId()) ? limitUsage.getClientRefId() : "";
        String GMI = StringUtils.isNotEmpty(getGmiSynonymForLimitUsage(limitUsage))
            ? getGmiSynonymForLimitUsage(limitUsage)
            : "";
        String marginUsage = String.format("%.1f", limitUsage.getUsage());
        String marginLimit = String.format("%.1f", limitUsage.getLimit());
        String currentPercentage = String.format("%.1f%%", calculatePercentage(limitUsage));
        String alertThreshold = String.format("%s%%", getThresholdForRule());
        String currency = limitUsage.getCurrency().toString();
        return String.format(
            "ClientRefId [%s] with GMI [%s] has reached margin usage percentage of [%s] (margin usage [%s %s], margin limit [%s %s]). "
                + "This has breached alerting threshold at [%s]. Please take necessary actions.",
            clientRefId,
            GMI,
            currentPercentage,
            marginUsage,
            currency,
            marginLimit,
            currency,
            alertThreshold
        );
    }

    public Alert.AlertActivity getLimitUsageAlertActivity(
        long timestamp,
        ClearingData.LimitUsage limitUsage
    ) {
        String alertMessage = getAlertRuleBreachingMessage(limitUsage);
        return AlertUtils.createLimitUsageAlertActivity(timestamp, alertRule, alertMessage);
    }

    public Alert getLimitUsageAlert(long timestamp, ClearingData.LimitUsage limitUsage) {
        String alertId = getAlertId(limitUsage);
        Alert.AlertActivity alertActivity = getLimitUsageAlertActivity(timestamp, limitUsage);
        return AlertUtils.createLimitUsageAlert(alertId, timestamp, application, alertActivity);
    }
}
