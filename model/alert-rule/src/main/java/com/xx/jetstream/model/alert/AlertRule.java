package com.xx.jetstream.model.alert;

import java.io.Serializable;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = AlertRule.AlertRuleBuilder.class)
public class AlertRule implements Serializable {

    // Rule options
    private Long expiryTimestamp;
    private String id;
    private int version;
    private Long updateTime;
    private String kerberos;
    private String targetDesk;
    private String message;
    private String orderId;
    private String parsedOrderId;
    private AlertFieldComparisonMode.Modes orderIdComparisonMode;
    private HierarchyType orderLevel;
    private AlertTrigger alertTrigger;
    private Long latencyThresholdMs;

    // Field matchers
    private boolean enabled;
    private HashSet<String> creatorId;
    private HashSet<String> customerTrader;
    private HashSet<String> accountId;
    private HashSet<Order.Type> tradingAlgorithm;
    private InstrumentSymbols.Type instrumentSymbolType;
    private String instrumentSymbol;
    private String parsedInstrumentSymbol;
    private AlertFieldComparisonMode.Modes instrumentSymbolComparisonMode;
    private Order.InteractionLevel interactionLevel;
    private HashSet<PrefixEnum<Order.Status>> orderStatus;
    private String mic;
    private String sourceChannel;
    private StatComparison<Double> quantity;
    private StatComparison<Double> filledQuantity;
    private Boolean hasLimit;
    private OrderData.Order.WholesaleIndicatorType wholesaleIndicatorType;
    private Boolean settleAsFixed;
    private Boolean synthetic;
    private HashSet<String> venue;
    private String explanation;
    private String parsedExplanation;
    private AlertFieldComparisonMode.Modes explanationComparisonMode;
    private String traderRegion;
    private String capacity;
    private String alertColour;
    private Boolean isClient;
    private HashSet<String> creatorFoxDesk;
    private Boolean toastNotification;
    private OrderData.Order.TransactionType transactionType;
    private Boolean overfilled;
    private HashSet<String> bbgUUID;
    private Boolean isClaimed;
    private Order.CancelReason cancelReason;
    private Order.RejectReason rejectReason;
    private HashSet<Order.TimeInForce> timeInForce;
    private HashSet<Coverage.Alert.SensitiveAlertType> sensitiveAlertTypes;

    // Notification options
    private boolean symphonyEnabled;
    private boolean audioEnabled;
    private boolean softDelete;
    private boolean recapEmail;
    private boolean genericEmail;
    private boolean clientRecapEmail; // Used for external client mails
    private boolean hasHistoricalClientRecapEmail;
    private boolean bbgAlert;
    private boolean hasHistoricalBbgAlert;
    private boolean clientLimitUsageEmail;
    private boolean hasHistoricalClientLimitUsageEmail;
    private String emailAddress; // Structural repair from verified caller usage; declaration is below the readable region.
    private Boolean desktopPopup;
    private String audioFilename;
    private String creationDateTime;
    private boolean sendOrderDetailsInSymphony;
    private String symphonyRoomName;
    private String symphonyTeamRoomName; // Structural repair from verified caller usage; declaration is below the readable region.
    private boolean internalTCAReport;
    private boolean externalTCAReport;
    private boolean elixirReport;
    private Boolean resolvable;
    private Boolean snoozable;
    private Boolean notifyViaLaunchpad;

    private String activeFrom;
    private String activeTo;

    // Time options
    private String orderByte;

    // Limit Usage Alert options
    private StatComparison<Double> limitUsageAlertThreshold;
    private String limitUsageAlertTime;
    private String limitUsageAlertTimezone;

    // Multi leg algo alerts
    private Boolean hasOverfilledLeg;
    private Boolean isLegged;

    // Fields for Alert Priority
    private String alertCategory;
    private int priority;
    private HashSet<String> actorsSet;

    private AlertRule() {}

    public AlertRuleBuilder toBuilder() {
        return new AlertRuleBuilder(this);
    }

    /**
     * Drives whether this rule is processed via the new or old custom alert generator code. Should check if any
     * of the algo-specific rules are set.
     *
     * @return {@code true} if an algo and so to be processed by the algo custom alert generator
     */
    @JsonIgnore
    public boolean isAlgoRule() {
        return getParticipationRateComparison() != null
            || getAheadBehindComparison() != null
            || getExecutionScheduleComparison() != null
            || getMinQtyComparison() != null
            || getAutoExecutionSchedule() != null
            || getOutsidePriceLimit() != null
            || getHasLimit() != null;
    }

    /**
     * It's expected that this method is not time dependent and so checking now vs in the future would yield the same
     * result, even if the alert is no longer valid for another reason such as being expired.
     *
     * @return {@code true} if valid
     */
    @JsonIgnore
    public boolean isValid() {
        return isLimitUsageRule()
            ? validateLimitUsageRule()
            : validateRequiredFields() && validateAlertingFields() && validateInstrumentSymbol();
    }

    private boolean validateRequiredFields() {
        // The full predicate is outside the current readable region; these confirmed checks are visible elsewhere.
        return getVersion() >= 0 && getKerberos() != null && getMessage() != null;
    }

    @JsonIgnore
    private boolean validateAlertingFields() {
        if (getCreatorId() != null && !getCreatorId().isEmpty()) return true;
        if (getCustomerTrader() != null && !getCustomerTrader().isEmpty()) return true;
        if (getAccountId() != null && !getAccountId().isEmpty()) return true;
        if (getTradingAlgorithm() != null && !getTradingAlgorithm().isEmpty()) return true;
        if (getInstrumentSymbolType() != null) return true;
        if (getInstrumentSymbol() != null) return true;
        if (getParsedInstrumentSymbol() != null) return true;
        if (getInstrumentSymbolComparisonMode() != null) return true;
        if (getInteractionLevel() != null) return true;
        if (getOrderStatus() != null && !getOrderStatus().isEmpty()) return true;
        if (getMic() != null) return true;
        if (getSourceChannel() != null) return true;
        if (getQuantity() != null) return true;
        if (getFilledQuantity() != null) return true;
        if (getHasLimit() != null) return true;
        if (getWholesaleIndicatorType() != null) return true;
        if (getSettleAsFixed() != null) return true;
        if (getSynthetic() != null) return true;
        if (getVenue() != null && !getVenue().isEmpty()) return true;
        if (getExplanation() != null) return true;
        if (getParsedExplanation() != null) return true;
        if (getExplanationComparisonMode() != null) return true;
        if (getTraderRegion() != null) return true;
        if (getCapacity() != null) return true;
        if (getAlertColour() != null) return true;
        if (getIsClient() != null) return true;
        if (getCreatorFoxDesk() != null && !getCreatorFoxDesk().isEmpty()) return true;
        if (overfilled != null && overfilled) return true;
        if (isClaimed != null) return true;
        if (cancelReason != null) return true;
        if (rejectReason != null) return true;

        // Algo specific attributes
        if (getTradingAlgorithm() != null && !getTradingAlgorithm().isEmpty()) return true;
        if (getParticipationRateComparison() != null) return true;
        if (getAutoExecutionSchedule() != null) return true;
        if (getOutsidePriceLimit() != null) return true;
        if (getExecutionScheduleComparison() != null) return true;
        if (getMinQtyComparison() != null) return true;
        if (getSplitQtyComparison() != null) return true;
        if (getAheadBehindComparison() != null) return true;
        if (getQuantity() != null) return true;
        if (getFilledQuantity() != null) return true;
        if (getHasLimit() != null) return true;

        // Multi leg algo attributes
        if (getHasOverfilledLeg() != null) return true;
        if (getIsLegged() != null) return true;

        return false;
    }

    private boolean validateInstrumentSymbol() {
        return (getInstrumentSymbol() == null) == (getInstrumentSymbolType() == null);
    }

    private boolean validateLimitUsageRule() {
        // The tail of this predicate is partially obscured; the threshold/time split is directly supported by nearby lines.
        return getVersion() >= 0
            && getKerberos() != null
            && getMessage() != null
            && getVenue() != null
            && (getLimitUsageAlertThreshold() != null || isTimeBasedLimitUsageRule());
    }

    @JsonIgnore
    public boolean isLimitUsageRule() {
        return getLimitUsageAlertThreshold() != null
            || StringUtils.isNotBlank(getLimitUsageAlertTime())
            || StringUtils.isNotBlank(getLimitUsageAlertTimezone());
    }

    @JsonIgnore
    public boolean isTimeBasedLimitUsageRule() {
        return StringUtils.isNotBlank(getLimitUsageAlertTime())
            && StringUtils.isNotBlank(getLimitUsageAlertTimezone());
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public String getKerberos() {
        return kerberos;
    }

    public String getTargetDesk() {
        return targetDesk;
    }

    public String getMessage() {
        return message;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public HashSet<String> getAccountId() {
        return accountId;
    }

    public HashSet<String> getVenue() {
        return venue;
    }

    public StatComparison<Double> getLimitUsageAlertThreshold() {
        return limitUsageAlertThreshold;
    }

    public String getLimitUsageAlertTime() {
        return limitUsageAlertTime;
    }

    public String getLimitUsageAlertTimezone() {
        return limitUsageAlertTimezone;
    }

    public boolean getGenericEmail() {
        return genericEmail;
    }

    public boolean getClientLimitUsageEmail() {
        return clientLimitUsageEmail;
    }

    public boolean getSymphonyEnabled() {
        return symphonyEnabled;
    }

    public String getSymphonyRoomName() {
        return symphonyRoomName;
    }

    public String getSymphonyTeamRoomName() {
        return symphonyTeamRoomName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public static class AlertRuleBuilder {

        public AlertRuleBuilder() {}

        public AlertRuleBuilder(AlertRule alertRule) {
            // Builder members continue below the currently readable image region.
        }
    }
}
