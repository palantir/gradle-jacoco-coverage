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

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

import org.junit.Assert

import com.google.common.io.Resources
import spock.lang.Specification

class JacocoCoverageTaskTest extends Specification {

    def getSampleCoverage() {
        def report = Resources.asByteSource(JacocoCoverageTaskTest.class.getResource("/jacocoTestReport.xml"))
        JacocoCoverageTask.extractCoverageFromReport(report.openBufferedStream())
    }

    JacocoCoverageExtension extension
    def setup() {
        extension = new JacocoCoverageExtension()
    }

    def "Parse sample Jacoco XML Report correctly"() {
        when: "A report XML file is parsed"
        def coverage = getSampleCoverage()

        then:
        assertNotNull(coverage)
        assertThat(coverage.keySet(), containsInAnyOrder("AnotherClass.java", "DummyClass.java"))
        assertThat(coverage.get("AnotherClass.java").keySet(), containsInAnyOrder(CoverageType.INSTRUCTION,
                CoverageType.LINE, CoverageType.COMPLEXITY, CoverageType.METHOD, CoverageType.CLASS))

        // Spot-check some extracted numbers.
        Assert.assertEquals(7, coverage.get("DummyClass.java").get(CoverageType.INSTRUCTION).covered)
        Assert.assertEquals(3, coverage.get("DummyClass.java").get(CoverageType.INSTRUCTION).missed)
    }

    def "No rules imply no violations"() {
        setup: "Empty rule set and non-empty coverage provided"
        def rules = []
        def coverage = getSampleCoverage()

        when: "Rules are applied"
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == []
    }

    def "When multiple rules fire, then the one with smaller threshold dominates"() {
        when:
        extension.threshold(0.5)
        extension.threshold(0.1, CoverageType.INSTRUCTION, "AnotherClass.java")
        def rules = extension.coverageRules
        def coverage = getSampleCoverage().findAll {clazz -> clazz.key == "AnotherClass.java"}
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.size() == 5
        violations.each {violation ->
            if (violation.type == CoverageType.INSTRUCTION) {
                assert violation.threshold == (double) 0.1
            } else {
                assert violation.threshold == (double) 0.5
            }
        }
    }

    def "Coverage threshold are inclusive lower bound"() {
        when:
        extension.threshold(0.3, CoverageType.INSTRUCTION, "DummyClass.java") // Coverage is exactly 3/(3+7).
        def violations = JacocoCoverageTask.applyRules(extension.coverageRules, getSampleCoverage())

        then:
        assert violations.isEmpty()
    }

    def "Rules have no effect on coverage types and classes that they don't apply to"() {
        // In particular, this check verifies that the "return 2.0" checks in JacocoPluginExtension#threshold are filtered out.
        when:
        extension.threshold(0.0, CoverageType.INSTRUCTION, "AnotherClass.java")
        def violations = JacocoCoverageTask.applyRules(extension.coverageRules, getSampleCoverage())

        then:
        assert violations.isEmpty()
    }

    def "A more specific rule with a higher threshold does not overrule a less specific one with smaller threshold"() {
        when:
        extension.threshold(0.0)
        extension.threshold(0.9, CoverageType.INSTRUCTION, "AnotherClass.java") // This rule would fail on its own.
        def rules = extension.coverageRules
        def coverage = getSampleCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }

    def "A more specific rule with a lower threshold overrules a less specific one with higher threshold"() {
        when: "Rule set and non-empty coverage provided"
        extension.threshold(0.9, CoverageType.INSTRUCTION) // This rule would fail on its own.
        extension.threshold(0.0, CoverageType.INSTRUCTION, "AnotherClass.java")
        def rules = extension.coverageRules
        def coverage = getSampleCoverage().findAll {clazz -> clazz.key == "AnotherClass.java"}
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }

    def "Whitelisted files are exempt from coverage requirements"() {
        when: "Rule set and non-empty coverage provided"
        extension.whitelist("AnotherClass.java")
        extension.whitelist(~"Dummy.*")
        extension.threshold(0.9, CoverageType.INSTRUCTION, "AnotherClass.java") // This rule would fail on its own.
        extension.threshold(0.9, CoverageType.INSTRUCTION, ~"Dummy.*.java") // This rule would fail on its own.
        def rules = extension.coverageRules
        def coverage = getSampleCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }
}
