package org.kpax.winfoom.api.json;

import org.kpax.winfoom.config.*;

public class Views {
    public interface Direct {
    }

    public interface Http extends Direct {
    }

    public interface HttpNonWindows extends Http {
    }

    public interface Socks4 extends Direct {
    }

    public interface Socks5 extends Socks4 {
    }

    public interface Pac extends Direct {
    }

    public static Class<?> getView(ProxyConfig.Type type) {
        switch (type) {
            case DIRECT:
                return Direct.class;
            case HTTP:
                if (SystemContext.IS_OS_WINDOWS) {
                    return Http.class;
                }
                return HttpNonWindows.class;
            case SOCKS4:
                return Socks4.class;
            case SOCKS5:
                return Socks5.class;
            case PAC:
                return Pac.class;
            default:
                throw new IllegalArgumentException("No view for type: " + type);
        }
    }
}
