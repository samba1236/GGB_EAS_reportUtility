package com.optum.c360.scheduler;

import com.optum.c360.constants.ApplicationConstants;
import com.optum.c360.elastic.ElasticSearchConfiguration;
import com.optum.c360.elastic.ElasticSearchProperties;
import com.optum.c360.elastic.ErrorDetailProcessor;
import com.optum.c360.email.EmailNotification;
import com.optum.c360.exception.GenericException;
import com.optum.c360.fileoperation.ExcelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ElasticScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticScheduler.class);


    @Autowired
    ElasticSearchProperties elasticSearchProperties;

    @Autowired
    Environment environment;

    @Autowired
    ExcelWriter excelWriter;

    @Autowired
    EmailNotification emailNotification;

    @Autowired
    ElasticSearchConfiguration elasticSearchConfiguration;


    public void runForFixedTime() {
        LOGGER.info("******************************************************** Application Started ******************************************************** ");

        LOGGER.info(ApplicationConstants.STRING_LINE);
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("GMT"));
        LOGGER.info("Execution started at {}", time);
        LOGGER.info(ApplicationConstants.STRING_LINE);

        process();

        LOGGER.info("******************************************************** Process Complete ******************************************************** ");
    }

    protected void process() {
        try {
            String lastRunDate= environment.getProperty("LAST_RUN_DATE");
            String AES = environment.getProperty("AES_KEY");

            ErrorDetailProcessor errorDetailProcessor = new ErrorDetailProcessor( elasticSearchProperties, environment, excelWriter, emailNotification, elasticSearchConfiguration, lastRunDate);
            errorDetailProcessor.process();
            System.exit(0);
        } catch (GenericException e) {
            throw new GenericException("Exception occurred while processing error details", e);
        }
    }

}