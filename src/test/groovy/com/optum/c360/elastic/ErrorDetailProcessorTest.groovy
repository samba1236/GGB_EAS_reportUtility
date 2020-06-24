package com.optum.c360.elastic

import com.optum.c360.email.EmailNotification
import com.optum.c360.fileoperation.ExcelWriter
import org.apache.lucene.util.BytesRef
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.ml.job.results.Bucket
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
import spock.lang.Shared
import spock.lang.Specification

import java.time.ZoneId
import java.time.ZonedDateTime

class ErrorDetailProcessorTest extends Specification {

    //@Shared
    //ErrorDetailProcessor errorDetailProcessor
    //String lastRunDate


//    def "process Test"() {
//        setup:
//        //String lastRunDate="2019-09-19T00:00:00"
//        ErrorDetailProcessor errorDetailProcessor  = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),"2019-09-19T00:00:00")
//
//
//        when:
//        errorDetailProcessor.process()
//
//        then:
//        assert true
//    }

//    def "process Exception Test"() {
//        setup:
//        //String lastRunDate="2019-09-19T00:00:00"
//        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),"2019-09-19T00:00:00") {
//            @Override
//            protected void processErrorData(TopicStatistics topicStatistics, SearchResponse errorResponse) {
//            }
//
//            @Override
//            protected void processSuccessData(String subjectArea, TopicStatistics topicStatistics, SearchResponse successResponse) {
//            }
//
//            @Override
//            protected SearchResponse executeQuery(SearchRequest request, RequestOptions options) {
//                return getSearchResponse()
//            }
//
//            @Override
//            protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
//                return getParsedDateHistogram()
//            }
//
//            @Override
//            protected void closeElasticConnection() {
//
//            }
//        }
//
//        when:
//        errorDetailProcessor.process()
//
//        then:
//        assert true
//    }

//    def "processSuccessData without success data Test"() {
//        setup:
//        String lastRunDate;
//        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),lastRunDate) {
//            @Override
//            protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
//                return getParsedDateHistogram()
//            }
//        }
//
//        when:
//        errorDetailProcessor.processSuccessData("subject-area-1", new TopicStatistics(), getSearchResponse())
//
//        then:
//        assert true
//
//    }

//    def "processSuccessData with success data Test"() {
//        setup:
//        String lastRunDate;
//        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),lastRunDate) {
//            @Override
//            protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
//                return getParsedDateHistogramWithBuckets()
//            }
//        }
//
//        when:
//        errorDetailProcessor.processSuccessData("subject-area-1", new TopicStatistics(), getSearchResponse())
//
//        then:
//        assert true
//
//    }

//    def "processErrorData without error data Test"() {
//        setup:
//        String lastRunDate;
//        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),lastRunDate) {
//            @Override
//            protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
//                return getParsedDateHistogram()
//            }
//
//            @Override
//            protected Terms getTerms(Histogram.Bucket bucket, String aggregatorName) {
//                return getTermsWithEmptyBuckets()
//            }
//        }
//
//        when:
//        errorDetailProcessor.processErrorData(new TopicStatistics(), getSearchResponse())
//
//        then:
//        assert true
//    }
//
//    def "processErrorData with error data Test"() {
//        setup:
//        String lastRunDate;
//        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
//                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),lastRunDate) {
//            @Override
//            protected ParsedDateHistogram getHistogram(SearchResponse searchResponse, String aggregatorName) {
//                return getParsedDateHistogramWithBuckets()
//            }
//
//            @Override
//            protected Terms getTerms(Histogram.Bucket bucket, String aggregatorName) {
//                return getTermsWithBuckets()
//            }
//        }
//
//        when:
//        errorDetailProcessor.processErrorData(new TopicStatistics(), getSearchResponse())
//
//        then:
//        assert true
//
//    }

    def "closeConnection NULL Test"() {
        setup:
        String lastRunDate;
        ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( getElasticSearchProperties(), getEnvironment(),
                getExcelWriter(), getEmailNotificationBean(), getElasticSearchConfiguration(),lastRunDate) {
        }
        errorDetailProcessor.restHighLevelClient = null
        when:
        errorDetailProcessor.closeElasticConnection()
        then:
        assert true
    }

    def getApplicationContext() {
        Mock(ApplicationContext) {
            getBean(_ as Class) >> getRestHighLevelClient()
        }
    }

    def getSearchResponse() {
        Mock(SearchResponse) {
            getAggregations() >> getAggregationObj()
            getHits() >> getSearchHits()
        }
    }

    def getSearchHits() {
        SearchHits hits = new SearchHits()
        hits.totalHits = 10
        return hits
    }

    def getAggregationObj() {
        Mock(Aggregations) {
            get(_ as String) >> Mock(ParsedHistogram)
        }
    }

    def getParsedDateHistogram() {
        Mock(ParsedDateHistogram) {
            getBuckets() >> {
                new ArrayList<Bucket>()
            }
        }
    }

    def getParsedDateHistogramWithBuckets() {
        Mock(ParsedDateHistogram) {
            getBuckets() >> {
                ArrayList<ParsedDateHistogram.ParsedBucket> bucketList = new ArrayList<>()
                ParsedDateHistogram.ParsedBucket b = new ParsedDateHistogram.ParsedBucket()
                b.key = 10
                b.docCount = 10
                b.keyAsString = "2019-07-12T00:00:00.000Z"

                bucketList.add(b)
                return bucketList
            }
        }
    }

    def getTermsWithEmptyBuckets() {
        Mock(ParsedStringTerms) {
            getBuckets() >> {
                new ArrayList<>()
            }
        }
    }

    def getTermsWithBuckets() {
        Mock(ParsedStringTerms) {
            getBuckets() >> {
                ArrayList<ParsedStringTerms.ParsedBucket> bucketList = new ArrayList<>()
                ParsedStringTerms.ParsedBucket b = new ParsedStringTerms.ParsedBucket()
                b.key = new BytesRef("Some error".bytes)
                b.docCount = 10
                b.keyAsString = "2019-07-12T00:00:00.000Z"

                bucketList.add(b)
                return bucketList
            }
        }
    }

    def getRestHighLevelClient() {
        Mock(RestHighLevelClient) {
            search(_ as SearchRequest, _ as RequestOptions) >> Mock(SearchResponse)
        }
    }

    def getCassandraDriver() {
        Mock(CassandraDriver) {
            getLastRun() >> ZonedDateTime.now(ZoneId.of("GMT"))
            updateLastRun(_ as ZonedDateTime) >> null
        }
    }

    def getElasticSearchConfiguration() {
        Mock(ElasticSearchConfiguration) {

        }
    }

    def lastRunTime
    def getEnvironment() {
        Environment environment = new MockEnvironment()
        environment.setProperty("file-storage-location", "/app/dir")
        environment.setProperty("file-name-prefix", "some-file-prefix")
        environment.setProperty("first-run-time", "2019-07-03T00:00:00")
        environment.setProperty("application-id", "ggb-reporting-utility")
        environment.setProperty("source.name", "GGB")
        environment.setProperty("spring.profiles.active", "abc")
        environment.setProperty("AES_KEY", "5b8c6126e439fdf4f0c5c690f836d58e")

        return environment
    }

    def getElasticSearchProperties() {
        ElasticSearchProperties elasticSearchProperties = new ElasticSearchProperties()
        List<String> hosts = new ArrayList<>()
        hosts.add("https://localhost1:9092")
        hosts.add("https://localhost2:9092")
        elasticSearchProperties.setHosts(hosts)
        elasticSearchProperties.setUsername("user")
        elasticSearchProperties.setPassword("1366d3cfad1675d45e41716499813f5af7e43783744f710d65692d8c12a5a66ecd35f2b0aee8ff57fd67a309")
        elasticSearchProperties.setTruststoreFile("src/test/resources/unit-test.jks")
        elasticSearchProperties.setTruststorePassword("23B137D6F6D75AA5218600D9C4B987D233A2EE00AC4F5B81A4F5E1EE1766007A16009821715E05C0B044F9E6")
        elasticSearchProperties.setKeystoreFile("src/test/resources/unit-test-1.jks")
        elasticSearchProperties.setKeystorePassword("23B137D6F6D75AA5218600D9C4B987D233A2EE00AC4F5B81A4F5E1EE1766007A16009821715E05C0B044F9E6")
        elasticSearchProperties.setSuccessIndex("dummy-success-index")
        elasticSearchProperties.setErrorIndex("dummy-error-index")
        List<String> subjectAreas = new ArrayList<>()
        subjectAreas.add("subject_area_1")
        subjectAreas.add("subject_area_2")
        elasticSearchProperties.setSubjectAreas(subjectAreas)

        return elasticSearchProperties
    }

    def getExcelWriter() {
        Mock(ExcelWriter) {
            write(_ as List<String>) >> Mock(File)
        }
    }

    def getEmailNotificationBean() {
        Mock(EmailNotification) {
            sendEmailNotificationWithAttachment(_ as ZonedDateTime, _ as File) >> null
        }
    }
}