/*
 * ***************************************************************
 *  Copyright (c) Optum, Inc 2019.
 *  This software and documentation contain confidential and
 *  proprietary information owned by Optum, Inc.
 *  Unauthorized use and distribution are prohibited.
 * **************************************************************
 */
package com.optum.c360.elastic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Configuration class encapsulating all properties that can be configured for elasticsearch using yaml/properties
 */
@Configuration
@ConfigurationProperties(prefix = "elastic")
public class ElasticSearchProperties {

    @NotEmpty
    private List<String> hosts;
    private String username;
    private String password;
    private String truststoreFile;
    private String truststorePassword;
    private String keystoreFile;
    private String keystorePassword;
    private String successIndex;
    private String errorIndex;
    private List<String> subjectAreas;

    public ElasticSearchProperties() {
        //public constructor
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getSuccessIndex() {
        return successIndex;
    }

    public void setSuccessIndex(String successIndex) {
        this.successIndex = successIndex;
    }

    public String getErrorIndex() {
        return errorIndex;
    }

    public void setErrorIndex(String errorIndex) {
        this.errorIndex = errorIndex;
    }

    public List<String> getSubjectAreas() {
        return subjectAreas;
    }

    public void setSubjectAreas(List<String> subjectAreas) {
        this.subjectAreas = subjectAreas;
    }
}
