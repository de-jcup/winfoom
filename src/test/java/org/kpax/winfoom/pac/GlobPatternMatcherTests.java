/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.pac;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class GlobPatternMatcherTests {

    @Test
    void convertGlobToRegEx_StartWithStar_Matches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("*.java"));
        boolean matches = pattern.matcher("bla/bla.java").matches();
        Assertions.assertTrue(matches);
    }

    @Test
    void convertGlobToRegEx_MatchesOneCharacter_Matches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("x?.java"));
        boolean matches = pattern.matcher("xc.java").matches();
        Assertions.assertTrue(matches);
    }

    @Test
    void convertGlobToRegEx_MatchesOneCharacter_NotMatches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("x?.java"));
        boolean matches = pattern.matcher("xab.java").matches();
        Assertions.assertFalse(matches);
    }

    @Test
    void convertGlobToRegEx_Range_Matches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("bla/x[abc].java"));
        boolean matches = pattern.matcher("bla/xb.java").matches();
        Assertions.assertTrue(matches);
    }

    @Test
    void convertGlobToRegEx_Range_NotMatches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("bla/x[abc].java"));
        boolean matches = pattern.matcher("bla/xbc.java").matches();
        Assertions.assertFalse(matches);
    }

    @Test
    void convertGlobToRegEx_Group_Matches() {
        String regex = GlobPatternMatcher.convertGlobToRegEx("xyz(ab|cd|ef).java");
        System.out.println(regex);
        Pattern pattern = Pattern.compile(regex);
        boolean matches = pattern.matcher("xyzab.java").matches();
        Assertions.assertTrue(matches);
    }

    @Test
    void convertGlobToRegEx_Group_NotMatches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("x(ab|cd|ef).java"));
        boolean matches = pattern.matcher("xbc.java").matches();
        Assertions.assertFalse(matches);
    }

    @Test
    void convertGlobToRegEx_Globstar_Matches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("**.java"));
        boolean matches = pattern.matcher("xbc/abc/bla/foo.java").matches();
        Assertions.assertTrue(matches);
    }

    @Test
    void convertGlobToRegEx_Star_NotMatches() {
        Pattern pattern = Pattern.compile(GlobPatternMatcher.convertGlobToRegEx("*.java"));
        boolean matches = pattern.matcher("xbc/abc/bla/foo.exe").matches();
        Assertions.assertFalse(matches);
    }

}
