package com.optum.c360;

import com.optum.c360.scheduler.ElasticScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan
@Configuration
@EnableAutoConfiguration

//@EnableScheduling
public class Application {

    @Autowired
    public static  ElasticScheduler elasticScheduler;

    public static void main(String[] args) {
        ConfigurableApplicationContext context =  SpringApplication.run(Application.class, new String[]{""});
        // ElasticScheduler elasticScheduler = new ElasticScheduler();
        // elasticScheduler.runForFixedTime();

        context.getBean(ElasticScheduler.class).runForFixedTime();
    }
}

