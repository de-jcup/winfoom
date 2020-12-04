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

package org.kpax.winfoom;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.*;
import org.apache.commons.configuration2.ex.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.config.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;

import javax.swing.*;
import java.beans.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * The entry point for Winfoom application.
 */
@EnableScheduling
@SpringBootApplication
public class FoomApplication {

    private static final Logger logger = LoggerFactory.getLogger(FoomApplication.class);

    @Bean
    static CustomEditorConfigurer propertyEditorRegistrar() {
        CustomEditorConfigurer customEditorConfigurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>();
        customEditors.put(String.class, Base64DecoderPropertyEditor.class);
        customEditorConfigurer.setCustomEditors(customEditors);
        return customEditorConfigurer;
    }

    public static void main(String[] args) {
        if (SystemContext.isGuiMode() && !SystemContext.IS_OS_WINDOWS) {
            System.err.println("Graphical mode is not supported on " + SystemContext.OS_NAME);
            System.exit(ExitCodes.EC_ERR_GUI_NOT_SUPPORTED);
        }

        logger.info("Application started at: {}", new Date());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutdown at: {}", new Date());
        }));

        // Disable Java code execution
        // within the PAC script for safety reasons
        System.setProperty("nashorn.args", "--no-java");

        if (SystemContext.isGuiMode()) {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } catch (Exception e) {
                logger.warn("Failed to set Windows L&F, use the default look and feel", e);
            }
        }

        // Check version
        try {
            checkAppVersion();
        } catch (Exception e) {
            logger.error("Failed to verify app version", e);
            UserMessages.error(String.format("Failed to verify application version.<br>" +
                            "Remove the %s directory then try again.",
                    Paths.get(System.getProperty("user.home"), SystemConfig.APP_HOME_DIR_NAME)));
            System.exit(ExitCodes.EC_ERR_CHECK_SETTINGS);
        }

        logger.info("Bootstrap Spring's application context");
        try {
            SpringApplication.run(FoomApplication.class, args);
        } catch (Exception e) {
            logger.error("Error on bootstrapping Spring's application context", e);
            UserMessages.error("Failed to launch the application." +
                    "<br>Please check the application's log file.");
            System.exit(ExitCodes.EC_ERR_SPRING_CONTEXT_FAILED);
        }
    }

    /**
     * Verify whether the existent system.properties file's releaseVersion property and
     * the application version (extracted from the MANIFEST file) are the same or backward compatible.
     * If not, the existent {@code *.properties} file are moved into a backup location.
     *
     * @throws IOException
     * @throws ConfigurationException
     */
    private static void checkAppVersion() throws IOException, ConfigurationException {
        logger.info("Check the application's version");
        Path appHomePath = Paths.get(System.getProperty("user.home"), SystemConfig.APP_HOME_DIR_NAME);
        if (Files.exists(appHomePath)) {
            Path proxyConfigPath = appHomePath.resolve(ProxyConfig.FILENAME);
            if (Files.exists(proxyConfigPath)) {
                Configuration configuration = new Configurations()
                        .propertiesBuilder(proxyConfigPath.toFile()).getConfiguration();
                String existingVersion = configuration.getString("app.version");
                logger.info("existingVersion [{}]", existingVersion);
                if (existingVersion != null) {
                    String actualVersion = FoomApplication.class.getPackage().getImplementationVersion();
                    logger.info("actualVersion [{}]", actualVersion);
                    if (actualVersion != null && !actualVersion.equals(existingVersion)) {
                        boolean isCompatibleProxyConfig = !Files.exists(proxyConfigPath)
                                || ProxyConfig.isCompatible(configuration);
                        logger.info("The existent proxy config is compatible with the new one: {}",
                                isCompatibleProxyConfig);

                        if (!isCompatibleProxyConfig) {
                            logger.info("Backup the existent proxy.properties file since is invalid" +
                                    " (from a previous incompatible version)");
                            InputOutputs.backupFile(proxyConfigPath,
                                    SystemContext.isGuiMode(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    logger.info("Version not found within proxy.properties, " +
                            "backup both config files since they are invalid (from a previous incompatible version)");
                    InputOutputs.backupFile(proxyConfigPath,
                            SystemContext.isGuiMode(),
                            StandardCopyOption.REPLACE_EXISTING);
                    InputOutputs.backupFile(appHomePath.resolve(SystemConfig.FILENAME),
                            SystemContext.isGuiMode(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                logger.info("No proxy.properties found, backup the system.properties file " +
                        "since is invalid (from a previous incompatible version)");
                InputOutputs.backupFile(appHomePath.resolve(SystemConfig.FILENAME),
                        SystemContext.isGuiMode(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

}
