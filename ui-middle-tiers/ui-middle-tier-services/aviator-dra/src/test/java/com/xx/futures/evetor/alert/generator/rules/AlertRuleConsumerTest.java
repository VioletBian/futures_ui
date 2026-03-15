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

}
