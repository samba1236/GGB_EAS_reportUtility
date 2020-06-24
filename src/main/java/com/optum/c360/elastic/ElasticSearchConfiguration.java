/*
 * ***************************************************************
 *  Copyright (c) Optum, Inc 2019.
 *  This software and documentation contain confidential and
 *  proprietary information owned by Optum, Inc.
 *  Unauthorized use and distribution are prohibited.
 * **************************************************************
 */
package com.optum.c360.elastic;

import com.optum.c360.exception.GenericException;
import com.optum.c360.security.AESCryptoException;
import com.optum.c360.security.AESUtilities;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;

/**
 * Provide common configuration for initializing a RestHighLevelClient based on properties configured in the ElasticSearchProperties Configuration class.
 */
@Configuration
public class ElasticSearchConfiguration {

    @Autowired
    Environment env;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    ElasticSearchProperties elasticSearchProperties;

    public ElasticSearchProperties getElasticSearchProperties() {
        return elasticSearchProperties;
    }

    public void setElasticSearchProperties(ElasticSearchProperties elasticSearchProperties) {
        this.elasticSearchProperties = elasticSearchProperties;
    }

    public RestHighLevelClient getRestHighLevelClient() throws MalformedURLException {
        ArrayList<HttpHost> httpHosts = new ArrayList<>();
        for (String host : elasticSearchProperties.getHosts()) {
            URL hostUrl = new URL(host);
            httpHosts.add(new HttpHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol()));
        }
        return new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[0]))
                .setHttpClientConfigCallback(this::setSSLContextAndCredentialsProvider));
    }

    /**
     * @param httpClientBuilder the HttpAsyncClientBuilder to customize
     * @return customized httpClientBuilder with SSL context set and Basic auth credentials provider if enabled.
     */
    protected HttpAsyncClientBuilder setSSLContextAndCredentialsProvider(HttpAsyncClientBuilder httpClientBuilder) {
        httpClientBuilder.setSSLContext(buildSSLContext());
        httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());
        return httpClientBuilder;
    }

    /**
     * @return a BasicCredentialsProvider with the configured username and password.
     */
    protected CredentialsProvider getCredentialsProvider() {
        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticSearchProperties.getUsername(), getDecryptedPassword(elasticSearchProperties.getPassword())));

        return credentialsProvider;
    }

    /**
     * @return SSLContext based on configured truststore and keystore properties
     */
    protected SSLContext buildSSLContext() {
        SSLContext sslContext = null;
        String trustCertPath = elasticSearchProperties.getTruststoreFile();
        String trustStorePassword = elasticSearchProperties.getTruststorePassword();
        Resource trustResource = resourceLoader.getResource(trustCertPath);

        try (InputStream trustStoreInputStream = trustResource.getInputStream()) {
            KeyStore tks = KeyStore.getInstance("JKS");
            tks.load(trustStoreInputStream, getDecryptedPassword(trustStorePassword).toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(tks);

            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), null);
        } catch (Exception ex) {
            throw new SecurityException("error while using truststore", ex);
        }

        return sslContext;
    }

    public String getDecryptedPassword(String encryptedPassword) {
        char[] passwordArray;
        try {
            passwordArray = AESUtilities.decrypt(env.getProperty("AES_KEY").toCharArray(), encryptedPassword);
        } catch (AESCryptoException e) {
            throw new GenericException("Error occurred while getting decrypted elastic password", e);
        }
        return new String(passwordArray);
    }
}

