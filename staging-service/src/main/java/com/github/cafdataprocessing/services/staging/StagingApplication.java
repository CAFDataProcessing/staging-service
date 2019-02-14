/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.github.cafdataprocessing.services.staging;

import java.io.File;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.util.UrlPathHelper;

import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.dao.FileSystemDao;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;

@SpringBootApplication
@ComponentScan(basePackages = {"io.swagger", "com.github.cafdataprocessing.services.staging"})
public class StagingApplication implements WebMvcConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingApplication.class);

    @Value("${https.port}")
    private int httpsPort;

    @Value("${staging.basePath}")
    private String basePath;

    @Value("${staging.subbatchSize}")
    private int subbatchSize;

    private final String keyAlias = System.getenv("SSL_CERT_ALIAS");
    private final String keyStore = System.getenv("SSL_KEYSTORE");
    private final String keyStorePath = System.getenv("SSL_KEYSTORE_PATH");
    private final String keyStorePassword = System.getenv("SSL_KEYSTORE_PASSWORD");

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        LOGGER.info("Starting staging service, service id : {}", ServiceIdentifier.getServiceId());
        SpringApplication.run(StagingApplication.class, args);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }


    @Bean
    public ServletWebServerFactory servletContainer() {
        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (validateConnectorParameters()) {
            tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        }
        return tomcat;
    }

    private Connector createStandardConnector() {
        final Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpsPort);
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setProperty("SSLEnabled", "true");
        connector.setProperty("keystoreFile", keyStorePath + File.separator + keyStore);
        connector.setProperty("keystorePass", keyStorePassword);
        connector.setProperty("keyAlias", keyAlias);
        return connector;
    }

    private boolean validateConnectorParameters() {
        return !StringUtils.isEmpty(keyAlias) && !StringUtils.isEmpty(keyStore) &&
                !StringUtils.isEmpty(keyStorePassword) && !StringUtils.isEmpty(keyStorePath);
    }

    @Bean
    public BatchDao fileSystemDao()
    {
        final BatchDao fileSystemDao = new FileSystemDao(basePath, subbatchSize);
        return fileSystemDao;
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer)
    {
        configurer.favorPathExtension(true)
                .ignoreAcceptHeader(true)
                .useRegisteredExtensionsOnly(false)
                .defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/swagger").setViewName("forward:/swagger/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry
//                .addResourceHandler("/swagger/")
//                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui-dist/3.20.6/index.html");

        registry
                .addResourceHandler("/swagger/**")
                .addResourceLocations(
                        "classpath:/swagger/",
                        "classpath:/com/github/cafdataprocessing/services/staging/contract/",
                        "classpath:/META-INF/resources/webjars/swagger-ui-dist/3.20.6/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }

}
