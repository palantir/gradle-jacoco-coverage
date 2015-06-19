/*
 * Copyright 2015 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.jacoco

import java.util.regex.Pattern

public class JacocoCoverageExtension {

    // For convenience in build scripts.
    def BRANCH = CoverageType.BRANCH
    def CLASS = CoverageType.CLASS
    def COMPLEXITY = CoverageType.COMPLEXITY
    def INSTRUCTION = CoverageType.INSTRUCTION
    def LINE = CoverageType.LINE
    def METHOD = CoverageType.METHOD

    public List<Closure<Double>> coverage = new ArrayList<>()

    /**
     * Adds a minimum coverage threshold for all files and coverage types.
     * @param value The minimum required threshold, a number in [0,1].
     */
    def threshold(double value) {
        coverage.add({
            CoverageType ct, String str -> value
        })
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and for all files.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType) {
        threshold(value, coverageType, ~/.*/)
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and files with the given filename (in any directory).
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param filename The name of the file(s) that this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType, String filename) {
        coverage.add({ CoverageType ct, String str ->
            if (coverageType == ct && str.equals(filename)) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and files (in any directory) matching a pattern.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param fileNamePattern A regular expression specifying the name of the file(s) that this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType, Pattern fileNamePattern) {
        coverage.add({ CoverageType ct, String str ->
            if (coverageType == ct && str ==~ fileNamePattern) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Exempts files with given {@code filename} from coverage requirements.
     * @param filename The name of the file(s) that are whitelisted.
     */
    def whitelist(String filename) {
        coverage.add({ CoverageType ct, String str ->
            if (str.equals(filename)) {
                return 0.0 // Whitelist.
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Exempts files (in any directory) whose name matches the given {@code fileNamePattern} from coverage requirements.
     * @param fileNamePattern A regular expression specifying the name of the file(s) that are to be whitelisted.
     */
    def whitelist(Pattern fileNamePattern) {
        coverage.add({ CoverageType ct, String str ->
            if (str ==~ fileNamePattern) {
                return 0.0 // Whitelist.
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }
}
