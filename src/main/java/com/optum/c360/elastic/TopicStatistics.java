package com.optum.c360.elastic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

public class TopicStatistics {
    String topic;
    Map<String, List<ErrorDetails>> errorMap;
    Map<String, Map<String, Long>> successMap;
    Set<String> dates = new LinkedHashSet<>();

    long successCount;
    long errorCount;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, List<ErrorDetails>> getErrorMap() {
        return errorMap;
    }

    public void setErrorMap(Map<String, List<ErrorDetails>> errorMap) {
        this.errorMap = errorMap;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public Set<String> getDates() {
        return dates;
    }

    public void setDates(Set<String> dates) {
        this.dates = dates;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public Map<String, Map<String, Long>> getSuccessMap() {
        return successMap;
    }

    public void setSuccessMap(Map<String, Map<String, Long>> successMap) {
        this.successMap = successMap;
    }
}

