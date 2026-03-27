package com.xx.futures.evetor.alert.generator.sources;

import com.xx.futures.evetor.alert.generator.AlertEngine;
import com.xx.futures.evetor.alert.generator.rules.LimitUsageRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class LimitUsageAlertSource implements AlertGeneratorSource {

    private static final OctaneLogger LOG = LogUtility.getLogger();
    private static final String LIMIT_USAGE_LOADER_URL = "limitUsageLoaderUrl";
    private static final String ACCOUNT_LIST_QUERY_PARAM = "accountlist";
    private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final String dateFormat = "yyyyMMdd";

    private final List<LimitUsageRule> thresholdRules = new CopyOnWriteArrayList<>();
    private final PermitEngine permitEngine;
    private final Common.Region alerterRegion;
    private final boolean disablePermitValidation;
    private final Common.Application application;
    private final ActiveAlerts activeAlerts;
    private final BaseEntitlementUtils baseEntitlementUtils;
    private final Messaging.MessagingExchange.EnvironmentNamespace environmentEnum;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledRules =
        new ConcurrentHashMap<>();
    private final String runDate;
    private JetstreamHttpClient jetstreamHttpClient;
    private final AlertEngine alertEngine;

    @Inject
    public LimitUsageAlertSource(
        AviatorContext aviatorContext,
        BaseEntitlementUtils baseEntitlementUtils,
        NamedProperties namedProperties,
        PermitEngine permitEngine,
        @AlerterProcessRegion Common.Region alerterRegion,
        @AlertApplication Common.Application application,
        ActiveAlerts activeAlerts,
        AlertEngine alertEngine
    ) {
        this.environmentEnum = aviatorContext.getEnvironment();
        this.baseEntitlementUtils = baseEntitlementUtils;
        this.permitEngine = permitEngine;
        this.alerterRegion = alerterRegion;
        this.application = application;
        this.activeAlerts = activeAlerts;
        this.runDate = namedProperties.getProperty(AviatorProperty.runDate);
        this.disablePermitValidation = namedProperties.getBooleanProperty(
            AviatorProperty.alertRuleDisablePermitValidation
        );
        this.jetstreamHttpClient = this.constructHttpClient(namedProperties);
        this.alertEngine = alertEngine;
    }

    private JetstreamHttpClient constructHttpClient(NamedProperties config) {
        JetstreamHttpClient client =
            new JetstreamHttpClient(config.getProperty(LIMIT_USAGE_LOADER_URL));
        client.initFromProperties(config);
        return client;
    }

    @Override
    public List<UnpublishedAlert> generateNewAlerts(AlertRecord alertRecord) {
        return null;
    }

    @Override
    public boolean canAlertRecordCloseAlert(AlertRecord alertRecord, SerializedOrderAlert alert) {
        return false;
    }

    private boolean removeThresholdRuleById(String ruleId) {
        return ruleId != null
            && thresholdRules.removeIf(rule -> ruleId.equals(rule.getLimitUsageAlertRuleId()));
    }

    private boolean removeTimeBasedRuleById(String ruleId) {
        ScheduledFuture<?> future = scheduledRules.remove(ruleId);
        if (future == null) {
            LOG.warn(
                "Limit Usage Alert Rule [{}] is absent in scheduled rules, this time-based rule need not be removed",
                ruleId
            );
            return false;
        }
        if (!future.isDone()) {
            LOG.info("Cancelled pending time-based alert rule [{}]", ruleId);
            future.cancel(true);
        }
        LOG.info("Removed time-based rule [{}]", ruleId);
        return true;
    }

    public boolean processRemoveAlertRule(AlertRule alertRule) {
        if (alertRule == null) {
            LOG.warn("Skip removing null alert rule");
            return false;
        }
        return alertRule.isTimeBasedLimitUsageRule()
            ? removeTimeBasedRuleById(alertRule.getId())
            : removeThresholdRuleById(alertRule.getId());
    }

    public void processNewAlertRule(AlertRule alertRule) {
        if (alertRule == null || !alertRule.isLimitUsageRule() || alertRule.getId() == null) {
            LOG.warn(
                "Limit Usage Alert Source is not adding rule [{}] as it is only configured for Limit Usage Alert Rules",
                alertRule != null ? alertRule.getId() : "null"
            );
            return;
        }

        // 中文注释：前端已做互斥校验，但规则也可能来自 consumer/polling，这里再次兜底 selector 的唯一性。
        if (!alertRule.hasValidLimitUsageVenueSelector()) {
            LOG.warn(
                "Limit Usage Alert Rule [{}] is invalid because MIC and MICFamily must be mutually exclusive and one selector must be set.",
                alertRule.getId()
            );
            return;
        }

        if (!this.disablePermitValidation && isInvalidUser(alertRule, permitEngine)) {
            return;
        }

        boolean ruleRemoved = processRemoveAlertRule(alertRule);
        if (isStaleRule(alertRule, ruleRemoved, LIMIT_USAGE)) {
            return;
        }

        if (!hasLimitsView(alertRule.getKerberos())) {
            LOG.warn(
                "Kerberos [{}] has no limits view, alert rule [{}] is invalid. Skipping this rule.",
                alertRule.getId(),
                alertRule.getKerberos()
            );
            return;
        }

        String verb = ruleRemoved ? "updating" : "adding";
        LimitUsageRule limitUsageRule = new LimitUsageRule(alertRule, application, activeAlerts);
        boolean isTimeBased = alertRule.isTimeBasedLimitUsageRule();
        LOG.info(
            "Limit Usage Alert Source is [{}] type=[{}] Alert Rule [{}] for kerberos=[{}] with region=[{}] ",
            verb,
            isTimeBased ? "time-based" : "threshold",
            alertRule.getId(),
            alertRule.getKerberos(),
            alerterRegion
        );

        if (isTimeBased) {
            scheduleTimeBasedRule(limitUsageRule);
        } else {
            thresholdRules.add(limitUsageRule);
        }
    }

    private void scheduleTimeBasedRule(LimitUsageRule limitUsageRule) {
        AlertRule alertRule = limitUsageRule.getAlertRule();
        long timeDelay = calculateTimeDelay(alertRule);
        String accountIds = limitUsageRule.getAccountsString();
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        // 中文注释：time-based 快照接口当前收的是 accountlist，这里对齐已有 API 定义避免装载后查不到数据。
        queryParams.add(ACCOUNT_LIST_QUERY_PARAM, accountIds);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String response = this.jetstreamHttpClient.get(
                    "/limitusage/accounts",
                    null,
                    queryParams,
                    null,
                    "application/json"
                );
                Map<String, Object> data =
                    objectMapper.readValue(response, new TypeReference<>() {});
                LOG.info(
                    "Received limit usage data [{}] for rule [{}] with selector type=[{}] values=[{}]",
                    data,
                    alertRule.getId(),
                    limitUsageRule.getVenueSelectorType(),
                    limitUsageRule.getVenueSelectorValues()
                );
                // 中文注释：不要在 Source 入口先把无匹配 row 的快照吞掉；旧链路需要让空 message 继续流向下游，触发 ERROR 类提示邮件。
                alertEngine.process(generateNewAlerts(limitUsageRule, data));
            } catch (JsonProcessingException e) {
                String errorMessage = String.format(
                    "Unable to retrieve margin usage data for alert rule [%s], accountsIds "
                        + "[%s] from limit usage loader, please reach out to Futures Dev",
                    alertRule.getId(),
                    accountIds
                );
                LOG.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } catch (Exception e) {
                String errorMessage = String.format(
                    "Error while processing time-based limit usage alert rule [%s], please"
                        + " reach out to Futures Dev",
                    alertRule.getId()
                );
                LOG.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        }, timeDelay, TimeUnit.MILLISECONDS);

        scheduledRules.put(alertRule.getId(), future);
        LOG.info(
            "Time-based limit usage rule ID [{}] for accounts [{}] is scheduled to run after a delay of [{}] at [{} ({})]",
            alertRule.getId(),
            accountIds,
            DateTimeUtil.convertMillisecondsToHoursAndMinutes(timeDelay),
            alertRule.getLimitUsageAlertTime(),
            alertRule.getLimitUsageAlertTimezone()
        );
    }

    long calculateTimeDelay(AlertRule alertRule) {
        long targetTimeMilli = getUTCEpochForTimeInATimezone(
            runDate,
            alertRule.getLimitUsageAlertTime(),
            alertRule.getLimitUsageAlertTimezone(),
            "HH:mm"
        ) / 1000;
        long currEpochMilli =
            LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli();
        return getTimeDelay(targetTimeMilli - currEpochMilli);
    }

    long getTimeDelay(long timeDelay) {
        if (timeDelay < 0) {
            return timeDelay + DAY_IN_MILLIS;
        }

        if (timeDelay > DAY_IN_MILLIS) {
            return timeDelay - DAY_IN_MILLIS;
        }

        return timeDelay;
    }

    public String getAlertGeneratedDate(String timezone) {
        return DateTimeFormatter.ofPattern(dateFormat)
            .format(LocalDate.now(ZoneId.of(timezone)));
    }

    public boolean hasLimitsView(String kerberos) {
        UserEntitlements entitlements = baseEntitlementUtils.getUserEntitlements(
            this.permitEngine,
            kerberos,
            environmentEnum
        );
        return entitlements.getLimitsView();
    }

    @Override
    public List<UnpublishedAlert> generateNewAlerts(ClearingData.LimitUsage limitUsage) {
        List<UnpublishedAlert> newAlertLists = new ArrayList<>();

        for (LimitUsageRule rule : thresholdRules) {
            UnpublishedAlert newAlert = rule.process(limitUsage);
            if (newAlert != null) {
                newAlertLists.add(newAlert);
            }
        }

        return newAlertLists.isEmpty() ? null : newAlertLists;
    }

    public List<UnpublishedAlert> generateNewAlerts(
        LimitUsageRule limitUsageRule,
        Map<String, Object> limitUsageMap
    ) {
        if (limitUsageMap == null) {
            return null;
        }

        AlertRule alertRule = limitUsageRule.getAlertRule();
        LOG.info(
            "Raising alert for time based rule. Rule is - RuleId:[{}] / SelectorType:[{}] / SelectorValues:{} / AccountId:{} / TimeToTrigger:[{}]",
            alertRule.getId(),
            limitUsageRule.getVenueSelectorType(),
            limitUsageRule.getVenueSelectorValues(),
            alertRule.getAccountId(),
            limitUsageRule.getTimeToTriggerForRule()
        );
        String snapshotTime = limitUsageRule.getTimeToTriggerForRule();
        String alertGeneratedDate =
            getAlertGeneratedDate(alertRule.getLimitUsageAlertTimezone());
        UnpublishedAlert alert =
            new ImmediateAlert(
                getTimeBasedLimitUsageAlert(
                    limitUsageRule,
                    getTimestamp(),
                    limitUsageMap,
                    snapshotTime,
                    alertGeneratedDate
                )
            );
        return List.of(alert);
    }

    public String getTimeBasedAlertRuleBreachingMessage(
        Map<String, Object> limitUsageMap,
        String snapshotTime,
        String alertDate
    ) {
        String tableContent = getTableContent(limitUsageMap);

        if (StringUtils.isBlank(tableContent)) {
            LOG.warn(
                "No valid data found for time-based limit usage alert. Stamping null in alert message to be processed later for ERROR email."
            );
            return null;
        }

        String messageIntro = String.format(
            "Please find below margin usage details for accounts %s on %s %s.<br/>",
            limitUsageMap.keySet(),
            alertDate,
            snapshotTime
        ) + "Note: These data do not contain any manual override done during the trading day.<br/>";

        return "<html><body>" + messageIntro + tableContent + "</body></html>";
    }

    // 中文注释：time-based alert 的 message 依赖 Source 里的表格渲染链路，因此整条 alert build 链继续保留在 Source。
    public Alert getTimeBasedLimitUsageAlert(
        LimitUsageRule limitUsageRule,
        long timestamp,
        Map<String, Object> limitUsageMap,
        String snapshotTime,
        String alertGeneratedDate
    ) {
        AlertRule alertRule = limitUsageRule.getAlertRule();
        String alertId = getTimeBasedAlertId(alertRule);
        Alert.AlertActivity alertActivity = getTimeBasedLimitUsageAlertActivity(
            limitUsageRule,
            timestamp,
            limitUsageMap,
            snapshotTime,
            alertGeneratedDate
        );
        return AlertUtils.createLimitUsageAlert(alertId, timestamp, application, alertActivity);
    }

    public Alert.AlertActivity getTimeBasedLimitUsageAlertActivity(
        LimitUsageRule limitUsageRule,
        long timestamp,
        Map<String, Object> limitUsageMap,
        String snapshotTime,
        String alertGeneratedDate
    ) {
        String alertMessage =
            getTimeBasedAlertRuleBreachingMessage(limitUsageMap, snapshotTime, alertGeneratedDate);
        return AlertUtils.createLimitUsageAlertActivity(
            timestamp,
            limitUsageRule.getAlertRule(),
            alertMessage
        );
    }

    public String getTimeBasedAlertId(AlertRule alertRule) {
        return String.format(
            "%s-%s-RuleId%s",
            Strings.join(alertRule.getAccountId(), ","),
            alertRule.getVersion(),
            alertRule.getId()
        );
    }

    public String getTableContent(Map<String, Object> limitUsageMap) {
        StringBuilder table = new StringBuilder();
        for (Map.Entry<String, Object> entry : limitUsageMap.entrySet()) {
            String contentRow = getBody(entry);
            if (StringUtils.isNotBlank(contentRow)) {
                table.append(contentRow);
            } else {
                LOG.warn(
                    "Account [{}] generated invalid data row based on Limit usage loader entry [{}]",
                    entry.getKey(),
                    entry
                );
            }
        }

        if (table.isEmpty()) {
            return null;
        }

        table.insert(
            0,
            getHeaderRow(
                Arrays.asList(
                    "Exchange",
                    "MicFamily",
                    "Account Id",
                    "Margin Usage",
                    "Margin Limit",
                    "Currency",
                    "Margin Usage Percentage"
                )
            )
        );
        return "<br><table style=\"border-collapse: collapse; width: 100%;\" border=\"1\">"
            + table
            + "</table>";
    }

    // 中文注释：合作方最新数据约定是 snapshot row 必须同时带 mic 与 micFamily，因此这里统一校验并把两列都写入邮件表格。
    public String getBody(Map.Entry<String, Object> entry) {
        if (entry.getValue() instanceof Map) {
            Map<String, Object> limitUsage = (Map<String, Object>) entry.getValue();
            if (!isValidLimitUsage(limitUsage)) {
                return null;
            }

            String mic = (String) limitUsage.get("mic");
            String micFamily = (String) limitUsage.get("micFamily");
            Double usage = (Double) limitUsage.get("usage");
            Double limit = (Double) limitUsage.get("limit");
            String currency = (String) limitUsage.get("currency");
            double percentage = calculatePercentage(usage, limit);
            return getContentRow(
                Arrays.asList(
                    mic,
                    micFamily,
                    entry.getKey(),
                    formatDouble(usage),
                    formatDouble(limit),
                    currency,
                    formatPercentage(percentage)
                )
            );
        }
        return null;
    }

    // 中文注释：time-based snapshot 现在要求 mic、micFamily、usage、limit、currency 全部存在，避免邮件里再丢 venue 维度信息。
    public boolean isValidLimitUsage(Map<String, Object> limitUsage) {
        List<String> columns = Arrays.asList("mic", "micFamily", "usage", "limit", "currency");
        for (String column : columns) {
            if (!limitUsage.containsKey(column) || ObjectUtils.isEmpty(limitUsage.get(column))) {
                return false;
            }
        }
        return true;
    }

    public String format(String position, String value) {
        return String.format("<td style=\"text-align:%s\">%s</td>", position, value);
    }

    public String formatCenter(String value) {
        return format("center", value);
    }

    public String formatHeader(String value) {
        return formatCenter(String.format("<strong>%s</strong>", value));
    }

    public String formatDouble(Double value) {
        return String.format("%.1f", value);
    }

    public String formatPercentage(Double value) {
        return String.format("%.1f%%", value);
    }

    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    public List<LimitUsageRule> getThresholdRules() {
        return thresholdRules;
    }

    public ConcurrentHashMap<String, ScheduledFuture<?>> getScheduledRules() {
        return scheduledRules;
    }

    public String getHeaderRow(List<String> colValues) {
        return getRow(colValues, true);
    }

    public String getContentRow(List<String> colValues) {
        return getRow(colValues, false);
    }

    public String getRow(List<String> colValues, boolean isHeader) {
        StringBuilder sb = new StringBuilder("<tr>");
        for (String col : colValues) {
            String formattedCol = isHeader ? formatHeader(col) : formatCenter(col);
            sb.append(formattedCol);
        }
        sb.append("</tr>");
        return sb.toString();
    }

    @VisibleForTesting
    void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @VisibleForTesting
    void setJetstreamHttpClient(JetstreamHttpClient jetstreamHttpClient) {
        this.jetstreamHttpClient = jetstreamHttpClient;
    }
}
