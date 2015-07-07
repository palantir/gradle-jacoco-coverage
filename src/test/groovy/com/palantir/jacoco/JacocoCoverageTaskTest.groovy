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

import com.google.common.io.Resources
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class JacocoCoverageTaskTest extends Specification {

    static def XML_REPORT = Resources.asByteSource(JacocoCoverageTaskTest.class.getResource("/jacocoTestReport.xml"))
    static def REPORT = JacocoCoverageTask.parseReport(XML_REPORT.openBufferedStream())

    def getSampleSourceFileCoverage() {
        return JacocoCoverageTask.extractScopeCoverage(REPORT, "sourcefile")
    }

    def getSampleCoverage() {
        JacocoCoverageTask.extractCoverage(REPORT)
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
        assertThat(coverage.get(CoverageRealm.FILE).keySet(), containsInAnyOrder(
                "AnotherClass.java", "DummyClass.java"))
        assertThat(coverage.get(CoverageRealm.CLASS).keySet(), containsInAnyOrder(
                "org/somepackage/DummyClass", "org/somepackage/AnotherClass"))
        assertThat(coverage.get(CoverageRealm.PACKAGE).keySet(), containsInAnyOrder("org/somepackage"))
        assertThat(coverage.get(CoverageRealm.REPORT).keySet(), containsInAnyOrder("test-report"))

        assertThat(coverage.get(CoverageRealm.FILE).get("AnotherClass.java").keySet(), containsInAnyOrder(
                CoverageType.INSTRUCTION,
                CoverageType.LINE,
                CoverageType.COMPLEXITY,
                CoverageType.METHOD,
                CoverageType.CLASS))

        // Spot-check some extracted numbers.
        assert 7 == coverage.get(CoverageRealm.FILE).get("DummyClass.java").get(CoverageType.INSTRUCTION).covered
        assert 1 == coverage.get(CoverageRealm.FILE).get("DummyClass.java").get(CoverageType.METHOD).missed
        assert 2 == coverage.get(CoverageRealm.CLASS).
                get("org/somepackage/AnotherClass").get(CoverageType.COMPLEXITY).missed
        assert 1 == coverage.get(CoverageRealm.PACKAGE).get("org/somepackage").get(CoverageType.LINE).missed
        assert 42 == coverage.get(CoverageRealm.REPORT).get("test-report").get(CoverageType.METHOD).missed
    }

    def "No rules imply no violations"() {
        setup: "Empty rule set and non-empty coverage provided"
        def rules = []
        def coverage = getSampleSourceFileCoverage()

        when: "Rules are applied"
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == []
    }

    def "When multiple rules fire, then the one with smaller threshold dominates"() {
        when:
        extension.threshold(CoverageRealm.FILE, 0.5)
        extension.threshold(CoverageRealm.FILE, 0.1, CoverageType.INSTRUCTION, "AnotherClass.java")
        def rules = extension.coverage.get(CoverageRealm.FILE)
        def coverage = getSampleSourceFileCoverage().findAll { clazz -> clazz.key == "AnotherClass.java" }
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == [new CoverageViolation("AnotherClass.java", 0, 0.5, 1, CoverageType.CLASS),
                              new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.COMPLEXITY),
                              new CoverageViolation("AnotherClass.java", 0, 0.1, 4, CoverageType.INSTRUCTION),
                              new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.LINE),
                              new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.METHOD)]
    }

    def "Coverage thresholds are inclusive lower bound"() {
        when:
        // Coverage is exactly 3/(3+7).
        extension.threshold(CoverageRealm.FILE, 0.3, CoverageType.INSTRUCTION, "DummyClass.java")
        def violations = JacocoCoverageTask.applyRules(
                extension.coverage.get(CoverageRealm.FILE), getSampleSourceFileCoverage())

        then:
        assert violations.isEmpty()
    }

    def "Rules have no effect on coverage types and classes that they don't apply to"() {
        // In particular, this check verifies that the "return 2.0" checks in JacocoCoverageExtension#threshold
        // are filtered out.
        when:
        extension.threshold(CoverageRealm.FILE, 0.0, CoverageType.INSTRUCTION, "AnotherClass.java")
        def violations = JacocoCoverageTask.applyRules(
                extension.coverage.get(CoverageRealm.FILE), getSampleSourceFileCoverage())

        then:
        assert violations.isEmpty()
    }

    def "A more specific rule with a higher threshold does not overrule a less specific one with smaller threshold"() {
        when:
        extension.threshold(CoverageRealm.FILE, 0.0)
        extension.threshold(CoverageRealm.FILE, 0.9, CoverageType.INSTRUCTION, "AnotherClass.java") // This rule would fail on its own.
        def rules = extension.coverage.get(CoverageRealm.FILE)
        def coverage = getSampleSourceFileCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }

    def "A more specific rule with a lower threshold overrules a less specific one with higher threshold"() {
        when:
        extension.threshold(CoverageRealm.FILE, 0.9, CoverageType.INSTRUCTION) // This rule would fail on its own.
        extension.threshold(CoverageRealm.FILE, 0.0, CoverageType.INSTRUCTION, "AnotherClass.java")
        def rules = extension.coverage.get(CoverageRealm.FILE)
        def coverage = getSampleSourceFileCoverage().findAll { clazz -> clazz.key == "AnotherClass.java" }
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }

    def "Whitelisted files are exempt from coverage requirements"() {
        when:
        extension.threshold(CoverageRealm.FILE, 0.0, "AnotherClass.java")
        extension.threshold(CoverageRealm.FILE, 0.0, ~"Dummy.*")
        extension.threshold(CoverageRealm.FILE, 0.9, CoverageType.INSTRUCTION, "AnotherClass.java") // This rule would fail on its own.
        extension.threshold(CoverageRealm.FILE, 0.9, CoverageType.INSTRUCTION, ~"Dummy.*.java") // This rule would fail on its own.
        def rules = extension.coverage.get(CoverageRealm.FILE)
        def coverage = getSampleSourceFileCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations.isEmpty()
    }

    def "Thresholds can be set for any scope"() {
        when:
        extension.threshold(CoverageRealm.PACKAGE, 0.5, CoverageType.COMPLEXITY, "org/somepackage")
        extension.threshold(CoverageRealm.PACKAGE, 0.5, CoverageType.LINE, "org/somepackage")  // Satisfied.
        extension.threshold(CoverageRealm.REPORT, 0.9, CoverageType.INSTRUCTION, "test-report")
        def rules = extension.coverage
        def coverage = getSampleCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == [new CoverageViolation("org/somepackage", 0, 0.5, 3, CoverageType.COMPLEXITY),
                              new CoverageViolation("test-report", 15, 0.9, 20, CoverageType.INSTRUCTION)]
    }

    def "Any scope can be whitelisted"() {
        when:
        extension.threshold(CoverageRealm.FILE, 1.0, CoverageType.CLASS)
        extension.threshold(CoverageRealm.CLASS, 1.0, CoverageType.CLASS)
        extension.threshold(CoverageRealm.PACKAGE, 1.0, CoverageType.CLASS)
        extension.threshold(CoverageRealm.REPORT, 1.0, CoverageType.CLASS)

        extension.threshold(CoverageRealm.FILE, 0.0, "AnotherClass.java")
        extension.threshold(CoverageRealm.CLASS, 0.0, "org/somepackage/DummyClass")
        extension.threshold(CoverageRealm.PACKAGE, 0.0, "org/somepackage")
        extension.threshold(CoverageRealm.REPORT, 0.0, "test-report")
        def rules = extension.coverage
        def coverage = getSampleCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == [new CoverageViolation("DummyClass.java", 0, 1.0, 1, CoverageType.CLASS),
                              new CoverageViolation("org/somepackage/AnotherClass", 0, 1.0, 1, CoverageType.CLASS)]
    }

    def "Thresholds can be defined for all scopes at once"() {
        when:
        extension.threshold(CoverageRealm.FILE, 0.5, "AnotherClass.java")
        extension.threshold(CoverageRealm.CLASS, 0.9, ~"org/somepackage/Dummy.*")
        def rules = extension.coverage
        def coverage = getSampleCoverage()
        def violations = JacocoCoverageTask.applyRules(rules, coverage)

        then:
        assert violations == [
                new CoverageViolation("AnotherClass.java", 0, 0.5, 1, CoverageType.CLASS),
                new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.COMPLEXITY),
                new CoverageViolation("AnotherClass.java", 0, 0.5, 4, CoverageType.INSTRUCTION),
                new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.LINE),
                new CoverageViolation("AnotherClass.java", 0, 0.5, 2, CoverageType.METHOD),
                new CoverageViolation("org/somepackage/DummyClass", 0, 0.9, 1, CoverageType.CLASS),
                new CoverageViolation("org/somepackage/DummyClass", 0, 0.9, 1, CoverageType.COMPLEXITY),
                new CoverageViolation("org/somepackage/DummyClass", 0, 0.9, 3, CoverageType.INSTRUCTION),
                new CoverageViolation("org/somepackage/DummyClass", 0, 0.9, 1, CoverageType.LINE),
                new CoverageViolation("org/somepackage/DummyClass", 0, 0.9, 1, CoverageType.METHOD)]
    }
}
