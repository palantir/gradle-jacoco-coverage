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

import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public class JacocoCoverageTask extends DefaultTask {

    private static final OrderBy<CoverageViolation> VIOLATION_ORDER =
            new OrderBy([{ violation -> violation.clazz }, { violation -> violation.type }])

    /**
     * Coverage results (CoverageType -> CoverageCounter) for different scopes, i.e., report name, package name, source
     * file name, class name. Populated by {@link JacocoCoverageTask#extractCoverage}, called from
     * {@link JacocoCoveragePlugin#apply}.
     */
    Map<CoverageRealm, Map<String, CoverageObservation>> coverage = Maps.newHashMap()

    @TaskAction
    def verifyCoverage() {
        JacocoCoverageExtension extension = getProject().getExtensions().getByType(JacocoCoverageExtension.class)
        List<CoverageViolation> violations = applyRules(extension.coverage, coverage)
        Collections.sort(violations, VIOLATION_ORDER)
        if (!violations.isEmpty()) {
            getLogger().quiet("Found the following Jacoco coverage violations")
            for (CoverageViolation violation : violations) {
                getLogger().quiet("{}", violation)
            }
            throw new GradleException("Coverage violations found")
        }
    }

    static List<CoverageViolation> applyRules(
            Multimap<CoverageRealm, Closure<Double>> rules,
            Map<CoverageRealm, Map<String, CoverageObservation>> coverageObservations) {
        List<CoverageViolation> violations = []
        CoverageRealm.values().each { realm ->
            violations += applyRules(rules.get(realm), coverageObservations.get(realm))
        }

        violations
    }

    /**
     * Applies the given coverage rules to the observed code coverage observations and returns a list of violating
     * observations.
     */
    static List<CoverageViolation> applyRules(Collection<Closure<Double>> rules,
                                              Map<String, CoverageObservation> coverageObservations) {

        List<CoverageViolation> violations = []
        coverageObservations.each { clazz, clazzScores ->
            clazzScores.each { coverageType, coverageCounter ->
                // Filter
                def thresholds = rules
                        .collect({ rule -> rule.call(coverageType, clazz) })
                        .findAll({ threshold -> threshold <= 1.0 }) // Filter out any check requiring >100% coverage.

                if (!thresholds.isEmpty()) {
                    def minThreshold = thresholds.min()
                    def total = coverageCounter.missed + coverageCounter.covered
                    if (coverageCounter.covered < minThreshold * total) {
                        violations.add(new CoverageViolation(
                                clazz, coverageCounter.covered, minThreshold, total, coverageType))
                    }
                }
            }
        }

        violations
    }

    /**
     * Extracts coverage from given report file and sets the {@code coverage} map accordingly.
     */
    static Map<CoverageRealm, Map<String, CoverageObservation>> extractCoverage(GPathResult jacocoXmlReport) {
        Map<CoverageRealm, Map<String, CoverageObservation>> extractedCoverage = Maps.newHashMap()
        CoverageRealm.values().each { realm ->
            extractedCoverage.put(realm, extractScopeCoverage(jacocoXmlReport, realm.tagName));

        }

        extractedCoverage
    }

    /**
     * Returns the Jacoco coverage results as a map <scope name> -> (<coverage type> -> (covered cases, missed cases)).
     */
    static Map<String, CoverageObservation> extractScopeCoverage(GPathResult jacocoXmlReport, String scope) {
        def Map<String, CoverageObservation> coverage = new HashMap<>()

        jacocoXmlReport.'**'.findAll { it.name() == scope }.each({ sourceFile ->
            String sourceFileName = sourceFile.@name
            CoverageObservation submap = coverage.get(sourceFileName, new CoverageObservation())
            sourceFile.counter.each({ counter ->
                CoverageType type = CoverageType.valueOf(counter.@type.toString())
                submap[type] = new CoverageCounter(counter.@covered.toInteger(), counter.@missed.toInteger())
            })
        })

        coverage
    }

    /**
     * Parses the given Jacoco XML report into a GPathResult object and returns it.
     */
    static GPathResult parseReport(InputStream jacocoXmlReport) {
        def parser = new XmlSlurper()
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        parser.setFeature("http://xml.org/sax/features/namespaces", false)

        parser.parse(jacocoXmlReport)
    }
}
