package com.xx.futures.evetor.alert.generator.rules.limitusage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsage;
import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsageAlert;
import static com.xx.futures.evetor.utils.TestUtils.generateLimitUsageAlertRule;
import static com.xx.futures.evetor.utils.TestUtils.generateMicFamilyLimitUsage;
import static com.xx.futures.evetor.utils.TestUtils.getAlertRuleBreachingMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void testShouldAlertAndProcess_ruleSatisfiedWithMicFamilyLimitUsage() {
        // 中文注释：覆盖 MICFamily selector 的 threshold 匹配，实时事件只要 micFamily 命中规则即可触发。
        rule = buildMicFamilyRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));
        Mockito.doReturn(1L).when(limitUsageRule).getTimestamp();

        ClearingData.LimitUsage limitUsage = generateMicFamilyLimitUsage("SFX", "DCI03456", "04860800", 51d);
        assertTrue(limitUsageRule.shouldAlert(limitUsage));
        assertNotNull(limitUsageRule.process(limitUsage));
    }

    @Test
    void testGetVenueSelectorType_ruleUsesMicFamily() {
        // 中文注释：覆盖新增 selector 类型切换，MICFamily 规则应走 MICFamily 分支。
        rule = buildMicFamilyRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        assertEquals("MICFamily", limitUsageRule.getVenueSelectorType());
    }

    @Test
    void testMatchesVenueSelector_ruleUsesMicFamilySelector() {
        // 中文注释：MICFamily 规则在实时路径应直接读取 limitUsage.micFamily 与规则 selector 比较。
        rule = buildMicFamilyRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        ClearingData.LimitUsage matchingLimitUsage = generateMicFamilyLimitUsage("SFX", "DCI03456", "04860800", 51d);
        ClearingData.LimitUsage nonMatchingLimitUsage =
            generateMicFamilyLimitUsage("NON_SFX", "DCI03456", "04860800", 51d);

        assertTrue(limitUsageRule.matchesVenueSelector(matchingLimitUsage));
        assertFalse(limitUsageRule.matchesVenueSelector(nonMatchingLimitUsage));
    }

    @Test
    void testGetAlertRuleBreachingMessage() {
        assertEquals(
            getAlertRuleBreachingMessage(),
            limitUsageRule.getAlertRuleBreachingMessage(generateLimitUsage())
        );
    }

    @Test
    void testTimeBasedHelpers() {
        // 中文注释：保留仍然纯属 rule metadata 的 helper，time-based alert build 链则回收到 Source。
        rule = buildMicFamilyTimeBasedRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        assertEquals("04860801,04860800", limitUsageRule.getAccountsString());
        assertEquals("10:20 Asia/Hong_Kong", limitUsageRule.getTimeToTriggerForRule());
    }

    // 中文注释：MICFamily 相关 helper 只为本轮 selector 改造补测试数据，保持原有用例构造不变。
    private AlertRule buildMicFamilyRule() {
        return new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule())
            .setVenue(null)
            .setMicFamily(new java.util.HashSet<>(java.util.Collections.singleton("SFX")))
            .build();
    }

    private AlertRule buildMicFamilyTimeBasedRule() {
        return new AlertRule.AlertRuleBuilder(
            com.xx.futures.evetor.utils.TestUtils.generateLimitUsageTimeBasedAlertRule()
        )
            .setVenue(null)
            .setMicFamily(new java.util.HashSet<>(java.util.Collections.singleton("SFX")))
            .build();
    }
}
