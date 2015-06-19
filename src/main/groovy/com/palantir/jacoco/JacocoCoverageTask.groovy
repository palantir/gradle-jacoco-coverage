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
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

public class JacocoCoverageTask extends DefaultTask {

    private static final OrderBy<CoverageViolation> VIOLATION_ORDER =
            new OrderBy([{ violation -> violation.clazz }, { violation -> violation.type }])

    /** The Jacoco coverage results for each file and coverage type; populated by {@link JacocoCoveragePlugin#apply}. */
    Map<String, Map<CoverageType, CoverageCounter>> coverage = Maps.newHashMap()

    @TaskAction
    def verifyCoverage() {
        JacocoCoverageExtension extension = getProject().getExtensions().getByType(JacocoCoverageExtension.class)
        List<CoverageViolation> violations = applyRules(extension.coverageRules, coverage)
        Collections.sort(violations, VIOLATION_ORDER)
        if (!violations.isEmpty()) {
            getLogger().quiet("Found the following Jacoco coverage violations")
            for (CoverageViolation violation : violations) {
                getLogger().quiet("{}", violation)
            }
            throw new GradleException("Coverage violations found")
        }
    }

    /**
     * Applies the given coverage rules to the observed code coverage observations and returns a list of violating
     * observations.
     */
    static List<CoverageViolation> applyRules(List<Closure<Double>> rules,
                                              Map<String, Map<CoverageType, CoverageCounter>> coverageObservations) {

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
     * Returns the Jacoco coverage results as a map <source file> -> (<coverage type> -> (covered cases, missed cases)).
     */
    static Map<String, Map<CoverageType, CoverageCounter>> extractCoverageFromReport(InputStream jacocoXmlReport) {
        def document = parseJacocoXmlReport(jacocoXmlReport)
        def Map<String, Map<CoverageType, CoverageCounter>> coverage = new HashMap<>()

        document.'**'.findAll { it.name() == "sourcefile" }.each({ sourceFile ->
            String sourceFileName = sourceFile.@name
            Map<CoverageType, CoverageCounter> submap = coverage.get(sourceFileName, new EnumMap<>(CoverageType.class))
            sourceFile.counter.each({ counter ->
                if (counter.name().equals("counter")) {
                    CoverageType type = CoverageType.valueOf(counter.@type.toString())
                    submap[type] = new CoverageCounter(counter.@covered.toInteger(), counter.@missed.toInteger())
                }
            })
        })

        coverage
    }

    /**
     * Parses the given Jacoco report into a GPathResult object and returns it.
     */
    static GPathResult parseJacocoXmlReport(InputStream jacocoXmlReport) {
        def parser = new XmlSlurper()
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        parser.setFeature("http://xml.org/sax/features/namespaces", false)

        parser.parse(jacocoXmlReport)
    }
}
