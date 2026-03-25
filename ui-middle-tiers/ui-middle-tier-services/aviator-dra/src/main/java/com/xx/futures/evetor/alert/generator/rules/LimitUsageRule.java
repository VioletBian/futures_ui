package com.xx.futures.evetor.alert.generator.rules;

public class LimitUsageRule {

    private static final OctaneLogger LOG = LogUtility.getLogger();
    private static final String MIC_SELECTOR = "MIC";
    private static final String MIC_FAMILY_SELECTOR = "MICFamily";

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
                "Limit usage is - Id:[{}] | Mic:[{}] | MicFamily:[{}] | ClientRefId:[{}] | GMI:[{}] | Usage:[{}]",
                limitUsage.getId(),
                limitUsage.getMic(),
                normalizeSelectorValue(limitUsage.getMicFamily()),
                limitUsage.getClientRefId(),
                getGmiSynonymForLimitUsage(limitUsage),
                calculatePercentage(limitUsage)
            );
            LOG.info(
                "Rule is - RuleId:[{}] | SelectorType:[{}] | SelectorValues:{} | AccountId:{} | Threshold:[{}]",
                alertRule.getId(),
                getVenueSelectorType(),
                getVenueSelectorValues(),
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
        return isMatchingThreshold && matchesVenueSelector(limitUsage) && containsAccount(limitUsage);
    }

    // 中文注释：把 MIC / MICFamily 选择器抽成统一入口，避免实时阈值和定时快照各自维护一套分支。
    public String getVenueSelectorType() {
        return alertRule.hasMicFamilySelection() ? MIC_FAMILY_SELECTOR : MIC_SELECTOR;
    }

    public HashSet<String> getVenueSelectorValues() {
        return alertRule.hasMicFamilySelection() ? alertRule.getMicFamily() : alertRule.getVenue();
    }

    public boolean matchesVenueSelector(ClearingData.LimitUsage limitUsage) {
        HashSet<String> selectorValues = getVenueSelectorValues();
        if (selectorValues == null || selectorValues.isEmpty()) {
            return false;
        }

        // 中文注释：强类型的 limit usage 事件现在已经具备 micFamily getter，这里直接读取 proto 字段，不再保留反射兼容逻辑。
        String selectorValue = alertRule.hasMicFamilySelection()
            ? normalizeSelectorValue(limitUsage.getMicFamily())
            : normalizeSelectorValue(limitUsage.getMic());
        return StringUtils.isNotBlank(selectorValue) && selectorValues.contains(selectorValue);
    }

    // 中文注释：time-based 规则拿到快照后先按 selector 过滤，只保留真正命中 MIC / MICFamily 的账户。
    public java.util.Map<String, Object> filterMatchingLimitUsages(java.util.Map<String, Object> limitUsageMap) {
        java.util.Map<String, Object> filteredLimitUsageMap = new java.util.LinkedHashMap<>();
        if (limitUsageMap == null || limitUsageMap.isEmpty()) {
            return filteredLimitUsageMap;
        }

        for (java.util.Map.Entry<String, Object> entry : limitUsageMap.entrySet()) {
            if (matchesVenueSelector(entry.getValue())) {
                filteredLimitUsageMap.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredLimitUsageMap;
    }

    public boolean matchesVenueSelector(Object limitUsageEntry) {
        if (!(limitUsageEntry instanceof java.util.Map<?, ?>)) {
            return false;
        }

        HashSet<String> selectorValues = getVenueSelectorValues();
        if (selectorValues == null || selectorValues.isEmpty()) {
            return false;
        }

        java.util.Map<?, ?> snapshotEntry = (java.util.Map<?, ?>) limitUsageEntry;
        // 中文注释：time-based 快照仍然来自 HTTP JSON 反序列化结果，这里只保留 map 读取逻辑。
        String selectorValue = alertRule.hasMicFamilySelection()
            ? getSnapshotMicFamily(snapshotEntry)
            : getSnapshotMic(snapshotEntry);
        return StringUtils.isNotBlank(selectorValue) && selectorValues.contains(selectorValue);
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

    private String getSnapshotMic(java.util.Map<?, ?> limitUsageEntry) {
        return normalizeSelectorValue(limitUsageEntry.get("mic"));
    }

    private String getSnapshotMicFamily(java.util.Map<?, ?> limitUsageEntry) {
        return normalizeSelectorValue(limitUsageEntry.get("micFamily"));
    }

    private String normalizeSelectorValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalizedValue = String.valueOf(rawValue);
        return StringUtils.isBlank(normalizedValue) ? null : normalizedValue;
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
