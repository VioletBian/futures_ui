package com.xx.jetstream.model.alert;

import static com.xx.jetstream.model.alert.StatComparison.ComparisonType.Of;
import static com.xx.jetstream.model.alert.StatComparison.ComparisonType.Value;
import static com.xx.jetstream.model.alert.TimeComparison.ComparisonType.ToEnd;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class AlertRuleTest {
    @Test
    public void testAddingInteractionLevelToExistingBeanGivesExpectedUndefinedResultForOldJSON() throws Exception {
        String oldJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"creatorId\": \"tudorambr_fix\","
            + "\"tradingAlgorithm\": \"ImplementationShortfall\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(oldJson, AlertRule.class);

        assertNull(rule.getInteractionLevel());

        assertEquals("AV5b4IasYwhu7r3z64iz", rule.getId());
        assertEquals(0, rule.getVersion());
        assertEquals("middlm", rule.getKerberos());
        assertEquals("IS Custom Alert", rule.getMessage());
        assertTrue(rule.getEnabled());
        assertTrue(rule.getCreatorId().contains("tudorambr_fix"));
        assertEquals(1, rule.getCreatorId().size());
        assertTrue(rule.getTradingAlgorithm().contains(Order.Type.ImplementationShortfall));
        assertEquals(1, rule.getTradingAlgorithm().size());
    }

    @Test
    public void testClaimedTTParentCanBeMapped() throws Exception {
        HierarchyType orderLevel = HierarchyType.EMS_PARENT;

        String newJson = "{"
            + "\"id\": \"alert-id-59\","
            + "\"version\": 8,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"test unClaimed\","
            + "\"enabled\": true,"
            + "\"orderLevel\": \"" + orderLevel + "\","
            + "\"isClaimed\": false,"
            + "\"activationComparison\": {\n"
            + "\"comparisonType\": \"Pause\",\n"
            + "\"micros\": 0\n"
            + "}\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals(orderLevel, rule.getOrderLevel());
        assertEquals(false, rule.getIsClaimed());
        assertEquals(0, rule.getActivationComparison().getMicros());
        assertEquals(TimeComparison.ComparisonType.Pause, rule.getActivationComparison().getComparisonType());
    }

    @Test
    public void testHighTouchLowTouchCanBeMapped() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"interactionLevel\": \"HighTouch\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals(Order.InteractionLevel.HighTouch, rule.getInteractionLevel());
    }

    @Test
    public void testActorsSetCanBeMapped() throws Exception {
        String json = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"actorsSet\": [\"rathul\", \"moojor\"]"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(json, AlertRule.class);

        assertEquals(2, rule.getActorsSet().size());
        assertTrue(rule.getActorsSet().contains("rathul"));
        assertTrue(rule.getActorsSet().contains("moojor"));
    }

    @Test
    public void testEmailAddressCanBeMapped() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("mr.gs@gs.com", rule.getEmailAddress());
    }

    @Test
    public void testSetters() {
        AlertRule.AlertRuleBuilder builder = new AlertRule.AlertRuleBuilder();
        builder.setQuantity(new StatComparison<Double>().setValue(50.1).setOperator(">"));
        builder.setSourceChannel("X");
        builder.setHasLimit(true);
        builder.setWholesaleIndicatorType(Order.WholesaleIndicatorType.Block);
        builder.setSettleAsFixed(true);
        builder.setSynthetic(false);

        AlertRule alertRule = builder.build();

        assertEquals(50.1, alertRule.getQuantity().getValue());
        assertEquals(">", alertRule.getQuantity().getOperator());
        assertEquals("X", alertRule.getSourceChannel());
        assertEquals(true, alertRule.getHasLimit());
        assertEquals("Block", alertRule.getWholesaleIndicatorType().toString());
        assertEquals(true, alertRule.getSettleAsFixed());
        assertEquals(false, alertRule.getSynthetic());
    }

    @Test
    public void testImmutability() throws Exception {
        AlertRule.AlertRuleBuilder builder = new AlertRule.AlertRuleBuilder().setId("abc").setKerberos("def");

        AlertRule rule1 = builder.build();
        AlertRule rule2 = builder.build();
        AlertRule rule3 = rule2.toBuilder().setKerberos("ghi").build();
        AlertRule rule4 = builder.setKerberos("ghi").build();

        assertAll(
            () -> assertNotSame(rule1, rule2, "Build should return new instances, never the same instance (1 and 2)"),
            () -> assertNotSame(rule2, rule3, "Build should return new instances, never the same instance (2 and 3)"),
            () -> assertNotSame(rule3, rule4, "Build should return new instances, never the same instance (3 and 4)"),
            () -> assertEquals(rule1, rule2, "Expected the rules to be equal despite not being the same instance (1 and 2)"),
            () -> assertEquals(rule3, rule4, "Expected the rules to be equal despite not being the same instance (3 and 4)"),
            () -> assertEquals("abc", rule2.getId(), "Expected ID to be correct (2)"),
            () -> assertEquals("abc", rule4.getId(), "Expected ID to be correct (4)"),
            () -> assertEquals("def", rule2.getKerberos(), "Expected Kerberos to be correct (2)"),
            () -> assertEquals("ghi", rule4.getKerberos(), "Expected Kerberos to be correct (4)")
        );
    }

    @Test
    public void testAlgoComparisonStats() throws Exception {
        AlertRule expectedRule = new AlertRule.AlertRuleBuilder()
            .setId("mooj1")
            .setTradingAlgorithm(new HashSet<>(Collections.singletonList(Order.Type.Closer)))
            .setParticipationRateComparison(new StatComparison<Double>()
                .setOperator(">")
                .setValue(12.34d)
                .setComparisonType(Value))
            .setAheadBehindComparison(new StatComparison<Double>()
                .setValue(10d)
                .setOperator("=")
                .setComparisonType(Value))
            .setExecutionScheduleComparison(new StatComparison<Double>()
                .setValue(1d)
                .setOperator("<=")
                .setComparisonType(Of))
            .setActivationComparison(new TimeComparison()
                .setComparisonType(ToEnd)
                .setMicros(9876L))
            .setAutoExecutionSchedule(true)
            .build();

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedRule);
        AlertRule actualRule = mapper.readValue(expectedJson, AlertRule.class);
        String actualJson = mapper.writeValueAsString(actualRule);

        assertAll(
            () -> assertEquals(expectedJson, actualJson,
                "Expected the JSON of serialization and serialization of de-serialized JSON to match."),
            () -> assertEquals(expectedRule, actualRule,
                "Expected the equals method to evaluate true for expected and actual rule."),
            () -> assertEquals(expectedRule.toString(), actualRule.toString()),
            () -> assertTrue(actualRule.isAlgoRule(), "Expected rule to be identified as an algo rule"),
            () -> assertEquals(12.34d, actualRule.getParticipationRateComparison().getValue()),
            () -> assertEquals(">", actualRule.getParticipationRateComparison().getOperator()),
            () -> assertEquals(Value, actualRule.getParticipationRateComparison().getComparisonType()),
            () -> assertEquals(10d, actualRule.getAheadBehindComparison().getValue()),
            () -> assertEquals("=", actualRule.getAheadBehindComparison().getOperator()),
            () -> assertEquals(Value, actualRule.getAheadBehindComparison().getComparisonType()),
            () -> assertEquals(1d, actualRule.getExecutionScheduleComparison().getValue()),
            () -> assertEquals("<=", actualRule.getExecutionScheduleComparison().getOperator()),
            () -> assertEquals(Of, actualRule.getExecutionScheduleComparison().getComparisonType()),
            () -> assertEquals(ToEnd, actualRule.getActivationComparison().getComparisonType()),
            () -> assertEquals(9876L, actualRule.getActivationComparison().getMicros()),
            () -> assertTrue(actualRule.getAutoExecutionSchedule())
        );
    }

    @Test
    public void missingComparisonTypesShouldReturnNull() throws JsonProcessingException {
        String badJson = "{"
            + "\"participationRateComparison\": {\n"
            + "\"operator\": \">\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"aheadBehindComparison\": {\n"
            + "\"operator\": \">\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"executionScheduleComparison\": {\n"
            + "\"operator\": \">\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"activationComparison\": {\n"
            + "\"micros\": 0\n"
            + "}\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();
        AlertRule rule = mapper.readValue(badJson, AlertRule.class);
        assertNull(rule.getActivationComparison());
        assertNull(rule.getParticipationRateComparison());
        assertNull(rule.getAheadBehindComparison());
        assertNull(rule.getExecutionScheduleComparison());
        assertFalse(rule.isAlgoRule());
    }

    @Test
    public void missingValueForComparisonShouldReturnNull() throws JsonProcessingException {
        String badJson = "{"
            + "\"participationRateComparison\": {\n"
            + "\"comparisonType\": \"Of\",\n"
            + "\"operator\": \">\"\n"
            + "},\n"
            + "\"aheadBehindComparison\": {\n"
            + "\"comparisonType\": \"Of\",\n"
            + "\"operator\": \">\"\n"
            + "},\n"
            + "\"executionScheduleComparison\": {\n"
            + "\"comparisonType\": \"Of\",\n"
            + "\"operator\": \">\"\n"
            + "},\n"
            + "\"activationComparison\": {\n"
            + "\"comparisonType\": \"After\"\n"
            + "}\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();
        AlertRule rule = mapper.readValue(badJson, AlertRule.class);
        assertNull(rule.getActivationComparison());
        assertNull(rule.getParticipationRateComparison());
        assertNull(rule.getAheadBehindComparison());
        assertNull(rule.getExecutionScheduleComparison());
        assertFalse(rule.isAlgoRule());
    }

    @Test
    public void missingOperatorShouldReturnNull() throws JsonProcessingException {
        String badJson = "{"
            + "\"participationRateComparison\": {\n"
            + "\"comparisonType\": \"Value\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"aheadBehindComparison\": {\n"
            + "\"comparisonType\": \"Value\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"executionScheduleComparison\": {\n"
            + "\"comparisonType\": \"Of\",\n"
            + "\"value\": 90\n"
            + "},\n"
            + "\"activationComparison\": {\n"
            + "\"comparisonType\": \"After\",\n"
            + "\"micros\": 0\n"
            + "}\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();
        AlertRule rule = mapper.readValue(badJson, AlertRule.class);
        assertNull(rule.getActivationComparison());
        assertNull(rule.getParticipationRateComparison());
        assertNull(rule.getAheadBehindComparison());
        assertNull(rule.getExecutionScheduleComparison());
        assertFalse(rule.isAlgoRule());
    }

    @Test
    public void testComparisonModeSetters() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"AlgoCancel\","
            + "\"instrumentSymbol\": \"EMINI\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals(AlertFieldComparisonMode.Modes.CONTAINS_CASE_INSENSITIVE, rule.getExplanationComparisonMode());
        assertEquals(AlertFieldComparisonMode.Modes.UNDEFINED, rule.getInstrumentSymbolComparisonMode());
        assertEquals("AlgoCancel", rule.getExplanation());
        assertEquals("AlgoCancel", rule.getParsedExplanation());
        assertEquals("EMINI", rule.getInstrumentSymbol());
        assertEquals("EMINI", rule.getParsedInstrumentSymbol());
    }

    @Test
    public void testExplanationComparisonModeSettersWithSingleCharValue() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"~\","
            + "\"instrumentSymbol\": \"~EMINI\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals(AlertFieldComparisonMode.Modes.UNDEFINED, rule.getExplanationComparisonMode());
        assertEquals(AlertFieldComparisonMode.Modes.CONTAINS_CASE_INSENSITIVE, rule.getInstrumentSymbolComparisonMode());
        assertEquals("~", rule.getExplanation());
        assertEquals("~", rule.getParsedExplanation());
        assertEquals("~EMINI", rule.getInstrumentSymbol());
        assertEquals("EMINI", rule.getParsedInstrumentSymbol());
    }

    @Test
    public void testInstrumentSymbolComparisonModeSettersWithSingleCharValue() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"~UserCancel\","
            + "\"instrumentSymbol\": \"E\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals(AlertFieldComparisonMode.Modes.CONTAINS_CASE_INSENSITIVE, rule.getExplanationComparisonMode());
        assertEquals(AlertFieldComparisonMode.Modes.UNDEFINED, rule.getInstrumentSymbolComparisonMode());
        assertEquals("~UserCancel", rule.getExplanation());
        assertEquals("UserCancel", rule.getParsedExplanation());
        assertEquals("E", rule.getInstrumentSymbol());
        assertEquals("E", rule.getParsedInstrumentSymbol());
    }

    @Test
    public void testUnparsedInstrumentSymbolAndExplanation() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"UserCancel\","
            + "\"instrumentSymbol\": \"EMINI\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("UserCancel", rule.getParsedExplanation());
        assertEquals("EMINI", rule.getParsedInstrumentSymbol());
    }

    @Test
    public void testUnparsedInstrumentSymbolAndExplanationWithTilda() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"~UserCancel\","
            + "\"instrumentSymbol\": \"~EMINI\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("UserCancel", rule.getParsedExplanation());
        assertEquals("~UserCancel", rule.getExplanation());
        assertEquals("EMINI", rule.getParsedInstrumentSymbol());
        assertEquals("~EMINI", rule.getInstrumentSymbol());

        String reSerialized = mapper.writeValueAsString(rule);
        assertFalse(reSerialized.contains("parsedInstrumentSymbol"));
        assertFalse(reSerialized.contains("parsedExplanation"));
        assertFalse(reSerialized.contains("instrumentSymbolComparisonMode"));
        assertFalse(reSerialized.contains("explanationComparisonMode"));
    }

    @Test
    public void testComparisonModeMultipleSerialization() throws Exception {
        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"explanation\": \"~UserCancel\","
            + "\"instrumentSymbol\": \"EMINI\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("UserCancel", rule.getParsedExplanation());
        assertEquals("~UserCancel", rule.getExplanation());
        assertEquals(AlertFieldComparisonMode.Modes.CONTAINS_CASE_INSENSITIVE, rule.getExplanationComparisonMode());
        assertEquals("EMINI", rule.getParsedInstrumentSymbol());
        assertEquals("EMINI", rule.getInstrumentSymbol());
        assertEquals(AlertFieldComparisonMode.Modes.UNDEFINED, rule.getInstrumentSymbolComparisonMode());

        String reSerialized = mapper.writeValueAsString(rule);
        assertFalse(reSerialized.contains("parsedInstrumentSymbol"));
        assertFalse(reSerialized.contains("parsedExplanation"));
        assertFalse(reSerialized.contains("instrumentSymbolComparisonMode"));
        assertFalse(reSerialized.contains("explanationComparisonMode"));
    }

    @Test
    public void testHashSetValuesWithStrings() throws Exception {
        String customerTrader = "moojor";
        String creatorId = "pavira";
        String venue = "CME";
        String accountId = "C09834";
        Order.Type tradingAlgorithm = Order.Type.VWAP;
        Order.Status orderStatus = Order.Status.Filled;

        String newJson = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"customerTrader\": \"" + customerTrader + "\","
            + "\"creatorId\": \"" + creatorId + "\","
            + "\"venue\": \"" + venue + "\","
            + "\"accountId\": \"" + accountId + "\","
            + "\"tradingAlgorithm\": \"" + tradingAlgorithm + "\","
            + "\"orderStatus\": \"" + orderStatus + "\""
            + "}";

        ObjectMapper mapper = new ObjectMapper();
        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        for (int i = 0; i < 3; i++) {
            assertTrue(rule.getCustomerTrader().contains(customerTrader));
            assertTrue(rule.getCreatorId().contains(creatorId));
            assertTrue(rule.getVenue().contains(venue));
            assertTrue(rule.getAccountId().contains(accountId));
            assertTrue(rule.getTradingAlgorithm().contains(tradingAlgorithm));
            assertTrue(rule.getOrderStatus().contains(new PrefixEnum<>(orderStatus.toString())));

            String reSerializedJson = mapper.writeValueAsString(rule);
            rule = mapper.readValue(reSerializedJson, AlertRule.class);
        }
    }

    @Test
    public void testEqualityOfRulesMadeFromJsonArraysAndJsonStrings() throws JsonProcessingException {
        String jsonWithStrings = "{\n"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\",\n"
            + "\"version\": 0,\n"
            + "\"kerberos\": \"middlm\",\n"
            + "\"message\": \"IS Custom Alert\",\n"
            + "\"enabled\": true,\n"
            + "\"emailAddress\": \"mr.gs@gs.com\",\n"
            + "\"customerTrader\": \"moojor\",\n"
            + "\"creatorId\": \"pavira\",\n"
            + "\"venue\": \"CME\",\n"
            + "\"accountId\": \"C09834\",\n"
            + "\"tradingAlgorithm\": \"VWAP\",\n"
            + "\"orderStatus\": \"Filled\"\n"
            + "}";

        String jsonWithArrays = "{\n"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\",\n"
            + "\"version\": 0,\n"
            + "\"kerberos\": \"middlm\",\n"
            + "\"message\": \"IS Custom Alert\",\n"
            + "\"enabled\": true,\n"
            + "\"creatorId\": [\n"
            + "\"pavira\"\n"
            + "],\n"
            + "\"customerTrader\": [\n"
            + "\"moojor\"\n"
            + "],\n"
            + "\"accountId\": [\n"
            + "\"C09834\"\n"
            + "],\n"
            + "\"tradingAlgorithm\": [\n"
            + "\"VWAP\"\n"
            + "],\n"
            + "\"orderStatus\": [\n"
            + "\"Filled\"\n"
            + "],\n"
            + "\"venue\": [\n"
            + "\"CME\"\n"
            + "],\n"
            + "\"symphonyEnabled\": false,\n"
            + "\"audioEnabled\": false,\n"
            + "\"softDelete\": false,\n"
            + "\"recapEmail\": false,\n"
            + "\"genericEmail\": false,\n"
            + "\"clientRecapEmail\": false,\n"
            + "\"hasHistoricalClientRecapEmail\": false,\n"
            + "\"sendOrderDetailsInSymphony\": false,\n"
            + "\"emailAddress\": \"mr.gs@gs.com\"\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule ruleFromStrings = mapper.readValue(jsonWithStrings, AlertRule.class);
        AlertRule ruleFromArrays = mapper.readValue(jsonWithArrays, AlertRule.class);

        assertEquals(ruleFromArrays, ruleFromStrings);
    }

    @Test
    public void testLockInHashSetParsingBehaviour() throws Exception {
        String jsonRule = "{"
            + "\"id\": \"AV5b4IasYwhu7r3z64iz\","
            + "\"version\": 0,"
            + "\"kerberos\": \"middlm\","
            + "\"message\": \"IS Custom Alert\","
            + "\"enabled\": true,"
            + "\"emailAddress\": \"mr.gs@gs.com\","
            + "\"customerTrader\": [\"moojor\"],"
            + "\"accountId\": [\"C09834\"],"
            + "\"creatorId\": []"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(jsonRule, AlertRule.class);

        for (int i = 0; i < 3; i++) {
            assertTrue(rule.getCustomerTrader().contains("moojor"));
            assertEquals(1, rule.getCustomerTrader().size());
            assertTrue(rule.getAccountId().contains("C09834"));
            assertEquals(1, rule.getAccountId().size());
            assertEquals(0, rule.getCreatorId().size());
            assertNull(rule.getOrderStatus());

            String reSerializedJson = mapper.writeValueAsString(rule);
            rule = mapper.readValue(reSerializedJson, AlertRule.class);
        }
    }

    @Test
    public void testResolvableAndSnoozable() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = new AlertRule.AlertRuleBuilder().build();
        assertNull(rule.isResolvable());
        assertNull(rule.isSnoozable());

        rule = mapper.readValue("{\"resolvable\": true, \"snoozable\": true}", AlertRule.class);
        assertTrue(rule.isResolvable());
        assertTrue(rule.isSnoozable());

        rule = mapper.readValue("{\"resolvable\": false, \"snoozable\": false}", AlertRule.class);
        assertFalse(rule.isResolvable());
        assertFalse(rule.isSnoozable());
    }

    @Test
    public void testCancelReason() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = new AlertRule.AlertRuleBuilder().build();
        assertNull(rule.getCancelReason());

        rule = mapper.readValue("{\"cancelReason\": \"InternalSMPCancelReason\"}", AlertRule.class);
        assertEquals(Order.CancelReason.InternalSMPCancelReason, rule.getCancelReason());
    }

    @Test
    public void testRejectReason() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = new AlertRule.AlertRuleBuilder().build();
        assertNull(rule.getRejectReason());

        rule = mapper.readValue("{\"rejectReason\": \"ExchangeReject\"}", AlertRule.class);
        assertEquals(Order.RejectReason.ExchangeReject, rule.getRejectReason());
    }

    @Test
    public void testLimitUsageRuleFieldsCanBeMapped() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("alert message", rule.getMessage());
        assertEquals(1, rule.getVenue().size());
        assertTrue(rule.getVenue().contains("XINE"));
        assertEquals(1, rule.getAccountId().size());
        assertTrue(rule.getAccountId().contains("INI04211"));
        assertEquals(85, rule.getLimitUsageAlertThreshold().getValue());
        assertEquals(">", rule.getLimitUsageAlertThreshold().getOperator());
        assertEquals(Value, rule.getLimitUsageAlertThreshold().getComparisonType());
    }

    @Test
    public void testLimitUsageRuleFieldsCanBeMappedWithNoAccountId() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("alert message", rule.getMessage());
        assertEquals(1, rule.getVenue().size());
        assertTrue(rule.getVenue().contains("XINE"));
        assertNull(rule.getAccountId());
        assertEquals(85, rule.getLimitUsageAlertThreshold().getValue());
        assertEquals(">", rule.getLimitUsageAlertThreshold().getOperator());
        assertEquals(Value, rule.getLimitUsageAlertThreshold().getComparisonType());
    }

    // 中文注释：覆盖 LimitUsage 新增的 MICFamily selector 映射和 helper 语义，确保 venue/micFamily 二选一逻辑在 model 层生效。
    @Test
    public void testLimitUsageRuleFieldsCanBeMappedWithMicFamily() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"micFamily\": ["
            + "\"SGX_FAMILY\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("alert message", rule.getMessage());
        assertNull(rule.getVenue());
        assertEquals(1, rule.getMicFamily().size());
        assertTrue(rule.getMicFamily().contains("SGX_FAMILY"));
        assertTrue(rule.hasMicFamilySelection());
        assertFalse(rule.hasMicVenueSelection());
        assertTrue(rule.hasValidLimitUsageVenueSelector());
        assertEquals(1, rule.getAccountId().size());
        assertTrue(rule.getAccountId().contains("INI04211"));
        assertEquals(85, rule.getLimitUsageAlertThreshold().getValue());
        assertEquals(">", rule.getLimitUsageAlertThreshold().getOperator());
        assertEquals(Value, rule.getLimitUsageAlertThreshold().getComparisonType());
    }

    @Test
    public void testLimitUsageRuleFields_LimitUsageAlertTime() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertTime\": \"15:00\","
            + "\"limitUsageAlertTimezone\": \"Asia/Hong_Kong\","
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertEquals("alert message", rule.getMessage());
        assertEquals(1, rule.getVenue().size());
        assertTrue(rule.getVenue().contains("XINE"));
        assertEquals(1, rule.getAccountId().size());
        assertTrue(rule.getAccountId().contains("INI04211"));
        assertNull(rule.getLimitUsageAlertThreshold());
        assertEquals("15:00", rule.getLimitUsageAlertTime());
        assertEquals("Asia/Hong_Kong", rule.getLimitUsageAlertTimezone());
    }

    @Test
    public void testValidateLimitUsageRule() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.isValid());
    }

    // 中文注释：覆盖 MICFamily selector 的有效规则校验，保证旧的 threshold/time-based 入口都能兼容新 selector。
    @Test
    public void testValidateLimitUsageRuleWithMicFamily() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"micFamily\": ["
            + "\"SGX_FAMILY\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.isValid());
    }

    // 中文注释：覆盖 MIC 与 MICFamily 互斥约束，避免同一条 LimitUsage 规则同时携带两个 selector。
    @Test
    public void testValidateLimitUsageRuleWithVenueAndMicFamily() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"micFamily\": ["
            + "\"SGX_FAMILY\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.hasMicVenueSelection());
        assertTrue(rule.hasMicFamilySelection());
        assertFalse(rule.hasValidLimitUsageVenueSelector());
        assertFalse(rule.isValid());
    }

    @Test
    public void testValidateLimitUsageRuleWithNoAccountId() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.isValid());
    }

    @Test
    public void testValidateLimitUsageRuleWithNoVenue() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertFalse(rule.isValid());
    }

    @Test
    public void testValidateLimitUsageRuleWithNoMessage() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertThreshold\": {"
            + "\"operator\": \">\","
            + "\"comparisonType\": \"Value\","
            + "\"value\": 85"
            + "},"
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertFalse(rule.isValid());
    }

    @Test
    public void testValidateLimitUsageRuleWithLimitUsageAlertTime() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertTime\": \"15:00\","
            + "\"limitUsageAlertTimezone\": \"Asia/Hong_Kong\","
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.isValid());
    }

    @Test
    public void testValidateLimitUsageRuleWithLimitUsageAlertTimeNoTimezone() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"venue\": ["
            + "\"XINE\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertTime\": \"15:00\","
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertFalse(rule.isValid());
    }

    // 中文注释：覆盖 time-based LimitUsage 规则走 MICFamily selector 时的合法性判断。
    @Test
    public void testValidateLimitUsageRuleWithMicFamilyLimitUsageAlertTime() throws JsonProcessingException {
        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"liyiyi\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"micFamily\": ["
            + "\"SGX_FAMILY\""
            + "],"
            + "\"accountId\": ["
            + "\"INI04211\""
            + "],"
            + "\"limitUsageAlertTime\": \"15:00\","
            + "\"limitUsageAlertTimezone\": \"Asia/Hong_Kong\","
            + "\"enabled\": true"
            + "}";

        ObjectMapper mapper = new ObjectMapper();

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);
        assertTrue(rule.isValid());
    }

    @Test
    public void testHasOverfilledLeg() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"dahunt\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"enabled\": true,"
            + "\"hasOverfilledLeg\": true"
            + "}";

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertAll(
            () -> assertTrue(rule.getHasOverfilledLeg()),
            () -> assertTrue(rule.isValid())
        );
    }

    @Test
    public void testGetNotifyViaLaunchpad() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"dahunt\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"enabled\": true,"
            + "\"hasOverfilledLeg\": true,"
            + "\"notifyViaLaunchpad\": true"
            + "}";

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertAll(
            () -> assertTrue(rule.getNotifyViaLaunchpad()),
            () -> assertTrue(rule.isValid())
        );
    }

    @Test
    public void testIsLegged() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String newJson = "{"
            + "\"symphonyEnabled\": false,"
            + "\"recapEmail\": false,"
            + "\"genericEmail\": false,"
            + "\"creationDateTime\": \"15-Feb-24 06:47:30.837\","
            + "\"kerberos\": \"dahunt\","
            + "\"desktopPopup\": false,"
            + "\"message\": \"alert message\","
            + "\"enabled\": true,"
            + "\"hasOverfilledLeg\": true,"
            + "\"isLegged\": true"
            + "}";

        AlertRule rule = mapper.readValue(newJson, AlertRule.class);

        assertAll(
            () -> assertTrue(rule.getIsLegged()),
            () -> assertTrue(rule.isValid())
        );
    }
}
