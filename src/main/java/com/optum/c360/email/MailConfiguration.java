package com.optum.c360.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mail")
public class MailConfiguration {

    private String from;
    private String[] to;
    private String[] cc;
    private String host;
    private int port;

    public MailConfiguration() {
        // public constructor
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String[] getTo() {
        return to;
    }

    public void setTo(String...to) {
        this.to = to;
    }

    public String[] getCc() {
        return cc;
    }

    public void setCc(String...cc) {
        this.cc = cc;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
