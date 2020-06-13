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

/*
 * Modifications copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.pac;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.kpax.winfoom.util.functional.SingletonSupplier;

import java.util.regex.Pattern;

/**
 * Methods and constants useful in PAC script evaluation.
 */
public class PacUtils {

    /**
     * Size of the cache used for precompiled GLOBs.
     */
    public static final int GLOB_PATTERN_CACHE_CAPACITY = 100;

    private static final SingletonSupplier<Cache<String, Pattern>> cacheSupplier =
            new SingletonSupplier<>(() -> Cache2kBuilder.of(
                    new Cache2kConfiguration<String, Pattern>()).name("precompiledGlobPattern")
                    .eternal(true)
                    .entryCapacity(GLOB_PATTERN_CACHE_CAPACITY)
                    .build());

    private PacUtils() {
    }

    /**
     * Translate a GLOB pattern into a RegExp pattern. GLOB patterns originate
     * from Unix hosts where they are primarily used for file pattern matching.
     * In the original PAC specification from Netscape a GLOB pattern is
     * referred to as a 'shell expression'.
     *
     * <p>
     * This method supports all GLOB wildcards, such as
     * <table>
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
     * A cache is used so that if a glob pattern has already been
     * translated previously, the result from the cache will be returned.
     *
     * @param glob the GLOB pattern.
     * @return the {@link Pattern} instance.
     */
    public static Pattern createGlobRegexPattern(String glob) {

        // First try the cache
        Cache<String, Pattern> cache = cacheSupplier.get();
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

}
