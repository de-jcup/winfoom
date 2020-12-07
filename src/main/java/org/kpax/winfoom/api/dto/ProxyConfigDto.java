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

import org.kpax.winfoom.config.*;
import org.kpax.winfoom.exception.*;
import org.kpax.winfoom.util.*;
import org.springframework.util.*;


public class ProxyConfigDto {

    private ProxyConfig.Type proxyType;

    private String proxyUsername;
    private String proxyPassword;
    private Boolean proxyStorePassword;
    private String proxyPacFileLocation;
    private Integer blacklistTimeout;
    private String proxyHost;
    private Integer proxyPort;
    private Integer localPort;
    private String proxyTestUrl;

    private Integer apiPort;
    private Boolean autodetect;
    private Boolean autostart;

    public ProxyConfig.Type getProxyType() {
        return proxyType;
    }

    public void setProxyType(ProxyConfig.Type proxyType) {
        this.proxyType = proxyType;
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

    public Boolean getProxyStorePassword() {
        return proxyStorePassword;
    }

    public void setProxyStorePassword(Boolean proxyStorePassword) {
        this.proxyStorePassword = proxyStorePassword;
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

    public void validate() throws InvalidProxySettingsException {
        if (proxyHost != null || proxyPort != null) {
            Assert.state(proxyType != null, "proxyType must be specified when proxyHost or proxyPort are provided");
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
    }

    @Override
    public String toString() {
        return "ProxyConfigDto{" +
                "proxyType=" + proxyType +
                ", proxyUsername=" + proxyUsername +
                ", proxyPassword=" + proxyPassword +
                ", commandPort=" + apiPort +
                ", proxyStorePassword=" + proxyStorePassword +
                ", proxyTestUrl=" + proxyTestUrl +
                ", proxyPacFileLocation=" + proxyPacFileLocation +
                ", blacklistTimeout=" + blacklistTimeout +
                ", proxyHost=" + proxyHost +
                ", localPort=" + localPort +
                ", proxyPort=" + proxyPort +
                ", autodetect=" + autodetect +
                ", autostart=" + autostart +
                '}';
    }
}
