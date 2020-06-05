/*
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
package org.kpax.winfoom.pac;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Methods and constants useful in PAC script evaluation.
 *
 * @author lbruun
 */
public class PacUtils {

    /**
     * Size of the cache used for precompiled GLOBs.
     */
    public static final int PRECOMPILED_GLOB_PATTERN_CACHE_MAX_ITEMS = 100;

    private static final Cache<String, Pattern> cache = Cache2kBuilder.of(new Cache2kConfiguration<String, Pattern>())
            .name("precompiledGlobPattern")
            .eternal(true)
            .entryCapacity(PRECOMPILED_GLOB_PATTERN_CACHE_MAX_ITEMS)
            .build();


    /**
     * Translate a GLOB pattern into a RegExp pattern. GLOB patterns originate
     * from Unix hosts where they are primarily used for file pattern matching.
     * In the original PAC specification from Netscape a GLOB pattern is
     * referred to as a 'shell expression'.
     *
     * <p>
     * This method supports all GLOB wildcards, such as
     * <table border="0" style="order-collapse: separate;border-spacing: 50px 0;" summary="">
     * <tr align="left"><td>{@code *}</td><td>matches any number of any
     * characters including none</td>
     * <tr align="left"><td>{@code ?}</td><td>matches any single character</td>
     * <tr align="left"><td>{@code [abc]}</td><td>matches one character given in
     * the bracket</td>
     * <tr align="left"><td>{@code [a-z]}</td><td>matches one character from the
     * range given in the bracket</td>
     * <tr align="left"><td>{@code [!abc]}</td><td>matches one character
     * <i>not</i> given in the bracket</td>
     * <tr align="left"><td>{@code [!a-z]}</td><td>matches one character
     * <i>not</i> from the range given in the bracket</td>
     * </table>
     *
     * <p>
     * A small cache is used so that if a glob pattern has already been
     * translated previously, the result from the cache will be returned.
     *
     * @param glob the GLOB pattern.
     * @return the pattern.
     */
    public static Pattern createRegexPatternFromGlob(String glob) {

        // First try the cache
        Pattern pattern = cache.get(glob);
        if (pattern != null) {
            return pattern;
        }

        StringBuilder out = new StringBuilder();
        out.append("^");
        for (int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    out.append(".*?");
                    break;
                case '?':
                    out.append(".{1}");
                    break;
                case '.':
                    out.append("\\.");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '!':
                    if (i > 0 && glob.charAt(i - 1) == '[') {
                        out.append('^');
                    } else {
                        out.append(c);
                    }
                    break;
                default:
                    out.append(c);
            }
        }
        out.append("$");
        pattern = Pattern.compile(out.toString());
        cache.put(glob, pattern);
        return pattern;
    }

    /**
     * Completes an IPv6 literal address if it is incomplete at the end.
     * This is done by appending "::" if needed.
     *
     * @param s
     * @return
     */
    public static String correctIPv6Str(String s) {
        int counter = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ':') {
                counter++;
                if (i > 0) {
                    if (s.charAt(i - 1) == ':') {
                        return s;
                    }
                }
            }
        }
        if (counter != 7) {
            return s + "::";
        } else {
            return s;
        }
    }

    /**
     * Cleans a URI into a format suitable for passing to the PAC script.
     * (meaning suitable for passing as {@code url} argument to
     * {@code FindProxyForURL(url, host)} or {@code FindProxyForURLEx(url, host)}
     * functions).
     * <p>
     * Because a PAC script is downloaded from a potentially malicious source it
     * may contain harmful code. Therefore, the amount of information passed to
     * the script should be limited to what is strictly necessary for the script
     * to make decisions about choice of proxy. Anything in the URL which can
     * potentially identity the user or which may contain session specific
     * information should be removed before passing to script.
     *
     * <p>
     * The following is removed:
     * <ul>
     *   <li><i>{@code user-info}</i></li>
     *   <li><i>{@code path}</i> and everything that follows after</li>
     * </ul>
     *
     * <p>
     * Example:
     * <pre>
     *    https://mary@netbeans.apache.org:8081/path/to/something?x1=Christmas&amp;user=unknown
     * becomes
     *    https://netbeans.apache.org:8081/
     * </pre>
     *
     * <p>
     * Note that the majority of PAC scripts out there do not make use of the
     * {@code url} parameter at all. Instead they only use the {@code host}
     * parameter. The stripping of information means that the {@code url}
     * parameter only has two pieces of information that the {@code host}
     * parameter doesn't have: protocol and port number.
     * <br>
     *
     * @param uri URL to be cleansed
     * @return stripped URL string
     */
    public static String toStrippedURLStr(URI uri) {
        return uri.getScheme() +
                "://" +
                uri.getHost() +
                (uri.getPort() == -1 ? "" : ":" + uri.getPort()) +
                "/";  // Chrome seems to always append the slash so we do it too
    }

}
