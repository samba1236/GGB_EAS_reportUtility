package com.optum.c360.fileoperation

import com.optum.c360.elastic.ErrorDetails
import com.optum.c360.elastic.TopicStatistics
import com.optum.c360.exception.GenericException
import org.springframework.core.env.Environment
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.ResourceLoader
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

class ExcelWriterTest extends Specification {

    def "write with data Test"() {

        //Mock save file to avoid exception
        setup:
        ExcelWriter writer = new ExcelWriter()
        writer.environment = getEnvironment()
        writer.resourceLoader = getResourceLoader()

        when:
        writer.write(getTopicStats())

        then:
        thrown(GenericException)
    }

    def "write without data Test"() {

        //Mock save file to avoid exception
        setup:
        ExcelWriter writer = new ExcelWriter()
        writer.environment = getEnvironment()
        writer.resourceLoader = getResourceLoader()

        when:
        writer.write(getEmptyErrorStats())

        then:
        thrown(GenericException)
    }

    def getEnvironment() {
        Environment environment = new MockEnvironment()
        environment.setProperty("file-name-prefix","file-prefix")
        environment.setProperty("file-storage-location","/some/test/location/")
        environment.setProperty("error-dictionary-file-path","src/test/resources/error-dictionary/error-dictionary.txt")
        return environment
    }

    def getTopicStats() {
        List<TopicStatistics> topicStatisticsList = new ArrayList<>()

        TopicStatistics topicStatistics1 = new TopicStatistics()

        Set<String> dates = new LinkedHashSet<>()
        dates.add("2019-07-01")
        dates.add("2019-07-02")
        dates.add("2019-07-03")
        topicStatistics1.setDates(dates)

        topicStatistics1.setErrorCount(300L)
        topicStatistics1.setSuccessCount(100L)
        topicStatistics1.setTopic("subject-area-1")

        // ERROR MAP
        Map<String, List<ErrorDetails>> errorMap1 = new LinkedHashMap<>();

        ErrorDetails errorDetails1 = new ErrorDetails()
        errorDetails1.setErrorMessage("Error Message 1")
        errorDetails1.setCount(100L)

        ErrorDetails errorDetails2 = new ErrorDetails()
        errorDetails2.setErrorMessage("Error Message 2")
        errorDetails1.setCount(100L)

        List<ErrorDetails> errorDetailsList = new ArrayList<>()
        errorDetailsList.add(errorDetails1)
        errorDetailsList.add(errorDetails2)

        errorMap1.put("2019-07-01", errorDetailsList)
        //


        //SUCCESS MAP
        Map<String, Map<String, Long>> successMap1 = new LinkedHashMap<>();

        Map<String, Long> dailySucessMap = new LinkedHashMap<>()
        dailySucessMap.put("2019-07-01", 50L)
        dailySucessMap.put("2019-07-02", 50L)
        successMap1.put("subject-area-1", dailySucessMap)
        //

        topicStatistics1.setErrorMap(errorMap1);
        topicStatistics1.setSuccessMap(successMap1);
        topicStatisticsList.add(topicStatistics1)

        return topicStatisticsList
    }


    def getEmptyErrorStats() {
        List<TopicStatistics> topicStatisticsList = new ArrayList<>()

        TopicStatistics topicStatistics1 = new TopicStatistics()
        Set<String> dates = new LinkedHashSet<>()
        topicStatistics1.setDates(dates)

        topicStatistics1.setErrorCount(0L)
        topicStatistics1.setSuccessCount(0L)
        topicStatistics1.setTopic("subject-area-2")

        topicStatisticsList.add(topicStatistics1)

        return topicStatisticsList
    }

    def resourceLoader = Mock(ResourceLoader) {
        getResource("src/test/resources/error-dictionary/error-dictionary.txt") >> new InputStreamResource(new FileInputStream("src/test/resources/error-dictionary/error-dictionary.txt"))
    }
}

//blank stats with empty dates to cover 163 to 174
