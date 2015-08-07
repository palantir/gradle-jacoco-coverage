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
import nebula.test.functional.ExecutionResult

class JacocoCoveragePluginTest extends IntegrationSpec {

    def standardBuildFile = '''
        apply plugin: 'java'
        apply plugin: 'com.palantir.jacoco-coverage'

        repositories {
            jcenter()
        }

        dependencies {
            testCompile "junit:junit:4.11"
        }
    '''.stripIndent()

    def setup() {
        fork = true  // Run test in separate JVM in order to isolate classloaders. Sadly, this makes debugging hard.
    }

    def 'check task depends on checkCoverage task'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile

        then:
        ExecutionResult result = runTasksSuccessfully('check')
        result.wasExecuted(':checkCoverage')
        result.wasExecuted(':check')
    }

    def 'check fails when coverage is required'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile
        buildFile << '''
            jacocoCoverage {
                fileThreshold 1.0
            }
        '''.stripIndent()

        then:
        def result = runTasksWithFailure('test', 'checkCoverage')
        assert result.standardOutput.contains("Found the following Jacoco coverage violations")
    }

    def 'checkCoverage succeeds when no coverage is required'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile
        buildFile << '''
            jacocoCoverage {
                fileThreshold 0.0
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('test', 'checkCoverage')
    }

    def 'All syntax variations work'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile
        buildFile << '''
            jacocoCoverage {
                fileThreshold 0.0
                fileThreshold 0.0, BRANCH
                fileThreshold 0.0, "HelloWorld.java"
                fileThreshold 0.0, ~"HelloWorld\\\\.*"
                fileThreshold 0.0, LINE, "HelloWorld.java"
                fileThreshold 0.0, COMPLEXITY, ~"HelloWorld\\\\..*"

                classThreshold 0.0, LINE, "nebula/hello/HelloWorld"
                packageThreshold 0.0, LINE, "nebula/hello"
                reportThreshold 0.0, LINE, "All-syntax-variations-work"
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('build', 'checkCoverage')
    }

    def 'Violations are reported for every realm'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile
        buildFile << '''
            jacocoCoverage {
                fileThreshold 1.0, LINE, "HelloWorld.java"
                classThreshold 1.0, LINE, "nebula/hello/HelloWorld"
                packageThreshold 1.0, LINE, "nebula/hello"
                reportThreshold 1.0, LINE, "Violations-are-reported-for-every-realm"
            }
        '''.stripIndent()

        then:
        def result = runTasksWithFailure('test', 'checkCoverage')
        assert result.standardOutput.contains('Violations-are-reported-for-every-realm (0/3 LINE coverage < 1.00000)')
        assert result.standardOutput.contains('HelloWorld.java (0/3 LINE coverage < 1.00000)')
        assert result.standardOutput.contains('nebula/hello (0/3 LINE coverage < 1.00000)')
        assert result.standardOutput.contains('nebula/hello/HelloWorld (0/3 LINE coverage < 1.00000)')
    }
}
