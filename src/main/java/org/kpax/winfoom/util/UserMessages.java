package org.kpax.winfoom.util;

import org.kpax.winfoom.config.*;

public class UserMessages {
    private UserMessages() {
    }

    public static void error(String message) {
        if (SystemContext.isGuiMode()) {
            SwingUtils.showErrorMessage(message);
        } else {
            System.err.println(message);
        }
    }

    public static void info(String message) {
        if (SystemContext.isGuiMode()) {
            SwingUtils.showInfoMessage(message);
        } else {
            System.out.println(message);
        }
    }
}
