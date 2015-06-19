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
     * Adds a minimum coverage threshold for all scopes and coverage types.
     * @param value The minimum required threshold, a number in [0,1].
     */
    def threshold(double value) {
        coverage.add({
            CoverageType ct, String str -> value
        })
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and for all scopes.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType) {
        threshold(value, coverageType, ~/.*/)
    }

    /**
     * Adds a minimum coverage threshold for all coverage types and the given scope name.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scope The scope (e.g., file name, class name, package name, report name) that this threshold applies to.
     */
    def threshold(double value, String scope) {
        CoverageType.values().each { coverageType ->
            coverage.add({ CoverageType ct, String str ->
                if (coverageType == ct && str.equals(scope)) {
                    return value
                }
                return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
            })
        }
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and scope (exact match).
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scope The scope that this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType, String scope) {
        coverage.add({ CoverageType ct, String str ->
            if (coverageType == ct && str.equals(scope)) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Adds a minimum coverage threshold for all coverage types and files scopes matching the given pattern.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scopePattern A regular expression specifying the names of the scopes that this threshold applies to.
     */
    def threshold(double value, Pattern scopePattern) {
        CoverageType.values().each { coverageType ->
            coverage.add({ CoverageType ct, String str ->
                if (coverageType == ct && str ==~ scopePattern) {
                    return value
                }
                return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
            })
        }
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type scopes matching the given pattern.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scopePattern A regular expression specifying the names of the scopes that this threshold applies to.
     */
    def threshold(double value, CoverageType coverageType, Pattern scopePattern) {
        coverage.add({ CoverageType ct, String str ->
            if (coverageType == ct && str ==~ scopePattern) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Exempts the given scope from coverage requirements.
     * @param scope The name of the scope that is to be whitelisted.
     */
    def whitelist(String scope) {
        coverage.add({ CoverageType ct, String str ->
            if (str.equals(scope)) {
                return 0.0 // Whitelist.
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Exempts scopes matching the given pattern from coverage requirements.
     * @param scopePattern A regular expression specifying the scope that is to be whitelisted.
     */
    def whitelist(Pattern scopePattern) {
        coverage.add({ CoverageType ct, String str ->
            if (str ==~ scopePattern) {
                return 0.0 // Whitelist.
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }
}
