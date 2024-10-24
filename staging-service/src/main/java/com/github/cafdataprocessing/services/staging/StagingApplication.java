/*
 * Copyright 2019-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.services.staging;

import com.github.cafapi.CAFSwaggerUI;
import com.github.cafapi.correlation.spring.CorrelationIdInterceptor;
import com.github.cafapi.util.spring.propertysource.CafConfigEnvironmentListener;
import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.dao.filesystem.FileSystemDao;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import com.hpe.caf.secret.SecretUtil;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@SpringBootApplication
@EnableScheduling
@CAFSwaggerUI("com.github.cafdataprocessing.services.staging.contract")
@ComponentScan(basePackages = {"com.github.cafdataprocessing.services.staging"})
@EnableConfigurationProperties(StagingProperties.class)
public class StagingApplication implements WebMvcConfigurer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingApplication.class);

    @Value("${https.port}")
    private int httpsPort;

    @Value("${management.server.port}")
    private int adminPort;

    private final String keyAlias = System.getenv("SSL_CERT_ALIAS");
    private final String keyStore = System.getenv("SSL_KEYSTORE");
    private final String keyStorePath = System.getenv("SSL_KEYSTORE_PATH");
    private final String keyStorePassword;

    public StagingApplication() throws IOException
    {
        this.keyStorePassword = SecretUtil.getSecret("SSL_KEYSTORE_PASSWORD");
    }

    public static void main(String[] args)
    {
        //TODO Verify this is needed for staging service
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        LOGGER.info("Starting staging service, service id : {}", ServiceIdentifier.getServiceId());
        final SpringApplication application = new SpringApplication(StagingApplication.class);
        application.addListeners(new CafConfigEnvironmentListener());
        application.run(args);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer)
    {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    @Bean
    public ServletWebServerFactory servletContainer()
    {
        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (validateConnectorParameters()) {
            tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        }
        return tomcat;
    }

    private Connector createStandardConnector()
    {
        final Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpsPort);
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setProperty("SSLEnabled", "true");
        connector.addSslHostConfig(createSSLHostConfig());
        return connector;
    }

    private SSLHostConfig createSSLHostConfig() {
        final SSLHostConfig sslHostConfig = new SSLHostConfig();
        sslHostConfig.addCertificate(createSSLHostConfigCertificate(sslHostConfig));
        return sslHostConfig;
    }

    private SSLHostConfigCertificate createSSLHostConfigCertificate(final SSLHostConfig sslHostConfig) {
        final SSLHostConfigCertificate sslCertConfig = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
        sslCertConfig.setCertificateKeystoreFile(keyStorePath + File.separator + keyStore);
        sslCertConfig.setCertificateKeystorePassword(keyStorePassword);
        sslCertConfig.setCertificateKeyAlias(keyAlias);
        return sslCertConfig;
    }

    private boolean validateConnectorParameters()
    {
        return !StringUtils.isEmpty(keyAlias) && !StringUtils.isEmpty(keyStore)
            && !StringUtils.isEmpty(keyStorePassword) && !StringUtils.isEmpty(keyStorePath);
    }

    @Bean
    public BatchDao fileSystemDao(final StagingProperties stagingProperties)
    {
        return new FileSystemDao(stagingProperties.getBasePath(),
                                 stagingProperties.getSubbatchSize(),
                                 stagingProperties.getStoragePath(),
                                 stagingProperties.getFieldValueSizeThreshold(),
                                 stagingProperties.getFileAgeThreshold(),
                                 stagingProperties.getSkipFileCleanUp());
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer)
    {
        configurer
            .ignoreAcceptHeader(true)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry)
    {
        registry.addInterceptor(new LoggingMDCInterceptor());
        registry.addInterceptor(new HealthcheckInterceptor(adminPort));
        registry.addInterceptor(new CorrelationIdInterceptor());
    }

}
