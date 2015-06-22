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

import nebula.test.IntegrationSpec

import java.nio.file.Files
import java.nio.file.Paths

class JacocoFullReportPluginTest extends IntegrationSpec {

    def standardBuildFile = '''
        allprojects {
            apply plugin: 'java'
            repositories {
                jcenter()
            }

            dependencies {
                testCompile "junit:junit:4.11"
            }
        }
    '''.stripIndent()

    def writeTestableClass(File baseDir) {
        createFile('src/main/java/Foo.java', baseDir) << '''
        public class Foo {
            public int fooA() {
                int variable = 1;
                if (variable == 1) {
                    variable = 2;
                }

                return variable;
            }

            public int fooB() {
                int variable = 2;
                return variable;
            }
        }'''.stripIndent()
    }

    def writeFooATest(File baseDir) {
        createFile('src/test/java/FooATest.java', baseDir) << '''
        import org.junit.Test;

        public class FooATest {
            @Test
            public void testFooA() {
                Foo foo = new Foo();
                foo.fooA();
            }
        }'''.stripIndent()
    }

    def writeFooBTest(File baseDir) {
        createFile('src/test/java/FooBTest.java', baseDir) << '''
        import org.junit.Test;

        public class FooBTest {
            @Test
            public void testFooB() {
                Foo foo = new Foo();
                foo.fooB();
            }
        }'''.stripIndent()
    }

    def setupSubprojectA() {
        def subProjects = helper.create(["testFooA"])

        def testFooA = subProjects["testFooA"]
        testFooA.buildGradle << '''
            apply plugin: 'java'
            apply plugin: 'jacoco'
            jacocoTestReport.reports.xml.enabled = true
        '''.stripIndent()
        writeTestableClass(testFooA.directory)
        writeFooATest(testFooA.directory)
    }

    def setupSubprojectsAb() {
        def subProjects = helper.create(["testFooA", "testFooB"])

        def testFooA = subProjects["testFooA"]
        testFooA.buildGradle << '''
            apply plugin: 'java'
            apply plugin: 'jacoco'
            jacocoTestReport.reports.xml.enabled = true
        '''.stripIndent()
        writeTestableClass(testFooA.directory)
        writeFooATest(testFooA.directory)

        def testFooB = subProjects["testFooB"]
        testFooB.buildGradle << '''
            apply plugin: 'java'
            apply plugin: 'jacoco'
            jacocoTestReport.reports.xml.enabled = true
        '''.stripIndent()
        writeTestableClass(testFooB.directory)
        writeFooBTest(testFooB.directory)
    }

    def numberMissedInstructions(String coverageXml) {
        def fullCoverage = JacocoCoverageTask.parseReport(file(coverageXml).newInputStream())
        def missedInstructions = fullCoverage
                .package
                .sourcefile.find {it.@name=="Foo.java"}
                .counter.find {it.@type=="INSTRUCTION"}
                .@missed.toInteger()

        missedInstructions
    }

    def 'jacoco-full-report can exclude sub projects'() {
        when:
        buildFile << 'apply plugin: "jacoco-full-report"'
        buildFile << standardBuildFile
        buildFile << '''
        jacocoFull {
            excludeProject ":testFooA", ":testFooB"
        }'''
        setupSubprojectsAb()

        then:
        runTasksSuccessfully('test', 'jacocoTestReport', 'jacocoFullReport')
        assert !Files.exists(Paths.get("build/reports/jacoco/jacocoFullReport/jacocoFullReport.xml"))
    }

    def 'jacoco-full-report reports on union of execution data'() {
        when:
        buildFile << 'apply plugin: "jacoco-full-report"'
        buildFile << standardBuildFile
        setupSubprojectsAb()

        then:
        runTasksSuccessfully('test', 'jacocoTestReport', 'jacocoFullReport')
        assert numberMissedInstructions("build/reports/jacoco/jacocoFullReport/jacocoFullReport.xml") == 0
        assert numberMissedInstructions("testFooA/build/reports/jacoco/test/jacocoTestReport.xml") == 4
    }

    def 'jacoco-full-report works when only some subprojects provide execution data'() {
        when:
        buildFile << 'apply plugin: "jacoco-full-report"'
        buildFile << standardBuildFile

        setupSubprojectA()
        helper.addSubproject("testFooB")

        then:
        runTasksSuccessfully('test', 'jacocoTestReport')
        runTasksSuccessfully('jacocoFullReport')
        assert numberMissedInstructions("testFooA/build/reports/jacoco/test/jacocoTestReport.xml") == 4
        assert numberMissedInstructions("build/reports/jacoco/jacocoFullReport/jacocoFullReport.xml") == 4
    }

    def 'jacocoFullReport object has non-null sourceDirectories even when task did not run'() {
        when:
        buildFile << 'apply plugin: "jacoco-full-report"'
        buildFile << standardBuildFile
        buildFile << '''
            // Java source directories are unknown before sub project is evaluated.
            assert tasks.jacocoFullReport.sourceDirectories.size() == 0

            project.afterEvaluate {
                assert tasks.jacocoFullReport.sourceDirectories.size() == 1
            }

            task afterTest() {
                dependsOn tasks.test
                doLast {
                    assert tasks.jacocoFullReport.sourceDirectories.size() == 1
                }
            }
        '''.stripIndent()
        setupSubprojectA()

        then:
        runTasksSuccessfully('test')
    }
}
