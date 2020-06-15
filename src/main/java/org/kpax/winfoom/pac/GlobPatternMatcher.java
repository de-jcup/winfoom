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

package org.kpax.winfoom.pac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * GLOB pattern matcher.
 */
@Lazy
@Component
public class GlobPatternMatcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * Translate a GLOB pattern into a {@link Pattern} instance.
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
     * <b>Note:</b> The result is cached.
     *
     * @param glob the GLOB pattern.
     * @return the {@link Pattern} instance.
     */
    @Cacheable("precompiledGlobPattern")
    public Pattern toPattern(String glob) {
        logger.debug("Create Pattern for {}", glob);
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
        return Pattern.compile(out.toString());
    }

}
