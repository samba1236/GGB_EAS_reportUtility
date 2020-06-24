package com.optum.c360.email;

import com.optum.c360.constants.ApplicationConstants;
import com.optum.c360.exception.GenericException;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.ZonedDateTime;


@Component
public class EmailNotification {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotification.class);

    @Autowired
    Environment environment;

    @Autowired
    MailConfiguration mailConfiguration;


    protected MultiPartEmail getEmailObject() {
        MultiPartEmail email;
        try {
            email = new HtmlEmail();
            email.setHostName(mailConfiguration.getHost());
            email.setSmtpPort(mailConfiguration.getPort());
            email.setFrom(mailConfiguration.getFrom());
            if (validateEmail(mailConfiguration.getTo())) {
                email.addTo(mailConfiguration.getTo());
            }
            if (validateEmail(mailConfiguration.getCc())) {
                email.addCc(mailConfiguration.getCc());
            }
        } catch (EmailException e) {
            throw new GenericException("Exception in creating email object", e);
        }
        return email;
    }

    public void sendEmailNotificationWithAttachment(ZonedDateTime startTime, File attachmentFile) {
        try {
            if (null != attachmentFile) {
                MultiPartEmail email = getEmailObject();

                String[] profile = environment.getActiveProfiles();
                email.setSubject("GGB Kafka Topic Statistics: [" + profile[0].toUpperCase() + " env]  from " + startTime);
                StringBuilder buffer = new StringBuilder(300);
                buffer.append("Hi Everyone,")
                        .append(ApplicationConstants.HTML_BREAK)
                        .append(ApplicationConstants.HTML_BREAK)
                        .append("Please find attached document with details of kafka topic statistics with this email.")
                        .append(ApplicationConstants.HTML_BREAK)
                        .append(ApplicationConstants.HTML_BREAK)
                        .append("This is an autogenerated email. Replies on this email may not be monitored")
                        .append(ApplicationConstants.HTML_BREAK)
                        .append(ApplicationConstants.HTML_BREAK)
                        .append("Thanks,")
                        .append(ApplicationConstants.HTML_BREAK)
                        .append("C360 ShockWave Team");
                email.setMsg(buffer.toString());

                EmailAttachment attachment = new EmailAttachment();
                attachment.setPath(attachmentFile.getAbsolutePath());
                attachment.setDisposition(EmailAttachment.ATTACHMENT);
                attachment.setDescription(attachmentFile.getName());
                attachment.setName(attachmentFile.getName());
                email.attach(attachment);

                email.send();
                LOGGER.info("notification email sent");
            }
        } catch (EmailException e) {
            throw new GenericException("exception while sending email", e);
        }
    }

    private boolean validateEmail(String... emails) {
        boolean returnVal = false;
        if (null != emails && emails.length > 0) {
            returnVal = true;
        }
        return returnVal;
    }
}