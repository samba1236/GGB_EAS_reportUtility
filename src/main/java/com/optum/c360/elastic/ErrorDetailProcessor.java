package com.optum.c360.elastic;

import com.optum.c360.constants.ApplicationConstants;
import com.optum.c360.email.EmailNotification;
import com.optum.c360.exception.GenericException;
import com.optum.c360.fileoperation.ExcelWriter;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ErrorDetailProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDetailProcessor.class);

    private static final String TERM_AGGREGATOR = "termAggregation";
    private static final String HISTOGRAM_AGGREGATOR = "histogramAggregation";
    private static final String STREAM_LOGS = "stream_logs";


    // CassandraDriver cassandraDriver;

    RestHighLevelClient restHighLevelClient;

    ElasticSearchProperties elasticSearchProperties;

    Environment environment;

    ExcelWriter excelWriter;

    EmailNotification emailNotification;

    ElasticSearchConfiguration elasticSearchConfiguration;

    String lastRunDate;

    public ErrorDetailProcessor( ElasticSearchProperties elasticSearchProperties, Environment environment, ExcelWriter excelWriter, EmailNotification emailNotification, ElasticSearchConfiguration elasticSearchConfiguration, String lastRunDate) {
        //this.cassandraDriver = cassandraDriver;
        this.elasticSearchProperties = elasticSearchProperties;
        this.environment = environment;
        this.excelWriter = excelWriter;
        this.emailNotification = emailNotification;
        this.elasticSearchConfiguration = elasticSearchConfiguration;
        this.lastRunDate = lastRunDate;

        System.out.println(this.lastRunDate);
    }

    public void process() {
        try {
            System.out.println(this.lastRunDate);
            ZonedDateTime lastRun = ZonedDateTime.of(LocalDateTime.parse(this.lastRunDate, DateTimeFormatter.ofPattern(ApplicationConstants.FIRST_RUN_TIME_FORMAT)), ZoneId.of("GMT"));
            ZonedDateTime endTime = ZonedDateTime.now(ZoneId.of("GMT"));
            restHighLevelClient = elasticSearchConfiguration.getRestHighLevelClient();
            LOGGER.info("________Opened elastic Connection_________");

            File file = excelWriter.write(runQuery(lastRun, endTime));
            //emailNotification.sendEmailNotificationWithAttachment(lastRun, file);
        } catch (MalformedURLException | GenericException e) {
            throw new GenericException("Error connecting to Elastic/Cassandra/Writing data to file", e);
        } finally {
            closeElasticConnection();
        }
    }

    protected List<TopicStatistics> runQuery(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<TopicStatistics> statistics = new ArrayList<>();
        try {
            for (String subjectArea : elasticSearchProperties.getSubjectAreas()) {
                TopicStatistics stat = new TopicStatistics();

                stat.setTopic(subjectArea);

                BoolQueryBuilder query = QueryBuilders.boolQuery();
                query.must(QueryBuilders.matchQuery(ApplicationConstants.STRING_FIELD_SOURCE_NAME, environment.getProperty(ApplicationConstants.SOURCE_NAME)));
                query.must(QueryBuilders.matchQuery(ApplicationConstants.STRING_FIELD_SUBJECT_AREA, subjectArea));
//                query.must(QueryBuilders.matchQuery("track_total_hits", "true"));
//                query.must(QueryBuilders.matchQuery("rest_total_hits_as_int", "true"));
                query.must(QueryBuilders.rangeQuery(ApplicationConstants.STRING_FIELD_TIMESTAMP).format("epoch_millis").gte(startTime.toInstant().toEpochMilli()).lt(endTime.toInstant().toEpochMilli()));

                SearchSourceBuilder errorBuilder = buildErrorQuery();
//                System.out.println("query=="+query);
                errorBuilder.query(query);
                SearchRequest errorRequestObj = new SearchRequest(elasticSearchProperties.getErrorIndex());
                errorRequestObj.types(STREAM_LOGS);
                errorRequestObj.source(errorBuilder);

                SearchSourceBuilder successBuilder = buildSuccessQuery();
                successBuilder.query(query);
                SearchRequest successRequestObj = new SearchRequest(elasticSearchProperties.getSuccessIndex());
                successRequestObj.types(STREAM_LOGS);
                successRequestObj.source(successBuilder);
//                System.out.println("successRequestObj"+successRequestObj);


                SearchResponse errorResponse = executeQuery(errorRequestObj, RequestOptions.DEFAULT);
                processErrorData(stat, errorResponse);

                SearchResponse successResponse = executeQuery(successRequestObj, RequestOptions.DEFAULT);
                System.out.println("successResponse"+ successResponse);
                processSuccessData(subjectArea, stat, successResponse);

                List<String> dateList = new ArrayList<>(stat.getDates());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                Collections.sort(dateList, (s1, s2) -> LocalDate.parse(s1, formatter).compareTo(LocalDate.parse(s2, formatter)));

                stat.getDates().clear();
                stat.getDates().addAll(dateList);

                statistics.add(stat);
            }
        } catch (GenericException e) {
            LOGGER.error("Exception while processing elastic error data ");
            throw new GenericException(e.getMessage(), e);
        }
        return statistics;
    }

    protected SearchResponse executeQuery(SearchRequest request, RequestOptions options) {
        try{
            return restHighLevelClient.search(request, options);
        } catch (IOException e) {
            throw new GenericException(e.getMessage(), e);
        }
    }

    protected void processSuccessData(String subjectArea, TopicStatistics topicStatistics, SearchResponse successResponse) {
        Map<String, Map<String, Long>> successMap = new LinkedHashMap<>();
        ParsedDateHistogram dateHistogram = getHistogram(successResponse, HISTOGRAM_AGGREGATOR);
        if (dateHistogram.getBuckets().isEmpty()) {
            LOGGER.info("No Success data found in elastic for {}", topicStatistics.getTopic());
        } else {
            LOGGER.info("Success data found in elastic for {} ", topicStatistics.getTopic());

            Map<String, Long> dateAndSuccessMap = new LinkedHashMap<>();
            LOGGER.info("SUCCESS");
            long successCount = 0;
            for (Histogram.Bucket entry : dateHistogram.getBuckets()) {

                String dateKey = entry.getKey().toString().split("T")[0];
                long dateSuccessValue = entry.getDocCount();
                dateAndSuccessMap.put(dateKey, dateSuccessValue);

                topicStatistics.getDates().add(dateKey);
                LOGGER.info("{} --> {}", dateKey, dateSuccessValue);

                successCount = successCount + dateSuccessValue;
            }
            successMap.put(subjectArea, dateAndSuccessMap);
            topicStatistics.setSuccessCount(successCount);

//            System.out.println("successCount"+successResponse.getHits().getTotalHits().value);

        }
        topicStatistics.setSuccessMap(successMap);
//        topicStatistics.setSuccessCount(successResponse.getHits().getTotalHits().value);
//        LOGGER.debug("******************************************* Total Success --> {}", successResponse.getHits().getTotalHits().value);
        LOGGER.debug("******************************************* Total Success --> {}", topicStatistics.getSuccessCount());

    }


    protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
        return searchResponse.getAggregations().get(aggregatorName);
    }

    protected Terms getTerms(Histogram.Bucket bucket, String aggregatorName) {
        return bucket.getAggregations().get(aggregatorName);
    }

    protected void processErrorData(TopicStatistics topicStatistics, SearchResponse errorResponse) {
        ParsedDateHistogram hist = getHistogram(errorResponse, HISTOGRAM_AGGREGATOR);
        long totalError = 0;
        Map<String, List<ErrorDetails>> errorMap = new LinkedHashMap<>();
        if (hist.getBuckets().isEmpty()) {
            LOGGER.info("No Error data found in elastic for {}", topicStatistics.getTopic());
        } else {
            LOGGER.info("Error data found in elastic for {}", topicStatistics.getTopic());
            for (Histogram.Bucket entry : hist.getBuckets()) {
                String dateKey = entry.getKey().toString().split("T")[0];
                long dateErrorValue = entry.getDocCount();
                if (entry.getDocCount() > 0) {
                    LOGGER.debug("{} --> {}", dateKey, dateErrorValue);
                    totalError += entry.getDocCount();
                    Terms agg = getTerms(entry, TERM_AGGREGATOR);

                    List<ErrorDetails> errorList = new ArrayList<>();
//                    long errorCount = 0;
                    for (Terms.Bucket e : agg.getBuckets()) {
                        Object key = e.getKey();                    // bucket key
                        long docCount = e.getDocCount();            // Doc count

                        ErrorDetails error = new ErrorDetails();
                        error.setErrorMessage(key.toString());
                        error.setCount(docCount);
                        errorList.add(error);
//                        errorCount = errorCount + docCount;

                        LOGGER.debug("\t {} --> {}", key, docCount);
                    }
                    errorMap.put(dateKey, errorList);
                    topicStatistics.getDates().add(dateKey);
                }
            }
        }
        topicStatistics.setErrorMap(errorMap);
        topicStatistics.setErrorCount(totalError);
        LOGGER.debug("*******************************************Total Error --> {}", totalError);
    }

    private SearchSourceBuilder buildErrorQuery() {
        AggregationBuilder termAggregationBuilder = getTermAggregation(TERM_AGGREGATOR, "errorMessage");
        AggregationBuilder histogramAggregationBuilder = getHistogramBuilder(HISTOGRAM_AGGREGATOR, ApplicationConstants.STRING_FIELD_TIMESTAMP);

        histogramAggregationBuilder.subAggregation(termAggregationBuilder);

        SearchSourceBuilder errorBuilder = new SearchSourceBuilder();
        errorBuilder.aggregation(histogramAggregationBuilder);

        return errorBuilder;
    }

    private SearchSourceBuilder buildSuccessQuery() {
        AggregationBuilder histogramAggregationBuilder = getHistogramBuilder(HISTOGRAM_AGGREGATOR, ApplicationConstants.STRING_FIELD_TIMESTAMP);

        SearchSourceBuilder successBuilder = new SearchSourceBuilder();
        successBuilder.aggregation(histogramAggregationBuilder);

        return successBuilder;
    }

    private AggregationBuilder getTermAggregation(String aggregationName, String filedName) {
        return AggregationBuilders.terms(aggregationName).field(filedName);
    }

    private AggregationBuilder getHistogramBuilder(String aggregationName, String filedName) {
        return AggregationBuilders.dateHistogram(aggregationName)
                .field(filedName)
                .dateHistogramInterval(DateHistogramInterval.DAY);
    }

    protected void closeElasticConnection() {
        try {
            if (null != restHighLevelClient) {
                restHighLevelClient.close();
                System.exit(0);
                LOGGER.info("________Closed elastic Connection_________");
            }
        } catch (IOException ex) {
            LOGGER.error("error while closing resources", ex);
        }
    }
}
