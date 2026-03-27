package com.xx.jetstream.model.alert;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import org.apache.commons.lang3.SerializationUtils;
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
    private HashSet<String> micFamily;
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
    private Boolean desktopPopup;
    private String audioFilename;
    private String creationDateTime;
    private boolean sendOrderDetailsInSymphony;
    private String symphonyRoomName;
    private String symphonyTeamRoomName; // Structural carry-over from previously confirmed workspace usage.
    private boolean internalTCAReport;
    private boolean externalTCAReport;
    private boolean elixirReport;
    private Boolean resolvable;
    private Boolean snoozable;
    private Boolean notifyViaLaunchpad;

    // Recap options
    private String emailAddress;
    private String clientEmailAddress;
    private String recapTemplateName;
    private String recapTemplateId;

    // Algo options
    private StatComparison<Double> participationRateComparison;
    private StatComparison<Double> aheadBehindComparison;
    private StatComparison<Double> executionScheduleComparison;
    private StatComparison<Double> minQtyComparison;
    private StatComparison<Double> splitQtyComparison;
    private Boolean autoExecutionSchedule;
    private TimeComparison activationComparison;
    private Boolean outsidePriceLimit;

    // Base Rule ID
    private String baseRuleId;

    // Rule Schedule
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

    @JsonIgnore
    public boolean isMultiLegAlgoRule() {
        return getHasOverfilledLeg() != null;
    }

    @JsonIgnore
    public boolean isValid() {
        return isLimitUsageRule()
            ? validateLimitUsageRule()
            : validateRequiredFields() && validateAlertingFieldsPresent() && validateInstrumentSymbol();
    }

    @JsonIgnore
    public boolean isExpired(Instant now) {
        return getExpiryTimestamp() != null && now.toEpochMilli() > getExpiryTimestamp();
    }

    @JsonIgnore
    public boolean isLimitUsageRule() {
        return getLimitUsageAlertThreshold() != null
            || StringUtils.isNotBlank(getLimitUsageAlertTime())
            || StringUtils.isNotBlank(getLimitUsageAlertTimezone());
    }

    private boolean validateRequiredFields() {
        return getVersion() >= 0
            && getKerberos() != null
            && getMessage() != null;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean validateAlertingFieldsPresent() {
        if (getCreatorId() != null && !getCreatorId().isEmpty()) return true;
        if (getCustomerTrader() != null && !getCustomerTrader().isEmpty()) return true;
        if (getAccountId() != null && !getAccountId().isEmpty()) return true;
        if (getTradingAlgorithm() != null && !getTradingAlgorithm().isEmpty()) return true;
        if (getInstrumentSymbolType() != null) return true;
        if (getInteractionLevel() != null) return true;
        if (getTimeInForce() != null && !getTimeInForce().isEmpty()) return true;
        if (getOrderStatus() != null && !getOrderStatus().isEmpty()) return true;
        if (getMic() != null) return true;
        if (StringUtils.isNotEmpty(getOrderId())) return true;
        if (getWholesaleIndicatorType() != null) return true;
        if (getSynthetic() != null) return true;
        if (getSettleAsFixed() != null) return true;
        if (getVenue() != null && !getVenue().isEmpty()) return true;
        if (getMicFamily() != null && !getMicFamily().isEmpty()) return true;
        if (getSensitiveAlertTypes() != null && !getSensitiveAlertTypes().isEmpty()) return true;
        if (getExplanation() != null) return true;
        if (getTraderRegion() != null) return true;
        if (getTransactionType() != null) return true;
        if (getCapacity() != null) return true;
        if (getAlertColour() != null) return true;
        if (getIsClient() != null) return true;
        if (getCreatorFoxDesk() != null && !getCreatorFoxDesk().isEmpty()) return true;
        if (overfilled != null && overfilled) return true;
        if (isClaimed != null) return true;
        if (cancelReason != null) return true;
        if (rejectReason != null) return true;

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

        if (getHasOverfilledLeg() != null) return true;
        if (getIsLegged() != null) return true;

        return false;
    }

    private boolean validateInstrumentSymbol() {
        return (getInstrumentSymbol() == null) == (getInstrumentSymbolType() == null);
    }

    private boolean validateLimitUsageRule() {
        return getVersion() >= 0
            && getKerberos() != null
            && getMessage() != null
            && hasValidLimitUsageVenueSelector()
            && (getLimitUsageAlertThreshold() != null || isTimeBasedLimitUsageRule());
    }

    @JsonIgnore
    public boolean isTimeBasedLimitUsageRule() {
        return StringUtils.isNotBlank(getLimitUsageAlertTime())
            && StringUtils.isNotBlank(getLimitUsageAlertTimezone());
    }

    @JsonIgnore
    public boolean hasMicVenueSelection() {
        return getVenue() != null && !getVenue().isEmpty();
    }

    @JsonIgnore
    public boolean hasMicFamilySelection() {
        return getMicFamily() != null && !getMicFamily().isEmpty();
    }

    @JsonIgnore
    public boolean hasValidLimitUsageVenueSelector() {
        return hasMicVenueSelection() ^ hasMicFamilySelection();
    }

    public Long getExpiryTimestamp() {
        return expiryTimestamp;
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getClientEmailAddress() {
        return clientEmailAddress;
    }

    public String getRecapTemplateName() {
        return recapTemplateName;
    }

    public String getOrderByte() {
        return orderByte;
    }

    public String getRecapTemplateId() {
        return recapTemplateId;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean getSoftDelete() {
        return softDelete;
    }

    public boolean getRecapEmail() {
        return recapEmail;
    }

    public boolean getClientRecapEmail() {
        return clientRecapEmail;
    }

    public boolean getBbgAlert() {
        return bbgAlert;
    }

    public boolean getClientLimitUsageEmail() {
        return clientLimitUsageEmail;
    }

    public boolean getHasHistoricalClientRecapEmail() {
        return hasHistoricalClientRecapEmail;
    }

    public boolean getHasHistoricalBbgAlert() {
        return hasHistoricalBbgAlert;
    }

    public boolean getHasHistoricalClientLimitUsageEmail() {
        return hasHistoricalClientLimitUsageEmail;
    }

    public AlertTrigger getAlertTrigger() {
        return alertTrigger;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public boolean getGenericEmail() {
        return genericEmail;
    }

    public boolean getSymphonyEnabled() {
        return symphonyEnabled;
    }

    public boolean getInternalTCAReport() {
        return internalTCAReport;
    }

    public boolean getElixirReport() {
        return elixirReport;
    }

    public boolean getExternalTCAReport() {
        return externalTCAReport;
    }

    public HashSet<String> getCreatorId() {
        return creatorId;
    }

    public String getAudioFilename() {
        return audioFilename;
    }

    public String getSymphonyRoomName() {
        return symphonyRoomName;
    }

    public String getSymphonyTeamRoomName() {
        return symphonyTeamRoomName;
    }

    public boolean getAudioEnabled() {
        return audioEnabled;
    }

    public HashSet<String> getCustomerTrader() {
        return customerTrader;
    }

    public HashSet<String> getAccountId() {
        return accountId;
    }

    public HashSet<String> getBbgUUID() {
        return bbgUUID;
    }

    public HashSet<Order.Type> getTradingAlgorithm() {
        return tradingAlgorithm;
    }

    public InstrumentSymbols.Type getInstrumentSymbolType() {
        return instrumentSymbolType;
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public String getParsedInstrumentSymbol() {
        return parsedInstrumentSymbol;
    }

    public AlertFieldComparisonMode.Modes getInstrumentSymbolComparisonMode() {
        return instrumentSymbolComparisonMode;
    }

    public Order.InteractionLevel getInteractionLevel() {
        return interactionLevel;
    }

    public HashSet<Order.TimeInForce> getTimeInForce() {
        return timeInForce;
    }

    public HashSet<PrefixEnum<Order.Status>> getOrderStatus() {
        return orderStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getParsedOrderId() {
        return parsedOrderId;
    }

    public AlertFieldComparisonMode.Modes getOrderIdComparisonMode() {
        return orderIdComparisonMode;
    }

    public HierarchyType getOrderLevel() {
        return orderLevel;
    }

    public StatComparison<Double> getParticipationRateComparison() {
        return participationRateComparison;
    }

    public StatComparison<Double> getAheadBehindComparison() {
        return aheadBehindComparison;
    }

    public StatComparison<Double> getExecutionScheduleComparison() {
        return executionScheduleComparison;
    }

    public StatComparison<Double> getMinQtyComparison() {
        return minQtyComparison;
    }

    public StatComparison<Double> getSplitQtyComparison() {
        return splitQtyComparison;
    }

    public Boolean getAutoExecutionSchedule() {
        return autoExecutionSchedule;
    }

    public Boolean getOutsidePriceLimit() {
        return outsidePriceLimit;
    }

    public TimeComparison getActivationComparison() {
        return activationComparison;
    }

    public Boolean getDesktopPopup() {
        return desktopPopup;
    }

    public String getMic() {
        return mic;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public StatComparison<Double> getQuantity() {
        return quantity;
    }

    public StatComparison<Double> getFilledQuantity() {
        return filledQuantity;
    }

    public Boolean getHasLimit() {
        return hasLimit;
    }

    public OrderData.Order.WholesaleIndicatorType getWholesaleIndicatorType() {
        return wholesaleIndicatorType;
    }

    public Boolean getSettleAsFixed() {
        return settleAsFixed;
    }

    public Boolean getSynthetic() {
        return synthetic;
    }

    public Long getLatencyThresholdMs() {
        return latencyThresholdMs;
    }

    public HashSet<String> getVenue() {
        return venue;
    }

    public HashSet<String> getMicFamily() {
        return micFamily;
    }

    public HashSet<Coverage.Alert.SensitiveAlertType> getSensitiveAlertTypes() {
        return sensitiveAlertTypes;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getParsedExplanation() {
        return parsedExplanation;
    }

    public AlertFieldComparisonMode.Modes getExplanationComparisonMode() {
        return explanationComparisonMode;
    }

    public String getTraderRegion() {
        return traderRegion;
    }

    public OrderData.Order.TransactionType getTransactionType() {
        return transactionType;
    }

    public String getCapacity() {
        return capacity;
    }

    public String getAlertColour() {
        return alertColour;
    }

    public Boolean getIsClient() {
        return isClient;
    }

    public boolean getSendOrderDetailsInSymphony() {
        return sendOrderDetailsInSymphony;
    }

    public Boolean getToastNotification() {
        return toastNotification;
    }

    public HashSet<String> getCreatorFoxDesk() {
        return creatorFoxDesk;
    }

    public String getBaseRuleId() {
        return baseRuleId;
    }

    public String getActiveFrom() {
        return activeFrom;
    }

    public String getActiveTo() {
        return activeTo;
    }

    public Boolean getOverfilled() {
        return overfilled;
    }

    public Boolean getIsClaimed() {
        return isClaimed;
    }

    public Order.CancelReason getCancelReason() {
        return cancelReason;
    }

    public Order.RejectReason getRejectReason() {
        return rejectReason;
    }

    public Boolean isResolvable() {
        return resolvable;
    }

    public Boolean isSnoozable() {
        return snoozable;
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

    public Boolean getNotifyViaLaunchpad() {
        return notifyViaLaunchpad;
    }

    public Boolean getHasOverfilledLeg() {
        return hasOverfilledLeg;
    }

    public Boolean getIsLegged() {
        return isLegged;
    }

    public String getAlertCategory() {
        return alertCategory;
    }

    public int getPriority() {
        return priority;
    }

    public HashSet<String> getActorsSet() {
        return actorsSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertRule alertRule = (AlertRule) o;
        return version == alertRule.version
            && Objects.equals(updateTime, alertRule.updateTime)
            && enabled == alertRule.enabled
            && symphonyEnabled == alertRule.symphonyEnabled
            && internalTCAReport == alertRule.internalTCAReport
            && elixirReport == alertRule.elixirReport
            && externalTCAReport == alertRule.externalTCAReport
            && audioEnabled == alertRule.audioEnabled
            && softDelete == alertRule.softDelete
            && recapEmail == alertRule.recapEmail
            && genericEmail == alertRule.genericEmail
            && clientRecapEmail == alertRule.clientRecapEmail
            && bbgAlert == alertRule.bbgAlert
            && clientLimitUsageEmail == alertRule.clientLimitUsageEmail
            && hasHistoricalClientRecapEmail == alertRule.hasHistoricalClientRecapEmail
            && hasHistoricalBbgAlert == alertRule.hasHistoricalBbgAlert
            && hasHistoricalClientLimitUsageEmail == alertRule.hasHistoricalClientLimitUsageEmail
            && sendOrderDetailsInSymphony == alertRule.sendOrderDetailsInSymphony
            && priority == alertRule.priority
            && Objects.equals(expiryTimestamp, alertRule.expiryTimestamp)
            && Objects.equals(latencyThresholdMs, alertRule.latencyThresholdMs)
            && Objects.equals(id, alertRule.id)
            && Objects.equals(kerberos, alertRule.kerberos)
            && Objects.equals(targetDesk, alertRule.targetDesk)
            && Objects.equals(message, alertRule.message)
            && Objects.equals(orderId, alertRule.orderId)
            && orderIdComparisonMode == alertRule.orderIdComparisonMode
            && orderLevel == alertRule.orderLevel
            && alertTrigger == alertRule.alertTrigger
            && Objects.equals(creatorId, alertRule.creatorId)
            && Objects.equals(customerTrader, alertRule.customerTrader)
            && Objects.equals(accountId, alertRule.accountId)
            && Objects.equals(bbgUUID, alertRule.bbgUUID)
            && Objects.equals(tradingAlgorithm, alertRule.tradingAlgorithm)
            && instrumentSymbolType == alertRule.instrumentSymbolType
            && Objects.equals(instrumentSymbol, alertRule.instrumentSymbol)
            && instrumentSymbolComparisonMode == alertRule.instrumentSymbolComparisonMode
            && interactionLevel == alertRule.interactionLevel
            && Objects.equals(timeInForce, alertRule.timeInForce)
            && Objects.equals(transactionType, alertRule.transactionType)
            && Objects.equals(orderStatus, alertRule.orderStatus)
            && Objects.equals(audioFilename, alertRule.audioFilename)
            && Objects.equals(symphonyRoomName, alertRule.symphonyRoomName)
            && Objects.equals(creationDateTime, alertRule.creationDateTime)
            && Objects.equals(emailAddress, alertRule.emailAddress)
            && Objects.equals(clientEmailAddress, alertRule.clientEmailAddress)
            && Objects.equals(recapTemplateName, alertRule.recapTemplateName)
            && Objects.equals(recapTemplateId, alertRule.recapTemplateId)
            && Objects.equals(orderByte, alertRule.orderByte)
            && Objects.equals(participationRateComparison, alertRule.participationRateComparison)
            && Objects.equals(executionScheduleComparison, alertRule.executionScheduleComparison)
            && Objects.equals(minQtyComparison, alertRule.minQtyComparison)
            && Objects.equals(splitQtyComparison, alertRule.splitQtyComparison)
            && Objects.equals(aheadBehindComparison, alertRule.aheadBehindComparison)
            && Objects.equals(autoExecutionSchedule, alertRule.autoExecutionSchedule)
            && Objects.equals(outsidePriceLimit, alertRule.outsidePriceLimit)
            && Objects.equals(activationComparison, alertRule.activationComparison)
            && Objects.equals(desktopPopup, alertRule.desktopPopup)
            && Objects.equals(quantity, alertRule.quantity)
            && Objects.equals(filledQuantity, alertRule.filledQuantity)
            && Objects.equals(hasLimit, alertRule.hasLimit)
            && Objects.equals(wholesaleIndicatorType, alertRule.wholesaleIndicatorType)
            && Objects.equals(settleAsFixed, alertRule.settleAsFixed)
            && Objects.equals(synthetic, alertRule.synthetic)
            && Objects.equals(mic, alertRule.mic)
            && Objects.equals(venue, alertRule.venue)
            && Objects.equals(micFamily, alertRule.micFamily)
            && Objects.equals(sensitiveAlertTypes, alertRule.sensitiveAlertTypes)
            && Objects.equals(explanation, alertRule.explanation)
            && explanationComparisonMode == alertRule.explanationComparisonMode
            && Objects.equals(traderRegion, alertRule.traderRegion)
            && Objects.equals(capacity, alertRule.capacity)
            && Objects.equals(alertColour, alertRule.alertColour)
            && Objects.equals(isClient, alertRule.isClient)
            && Objects.equals(toastNotification, alertRule.toastNotification)
            && Objects.equals(creatorFoxDesk, alertRule.creatorFoxDesk)
            && Objects.equals(baseRuleId, alertRule.baseRuleId)
            && Objects.equals(activeFrom, alertRule.activeFrom)
            && Objects.equals(activeTo, alertRule.activeTo)
            && Objects.equals(overfilled, alertRule.overfilled)
            && Objects.equals(isClaimed, alertRule.isClaimed)
            && Objects.equals(cancelReason, alertRule.cancelReason)
            && Objects.equals(rejectReason, alertRule.rejectReason)
            && Objects.equals(resolvable, alertRule.resolvable)
            && Objects.equals(snoozable, alertRule.snoozable)
            && Objects.equals(limitUsageAlertThreshold, alertRule.limitUsageAlertThreshold)
            && Objects.equals(limitUsageAlertTime, alertRule.limitUsageAlertTime)
            && Objects.equals(limitUsageAlertTimezone, alertRule.limitUsageAlertTimezone)
            && Objects.equals(notifyViaLaunchpad, alertRule.notifyViaLaunchpad)
            && Objects.equals(hasOverfilledLeg, alertRule.hasOverfilledLeg)
            && Objects.equals(isLegged, alertRule.isLegged)
            && Objects.equals(alertCategory, alertRule.alertCategory)
            && Objects.equals(actorsSet, alertRule.actorsSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            expiryTimestamp,
            id,
            version,
            updateTime,
            kerberos,
            targetDesk,
            message,
            orderId,
            orderIdComparisonMode,
            orderLevel,
            alertTrigger,
            enabled,
            creatorId,
            customerTrader,
            accountId,
            bbgUUID,
            tradingAlgorithm,
            instrumentSymbolType,
            instrumentSymbol,
            instrumentSymbolComparisonMode,
            interactionLevel,
            timeInForce,
            orderStatus,
            symphonyEnabled,
            internalTCAReport,
            elixirReport,
            externalTCAReport,
            audioEnabled,
            softDelete,
            recapEmail,
            genericEmail,
            clientRecapEmail,
            bbgAlert,
            clientLimitUsageEmail,
            hasHistoricalClientRecapEmail,
            hasHistoricalBbgAlert,
            hasHistoricalClientLimitUsageEmail,
            creationDateTime,
            emailAddress,
            clientEmailAddress,
            recapTemplateName,
            recapTemplateId,
            orderByte,
            participationRateComparison,
            executionScheduleComparison,
            minQtyComparison,
            splitQtyComparison,
            autoExecutionSchedule,
            outsidePriceLimit,
            activationComparison,
            aheadBehindComparison,
            desktopPopup,
            mic,
            quantity,
            filledQuantity,
            hasLimit,
            wholesaleIndicatorType,
            settleAsFixed,
            synthetic,
            venue,
            micFamily,
            sensitiveAlertTypes,
            explanation,
            explanationComparisonMode,
            traderRegion,
            transactionType,
            capacity,
            alertColour,
            isClient,
            sendOrderDetailsInSymphony,
            toastNotification,
            creatorFoxDesk,
            baseRuleId,
            activeFrom,
            activeTo,
            overfilled,
            isClaimed,
            cancelReason,
            rejectReason,
            resolvable,
            snoozable,
            latencyThresholdMs,
            limitUsageAlertThreshold,
            limitUsageAlertTime,
            limitUsageAlertTimezone,
            notifyViaLaunchpad,
            hasOverfilledLeg,
            isLegged,
            alertCategory,
            priority,
            actorsSet
        );
    }

    @Override
    public String toString() {
        return "AlertRule{"
            + "expiryTimestamp=" + expiryTimestamp
            + ", id='" + id + '\''
            + ", version=" + version
            + ", updateTime=" + updateTime
            + ", kerberos='" + kerberos + '\''
            + ", targetDesk='" + targetDesk + '\''
            + ", message='" + message + '\''
            + ", orderId='" + orderId + '\''
            + ", orderIdComparisonMode=" + orderIdComparisonMode
            + ", orderLevel=" + orderLevel
            + ", alertTrigger=" + alertTrigger
            + ", enabled=" + enabled
            + ", creatorId=" + creatorId
            + ", customerTrader=" + customerTrader
            + ", accountId=" + accountId
            + ", bbgUUID=" + bbgUUID
            + ", tradingAlgorithm=" + tradingAlgorithm
            + ", instrumentSymbolType=" + instrumentSymbolType
            + ", instrumentSymbol='" + instrumentSymbol + '\''
            + ", instrumentSymbolComparisonMode=" + instrumentSymbolComparisonMode
            + ", interactionLevel=" + interactionLevel
            + ", timeInForce=" + timeInForce
            + ", orderStatus=" + orderStatus
            + ", symphonyEnabled=" + symphonyEnabled
            + ", internalTCAReport=" + internalTCAReport
            + ", elixirReport=" + elixirReport
            + ", externalTCAReport=" + externalTCAReport
            + ", audioEnabled=" + audioEnabled
            + ", softDelete=" + softDelete
            + ", recapEmail=" + recapEmail
            + ", genericEmail=" + genericEmail
            + ", clientRecapEmail=" + clientRecapEmail
            + ", bbgAlert=" + bbgAlert
            + ", clientLimitUsageEmail=" + clientLimitUsageEmail
            + ", hasHistoricalClientRecapEmail=" + hasHistoricalClientRecapEmail
            + ", hasHistoricalBbgAlert=" + hasHistoricalBbgAlert
            + ", hasHistoricalClientLimitUsageEmail=" + hasHistoricalClientLimitUsageEmail
            + ", creationDateTime='" + creationDateTime + '\''
            + ", emailAddress='" + emailAddress + '\''
            + ", clientEmailAddress='" + clientEmailAddress + '\''
            + ", recapTemplateName='" + recapTemplateName + '\''
            + ", recapTemplateId='" + recapTemplateId + '\''
            + ", orderByte='" + orderByte + '\''
            + ", participationRateComparison=" + participationRateComparison
            + ", executionScheduleComparison=" + executionScheduleComparison
            + ", minQtyComparison=" + minQtyComparison
            + ", splitQtyComparison=" + splitQtyComparison
            + ", autoExecutionSchedule=" + autoExecutionSchedule
            + ", outsidePriceLimit=" + outsidePriceLimit
            + ", activationComparison=" + activationComparison
            + ", aheadBehindComparison=" + aheadBehindComparison
            + ", desktopPopup=" + desktopPopup
            + ", mic='" + mic + '\''
            + ", sourceChannel='" + sourceChannel + '\''
            + ", quantity=" + quantity
            + ", filledQuantity=" + filledQuantity
            + ", hasLimit=" + hasLimit
            + ", wholesaleIndicatorType=" + wholesaleIndicatorType
            + ", settleAsFixed=" + settleAsFixed
            + ", synthetic=" + synthetic
            + ", latencyThresholdMs=" + latencyThresholdMs
            + ", venue=" + venue
            + ", micFamily=" + micFamily
            + ", sensitiveAlertTypes=" + sensitiveAlertTypes
            + ", explanation='" + explanation + '\''
            + ", explanationComparisonMode=" + explanationComparisonMode
            + ", traderRegion='" + traderRegion + '\''
            + ", transactionType=" + transactionType
            + ", capacity='" + capacity + '\''
            + ", alertColour='" + alertColour + '\''
            + ", isClient=" + isClient
            + ", sendOrderDetailsInSymphony=" + sendOrderDetailsInSymphony
            + ", toastNotification=" + toastNotification
            + ", creatorFoxDesk=" + creatorFoxDesk
            + ", baseRuleId='" + baseRuleId + '\''
            + ", activeFrom='" + activeFrom + '\''
            + ", activeTo='" + activeTo + '\''
            + ", overfilled=" + overfilled
            + ", isClaimed=" + isClaimed
            + ", cancelReason=" + cancelReason
            + ", rejectReason=" + rejectReason
            + ", resolvable=" + resolvable
            + ", snoozable=" + snoozable
            + ", limitUsageAlertThreshold=" + limitUsageAlertThreshold
            + ", limitUsageAlertTime='" + limitUsageAlertTime + '\''
            + ", limitUsageAlertTimezone='" + limitUsageAlertTimezone + '\''
            + ", notifyViaLaunchpad=" + notifyViaLaunchpad
            + ", hasOverfilledLeg=" + hasOverfilledLeg
            + ", isLegged=" + isLegged
            + ", alertCategory='" + alertCategory + '\''
            + ", priority=" + priority
            + ", actorsSet=" + actorsSet
            + '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static class AlertRuleBuilder {
        private final AlertRule building;

        // Required for Jackson
        public AlertRuleBuilder() {
            building = new AlertRule();
        }

        public AlertRuleBuilder(AlertRule alertRule) {
            building = SerializationUtils.clone(alertRule);
        }

        public AlertRuleBuilder setId(String id) {
            building.id = id;
            return this;
        }

        public AlertRuleBuilder setVersion(int version) {
            building.version = version;
            return this;
        }

        public AlertRuleBuilder setUpdateTime(Long updateTime) {
            building.updateTime = updateTime;
            return this;
        }

        public AlertRuleBuilder setKerberos(String kerberos) {
            building.kerberos = kerberos;
            return this;
        }

        public AlertRuleBuilder setTargetDesk(String targetDesk) {
            building.targetDesk = targetDesk;
            return this;
        }

        public AlertRuleBuilder setMessage(String message) {
            building.message = message;
            return this;
        }

        public AlertRuleBuilder setEmailAddress(String emailAddress) {
            building.emailAddress = emailAddress;
            return this;
        }

        public AlertRuleBuilder setClientEmailAddress(String clientEmailAddress) {
            building.clientEmailAddress = clientEmailAddress;
            return this;
        }

        public AlertRuleBuilder setRecapTemplateName(String recapTemplateName) {
            building.recapTemplateName = recapTemplateName;
            return this;
        }

        public AlertRuleBuilder setOrderByte(String orderByte) {
            building.orderByte = orderByte;
            return this;
        }

        public AlertRuleBuilder setRecapTemplateId(String recapTemplateId) {
            building.recapTemplateId = recapTemplateId;
            return this;
        }

        public AlertRuleBuilder setEnabled(boolean enabled) {
            building.enabled = enabled;
            return this;
        }

        public AlertRuleBuilder setSoftDelete(boolean softDelete) {
            building.softDelete = softDelete;
            return this;
        }

        public AlertRuleBuilder setRecapEmail(boolean recapEmail) {
            building.recapEmail = recapEmail;
            return this;
        }

        public AlertRuleBuilder setClientRecapEmail(boolean clientRecapEmail) {
            building.clientRecapEmail = clientRecapEmail;
            return this;
        }

        public AlertRuleBuilder setBbgAlert(boolean bbgAlert) {
            building.bbgAlert = bbgAlert;
            return this;
        }

        public AlertRuleBuilder setClientLimitUsageEmail(boolean clientLimitUsageEmail) {
            building.clientLimitUsageEmail = clientLimitUsageEmail;
            return this;
        }

        public AlertRuleBuilder setHasHistoricalClientRecapEmail(boolean hasHistoricalClientRecapEmail) {
            building.hasHistoricalClientRecapEmail = hasHistoricalClientRecapEmail;
            return this;
        }

        public AlertRuleBuilder setHasHistoricalBbgAlert(boolean hasHistoricalBbgAlert) {
            building.hasHistoricalBbgAlert = hasHistoricalBbgAlert;
            return this;
        }

        public AlertRuleBuilder setHasHistoricalClientLimitUsageEmail(boolean hasHistoricalClientLimitUsageEmail) {
            building.hasHistoricalClientLimitUsageEmail = hasHistoricalClientLimitUsageEmail;
            return this;
        }

        public AlertRuleBuilder setAlertTrigger(AlertTrigger alertTrigger) {
            building.alertTrigger = alertTrigger;
            return this;
        }

        public AlertRuleBuilder setCreationDateTime(String creationDateTime) {
            building.creationDateTime = creationDateTime;
            return this;
        }

        public AlertRuleBuilder setGenericEmail(boolean genericEmail) {
            building.genericEmail = genericEmail;
            return this;
        }

        public AlertRuleBuilder setSymphonyEnabled(boolean symphonyEnabled) {
            building.symphonyEnabled = symphonyEnabled;
            return this;
        }

        public AlertRuleBuilder setSendOrderDetailsInSymphony(boolean sendOrderDetailsInSymphony) {
            building.sendOrderDetailsInSymphony = sendOrderDetailsInSymphony;
            return this;
        }

        public AlertRuleBuilder setInternalTCAReport(boolean internalTCAReport) {
            building.internalTCAReport = internalTCAReport;
            return this;
        }

        public AlertRuleBuilder setElixirReport(boolean elixirReport) {
            building.elixirReport = elixirReport;
            return this;
        }

        public AlertRuleBuilder setExternalTCAReport(boolean externalTCAReport) {
            building.externalTCAReport = externalTCAReport;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setCreatorId(HashSet<String> creatorId) {
            building.creatorId = creatorId;
            return this;
        }

        public AlertRuleBuilder setAudioFilename(String audioFilename) {
            building.audioFilename = audioFilename;
            return this;
        }

        public AlertRuleBuilder setSymphonyRoomName(String symphonyRoomName) {
            building.symphonyRoomName = symphonyRoomName;
            return this;
        }

        public AlertRuleBuilder setSymphonyTeamRoomName(String symphonyTeamRoomName) {
            building.symphonyTeamRoomName = symphonyTeamRoomName;
            return this;
        }

        public AlertRuleBuilder setAudioEnabled(boolean audioEnabled) {
            building.audioEnabled = audioEnabled;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setCustomerTrader(HashSet<String> customerTrader) {
            building.customerTrader = customerTrader;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setAccountId(HashSet<String> accountId) {
            building.accountId = accountId;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setBbgUUID(HashSet<String> bbgUUID) {
            building.bbgUUID = bbgUUID;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setTradingAlgorithm(HashSet<Order.Type> tradingAlgorithm) {
            building.tradingAlgorithm = tradingAlgorithm;
            return this;
        }

        public AlertRuleBuilder setInstrumentSymbolType(InstrumentSymbols.Type instrumentSymbolType) {
            building.instrumentSymbolType = instrumentSymbolType;
            return this;
        }

        public AlertRuleBuilder setInstrumentSymbol(String instrumentSymbol) {
            building.instrumentSymbolComparisonMode = AlertFieldComparisonMode.Modes.find(instrumentSymbol);
            building.parsedInstrumentSymbol = building.instrumentSymbolComparisonMode.parse(instrumentSymbol);
            building.instrumentSymbol = instrumentSymbol;
            return this;
        }

        public AlertRuleBuilder setInteractionLevel(Order.InteractionLevel interactionLevel) {
            building.interactionLevel = interactionLevel;
            return this;
        }

        public AlertRuleBuilder setTimeInForce(HashSet<Order.TimeInForce> timeInForce) {
            building.timeInForce = timeInForce;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setOrderStatus(HashSet<PrefixEnum<Order.Status>> orderStatus) {
            building.orderStatus = orderStatus;
            return this;
        }

        public AlertRuleBuilder setExpiryTimestamp(Long expiryTimestamp) {
            building.expiryTimestamp = expiryTimestamp;
            return this;
        }

        public AlertRuleBuilder setOrderId(String orderId) {
            building.orderIdComparisonMode = AlertFieldComparisonMode.Modes.find(orderId);
            building.parsedOrderId = building.orderIdComparisonMode.parse(orderId);
            building.orderId = orderId;
            return this;
        }

        public AlertRuleBuilder setOrderLevel(HierarchyType orderLevel) {
            building.orderLevel = orderLevel;
            return this;
        }

        public AlertRuleBuilder setParticipationRateComparison(StatComparison<Double> participationRateComparison) {
            building.participationRateComparison = participationRateComparison;
            return this;
        }

        public AlertRuleBuilder setAheadBehindComparison(StatComparison<Double> aheadBehindComparison) {
            building.aheadBehindComparison = aheadBehindComparison;
            return this;
        }

        public AlertRuleBuilder setExecutionScheduleComparison(StatComparison<Double> executionScheduleComparison) {
            building.executionScheduleComparison = executionScheduleComparison;
            return this;
        }

        public AlertRuleBuilder setMinQtyComparison(StatComparison<Double> minQtyComparison) {
            building.minQtyComparison = minQtyComparison;
            return this;
        }

        public AlertRuleBuilder setSplitQtyComparison(StatComparison<Double> splitQtyComparison) {
            building.splitQtyComparison = splitQtyComparison;
            return this;
        }

        public AlertRuleBuilder setAutoExecutionSchedule(Boolean autoExecutionSchedule) {
            building.autoExecutionSchedule = autoExecutionSchedule;
            return this;
        }

        public AlertRuleBuilder setOutsidePriceLimit(Boolean outsidePriceLimit) {
            building.outsidePriceLimit = outsidePriceLimit;
            return this;
        }

        public AlertRuleBuilder setActivationComparison(TimeComparison activationComparison) {
            building.activationComparison = activationComparison;
            return this;
        }

        public AlertRuleBuilder setDesktopPopup(Boolean desktopPopup) {
            building.desktopPopup = desktopPopup;
            return this;
        }

        public AlertRuleBuilder setMic(String mic) {
            building.mic = mic;
            return this;
        }

        public AlertRuleBuilder setSourceChannel(String sourceChannel) {
            building.sourceChannel = sourceChannel;
            return this;
        }

        public AlertRuleBuilder setQuantity(StatComparison<Double> quantity) {
            building.quantity = quantity;
            return this;
        }

        public AlertRuleBuilder setFilledQuantity(StatComparison<Double> filledQuantity) {
            building.filledQuantity = filledQuantity;
            return this;
        }

        public AlertRuleBuilder setHasLimit(Boolean hasLimit) {
            building.hasLimit = hasLimit;
            return this;
        }

        public AlertRuleBuilder setWholesaleIndicatorType(OrderData.Order.WholesaleIndicatorType wholesaleIndicatorType) {
            building.wholesaleIndicatorType = wholesaleIndicatorType;
            return this;
        }

        public AlertRuleBuilder setSettleAsFixed(Boolean settleAsFixed) {
            building.settleAsFixed = settleAsFixed;
            return this;
        }

        public AlertRuleBuilder setSynthetic(Boolean synthetic) {
            building.synthetic = synthetic;
            return this;
        }

        public AlertRuleBuilder setLatencyThresholdMs(Long latencyThresholdMs) {
            building.latencyThresholdMs = latencyThresholdMs;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setVenue(HashSet<String> venue) {
            building.venue = venue;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setMicFamily(HashSet<String> micFamily) {
            // 中文注释：/rests/alerts/rule 的创建请求会走 builder 反序列化，micFamily-only payload 能否过 400 取决于这里是否接住该字段。
            building.micFamily = micFamily;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setSensitiveAlertTypes(HashSet<Coverage.Alert.SensitiveAlertType> sensitiveAlertTypes) {
            building.sensitiveAlertTypes = sensitiveAlertTypes;
            return this;
        }

        public AlertRuleBuilder setExplanation(String explanation) {
            building.explanationComparisonMode = AlertFieldComparisonMode.Modes.find(explanation);
            building.parsedExplanation = building.explanationComparisonMode.parse(explanation);
            building.explanation = explanation;
            return this;
        }

        public AlertRuleBuilder setTraderRegion(String traderRegion) {
            building.traderRegion = traderRegion;
            return this;
        }

        public AlertRuleBuilder setTransactionType(OrderData.Order.TransactionType transactionType) {
            building.transactionType = transactionType;
            return this;
        }

        public AlertRuleBuilder setCapacity(String capacity) {
            building.capacity = capacity;
            return this;
        }

        public AlertRuleBuilder setAlertColour(String alertColour) {
            building.alertColour = alertColour;
            return this;
        }

        public AlertRuleBuilder setIsClient(Boolean isClient) {
            building.isClient = isClient;
            return this;
        }

        public AlertRuleBuilder setToastNotification(Boolean toastNotification) {
            building.toastNotification = toastNotification;
            return this;
        }

        public AlertRuleBuilder setCreatorFoxDesk(HashSet<String> creatorFoxDesk) {
            building.creatorFoxDesk = creatorFoxDesk;
            return this;
        }

        public AlertRuleBuilder setBaseRuleId(String baseRuleId) {
            building.baseRuleId = baseRuleId;
            return this;
        }

        public AlertRuleBuilder setActiveFrom(String activeFrom) {
            building.activeFrom = activeFrom;
            return this;
        }

        public AlertRuleBuilder setActiveTo(String activeTo) {
            building.activeTo = activeTo;
            return this;
        }

        public AlertRuleBuilder setOverfilled(Boolean overfilled) {
            building.overfilled = overfilled;
            return this;
        }

        public AlertRuleBuilder setIsClaimed(Boolean isClaimed) {
            building.isClaimed = isClaimed;
            return this;
        }

        public AlertRuleBuilder setCancelReason(Order.CancelReason cancelReason) {
            building.cancelReason = cancelReason;
            return this;
        }

        public AlertRuleBuilder setRejectReason(Order.RejectReason rejectReason) {
            building.rejectReason = rejectReason;
            return this;
        }

        public AlertRuleBuilder setResolvable(Boolean resolvable) {
            building.resolvable = resolvable;
            return this;
        }

        public AlertRuleBuilder setSnoozable(Boolean snoozable) {
            building.snoozable = snoozable;
            return this;
        }

        public AlertRuleBuilder setLimitUsageAlertThreshold(StatComparison<Double> limitUsageAlertThreshold) {
            building.limitUsageAlertThreshold = limitUsageAlertThreshold;
            return this;
        }

        public AlertRuleBuilder setLimitUsageAlertTime(String limitUsageAlertTime) {
            building.limitUsageAlertTime = limitUsageAlertTime;
            return this;
        }

        public AlertRuleBuilder setLimitUsageAlertTimezone(String limitUsageAlertTimezone) {
            building.limitUsageAlertTimezone = limitUsageAlertTimezone;
            return this;
        }

        public AlertRuleBuilder setHasOverfilledLeg(Boolean hasOverfilledLeg) {
            building.hasOverfilledLeg = hasOverfilledLeg;
            return this;
        }

        public AlertRuleBuilder setNotifyViaLaunchpad(Boolean notifyViaLaunchpad) {
            building.notifyViaLaunchpad = notifyViaLaunchpad;
            return this;
        }

        public AlertRuleBuilder setIsLegged(Boolean isLegged) {
            building.isLegged = isLegged;
            return this;
        }

        public AlertRuleBuilder setAlertCategory(String alertCategory) {
            building.alertCategory = alertCategory;
            return this;
        }

        public AlertRuleBuilder setPriority(int priority) {
            building.priority = priority;
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public AlertRuleBuilder setActorsSet(HashSet<String> actorsSet) {
            building.actorsSet = actorsSet;
            return this;
        }

        public AlertRule build() {
            return SerializationUtils.clone(building);
        }
    }
}
