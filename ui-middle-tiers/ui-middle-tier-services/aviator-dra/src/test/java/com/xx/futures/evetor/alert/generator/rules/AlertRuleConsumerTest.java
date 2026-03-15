package com.xx.futures.evetor.alert.generator.rules;

public class AlertRuleConsumerTest {

    private AlertRuleCache alertRulesCache;
    private AlertRuleConsumer alertRuleConsumer;
    private NamedProperties properties;
    private CustomAlertSource customAlertSource;
    private LimitUsageAlertSource limitUsageAlertSource;

    @BeforeEach
    public void setup() {
        this.properties = new NamedProperties();
        this.customAlertSource = mock(CustomAlertSource.class);
        this.limitUsageAlertSource = mock(LimitUsageAlertSource.class);
        this.alertRulesCache = new AlertRuleCacheImpl(mock(AlertRuleExpirePredicate.class), properties);
        this.alertRuleConsumer = new AlertRuleConsumer(
            mock(AlertRuleSource.class),
            alertRulesCache,
            Optional.of(customAlertSource),
            Optional.of(limitUsageAlertSource)
        );
    }

    @Test
    void testNoConsume_notAlertRule() throws DataConsumptionException {
        assertTrue(alertRulesCache.ruleList().isEmpty());

        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.Account)
            .build();
        alertRuleConsumer.consume(alert);

        assertTrue(alertRulesCache.ruleList().isEmpty());
    }

    @Test
    void testConsume_addAndRemoveCustomRule() throws DataConsumptionException, JsonProcessingException {
        // test custom rule is added
        String ruleStr = getAlertRuleString();
        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.AlertRule)
            .setNotificationContent(ruleStr)
            .build();
        AlertRule rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        assertEquals(1, alertRulesCache.ruleList().size());
        assertEquals(rule, alertRulesCache.findRule("rule1"));

        // test custom rule is removed
        ruleStr = getSoftDeletedAlertRuleString();
        alert = Coverage.NotificationAlert.newBuilder(alert).setNotificationContent(ruleStr).build();
        rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        assertEquals(0, alertRulesCache.ruleList().size());
        assertNull(alertRulesCache.findRule("rule1"));
    }

    @Test
    void testConsume_addAndRemoveLimitUsageRule_thresholdBased()
        throws DataConsumptionException, JsonProcessingException {
        String ruleStr = getLimitUsageAlertRuleString();
        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.AlertRule)
            .setNotificationContent(ruleStr)
            .build();
        AlertRule rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        assertEquals(1, alertRulesCache.ruleList().size());
        assertEquals(rule, alertRulesCache.findRule("rule2"));

        ruleStr = getSoftDeletedLimitUsageAlertRuleString();
        alert = Coverage.NotificationAlert.newBuilder(alert).setNotificationContent(ruleStr).build();
        rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        assertEquals(0, alertRulesCache.ruleList().size());
        assertNull(alertRulesCache.findRule("rule2"));
    }

    @Test
    void testConsume_addAndRemoveLimitUsageRule_timeBased()
        throws DataConsumptionException, JsonProcessingException {
        String ruleStr = getLimitUsageTimeBasedAlertRuleString();
        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.AlertRule)
            .setNotificationContent(ruleStr)
            .build();
        AlertRule rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        assertEquals(1, alertRulesCache.ruleList().size());
        assertEquals(rule, alertRulesCache.findRule("rule3"));

        ruleStr = getSoftDeletedLimitUsageTimeBasedAlertRuleString();
        alert = Coverage.NotificationAlert.newBuilder(alert).setNotificationContent(ruleStr).build();
        rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        assertEquals(0, alertRulesCache.ruleList().size());
        assertNull(alertRulesCache.findRule("rule3"));
    }

    @Test
    void testConsume_addAndRemoveLimitUsageRule_thresholdBasedMicFamily()
        throws DataConsumptionException, JsonProcessingException {
        // 中文注释：覆盖新增 MICFamily payload 通过 consumer -> cache -> limit usage source 的透传链路。
        String ruleStr = getLimitUsageAlertRuleMicFamilyString();
        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.AlertRule)
            .setNotificationContent(ruleStr)
            .build();
        AlertRule rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        assertEquals(1, alertRulesCache.ruleList().size());
        assertEquals(rule, alertRulesCache.findRule("rule4"));

        ruleStr = getSoftDeletedLimitUsageAlertRuleMicFamilyString();
        alert = Coverage.NotificationAlert.newBuilder(alert).setNotificationContent(ruleStr).build();
        rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        assertEquals(0, alertRulesCache.ruleList().size());
        assertNull(alertRulesCache.findRule("rule4"));
    }

    @Test
    void testConsume_addAndRemoveLimitUsageRule_timeBasedMicFamily()
        throws DataConsumptionException, JsonProcessingException {
        // 中文注释：覆盖 time-based 的 MICFamily payload 也能沿用同一条 consumer 装载链路。
        String ruleStr = getLimitUsageTimeBasedAlertRuleMicFamilyString();
        Coverage.NotificationAlert alert = Coverage.NotificationAlert.newBuilder()
            .setNotificationType(Coverage.NotificationAlert.NotificationType.AlertRule)
            .setNotificationContent(ruleStr)
            .build();
        AlertRule rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processNewAlertRule(rule);
        assertEquals(1, alertRulesCache.ruleList().size());
        assertEquals(rule, alertRulesCache.findRule("rule5"));

        ruleStr = getSoftDeletedLimitUsageTimeBasedAlertRuleMicFamilyString();
        alert = Coverage.NotificationAlert.newBuilder(alert).setNotificationContent(ruleStr).build();
        rule = new ObjectMapper().readValue(ruleStr, AlertRule.class);

        alertRuleConsumer.consume(alert);

        Mockito.verify(customAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        Mockito.verify(limitUsageAlertSource, Mockito.times(1)).processRemoveAlertRule(rule);
        assertEquals(0, alertRulesCache.ruleList().size());
        assertNull(alertRulesCache.findRule("rule5"));
    }

    // 中文注释：这些 helper 只补 MICFamily 规则 payload，避免去改已有测试数据生成方式。
    private String getLimitUsageAlertRuleMicFamilyString() {
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

    private String getSoftDeletedLimitUsageAlertRuleMicFamilyString() {
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

    private String getLimitUsageTimeBasedAlertRuleMicFamilyString() {
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

    private String getSoftDeletedLimitUsageTimeBasedAlertRuleMicFamilyString() {
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
}
