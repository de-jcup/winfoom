/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.api.dto;

import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemContext;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.util.HttpUtils;
import org.springframework.util.Assert;

/**
 * The DTO for API server.
 */
public class ProxyConfigDto {

    private ProxyConfig.Type proxyType;
    private Boolean useCurrentCredentials;
    private String proxyUsername;
    private String proxyPassword;
    private String proxyPacFileLocation;
    private Integer blacklistTimeout;
    private String proxyHost;
    private Integer proxyPort;
    private Integer localPort;
    private String proxyTestUrl;

    private ProxyConfig.HttpAuthProtocol httpAuthProtocol;

    private Integer apiPort;
    private Boolean autodetect;
    private Boolean autostart;

    public ProxyConfig.Type getProxyType() {
        return proxyType;
    }

    public void setProxyType(ProxyConfig.Type proxyType) {
        this.proxyType = proxyType;
    }

    public Boolean getUseCurrentCredentials() {
        return useCurrentCredentials;
    }

    public void setUseCurrentCredentials(Boolean useCurrentCredentials) {
        this.useCurrentCredentials = useCurrentCredentials;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public Integer getApiPort() {
        return apiPort;
    }

    public void setApiPort(Integer apiPort) {
        this.apiPort = apiPort;
    }

    public String getProxyTestUrl() {
        return proxyTestUrl;
    }

    public void setProxyTestUrl(String proxyTestUrl) {
        this.proxyTestUrl = proxyTestUrl;
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

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Boolean getAutodetect() {
        return autodetect;
    }

    public void setAutodetect(Boolean autodetect) {
        this.autodetect = autodetect;
    }

    public Boolean getAutostart() {
        return autostart;
    }

    public void setAutostart(Boolean autostart) {
        this.autostart = autostart;
    }

    public ProxyConfig.HttpAuthProtocol getHttpAuthProtocol() {
        return httpAuthProtocol;
    }

    public void setHttpAuthProtocol(ProxyConfig.HttpAuthProtocol httpAuthProtocol) {
        this.httpAuthProtocol = httpAuthProtocol;
    }

    public void validate() throws InvalidProxySettingsException {
        if (proxyHost != null || proxyPort != null || useCurrentCredentials != null) {
            Assert.state(proxyType != null, "proxyType must be specified when proxyHost or proxyPort or useCurrentCredentials are provided");
            Assert.state(proxyType != ProxyConfig.Type.DIRECT, "When proxyType is DIRECT, neither proxyHost nor proxyPort can be provided");
        }

        if (proxyPort != null) {
            if (!HttpUtils.isValidPort(proxyPort)) {
                throw new InvalidProxySettingsException("Invalid proxyPort, allowed range: 1 - 65535");
            }
        }

        if (localPort != null) {
            if (!HttpUtils.isValidPort(localPort)) {
                throw new InvalidProxySettingsException("Invalid localPort, allowed range: 1 - 65535");
            }
        }

        if (apiPort != null) {
            if (!HttpUtils.isValidPort(apiPort)) {
                throw new InvalidProxySettingsException("Invalid apiPort, allowed range: 1 - 65535");
            }
        }

        if (useCurrentCredentials != null) {
            Assert.state(proxyType == ProxyConfig.Type.HTTP, "proxyType must be HTTP when useCurrentCredentials is provided");
            if (useCurrentCredentials && !SystemContext.IS_OS_WINDOWS) {
                throw new InvalidProxySettingsException("The field useCurrentCredentials is only allowed on Windows OS");
            }
        }
    }

    @Override
    public String toString() {
        return "ProxyConfigDto{" +
                "proxyType=" + proxyType +
                ", useCurrentCredentials=" + useCurrentCredentials +
                ", proxyUsername='" + proxyUsername + '\'' +
                ", proxyPassword='" + proxyPassword + '\'' +
                ", proxyPacFileLocation='" + proxyPacFileLocation + '\'' +
                ", blacklistTimeout=" + blacklistTimeout +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", localPort=" + localPort +
                ", proxyTestUrl='" + proxyTestUrl + '\'' +
                ", httpAuthProtocol=" + httpAuthProtocol +
                ", apiPort=" + apiPort +
                ", autodetect=" + autodetect +
                ", autostart=" + autostart +
                '}';
    }
}
