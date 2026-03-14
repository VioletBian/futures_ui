package com.xx.futures.evetor.recapserver.dao;

public class ConfigServerDao implements IConfigServerDao {

    private static final OctaneLogger log = LogUtility.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final JetstreamHttpClient httpClient;

    public static final String CONFIG_SERVER_URL = "configServerUrl";
    private static final String WATERFALL_USER = "whitewaterUser";
    private static final String WATERFALL_KEYTAB = "whitewaterKeyTab";

    @Inject
    public ConfigServerDao(
        @Named(CONFIG_SERVER_URL) JetstreamHttpClient waterfallHttpClient,
        @Named(WATERFALL_USER) String waterfallUser,
        @Named(WATERFALL_KEYTAB) String waterfallKeyTabPath
    ) {
        this.httpClient = waterfallHttpClient;
        this.httpClient.setUsername(waterfallUser);
        this.httpClient.setKeyTabPath(waterfallKeyTabPath);
        this.httpClient.setNumRetries(5);
    }

    @Override
    public AlertRule getAlertRuleById(String ruleId) throws IOException {
        try {
            String res =
                httpClient.get("/rest/elasticsearch/alertRule/" + ruleId, null, null, null, "application/json");
            if (StringUtils.isEmpty(res)) {
                return null;
            }

            AlertRule newRule = objectMapper.readValue(res, new TypeReference<AlertRule>() {});
            return newRule;
        } catch (Exception ex) {
            log.warn("Getting Rule By Id", ex);
        }
        return null;
    }
}
