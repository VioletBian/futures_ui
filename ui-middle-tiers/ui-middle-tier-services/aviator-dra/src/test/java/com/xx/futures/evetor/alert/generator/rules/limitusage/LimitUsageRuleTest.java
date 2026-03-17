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
    void testGetVenueSelectorType_ruleUsesMicFamily() {
        // 中文注释：覆盖新增 selector 类型切换，MICFamily 规则应走 MICFamily 分支。
        rule = buildMicFamilyRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        assertEquals("MICFamily", limitUsageRule.getVenueSelectorType());
        assertEquals(new HashSet<>(Collections.singleton("SFX")), limitUsageRule.getVenueSelectorValues());
    }

    @Test
    void testFilterMatchingLimitUsages_ruleUsesMicFamilySelector() {
        // 中文注释：覆盖 time-based 共享 selector 逻辑，MICFamily 规则只保留命中的快照行。
        rule = buildMicFamilyRule();
        limitUsageRule = Mockito.spy(new LimitUsageRule(rule, application, activeAlerts));

        Map<String, Object> limitUsageMap = new HashMap<>();
        limitUsageMap.put("04860800", buildLimitUsageSnapshot("SFX"));
        limitUsageMap.put("04860801", buildLimitUsageSnapshot("NON_SFX"));

        Map<String, Object> filteredLimitUsages = limitUsageRule.filterMatchingLimitUsages(limitUsageMap);
        assertEquals(1, filteredLimitUsages.size());
        assertTrue(filteredLimitUsages.containsKey("04860800"));
        assertFalse(filteredLimitUsages.containsKey("04860801"));
        assertTrue(limitUsageRule.matchesVenueSelector(filteredLimitUsages.get("04860800")));
        assertFalse(limitUsageRule.matchesVenueSelector(limitUsageMap.get("04860801")));
    }

    @Test
    void testGetAlertRuleBreachingMessage() {
        assertEquals(
            getAlertRuleBreachingMessage(),
            limitUsageRule.getAlertRuleBreachingMessage(generateLimitUsage())
        );
    }

    // 中文注释：MICFamily 相关 helper 只为本轮 selector 改造补测试数据，保持原有用例构造不变。
    private AlertRule buildMicFamilyRule() {
        return new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule())
            .setVenue(null)
            .setMicFamily("SFX")
            .build();
    }

    private Map<String, Object> buildLimitUsageSnapshot(String micFamily) {
        Map<String, Object> limitUsageSnapshot = new HashMap<>();
        limitUsageSnapshot.put("usage", 11495905.20272978);
        limitUsageSnapshot.put("limit", 16167814.417355172);
        limitUsageSnapshot.put("currency", "USD");
        limitUsageSnapshot.put("mic", "XZCE");
        limitUsageSnapshot.put("micFamily", micFamily);
        return limitUsageSnapshot;
    }
}
