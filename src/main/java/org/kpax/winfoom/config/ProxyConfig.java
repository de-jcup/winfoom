/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.kpax.winfoom.proxy.ProxyType;
import org.kpax.winfoom.util.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * The proxy facade configuration.
 *
 * @author Eugen Covaci
 */
@Component
@PropertySource(value = "file:${user.home}/" + SystemConfig.APP_HOME_DIR_NAME + "/" + ProxyConfig.FILENAME,
        ignoreResourceNotFound = true)
public class ProxyConfig {

    public static final String FILENAME = "proxy.properties";

    private final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

    @Value("${app.version}")
    private String appVersion;

    @Value("${local.port:3129}")
    private Integer localPort;

    @Value("${proxy.http.host:}")
    private String proxyHttpHost;

    @Value("${proxy.socks5.host:}")
    private String proxySocks5Host;

    @Value("${proxy.socks4.host:}")
    private String proxySocks4Host;

    @Value("${proxy.http.port:0}")
    private Integer proxyHttpPort;

    @Value("${proxy.socks5.port:0}")
    private Integer proxySocks5Port;

    @Value("${proxy.socks4.port:0}")
    private Integer proxySocks4Port;

    @Value("${proxy.test.url:http://example.com}")
    private String proxyTestUrl;

    @Value("${proxy.type:HTTP}")
    private Type proxyType;

    @Value("${proxy.username:#{null}}")
    private String proxyUsername;

    @Value("${proxy.storePassword:false}")
    private boolean proxyStorePassword;

    @Value("${proxy.password:#{null}}")
    private String proxyPassword;

    @Value("${proxy.pac.fileLocation:#{null}}")
    private String proxyPacFileLocation;

    @Value("${blacklist.timeout:30}")// minutes
    private Integer blacklistTimeout;

    @Value("${autostart:false}")
    private boolean autostart;

    private Path tempDirectory;

    @PostConstruct
    public void init() throws IOException, ConfigurationException {
        File userProperties = Paths.get(System.getProperty("user.home"), SystemConfig.APP_HOME_DIR_NAME,
                ProxyConfig.FILENAME).toFile();

        // Make sure the file exists.
        // If not, create a new one and write the app.version
        if (!userProperties.exists()) {
            userProperties.createNewFile();
            FileBasedConfigurationBuilder<PropertiesConfiguration> propertiesBuilder = new Configurations()
                    .propertiesBuilder(userProperties);
            Configuration config = propertiesBuilder.getConfiguration();
            config.setProperty("app.version", appVersion);
            propertiesBuilder.save();
        }

        if (proxyType != Type.DIRECT && StringUtils.isEmpty(getProxyHost())) {
            try {
                CommandExecutor.getSystemProxy().ifPresent((s) -> {
                    logger.info("proxyLine: {}", s);
                    String[] proxies = s.split(";");
                    if (proxies.length > 0) {
                        String firstProxy = proxies[0];
                        logger.info("firstProxy: {}", firstProxy);
                        String[] elements = firstProxy.split(":");
                        if (elements.length > 1) {
                            if (elements[0].startsWith("http=")) {
                                setProxyHost(elements[0].substring("http=".length()));
                                proxyType = Type.HTTP;
                            } else if (elements[0].startsWith("socks=")) {
                                setProxyHost(elements[0].substring("socks=".length()));
                                proxyType = Type.SOCKS5;
                            } else {
                                setProxyHost(elements[0]);
                            }
                            setProxyPort(Integer.parseInt(elements[1]));
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Error on getting system proxy", e);
            }
        }
        logger.info("Check temp directory");
        if (!Files.exists(tempDirectory)) {
            logger.info("Create temp directory {}", tempDirectory);
            Files.createDirectories(tempDirectory);
        } else if (!Files.isDirectory(tempDirectory)) {
            throw new IllegalStateException(
                    String.format("The file [%s] should be a directory, not a regular file", tempDirectory));
        } else {
            logger.info("Using temp directory {}", tempDirectory);
        }
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getProxyHost() {
        switch (proxyType) {
            case HTTP:
                return proxyHttpHost;
            case SOCKS4:
                return proxySocks4Host;
            case SOCKS5:
                return proxySocks5Host;
        }
        return null;
    }

    public void setProxyHost(String proxyHost) {
        switch (proxyType) {
            case HTTP:
                this.proxyHttpHost = proxyHost;
            case SOCKS4:
                this.proxySocks4Host = proxyHost;
            case SOCKS5:
                this.proxySocks5Host = proxyHost;
        }
    }

    public Integer getProxyPort() {
        switch (proxyType) {
            case HTTP:
                return proxyHttpPort;
            case SOCKS4:
                return proxySocks4Port;
            case SOCKS5:
                return proxySocks5Port;
        }
        return 0;
    }

    public void setProxyPort(Integer proxyPort) {
        switch (proxyType) {
            case HTTP:
                this.proxyHttpPort = proxyPort;
            case SOCKS4:
                this.proxySocks4Port = proxyPort;
            case SOCKS5:
                this.proxySocks5Port = proxyPort;
        }
    }

    public String getProxyTestUrl() {
        return proxyTestUrl;
    }

    public void setProxyTestUrl(String proxyTestUrl) {
        this.proxyTestUrl = proxyTestUrl;
    }

    public Path getTempDirectory() {
        return tempDirectory;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public void setProxyType(Type proxyType) {
        this.proxyType = proxyType;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        if (StringUtils.isNotEmpty(proxyPassword)) {
            return new String(Base64.getDecoder().decode(proxyPassword));
        } else {
            return null;
        }
    }

    public void setProxyPassword(String proxyPassword) {
        if (StringUtils.isNotEmpty(proxyPassword)) {
            this.proxyPassword = Base64.getEncoder().encodeToString(proxyPassword.getBytes());
        } else {
            this.proxyPassword = null;
        }
    }

    public boolean isProxyStorePassword() {
        return proxyStorePassword;
    }

    public void setProxyStorePassword(boolean proxyStorePassword) {
        this.proxyStorePassword = proxyStorePassword;
    }

    public String getProxyPacFileLocation() {
        return proxyPacFileLocation;
    }

    public void setProxyPacFileLocation(String proxyPacFileLocation) {
        this.proxyPacFileLocation = proxyPacFileLocation;
    }

    public Integer getBlacklistTimeout() {
        return blacklistTimeout;
    }

    public void setBlacklistTimeout(Integer blacklistTimeout) {
        this.blacklistTimeout = blacklistTimeout;
    }

    public URL getProxyPacFileLocationAsURL() throws MalformedURLException {
        if (StringUtils.isNotEmpty(proxyPacFileLocation)) {
            if (proxyPacFileLocation.startsWith("http")) {
                return new URL(proxyPacFileLocation);
            } else {
                return new URL("file:///" + proxyPacFileLocation);
            }
        }
        return null;
    }

    public boolean isAutoConfig() {
        return this.proxyType.isPac();
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    public boolean isAutostart() {
        return autostart;
    }

    @Autowired
    private void setTempDirectory(@Value("${user.home}") String userHome) {
        tempDirectory = Paths.get(userHome, SystemConfig.APP_HOME_DIR_NAME, "temp");
    }

    /**
     * Save the current settings to the home application directory, overwriting the existing values.
     *
     * @throws ConfigurationException
     */
    public void save() throws ConfigurationException {
        File userProperties = Paths.get(System.getProperty("user.home"), SystemConfig.APP_HOME_DIR_NAME,
                ProxyConfig.FILENAME).toFile();
        FileBasedConfigurationBuilder<PropertiesConfiguration> propertiesBuilder = new Configurations()
                .propertiesBuilder(userProperties);
        Configuration config = propertiesBuilder.getConfiguration();
        config.setProperty("proxy.type", proxyType);

        if (proxyType.isHttp()) {
            config.setProperty("proxy.http.host", proxyHttpHost);
            config.setProperty("proxy.http.port", proxyHttpPort);
        } else if (proxyType.isSocks4()) {
            config.setProperty("proxy.socks4.host", proxySocks4Host);
            config.setProperty("proxy.socks4.port", proxySocks4Port);
        } else if (proxyType.isSocks5()) {
            config.setProperty("proxy.socks5.host", proxySocks5Host);
            config.setProperty("proxy.socks5.port", proxySocks5Port);
        }

        config.setProperty("local.port", localPort);
        config.setProperty("proxy.test.url", proxyTestUrl);

        if (proxyType.isSocks5()) {
            config.setProperty("proxy.username", proxyUsername);
            config.setProperty("proxy.storePassword", proxyStorePassword);
            if (proxyStorePassword) {
                config.setProperty("proxy.password", proxyPassword);
            } else {
                // Clear the stored password
                config.clearProperty("proxy.password");
            }
        } else {
            // Make sure we nullify the proxy password
            // if it is not stored
            if (!proxyStorePassword) {
                proxyPassword = null;
            }
        }

        if (proxyType.isPac()) {
            config.setProperty("proxy.pac.fileLocation", proxyPacFileLocation);
            config.setProperty("blacklist.timeout", blacklistTimeout);
        }

        config.setProperty("autostart", autostart);
        propertiesBuilder.save();
    }

    public void saveAutostart() throws ConfigurationException {
        File userProperties = Paths.get(System.getProperty("user.home"), SystemConfig.APP_HOME_DIR_NAME,
                ProxyConfig.FILENAME).toFile();
        FileBasedConfigurationBuilder<PropertiesConfiguration> propertiesBuilder = new Configurations()
                .propertiesBuilder(userProperties);
        Configuration config = propertiesBuilder.getConfiguration();
        config.setProperty("autostart", autostart);
        propertiesBuilder.save();
    }

    /**
     * Check whether a {@link Configuration} instance is compatible with the current {@link ProxyConfig} structure.
     *
     * @param proxyConfig the {@link Configuration} instance
     * @return {@code true} if each {@link Configuration} key is among
     * the {@link ProxyConfig}'s {@link Value} annotated fields.
     */
    public static boolean isCompatible(Configuration proxyConfig) {
        List<String> keys = new ArrayList<>();
        for (Field field : ProxyConfig.class.getDeclaredFields()) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                keys.add(valueAnnotation.value().replaceAll("[${}]", "").split(":")[0]);
            }
        }

        for (Iterator<String> itr = proxyConfig.getKeys(); itr.hasNext(); ) {
            String key = itr.next();
            if (!keys.contains(key)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "ProxyConfig{" +
                "appVersion='" + appVersion + '\'' +
                ", localPort=" + localPort +
                ", proxyHost='" + getProxyHost() + '\'' +
                ", proxyTestUrl='" + proxyTestUrl + '\'' +
                ", proxyPort=" + getProxyPort() +
                ", proxyType=" + proxyType +
                ", proxyUsername='" + proxyUsername + '\'' +
                ", proxyStorePassword=" + proxyStorePassword +
                ", proxyPacFileLocation='" + proxyPacFileLocation + '\'' +
                ", blacklistTimeout=" + blacklistTimeout +
                ", autostart=" + autostart +
                ", tempDirectory=" + tempDirectory +
                '}';
    }

    public enum Type implements ProxyType {
        HTTP, SOCKS4, SOCKS5, PAC, DIRECT;

        public boolean isPac() {
            return this == PAC;
        }

        @Override
        public boolean isSocks4() {
            return this == SOCKS4;
        }

        @Override
        public boolean isSocks5() {
            return this == SOCKS5;
        }

        @Override
        public boolean isHttp() {
            return this == HTTP;
        }

        @Override
        public boolean isDirect() {
            return this == DIRECT;
        }

    }
}
