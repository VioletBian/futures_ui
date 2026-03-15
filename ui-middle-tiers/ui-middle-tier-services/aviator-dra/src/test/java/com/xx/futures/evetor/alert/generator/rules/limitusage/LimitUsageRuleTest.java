package com.xx.futures.evetor.alert.generator.rules.limitusage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsage;
import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsageAlert;
import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsageAlertRule;
import static com.xx.futures.evetor.utils.TestUtils.getAlertRuleBreachingMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LimitUsageRuleTest {

    private AlertRule rule;
    private ActiveAlerts activeAlerts;
    private Common.Application application;
    private LimitUsageRule limitUsageRule;

    @BeforeEach
    void setup() {
        rule = generateLimitUsageAlertRule();
        application = Common.Application.newBuilder()
            .setAppName(Common.Application.AppName.Aviator)
            .setInstanceId("junit")
            .build();
        activeAlerts = new ActiveAlerts();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));
        Mockito.doReturn(1L).when(limitUsageRule).getTimestamp();
    }

    @Test
    void testShouldAlertAndProcess_ruleNotSatisfied() {
        ClearingData.LimitUsage limitUsage = generateLimitUsage("XDCE", "DCI03456", "04860800", 50);
        assertFalse(limitUsageRule.shouldAlert(limitUsage));
        assertNull(limitUsageRule.process(limitUsage));

        limitUsage = generateLimitUsage("XINE", "JNT03456", "14860800", 51);
        assertFalse(limitUsageRule.shouldAlert(limitUsage));
        assertNull(limitUsageRule.process(limitUsage));
    }

    @Test
    void testShouldAlertAndProcess_ruleSatisfiedWithAccount() {
        ClearingData.LimitUsage limitUsage = generateLimitUsage();
        assertTrue(limitUsageRule.shouldAlert(limitUsage));

        UnpublishedAlert alert = limitUsageRule.process(limitUsage);
        assertEquals(
            generateLimitUsageAlert(
                application,
                rule,
                "04860800-DCI03456-0-RuleIdrule1",
                getAlertRuleBreachingMessage()
            ),
            alert.getAlert()
        );

        activeAlerts.addLimitUsageAlert(alert.getAlert());
        String alertId = alert.getAlert().getAlertId();
        assertTrue(activeAlerts.hasActiveLimitUsageAlert(alertId));
        assertEquals(new SerializedAlert(alert.getAlert()), activeAlerts.getLimitUsageAlertById(alertId));

        // Should not process as same account should only be alerted by same rule once
        limitUsage = generateLimitUsage("XDCE", "DCI03456", "04860800", 52);
        assertFalse(limitUsageRule.shouldAlert(limitUsage));
        assertNull(limitUsageRule.process(limitUsage));
    }

    @Test
    void testShouldAlertAndProcess_ruleSatisfiedWithoutAccount() {
        rule = new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule()).setAccountId(null).build();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        ClearingData.LimitUsage limitUsage = generateLimitUsage();
        assertTrue(limitUsageRule.shouldAlert(limitUsage));

        UnpublishedAlert alert = limitUsageRule.process(limitUsage);
        activeAlerts.addLimitUsageAlert(alert.getAlert());

        limitUsage = generateLimitUsage("XDCE", "DCI03456", "04860800", 52);
        assertFalse(limitUsageRule.shouldAlert(limitUsage));
        assertNull(limitUsageRule.process(limitUsage));
    }

    @Test
    void testGetAlertRuleBreachingMessage() {
        assertEquals(
            getAlertRuleBreachingMessage(),
            limitUsageRule.getAlertRuleBreachingMessage(generateLimitUsage())
        );
    }
}
