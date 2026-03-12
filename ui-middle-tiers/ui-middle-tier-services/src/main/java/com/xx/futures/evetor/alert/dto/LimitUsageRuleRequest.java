package com.xx.futures.evetor.alert.dto;

import java.util.ArrayList;
import java.util.List;

public class LimitUsageRuleRequest {
    private String emailSubject;
    private List<String> mic = new ArrayList<>();
    private List<String> micFamily = new ArrayList<>();
    private String internalEmail;
    private String externalEmail;
    private String alertRuleType;
    private String limitUsageThreshold;
    private String limitUsageAlertTime;
    private String limitUsageAlertTimezone;
    private String accountType;
    private List<String> accountIds = new ArrayList<>();
    private boolean external;

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public List<String> getMic() {
        return mic;
    }

    public void setMic(List<String> mic) {
        this.mic = mic == null ? new ArrayList<>() : mic;
    }

    public List<String> getMicFamily() {
        return micFamily;
    }

    public void setMicFamily(List<String> micFamily) {
        this.micFamily = micFamily == null ? new ArrayList<>() : micFamily;
    }

    public String getInternalEmail() {
        return internalEmail;
    }

    public void setInternalEmail(String internalEmail) {
        this.internalEmail = internalEmail;
    }

    public String getExternalEmail() {
        return externalEmail;
    }

    public void setExternalEmail(String externalEmail) {
        this.externalEmail = externalEmail;
    }

    public String getAlertRuleType() {
        return alertRuleType;
    }

    public void setAlertRuleType(String alertRuleType) {
        this.alertRuleType = alertRuleType;
    }

    public String getLimitUsageThreshold() {
        return limitUsageThreshold;
    }

    public void setLimitUsageThreshold(String limitUsageThreshold) {
        this.limitUsageThreshold = limitUsageThreshold;
    }

    public String getLimitUsageAlertTime() {
        return limitUsageAlertTime;
    }

    public void setLimitUsageAlertTime(String limitUsageAlertTime) {
        this.limitUsageAlertTime = limitUsageAlertTime;
    }

    public String getLimitUsageAlertTimezone() {
        return limitUsageAlertTimezone;
    }

    public void setLimitUsageAlertTimezone(String limitUsageAlertTimezone) {
        this.limitUsageAlertTimezone = limitUsageAlertTimezone;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = accountIds == null ? new ArrayList<>() : accountIds;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public boolean hasMicSelection() {
        return !mic.isEmpty();
    }

    public boolean hasMicFamilySelection() {
        return !micFamily.isEmpty();
    }

    public boolean hasExclusiveVenueSelector() {
        return hasMicSelection() ^ hasMicFamilySelection();
    }
}
