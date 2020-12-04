package org.kpax.winfoom.api.json;

import org.kpax.winfoom.config.*;

public class Views {

    public interface Windows {
    }

    public interface NonWindows {
    }

    public interface Common {
    }

    public interface Direct extends Common {
    }

    public interface DirectWindows extends Direct, Windows {
    }

    public interface DirectNonWindows extends Direct, NonWindows {
    }

    public interface Http extends Common {
    }

    public interface HttpWindows extends Http, Windows {
    }

    public interface HttpNonWindows extends Http, NonWindows {
    }

    public interface Socks4 extends Common {
    }

    public interface Socks4Windows extends Socks4, Windows {
    }

    public interface Socks4NonWindows extends Socks4, NonWindows {
    }

    public interface Socks5 extends Socks4 {
    }

    public interface Socks5Windows extends Socks5, Windows {
    }

    public interface Socks5NonWindows extends Socks5, NonWindows {
    }

    public interface Pac extends Common {
    }

    public interface PacWindows extends Pac, Windows {
    }

    public interface PacNonWindows extends Pac, NonWindows {
    }

    public static Class<?> getViewForType(ProxyConfig.Type type) {
        switch (type) {
            case DIRECT:
                if (SystemContext.IS_OS_WINDOWS) {
                    return DirectWindows.class;
                } else {
                    return DirectNonWindows.class;
                }
            case HTTP:
                if (SystemContext.IS_OS_WINDOWS) {
                    return HttpWindows.class;
                } else {
                    return HttpNonWindows.class;
                }
            case SOCKS4:
                if (SystemContext.IS_OS_WINDOWS) {
                    return Socks4Windows.class;
                } else {
                    return Socks4NonWindows.class;
                }
            case SOCKS5:
                if (SystemContext.IS_OS_WINDOWS) {
                    return Socks5Windows.class;
                } else {
                    return Socks5NonWindows.class;
                }
            case PAC:
                if (SystemContext.IS_OS_WINDOWS) {
                    return PacWindows.class;
                } else {
                    return PacNonWindows.class;
                }
            default:
                throw new IllegalArgumentException("No view for type: " + type);
        }
    }

}
