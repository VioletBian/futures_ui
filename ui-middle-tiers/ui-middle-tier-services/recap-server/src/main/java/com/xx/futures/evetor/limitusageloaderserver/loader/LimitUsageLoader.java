package com.xx.futures.evetor.limitusageloaderserver.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LimitUsageLoader implements DataConsumer<ClearingData.LimitUsage> {

    private final NamedProperties properties;
    private final static OctaneLogger log = LogUtility.getLogger();
    private final List<String> topics;
    private final Map<String, ClearingData.LimitUsage> limitUsageCacheMap = new ConcurrentHashMap<>();

    @Inject
    public LimitUsageLoader(NamedProperties properties) {
        this.properties = properties;
        this.topics = List.of(properties.getProperty("kafka.subscription.topics"));
    }

    @Override
    public CallbackPriority getCallbackPriority() {
        return CallbackPriority.NORMAL;
    }

    @Override
    public void consume(ClearingData.LimitUsage limitUsageData) {
        updateCache(limitUsageData);
    }

    public void configureAndStartKafkaSubscription() {
        try {
            List<Messaging.BindingKey> bindingKeys = ConfigUtils.getBindingKeys(properties);
            DataParser<Messaging.OctaneMessage, ClearingData.LimitUsage> parser = new ProtoParser<>();
            DataConsumer<Messaging.OctaneMessage> consumer = DataConsumers.newDataConsumer(parser, this);
            OctaneSubscriberKafka subscriber = KafkaFactory.newJetstreamKafkaSubscriber(
                properties,
                topics,
                bindingKeys,
                consumer,
                consumer
            );
            log.info("Start Kafka Hydration...");
            // hydrate data from start of rundate till offset for that partition
            long fromTimeUtc =
                DateUtils.convertDateStringWithoutTimeToDateTimeUtc(
                    properties.getProperty(JetstreamProperty.runDate)
                ) / 1000;
            log.info(
                "Hydrating messages from kafka topic [{}] from [{}]",
                topics,
                DateUtils.convertUtcTimeInMicrosToString(fromTimeUtc * 1000, "UTC")
            );
            subscriber.hydrate(fromTimeUtc, null, null);
            log.info("Hydration Finished.");
            subscriber.start();
            log.info("INITIALISE! Started kafka consumer.");
        } catch (Exception e) {
            log.warn("Limit Usage Loader failed to initialize due to kafka subscriber failed to start.", e);
        }
    }

    public void updateCache(ClearingData.LimitUsage limitUsageData) {
        // identify clientRefId row - it is not mandatory field in ClearingData.LimitUsage
        if (StringUtils.isNotEmpty(limitUsageData.getClientRefId())) {
            limitUsageCacheMap.put(limitUsageData.getClientRefId(), limitUsageData);
            log.info(
                "Added limit usage data to cache for clientRefId: [{}]",
                limitUsageData.getClientRefId()
            );
        } else {
            log.warn("ClientRefId is empty for limit usage message received: [{}]", limitUsageData);
        }

        if (StringUtils.isNotEmpty(getGmiSynonymForLimitUsage(limitUsageData))) {
            limitUsageCacheMap.put(getGmiSynonymForLimitUsage(limitUsageData), limitUsageData);
            log.info(
                "Added limit usage data to cache for GMI: [{}]",
                getGmiSynonymForLimitUsage(limitUsageData)
            );
        }
    }

    public Map<String, ClearingData.LimitUsage> getLimitUsageCache() {
        log.debug("Current limit usage cache: - [{}]", limitUsageCacheMap);
        return limitUsageCacheMap;
    }

    public Map<String, Object> findLimitUsage(String account) {
        log.debug("Finding limit usage for account: - [{}]", account);
        return Map.of(account, getLimitUsageObject(account));
    }

    public Map<String, Object> findLimitUsages(String[] accounts) {
        log.debug("Finding limit usage list for accounts: - [{}]", accounts);
        Map<String, Object> limitUsages = new HashMap<>();
        for (String account : accounts) {
            limitUsages.put(account, getLimitUsageObject(account));
        }
        return limitUsages;
    }

    public Object getLimitUsageObject(String account) {
        return limitUsageCacheMap.get(account) != null
            ? limitUsageCacheMap.get(account)
            : "Limit Usage couldn't be retrieved.";
    }

    public static String getGmiSynonymForLimitUsage(ClearingData.LimitUsage limitUsage) {
        for (ReferenceData.Account account : limitUsage.getAccountsList()) {
            if (ReferenceData.Account.Type.GMI.equals(account.getAccountType())) {
                return account.getAccountId();
            }
        }
        return null;
    }
}
