package com.optum.c360.scheduler

import com.optum.c360.exception.GenericException
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

class ElasticSchedulerTest extends Specification {

    def "runForFixedTime Test"() {
        setup:
        ElasticScheduler elasticScheduler = new ElasticScheduler() {
            @Override
            protected void process() {}
        }
        elasticScheduler.environment = getEnvironment()
        when:
        elasticScheduler.runForFixedTime()
        then:
        assert true
    }

    def getEnvironment() {
        def environment = new MockEnvironment()
        environment.setProperty("scheduler.timeInterval", "180000")
        return environment
    }
}
