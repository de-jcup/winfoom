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
    private String proxyTestUrl;

    private String proxyPacFileLocation;
    private Integer blacklistTimeout;

    private String proxyHost;
    private Integer proxyPort;

    private Integer localPort;

    private Integer commandPort;
    private String commandUserPassword;
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

    public Integer getCommandPort() {
        return commandPort;
    }

    public void setCommandPort(Integer commandPort) {
        this.commandPort = commandPort;
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

    public String getCommandUserPassword() {
        return commandUserPassword;
    }

    public void setCommandUserPassword(String commandUserPassword) {
        this.commandUserPassword = commandUserPassword;
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

        if (commandPort != null) {
            if (!HttpUtils.isValidPort(commandPort)) {
                throw new InvalidProxySettingsException("Invalid commandPort, allowed range: 1 - 65535");
            }
        }
    }

    @Override
    public String toString() {
        return "ProxyConfigDto{" +
                "proxyType=" + proxyType +
                ", proxyUsername=" + proxyUsername +
                ", proxyPassword=" + proxyPassword +
                ", commandPort=" + commandPort +
                ", proxyStorePassword=" + proxyStorePassword +
                ", proxyTestUrl=" + proxyTestUrl +
                ", commandUserPassword=" + commandUserPassword +
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
