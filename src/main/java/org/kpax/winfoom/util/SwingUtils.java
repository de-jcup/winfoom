/*******************************************************************************
 * Copyright (c) 2018 Eugen Covaci.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * Contributors:
 *     Eugen Covaci - initial design and implementation
 *******************************************************************************/

package org.kpax.winfoom.util;

import org.kpax.winfoom.annotation.*;
import org.slf4j.*;
import org.springframework.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Various Swing related methods.
 *
 * @author Eugen Covaci
 */
public class SwingUtils {

    private static final Logger logger = LoggerFactory.getLogger(SwingUtils.class);

    private static final String DLG_ERR_TITLE = "Winfoom: Error";

    private static final String DLG_INFO_TITLE = "Winfoom: Info";

    private static final String DLG_WARN_TITLE = "Winfoom: Warning";

    /**
     * Enable/disable a component and all it's sub-components.
     *
     * @param component the {@link Component} to be enabled/disabled.
     * @param enabled   {@code true} or {@code false}.
     */
    public static void setEnabled(@NotNull final Component component, final boolean enabled, Class... excluded) {
        java.util.List<Class> excludedClasses;
        if (excluded != null) {
            excludedClasses = Arrays.asList(excluded);
        } else {
            excludedClasses = Collections.emptyList();
        }
        if (!excludedClasses.contains(component.getClass())) {
            component.setEnabled(enabled);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabled(child, enabled, excluded);
            }
        }
    }

    /**
     * Accept an input value only if it is valid.
     *
     * @param spinner the {@link JSpinner} instance.
     */
    public static void commitsOnValidEdit(@NotNull final JSpinner spinner) {
        JComponent comp = spinner.getEditor();
        JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
        ((DefaultFormatter) field.getFormatter()).setCommitsOnValidEdit(true);
    }

    public static void showMessage(final Component parentComponent,
                                   final String title,
                                   final String message,
                                   final int type) {
        JOptionPane.showMessageDialog(parentComponent,
                String.format("<html><body><p style='word-wrap: break-word;width: 300px;'>%s</p></body></html>",
                        message),
                title,
                type);
    }


    public static void showErrorMessage(final String message) {
        showErrorMessage(null, DLG_ERR_TITLE, message);
    }

    public static void showErrorMessage(final Component parentComponent,
                                        final String message) {
        showErrorMessage(parentComponent, DLG_ERR_TITLE, message);
    }

    public static void showErrorMessage(final Component parentComponent,
                                        final String title,
                                        final String message) {
        showMessage(parentComponent, title, message, JOptionPane.ERROR_MESSAGE);
    }


    public static void showInfoMessage(final String message) {
        showInfoMessage(null, DLG_INFO_TITLE, message);
    }

    public static void showInfoMessage(final Component parentComponent,
                                       final String message) {
        showInfoMessage(parentComponent, DLG_INFO_TITLE, message);
    }

    public static void showInfoMessage(final Component parentComponent,
                                       final String title,
                                       final String message) {
        showMessage(parentComponent, title, message, JOptionPane.INFORMATION_MESSAGE);
    }


    public static void showWarningMessage(final String message) {
        showWarningMessage(null, DLG_WARN_TITLE, message);
    }

    public static void showWarningMessage(final Component parentComponent,
                                          final String message) {
        showWarningMessage(parentComponent, DLG_WARN_TITLE, message);
    }

    public static void showWarningMessage(final Component parentComponent,
                                          final String title,
                                          final String message) {
        showMessage(parentComponent, title, message, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Execute a {@link Callable}, showing a waiting cursor until the execution ends.
     *
     * @param callable the {@link Callable} instance (not null)
     * @param frame    the current {@link JFrame}
     */
    public static <T> T executeCallable(@NotNull final Callable<T> callable, @NotNull final JFrame frame) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executorService.submit(callable);
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                return future.get();
            } catch (InterruptedException e) {
                logger.error("Execution interrupted", e);
                return null;
            } catch (ExecutionException e) {
                logger.warn("Error on computing the Callable instance, this isn't supposed to happen", e);
                return null;
            } finally {
                EventQueue.invokeLater(() ->
                        frame.setCursor(Cursor.getDefaultCursor()));
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * Load an image from the {@code img} folder.
     *
     * @param cls      used to localize the image URL.
     * @param filename the image's filename.
     * @return the loaded {@link Image}.
     */
    public static Image loadImage(@NotNull final Class<?> cls,
                                  @NotNull final String filename) {
        try {
            URL resource = cls.getResource("/img/" + filename);
            if (resource != null) {
                return ImageIO.read(resource);
            } else {
                throw new FileNotFoundException("Resource not found");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load the image named: " + filename, e);
        }
    }

}
