package com.optum.c360.elastic

import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.ResourceLoader
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

class ElasticSearchConfigurationTest extends Specification {

    def "getRestHighLevelClient Test"() {
        setup:
        ElasticSearchConfiguration elasticSearchConfiguration = new ElasticSearchConfiguration()

        elasticSearchConfiguration.elasticSearchProperties = getElasticSearchProperties()
        elasticSearchConfiguration.resourceLoader = getResourceLoader()
        elasticSearchConfiguration.env = getEnv()

        when:
        elasticSearchConfiguration.getRestHighLevelClient()

        then:
        assert true
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

    def getEnv() {
        def env = new MockEnvironment()
        env.setProperty("first-run-time", "2019-07-03T00:00:00")
        env.setProperty("application-id", "ggb-reporting-utility")
        env.setProperty("source.name", "GGB")
        env.setProperty("spring.profiles.active", "abc")
        env.setProperty("AES_KEY", "5b8c6126e439fdf4f0c5c690f836d58e")
        return env
    }


    def resourceLoader = Mock(ResourceLoader) {
        getResource("src/test/resources/unit-test.jks") >> new InputStreamResource(new FileInputStream("src/test/resources/unit-test.jks"))
        getResource("src/test/resources/unit-test-1.jks") >> new InputStreamResource(new FileInputStream("src/test/resources/unit-test-1.jks"))
    }
}
