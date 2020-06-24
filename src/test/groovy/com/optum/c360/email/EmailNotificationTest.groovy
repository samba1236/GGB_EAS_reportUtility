package com.optum.c360.email

import com.optum.c360.exception.GenericException
import org.apache.commons.io.FileUtils
import org.apache.commons.mail.Email
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.MultiPartEmail
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

import java.time.ZoneId
import java.time.ZonedDateTime

class EmailNotificationTest extends Specification {

    def "sendEmailNotificationWithAttachment Test"() {
        setup:
        ZonedDateTime startTime = ZonedDateTime.now(ZoneId.of("GMT"))
        File file = FileUtils.getFile("resources/attachment.xlsx")

        EmailNotification emailNotification = new EmailNotification() {
            @Override
            protected MultiPartEmail getEmailObject() {
                return multiPartEmail
            }
        }
        emailNotification.environment = getEnv()

        when:
        emailNotification.sendEmailNotificationWithAttachment(startTime, file)

        then:
        assert true
    }

    def "sendEmailNotificationWithAttachment with Blank attachment Test"() {
        setup:
        ZonedDateTime startTime = ZonedDateTime.now(ZoneId.of("GMT"))
        File file = null

        EmailNotification emailNotification = new EmailNotification() {
            @Override
            protected MultiPartEmail getEmailObject() {
                return multiPartEmail
            }
        }
        emailNotification.environment = getEnv()

        when:
        emailNotification.sendEmailNotificationWithAttachment(startTime, file)

        then:
        assert true
    }

    def "sendEmailNotificationWithAttachment with EmailException Test"() {
        setup:
        ZonedDateTime startTime = ZonedDateTime.now(ZoneId.of("GMT"))
        File file = FileUtils.getFile("resources/attachment.xlsx")

        EmailNotification emailNotification = new EmailNotification() {
            @Override
            protected MultiPartEmail getEmailObject() {
                return getMultiPartEmailForException()
            }
        }
        emailNotification.environment = getEnv()

        when:
        emailNotification.sendEmailNotificationWithAttachment(startTime, file)

        then:
        thrown(GenericException)
    }

    def "getEmailObject Test"() {
        setup:
        EmailNotification emailNotification = new EmailNotification()
        emailNotification.mailConfiguration = getMailConfiguration()

        when:
        emailNotification.getEmailObject()

        then:
        assert true
    }

    def "getEmailObject with blank To AND CC Test"() {
        setup:
        EmailNotification emailNotification = new EmailNotification()
        emailNotification.mailConfiguration = getMailConfigurationWithBlankToAndCC()

        when:
        emailNotification.getEmailObject()

        then:
        assert true
    }

    def "getEmailObject with Exception Test"() {
        setup:
        EmailNotification emailNotification = new EmailNotification()
        emailNotification.mailConfiguration = getMailConfiguration()
        emailNotification.mailConfiguration.to = "abc"

        when:
        emailNotification.getEmailObject()

        then:
        thrown(GenericException)
    }

    def getMultiPartEmail() {
        def multiPartEmail = Mock(MultiPartEmail) {
            send() >> "SUCCESS"
        }
        return multiPartEmail
    }

    def getMultiPartEmailForException() {
        def multiPartEmail = Mock(MultiPartEmail) {
            send() >> { throw new EmailException("test exception") }
        }
        return multiPartEmail
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

    def getMailConfiguration() {
        MailConfiguration mailConfiguration = new MailConfiguration()
        mailConfiguration.setPort(25)
        mailConfiguration.setHost("mail.uhc.com")
        mailConfiguration.setFrom("test@test.com")
        String[] emails = new String[2]
        emails[0] = "test@test.com"
        emails[1] = "test@test.com"
        mailConfiguration.setTo(emails)
        mailConfiguration.setCc(emails)
        return mailConfiguration
    }

    def getMailConfigurationWithBlankToAndCC() {
        MailConfiguration mailConfiguration = new MailConfiguration()
        mailConfiguration.setPort(25)
        mailConfiguration.setHost("mail.uhc.com")
        mailConfiguration.setFrom("test@test.com")
        String[] emails = new String[0]
        mailConfiguration.setTo(emails)
        mailConfiguration.setCc(emails)
        return mailConfiguration
    }
}
