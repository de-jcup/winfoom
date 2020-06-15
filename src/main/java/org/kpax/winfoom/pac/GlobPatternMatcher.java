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
import org.springframework.util.Assert;

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
     * <ul>
     * <li> The {@code *} character matches zero or more characters.</li>
     * <li> {@code ?} matches exactly one character.</li>
     * <li> {@code {ab,cd,ef} }matches exactly one of the "parts" provided.
     *  These "parts" can also span multiple "sections" of the glob, so you can include directory separators in them.
     * For example, {@code {ab,cd/ef}/*.js} is valid and would match ab/file.js as well as {@code cd/ef/file.js}.</li>
     * <li>  The use of {@code [abc]} matches a range of characters.
     *  If the first character of the range is {@code !} or {@code ^} then it matches any character not in the range.</li>
     * <li> When {@code **}, the "globstar", is used it matches zero or more directories and subdirectories. This allows for recursive directory searching easily.</li>
     *</ul>
     * <p>
     * <b>Note:</b> The result is cached.
     *
     * @param glob the GLOB pattern.
     * @return the {@link Pattern} instance.
     */
    @Cacheable("precompiledGlobPattern")
    public Pattern toPattern(String glob) {
        Assert.notNull(glob, "glob cannot be null");
        logger.debug("Create Pattern for {}", glob);
        String regexPattern = convertGlobToRegEx(glob.trim());
        logger.debug("glob regexPattern={}", regexPattern);
        return Pattern.compile(regexPattern);
    }

    public String convertGlobToRegEx(String globExpression) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("^");
        for (int i = 0; i < globExpression.length(); ++i) {
            char c = globExpression.charAt(i);
            switch (c) {
                case '*':
                    stringBuilder.append(".*?");
                    break;
                case '?':
                    stringBuilder.append(".{1}");
                    break;
                case '.':
                    stringBuilder.append("\\.");
                    break;
                case '\\':
                    stringBuilder.append("\\\\");
                    break;
                case '!':
                    if (i > 0 && globExpression.charAt(i - 1) == '[') {
                        stringBuilder.append('^');
                    } else {
                        stringBuilder.append(c);
                    }
                    break;
                default:
                    stringBuilder.append(c);
            }
        }
        stringBuilder.append("$");
        return stringBuilder.toString();
    }


}
