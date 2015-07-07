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

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap

import java.util.regex.Pattern

/**
 * Provides configuration options for the {@link JacocoCoveragePlugin}, in particular the fileThreshold, classThreshold,
 * packageThreshold, reportThreshold keywords for specifying coverage requirements.
 */
public class JacocoCoverageExtension {

    // For convenience in build scripts.
    def BRANCH = CoverageType.BRANCH
    def CLASS = CoverageType.CLASS
    def COMPLEXITY = CoverageType.COMPLEXITY
    def INSTRUCTION = CoverageType.INSTRUCTION
    def LINE = CoverageType.LINE
    def METHOD = CoverageType.METHOD

    public Multimap<CoverageRealm, Closure<Double>> coverage = ArrayListMultimap.create();

    public JacocoCoverageExtension() {
        def mc = new ExpandoMetaClass(JacocoCoverageExtension, false, true)
        mc.initialize()
        this.metaClass = mc

        // For each realm, add configuration specifiers (fileThreshold, classThreshold, etc) to the extension.
        CoverageRealm.values().each { realm ->
            mc."${realm.realmName}" = { double value ->
                threshold(realm, value)
            }

            mc."${realm.realmName}" = { double value, CoverageType coverageType ->
                threshold(realm, value, coverageType)
            }

            mc."${realm.realmName}" = { double value, String scope ->
                threshold(realm, value, scope)
            }

            mc."${realm.realmName}" = { double value, CoverageType coverageType, String scope ->
                threshold(realm, value, coverageType, scope)
            }

            mc."${realm.realmName}" = { double value, Pattern scopePattern ->
                threshold(realm, value, scopePattern)
            }

            mc."${realm.realmName}" = { double value, CoverageType coverageType, Pattern scopePattern ->
                threshold(realm, value, coverageType, scopePattern)
            }
        }
    }

    /**
     * Adds a minimum coverage threshold for all scopes and coverage types in the given realm.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     */
    def threshold(CoverageRealm realm, double value) {
        coverage.put(realm, {
            CoverageType ct, String str -> value
        })
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and for all scopes in the given realm.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this threshold applies to.
     */
    def threshold(CoverageRealm realm, double value, CoverageType coverageType) {
        threshold(realm, value, coverageType, ~/.*/)
    }

    /**
     * Adds a minimum coverage threshold for all coverage types and the given scope name in the given realm.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scope The scope (e.g., file name, class name, package name, report name) that this threshold applies to.
     */
    def threshold(CoverageRealm realm, double value, String scope) {
        CoverageType.values().each { coverageType ->
            coverage.put(realm, { CoverageType ct, String str ->
                if (coverageType == ct && str.equals(scope)) {
                    return value
                }
                return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
            })
        }
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and scope name (exact match) in the given realm.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scope The scope name that this threshold applies to.
     */
    def threshold(CoverageRealm realm, double value, CoverageType coverageType, String scope) {
        coverage.put(realm, { CoverageType ct, String str ->
            if (coverageType == ct && str.equals(scope)) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }

    /**
     * Adds a minimum coverage threshold for all coverage types and scopes (in the given realm) matching the given
     * pattern.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scopePattern A regular expression specifying the names of the scopes that this threshold applies to.
     */
    def threshold(CoverageRealm realm, double value, Pattern scopePattern) {
        CoverageType.values().each { coverageType ->
            coverage.put(realm, { CoverageType ct, String str ->
                if (coverageType == ct && str ==~ scopePattern) {
                    return value
                }
                return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
            })
        }
    }

    /**
     * Adds a minimum coverage threshold for the given coverage type and scopes matching the given pattern.
     * @param realm The realm that this threshold applies to.
     * @param value The minimum required threshold, a number in [0,1].
     * @param coverageType The type of JaCoCo coverage this rule applies to.
     * @param scopePattern A regular expression specifying the names of the scopes that this threshold applies to.
     */
    def threshold(CoverageRealm realm, double value, CoverageType coverageType, Pattern scopePattern) {
        coverage.put(realm, { CoverageType ct, String str ->
            if (coverageType == ct && str ==~ scopePattern) {
                return value
            }
            return 2.0 // Thresholds >1.0 are never considered, i.e. this rule will be ignored.
        })
    }
}
