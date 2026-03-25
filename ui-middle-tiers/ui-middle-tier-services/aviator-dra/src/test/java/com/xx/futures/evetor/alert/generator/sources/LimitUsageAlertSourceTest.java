package com.xx.futures.evetor.alert.generator.sources;

class LimitUsageAlertSourceTest {

    private LimitUsageAlertSource limitUsageAlertSource;
    private Common.Application application;
    private NamedProperties properties;
    private PermitEngine permitEngine;
    private BaseEntitlementUtils baseEntitlementUtils;
    private ActiveAlerts activeAlerts;
    private ScheduledExecutorService scheduler;
    private JetstreamHttpClient jetstreamHttpClient;
    private AlertEngine alertEngine;

    @BeforeEach
    void setup() {
        properties = new NamedProperties();
        properties.setProperty(AviatorProperty.alerterRegion, "AP");
        properties.setProperty(AviatorProperty.runDate, "20250120");

        permitEngine = mock(PermitEngine.class);
        when(permitEngine.getRoles(Mockito.matches("liyiyi"))).thenReturn(Collections.singletonList("role1"));

        application = Common.Application.newBuilder()
            .setAppName(Common.Application.AppName.Aviator)
            .setInstanceId("junit")
            .build();

        activeAlerts = new ActiveAlerts();

        baseEntitlementUtils = Mockito.mock(BaseEntitlementUtils.class);
        when(
            baseEntitlementUtils.getUserEntitlements(
                any(PermitEngine.class),
                any(String.class),
                any(Messaging.MessagingExchange.EnvironmentNamespace.class)
            )
        ).thenReturn(new UserEntitlements.UserEntitlementsBuilder("").setLimitsView(true).build());

        scheduler = mock(ScheduledExecutorService.class);
        jetstreamHttpClient = mock(JetstreamHttpClient.class);
        alertEngine = mock(AlertEngine.class);
        limitUsageAlertSource = spy(
            new LimitUsageAlertSource(
                new AviatorContext(),
                baseEntitlementUtils,
                properties,
                permitEngine,
                Common.Region.AP,
                application,
                activeAlerts,
                alertEngine
            )
        );
    }

    @Test
    void testRuleIsAddedIf_limitUsageAlert() {
        AlertRule rule = generateLimitUsageAlertRule();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            1,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a Limit usage threshold alert rule to be processed"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a Limit usage threshold rule to not be processed as time-based rule"
        );
    }

    @Test
    void testRuleIsAddedIf_limitUsageAlert_micFamily() {
        // 中文注释：覆盖新增 MICFamily selector 规则可以进入 thresholdRules。
        AlertRule rule = buildMicFamilyLimitUsageAlertRule();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            1,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a Limit usage MICFamily threshold alert rule to be processed"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a Limit usage MICFamily threshold rule to not be processed as time-based rule"
        );
    }

    @Test
    void testRuleIsAddedIf_limitUsageTimeBasedAlert() {
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        // time based rule is inserted into concurrent map
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected Limit usage time-based rule to not be processed as threshold rule"
        );
        assertEquals(
            1,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected Limit usage time-based rule to be processed"
        );
    }

    @Test
    void testRuleIsNotAddedIf_limitUsageSelectorConflict() {
        // 中文注释：覆盖规则层互斥语义，MIC 和 MICFamily 同时出现时不允许进入运行态。
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule())
            .setMicFamily(new HashSet<>(Collections.singleton("SFX")))
            .build();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a rule with both MIC and MICFamily to be rejected"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a rule with both MIC and MICFamily to be rejected"
        );
    }

    @Test
    void testRuleIsNotAddedIf_softDelete() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule()).setSoftDelete(true).build();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a rule with softDelete=true to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_softDeleteTimeBased() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageTimeBasedAlertRule()).setSoftDelete(true).build();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a rule with softDelete=true to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_notEnabled() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule()).setEnabled(false).build();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a rule with enabled=false to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_notEnabledTimeBased() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageTimeBasedAlertRule()).setEnabled(false).build();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a rule with enabled=false to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_invalid() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule()).setMessage(null).build();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a rule with message=null to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_invalidTimeBased() {
        AlertRule rule = new AlertRule.AlertRuleBuilder(generateLimitUsageTimeBasedAlertRule()).setMessage(null).build();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a rule with message=null to not be processed and to remove the existing rule"
        );
    }

    @Test
    void testRuleNotAddedPermitRoleMissing() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi"))).thenReturn(Collections.emptyList());

        AlertRule rule = generateLimitUsageAlertRule();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a user with no roles not to have their rule added"
        );
    }

    @Test
    void testRuleNotAddedPermitRoleMissing_timeBasedRule() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi"))).thenReturn(Collections.emptyList());
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a user with no roles not to have their rule added"
        );
    }

    @Test
    void testRuleAddedPermitGeneralException() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi")))
            .thenThrow(new RuntimeException("Errors should be ignored"));

        AlertRule rule = generateLimitUsageAlertRule();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            1,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a user with an exception thrown to still have their rule added"
        );
    }

    @Test
    void testRuleAddedPermitGeneralException_timeBasedRule() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi")))
            .thenThrow(new RuntimeException("Errors should be ignored"));

        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            1,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a user with an exception thrown to still have their rule added"
        );
    }

    @Test
    void testRuleNotAddedPermitMissingRoleException() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi")))
            .thenThrow(new PermitException("Error in getRoles() results: \"Engine error in "
                + "RoleService.getRoles: Engine does not have data for User kerberos/davile\""));

        AlertRule rule = generateLimitUsageAlertRule();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a Permit Exception for missing Role to not result in an added alert rule"
        );
    }

    @Test
    void testRuleNotAddedPermitMissingRoleException_timeBasedRule() {
        when(permitEngine.getRoles(Mockito.matches("liyiyi")))
            .thenThrow(new PermitException("Error in getRoles() results: \"Engine error in "
                + "RoleService.getRoles: Engine does not have data for User kerberos/davile\""));

        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a Permit Exception for missing Role to not result in an added alert rule"
        );
    }

    @Test
    void testRuleIsNotAddedIf_notLimitUsageAlert() {
        AlertRule rule = generateAlertRule("rule1", "liyiyi");
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a non Limit usage alert rule to not be processed"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a non limit usage alert rule to not be processed"
        );
    }

    @Test
    void testRuleIsNotRemovedIf_notLimitUsageAlert() {
        AlertRule rule = generateAlertRule("rule1", "liyiyi");
        limitUsageAlertSource.processRemoveAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a non Limit usage alert rule to not be processed"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a non limit usage alert rule to not be processed"
        );
    }

    @Test
    void testRuleIsNotAddedIf_limitUsageAlert_noLimitsView() {
        when(
            baseEntitlementUtils.getUserEntitlements(
                any(PermitEngine.class),
                any(String.class),
                any(Messaging.MessagingExchange.EnvironmentNamespace.class)
            )
        ).thenReturn(new UserEntitlements.UserEntitlementsBuilder("").setLimitsView(false).build());

        AlertRule rule = generateLimitUsageAlertRule();
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(
            0,
            limitUsageAlertSource.getThresholdRules().size(),
            "Expected a limit usage alert rule to not be processed"
        );
        assertEquals(
            0,
            limitUsageAlertSource.getScheduledRules().size(),
            "Expected a limit usage alert rule to not be processed"
        );
    }

    @Test
    void testScheduleTimeBasedRuleWithMockedCalculateTimeDelay() {
        // Arrange mocks
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        // Mock the calculateTimeDelay method to return a specific value
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        // Act
        limitUsageAlertSource.processNewAlertRule(rule);

        // verify
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(scheduler).schedule(runnableCaptor.capture(), delayCaptor.capture(), timeUnitCaptor.capture());
        assertEquals(5000L, delayCaptor.getValue());
        assertEquals(TimeUnit.MILLISECONDS, timeUnitCaptor.getValue());
        assertEquals(future, limitUsageAlertSource.getScheduledRules().get("rule2"));
    }

    @Test
    void testScheduleTimeBasedRuleWithMockedHttpClient_validResponse() {
        // Arrange
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        String mockResponse = "{\n" +
            "\"DCI03456\": {\n" +
            "\"id\": \"ZCI02927-1737424527099\",\n" +
            "\"lastUpdateTimeUtc\": 1737424527099,\n" +
            "\"clientRefId\": \"55300015\",\n" +
            "\"type\": \"Margin\",\n" +
            "\"usage\": 8763150.736664178,\n" +
            "\"limit\": 11627295.86508448,\n" +
            "\"mic\": \"XZCE\",\n" +
            "\"currency\": \"USD\",\n" +
            "\"accounts\": [\n" +
            "{\n" +
            "\"accountId\": \"ZCI02927\",\n" +
            "\"accountType\": \"GMI\"\n" +
            "}\n" +
            "]\n" +
            "},\n" +
            "\"04860800\": {\n" +
            "\"id\": \"ZCI02972-1737424527567\",\n" +
            "\"lastUpdateTimeUtc\": 1737424527567,\n" +
            "\"clientRefId\": \"55300022\",\n" +
            "\"type\": \"Margin\",\n" +
            "\"usage\": 8258828.927051164,\n" +
            "\"limit\": 11625315.71098684,\n" +
            "\"mic\": \"XZCE\",\n" +
            "\"currency\": \"USD\",\n" +
            "\"accounts\": [\n" +
            "{\n" +
            "\"accountId\": \"ZCI02972\",\n" +
            "\"accountType\": \"GMI\"\n" +
            "}\n" +
            "]\n" +
            "}\n" +
            "}";
        when(jetstreamHttpClient.get(anyString(), isNull(), any(MultivaluedMap.class), isNull(), eq("application/json")))
            .thenReturn(mockResponse);
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        // Act
        limitUsageAlertSource.processNewAlertRule(rule);

        // Capture the Runnable and execute it
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), anyLong(), any(TimeUnit.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verify the HTTP client call
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MultivaluedMap<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        verify(jetstreamHttpClient).get(urlCaptor.capture(), isNull(), queryParamsCaptor.capture(), isNull(), eq("application/json"));

        assertEquals("/limitusage/accounts", urlCaptor.getValue());
        Set<String> expectedQueryParamKeys = new HashSet<>();
        expectedQueryParamKeys.add("accountlist");
        Set<String> expectedAccountIds = new HashSet<>();
        expectedAccountIds.add("04860800");
        expectedAccountIds.add("04860801");
        Set<String> actualAccountIds = new HashSet<>(List.of(queryParamsCaptor.getValue().getFirst("accountlist").split(",")));
        assertEquals(expectedQueryParamKeys, queryParamsCaptor.getValue().keySet());
        assertEquals(expectedAccountIds, actualAccountIds);
    }

    @Test
    void testScheduleTimeBasedRuleWithMockedHttpClient_nullResponse() {
        // Arrange
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        String mockResponse = "";
        when(jetstreamHttpClient.get(anyString(), isNull(), any(MultivaluedMap.class), isNull(), eq("application/json")))
            .thenReturn(mockResponse);
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        // Act
        limitUsageAlertSource.processNewAlertRule(rule);

        // Capture the Runnable and execute it
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), anyLong(), any(TimeUnit.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        assertThrows(RuntimeException.class, scheduledTask::run, "Failed to parse limit usage data for rule [rule2]");
    }

    @Test
    void testScheduleTimeBasedRuleWithMockedHttpClient_micFamilyNoMatchSkipsAlert() {
        // 中文注释：覆盖 time-based 的 MICFamily 快照过滤，不命中 selector 时不应生成 alert。
        AlertRule rule = buildMicFamilyTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        when(jetstreamHttpClient.get(anyString(), isNull(), any(MultivaluedMap.class), isNull(), eq("application/json")))
            .thenReturn(buildMicFamilyTimeBasedResponse("UNKNOWN", "NON_SFX"));
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        limitUsageAlertSource.processNewAlertRule(rule);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), anyLong(), any(TimeUnit.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        verify(alertEngine, Mockito.never()).process(any(List.class));
    }

    @Test
    void testScheduleTimeBasedRuleWithMockedHttpClient_micFamilyMatchesButConcreteMicSkipsAlert() {
        // 中文注释：覆盖 corner case，若快照同时带真实 mic 和 micFamily，则不应被当成 GMI 聚合级数据触发告警。
        AlertRule rule = buildMicFamilyTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        when(jetstreamHttpClient.get(anyString(), isNull(), any(MultivaluedMap.class), isNull(), eq("application/json")))
            .thenReturn(buildMicFamilyTimeBasedResponse("XZCE", "SFX"));
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        limitUsageAlertSource.processNewAlertRule(rule);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), anyLong(), any(TimeUnit.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        verify(alertEngine, Mockito.never()).process(any(List.class));
    }

    @Test
    void testRemoveRule_nullRule() {
        assertFalse(limitUsageAlertSource.processRemoveAlertRule(null));
    }

    @Test
    void testRemoveTimeBasedRuleById_FutureNotFound() {
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        assertFalse(limitUsageAlertSource.processRemoveAlertRule(rule));
    }

    @Test
    void testRemoveTimeBasedRuleById_FutureIsDone() {
        // Arrange
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        when(future.isDone()).thenReturn(true);
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        // Act
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(1, limitUsageAlertSource.getScheduledRules().size());

        // delete done rule - returns true
        assertTrue(limitUsageAlertSource.processRemoveAlertRule(rule));
    }

    @Test
    void testRemoveTimeBasedRuleById_FutureNotDone() {
        // Arrange
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        when(future.isDone()).thenReturn(false);
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        // Act
        limitUsageAlertSource.processNewAlertRule(rule);
        assertEquals(1, limitUsageAlertSource.getScheduledRules().size());

        // delete done rule - returns false
        assertTrue(limitUsageAlertSource.processRemoveAlertRule(rule));
        verify(future).cancel(true);
    }

    @Test
    void testGetTimeBasedAlertId() {
        assertEquals(
            "04860801,04860800-0-RuleIdrule2",
            limitUsageAlertSource.getTimeBasedAlertId(generateLimitUsageTimeBasedAlertRule())
        );
    }

    @Test
    void testGetTimeBasedAlertRuleBreachingMessage() {
        String timezone = "Asia/Hong_Kong";
        String timestamp = "10:20 Asia/Hong_Kong";
        doReturn("20250120").when(limitUsageAlertSource).getAlertGeneratedDate(timezone);
        String date = limitUsageAlertSource.getAlertGeneratedDate(timezone);
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(0),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(0), timestamp, date)
        );
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(1),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(1), timestamp, date)
        );
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(2),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(2), timestamp, date)
        );
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(3),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(3), timestamp, date)
        );
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(4),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(4), timestamp, date)
        );
        assertEquals(
            getTimeBasedAlertRuleBreachingMessage(5),
            limitUsageAlertSource.getTimeBasedAlertRuleBreachingMessage(generateLimitUsageMap(5), timestamp, date)
        );
    }

    @Test
    void testGenerateNewAlerts() {
        // 中文注释：主代码现在接收运行态 LimitUsageRule，这里同步覆盖新的 time-based 生成入口。
        AlertRule rule = generateLimitUsageTimeBasedAlertRule();
        limitUsageAlertSource.setScheduler(scheduler);
        limitUsageAlertSource.setJetstreamHttpClient(jetstreamHttpClient);

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
        when(future.isDone()).thenReturn(false);
        doReturn(5000L).when(limitUsageAlertSource).calculateTimeDelay(rule);

        com.xx.futures.evetor.alert.generator.rules.LimitUsageRule limitUsageRule =
            new com.xx.futures.evetor.alert.generator.rules.LimitUsageRule(rule, application, activeAlerts);
        assertEquals(1, limitUsageAlertSource.generateNewAlerts(limitUsageRule, generateLimitUsageMap(0)).size());
    }

    @Test
    void testGetBodyWithValidLimitUsage() {
        Map<String, Object> limitUsage = new HashMap<>();
        limitUsage.put("usage", 11495905.20272978);
        limitUsage.put("limit", 16167814.417355172);
        limitUsage.put("currency", "USD");

        Map.Entry<String, Object> entry = new HashMap.SimpleEntry<>("55300015", limitUsage);
        String expected = "<tr><td style=\"text-align:center\">55300015</td><td style=\"text-align:center\">11495905.2</td>" +
            "<td style=\"text-align:center\">16167814.4</td><td style=\"text-align:center\">USD</td><td style=\"text-align:center\">71.1%</td></tr>";
        String actual = limitUsageAlertSource.getBody(entry);
        assertEquals(expected, actual);
    }

    @Test
    void testGetBodyWithInvalidLimitUsage() {
        Map.Entry<String, Object> entry = new HashMap.SimpleEntry<>("123", "Limit Usage couldn't be retrieved.");
        String actual = limitUsageAlertSource.getBody(entry);
        Assertions.assertNull(actual);
    }

    @Test
    void testGetTimeDelay() {
        assertEquals(86000000, limitUsageAlertSource.getTimeDelay(-400000));
        assertEquals(10, limitUsageAlertSource.getTimeDelay(86400010));
        assertEquals(1000, limitUsageAlertSource.getTimeDelay(1000));
    }

    // 中文注释：MICFamily 相关 helper 只补本轮 selector 测试数据，尽量不动原有测试构造。
    private AlertRule buildMicFamilyLimitUsageAlertRule() {
        return new AlertRule.AlertRuleBuilder(generateLimitUsageAlertRule())
            .setVenue(null)
            .setMicFamily(new HashSet<>(Collections.singleton("SFX")))
            .build();
    }

    private AlertRule buildMicFamilyTimeBasedAlertRule() {
        return new AlertRule.AlertRuleBuilder(generateLimitUsageTimeBasedAlertRule())
            .setVenue(null)
            .setMicFamily(new HashSet<>(Collections.singleton("SFX")))
            .build();
    }

    private String buildMicFamilyTimeBasedResponse(String mic, String micFamily) {
        return "{\n" +
            "\"04860800\": {\n" +
            "\"id\": \"ZCI02972-1737424527567\",\n" +
            "\"lastUpdateTimeUtc\": 1737424527567,\n" +
            "\"clientRefId\": \"55300022\",\n" +
            "\"type\": \"Margin\",\n" +
            "\"usage\": 8258828.927051164,\n" +
            "\"limit\": 11625315.71098684,\n" +
            "\"mic\": \"" + mic + "\",\n" +
            "\"micFamily\": \"" + micFamily + "\",\n" +
            "\"currency\": \"USD\",\n" +
            "\"accounts\": [\n" +
            "{\n" +
            "\"accountId\": \"ZCI02972\",\n" +
            "\"accountType\": \"GMI\"\n" +
            "}\n" +
            "]\n" +
            "}\n" +
            "}";
    }
}
