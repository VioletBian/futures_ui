package com.xx.futures.evetor.utils;

public class TestUtils {

    public static InstrumentData.Instrument getTestInst(
        Common.MicFamily micFamily,
        Common.Mic mic,
        String underlier,
        ReferenceData.ProductType productType
    ) {
        return InstrumentData.Instrument.newBuilder()
            .setMicFamily(micFamily)
            .setExchange(mic)
            .setUnderlierRootSymbols(
                ReferenceData.InstrumentSymbols.newBuilder()
                    .addSymbols(
                        ReferenceData.InstrumentSymbols.Symbol.newBuilder()
                            .setName(underlier)
                            .setType(ExchangeInstrumentId)
                    )
                    .setPrimaryKeyType(ExchangeInstrumentId)
            )
            .setInstrumentSymbols(
                ReferenceData.InstrumentSymbols.newBuilder()
                    .addSymbols(
                        ReferenceData.InstrumentSymbols.Symbol.newBuilder()
                            .setName("lexId")
                            .setType(LexiconId)
                    )
                    .setPrimaryKeyType(LexiconId)
            )
            .setProductType(productType)
            .build();
    }

    public static OrdersData.Execution.Builder getTestExecutionBuilder() {
        return OrdersData.Execution.newBuilder()
            .setExecutionId("some_execution_id")
            .setVersion(0)
            .setStatus(OrderData.Execution.Status.New)
            .setExchange(Common.Mic.XCME)
            .setVenue(Common.Mic.XNYM)
            .setFillPrice(23)
            .setFillQuantity(5)
            .setExecutionTimeUtc(java.time.Instant.now().toEpochMilli() + 1000);
    }

    public static ClearingData.LimitUsage generateLimitUsage() {
        return generateLimitUsage("XDCE", "DCI03456", "04860800", 51d);
    }

    public static java.util.Map<String, Object> generateLimitUsageMap(int testCase) {
        java.util.Map<String, Object> limitUsageMap = new java.util.HashMap<>();
        switch (testCase) {
            case 1:
                limitUsageMap.put("04860800", generateLimitUsageMap("XDCE", "DCI03456", "04860800", 51d));
                limitUsageMap.put("55300017", generateLimitUsageMap("XZCE", "ZZI02927", "55300017", 70d));
                break;
            case 2:
                limitUsageMap.put("04860800", generateLimitUsageMap("XDCE", "DCI03456", "04860800", 51d));
                limitUsageMap.put("04860801", "Limit Usage couldn't be retrieved.");
                break;
            case 3:
                limitUsageMap.put("04860801", "Limit Usage couldn't be retrieved.");
                limitUsageMap.put("04860802", "Limit Usage couldn't be retrieved.");
                break;
            case 4:
                limitUsageMap.put("04860800", generateLimitUsageMap("XDCE", "DCI03456", "04860800", 51d, ""));
                break;
            case 5:
                break;
            default:
                limitUsageMap.put("04860800", generateLimitUsageMap("XDCE", "DCI03456", "04860800", 51d));
                break;
        }
        return limitUsageMap;
    }

    public static java.util.Map<String, Object> generateLimitUsageMap(
        String venue,
        String gmi,
        String clientRefId,
        double usage
    ) {
        return generateLimitUsageMap(venue, gmi, clientRefId, usage, "USD");
    }

    public static java.util.Map<String, Object> generateLimitUsageMap(
        String venue,
        String gmi,
        String clientRefId,
        double usage,
        String currency
    ) {
        java.util.Map<String, Object> limitUsageMap = new java.util.HashMap<>();
        limitUsageMap.put("venue", venue);
        limitUsageMap.put("gmi", gmi);
        limitUsageMap.put("clientRefId", clientRefId);
        limitUsageMap.put("usage", usage);
        limitUsageMap.put("limit", 100d);
        limitUsageMap.put("currency", currency);
        limitUsageMap.put("mic", venue);
        return limitUsageMap;
    }

    public static ClearingData.LimitUsage generateLimitUsage(
        String venue,
        String gmi,
        String clientRefId,
        double usage
    ) {
        return generateLimitUsage(venue, gmi, clientRefId, usage, Common.Currency.USD);
    }

    public static ClearingData.LimitUsage generateLimitUsage(
        String venue,
        String gmi,
        String clientRefId,
        double usage,
        Common.Currency currency
    ) {
        return ClearingData.LimitUsage.newBuilder()
            .setId(clientRefId + "-" + gmi)
            .setClientRefId(clientRefId)
            .setUsage(usage)
            .setLimit(100d)
            .setCurrency(currency)
            .setMic(Common.Mic.valueOf(venue))
            .addAccounts(
                ReferenceData.Account.newBuilder()
                    .setAccountId(gmi)
                    .setAccountType(ReferenceData.Account.Type.GMI)
                    .build()
            )
            .build();
    }

    // 中文注释：补充带 micFamily 的 limit usage 测试数据，默认给一个可识别的 mic，避免把临时业务假设固化成公共测试前提。
    public static ClearingData.LimitUsage generateMicFamilyLimitUsage(
        String micFamily,
        String gmi,
        String clientRefId,
        double usage
    ) {
        return generateMicFamilyLimitUsage("XZCE", micFamily, gmi, clientRefId, usage, Common.Currency.USD);
    }

    public static ClearingData.LimitUsage generateMicFamilyLimitUsage(
        String mic,
        String micFamily,
        String gmi,
        String clientRefId,
        double usage,
        Common.Currency currency
    ) {
        return ClearingData.LimitUsage.newBuilder()
            .setId(clientRefId + "-" + gmi)
            .setClientRefId(clientRefId)
            .setUsage(usage)
            .setLimit(100d)
            .setCurrency(currency)
            .setVenue(Common.Mic.valueOf(mic))
            .setMicFamily(Common.MicFamily.valueOf(micFamily))
            .addAccounts(
                ReferenceData.Account.newBuilder()
                    .setAccountId(gmi)
                    .setAccountType(ReferenceData.Account.Type.GMI)
                    .build()
            )
            .build();
    }

    public static AlertRule generateLimitUsageAlertRule() {
        return new AlertRule.AlertRuleBuilder()
            .setId("rule1")
            .setVersion(0)
            .setKerberos("liyiyi")
            .setMessage("Limit usage message")
            .setVenue(new java.util.HashSet<>(java.util.Collections.singletonList("XDCE")))
            .setAccountId(new java.util.HashSet<>(java.util.List.of("04860800", "04860801")))
            .setLimitUsageAlertThreshold(
                new StatComparison<Double>()
                    .setComparisonType(StatComparison.ComparisonType.Value)
                    .setValue(50d)
            )
            .setRecapEmail(true)
            .setSoftDelete(false)
            .setEnabled(true)
            .setEmailAddress("liying_1@gs.com")
            .build();
    }

    public static AlertRule generateTimeBasedLimitUsageAlertRule() {
        return new AlertRule.AlertRuleBuilder()
            .setId("rule2")
            .setVersion(0)
            .setKerberos("liyiyi")
            .setMessage("Limit usage time-based alert")
            .setVenue(new java.util.HashSet<>(java.util.Collections.singletonList("XZCE")))
            .setAccountId(new java.util.HashSet<>(java.util.List.of("04860801", "04860800")))
            .setRecapEmail(true)
            .setSoftDelete(false)
            .setEnabled(true)
            .setEmailAddress("liying_1@gs.com")
            .setLimitUsageAlertTime("10:20")
            .setLimitUsageAlertTimezone("Asia/Hong_Kong")
            .build();
    }

    public static AlertRule generateLimitUsageTimeBasedAlertRule() {
        return generateTimeBasedLimitUsageAlertRule();
    }

    public static Coverage.Alert generateLimitUsageAlert() {
        return generateLimitUsageAlert(
            Common.Application.newBuilder().build(),
            generateLimitUsageAlertRule(),
            "04860800-DCI03456-0-RuleIdrule1",
            getAlertRuleBreachingMessage()
        );
    }

    public static Coverage.Alert generateTimedBasedLimitUsageAlert() {
        return generateLimitUsageAlert(
            Common.Application.newBuilder().build(),
            generateTimeBasedLimitUsageAlertRule(),
            "04860801,04860800-0-RuleIdrule2",
            getTimeBasedAlertRuleBreachingMessage(0)
        );
    }

    public static Coverage.Alert generateLimitUsageAlert(
        Common.Application application,
        AlertRule rule,
        String alertId,
        String alertRuleBreachingMessage
    ) {
        Coverage.Alert.AlertActivity activity = Coverage.Alert.AlertActivity.newBuilder()
            .setAction(Coverage.Alert.AlertActivity.AlertAction.Raised)
            .setActionTimestampUtc(1L)
            .setActorType(Coverage.Alert.AlertActivity.ActorType.Kerberos)
            .setActorId("liyiyi")
            .setAlertVersion(0)
            .setResultingPriority(Coverage.Alert.Priority.High)
            .setActionInformation(
                AlertUtils.populateActionInformation(alertRuleBreachingMessage, rule, null)
            )
            .build();

        return Coverage.Alert.newBuilder()
            .setAlertId(alertId)
            .setTimestamp(1L)
            .setPriority(Coverage.Alert.Priority.High)
            .setType(Coverage.Alert.AlertType.LimitUsageAlert)
            .addActivity(activity)
            .setAlertVersion(0)
            .setSource(application)
            .setLatestAlertAction(Coverage.Alert.AlertActivity.AlertAction.Raised)
            .build();
    }

    public static AlertRule generateAlertRule(String id, String kerberos) {
        return generateAlertRule(id, null, kerberos);
    }

    public static AlertRule generateAlertRule(String id, AlertTrigger trigger, String kerberos) {
        return generateAlertRule(id, trigger, kerberos, null, 0, null);
    }

    public static AlertRule generateAlertRule(
        String id,
        AlertTrigger trigger,
        String kerberos,
        String alertCategory,
        int priority,
        java.util.HashSet<String> actorsSet
    ) {
        AlertRule.AlertRuleBuilder alertRuleBuilder = new AlertRule.AlertRuleBuilder()
            .setId(id)
            .setTradingAlgorithm(
                new java.util.HashSet<>(java.util.Collections.singletonList(OrderData.Order.Type.VWAP))
            )
            .setParticipationRateComparison(
                new StatComparison<Double>()
                    .setComparisonType(StatComparison.ComparisonType.Value)
                    .setValue(0d)
            )
            .setMessage("custom algo for vwap")
            .setKerberos(kerberos)
            .setAlertCategory(alertCategory)
            .setPriority(priority)
            .setActorsSet(actorsSet)
            .setEnabled(true);
        if (trigger != null) {
            alertRuleBuilder.setAlertTrigger(trigger);
        }
        return alertRuleBuilder.build();
    }

    public static String getAlertRuleString() {
        return "{"
            + "\"id\":\"rule1\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": false,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"]"
            + "}";
    }

    public static String getSoftDeletedAlertRuleString() {
        return "{"
            + "\"id\":\"rule1\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": true,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"]"
            + "}";
    }

    public static String getLimitUsageAlertRuleString() {
        return "{"
            + "\"id\":\"rule2\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": false,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"],\n"
            + "\"limitUsageAlertThreshold\": {\"operator\": \">\", \"comparisonType\": \"Value\", \"value\": 85 }"
            + "}";
    }

    public static String getLimitUsageTimeBasedAlertRuleString() {
        return "{"
            + "\"id\":\"rule3\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": false,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"],\n"
            + "\"limitUsageAlertTime\": \"10:20\",\n"
            + "\"limitUsageAlertTimezone\": \"Asia/Hong_Kong\""
            + "}";
    }

    public static String getSoftDeletedLimitUsageAlertRuleString() {
        return "{"
            + "\"id\":\"rule2\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": true,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"],\n"
            + "\"limitUsageAlertThreshold\": {\"operator\": \">\", \"comparisonType\": \"Value\", \"value\": 85 }"
            + "}";
    }

    public static String getSoftDeletedLimitUsageTimeBasedAlertRuleString() {
        return "{"
            + "\"id\":\"rule3\",\n"
            + "\"enabled\": true,\n"
            + "\"kerberos\": \"liyiyi\",\n"
            + "\"softDelete\": true,\n"
            + "\"message\": \"message\",\n"
            + "\"venue\": [\"XINE\"],\n"
            + "\"accountId\": [\"IND042311\"],\n"
            + "\"limitUsageAlertTime\": \"10:20\",\n"
            + "\"limitUsageAlertTimezone\": \"Asia/Hong_Kong\""
            + "}";
    }

    public static String getLimitUsageAlertRuleMicFamilyString() {
        return "{"
            + "\"id\":\"rule4\","
            + "\"version\":0,"
            + "\"kerberos\":\"liyiyi\","
            + "\"message\":\"Limit usage alert micFamily\","
            + "\"enabled\":true,"
            + "\"softDelete\":false,"
            + "\"accountId\":[\"04860800\",\"04860801\"],"
            + "\"micFamily\":[\"SFX\"],"
            + "\"limitUsageAlertThreshold\":{\"operator\":\">\",\"value\":70.0}"
            + "}";
    }

    public static String getSoftDeletedLimitUsageAlertRuleMicFamilyString() {
        return "{"
            + "\"id\":\"rule4\","
            + "\"version\":0,"
            + "\"kerberos\":\"liyiyi\","
            + "\"message\":\"Limit usage alert micFamily\","
            + "\"enabled\":true,"
            + "\"softDelete\":true,"
            + "\"accountId\":[\"04860800\",\"04860801\"],"
            + "\"micFamily\":[\"SFX\"],"
            + "\"limitUsageAlertThreshold\":{\"operator\":\">\",\"value\":70.0}"
            + "}";
    }

    public static String getLimitUsageTimeBasedAlertRuleMicFamilyString() {
        return "{"
            + "\"id\":\"rule5\","
            + "\"version\":0,"
            + "\"kerberos\":\"liyiyi\","
            + "\"message\":\"Limit usage time-based alert micFamily\","
            + "\"enabled\":true,"
            + "\"softDelete\":false,"
            + "\"accountId\":[\"04860800\",\"04860801\"],"
            + "\"micFamily\":[\"SFX\"],"
            + "\"limitUsageAlertTime\":\"10:20\","
            + "\"limitUsageAlertTimezone\":\"Asia/Hong_Kong\""
            + "}";
    }

    public static String getSoftDeletedLimitUsageTimeBasedAlertRuleMicFamilyString() {
        return "{"
            + "\"id\":\"rule5\","
            + "\"version\":0,"
            + "\"kerberos\":\"liyiyi\","
            + "\"message\":\"Limit usage time-based alert micFamily\","
            + "\"enabled\":true,"
            + "\"softDelete\":true,"
            + "\"accountId\":[\"04860800\",\"04860801\"],"
            + "\"micFamily\":[\"SFX\"],"
            + "\"limitUsageAlertTime\":\"10:20\","
            + "\"limitUsageAlertTimezone\":\"Asia/Hong_Kong\""
            + "}";
    }

    public static String getAlertRuleBreachingMessage() {
        return "ClientRefId [04860800] with GMI [DCI03456] has reached margin usage percentage of [51.0%] "
            + "(margin usage [51.0 USD], margin limit [100.0 USD]). This has breached alerting threshold at [>50.0%]. "
            + "Please take necessary actions.";
    }

    public static String getTimeBasedAlertRuleBreachingMessage(int testCase) {
        switch (testCase) {
            case 1:
                return "<html><body>Please find below margin usage details for accounts [04860800, 55300017] on 20250120 10:20 Asia/Hong_Kong.<br/>"
                    + "Note: These data do not contain any manual override done during the trading day.<br/>"
                    + "<br><table style=\"border-collapse: collapse; width: 100%;\" border=\"1\">"
                    + "<tr><td style=\"text-align:center\"><strong>Account Id</strong></td><td style=\"text-align:center\"><strong>Margin Usage</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Limit</strong></td><td style=\"text-align:center\"><strong>Currency</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Usage Percentage</strong></td></tr>"
                    + "<tr><td style=\"text-align:center\">04860800</td><td style=\"text-align:center\">51.0</td><td style=\"text-align:center\">100.0</td>"
                    + "<td style=\"text-align:center\">USD</td><td style=\"text-align:center\">51.0%</td></tr>"
                    + "<tr><td style=\"text-align:center\">55300017</td><td style=\"text-align:center\">70.0</td><td style=\"text-align:center\">100.0</td>"
                    + "<td style=\"text-align:center\">USD</td><td style=\"text-align:center\">70.0%</td></tr></table></body></html>";
            case 2:
                return "<html><body>Please find below margin usage details for accounts [04860800, 04860801] on 20250120 10:20 Asia/Hong_Kong.<br/>"
                    + "Note: These data do not contain any manual override done during the trading day.<br/>"
                    + "<br><table style=\"border-collapse: collapse; width: 100%;\" border=\"1\">"
                    + "<tr><td style=\"text-align:center\"><strong>Account Id</strong></td><td style=\"text-align:center\"><strong>Margin Usage</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Limit</strong></td><td style=\"text-align:center\"><strong>Currency</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Usage Percentage</strong></td></tr>"
                    + "<tr><td style=\"text-align:center\">04860800</td><td style=\"text-align:center\">51.0</td><td style=\"text-align:center\">100.0</td>"
                    + "<td style=\"text-align:center\">USD</td><td style=\"text-align:center\">51.0%</td></tr></table></body></html>";
            case 3:
            case 4:
            case 5:
                return null;
            default:
                return "<html><body>Please find below margin usage details for accounts [04860800] on 20250120 10:20 Asia/Hong_Kong.<br/>"
                    + "Note: These data do not contain any manual override done during the trading day.<br/>"
                    + "<br><table style=\"border-collapse: collapse; width: 100%;\" border=\"1\">"
                    + "<tr><td style=\"text-align:center\"><strong>Account Id</strong></td><td style=\"text-align:center\"><strong>Margin Usage</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Limit</strong></td><td style=\"text-align:center\"><strong>Currency</strong></td>"
                    + "<td style=\"text-align:center\"><strong>Margin Usage Percentage</strong></td></tr>"
                    + "<tr><td style=\"text-align:center\">04860800</td><td style=\"text-align:center\">51.0</td><td style=\"text-align:center\">100.0</td>"
                    + "<td style=\"text-align:center\">USD</td><td style=\"text-align:center\">51.0%</td></tr></table></body></html>";
        }
    }
}
