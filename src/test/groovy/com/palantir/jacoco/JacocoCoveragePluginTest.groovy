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
        apply plugin: 'jacoco-coverage'

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
                threshold 1.0
            }
        '''.stripIndent()

        then:
        runTasksWithFailure('test', 'checkCoverage')
    }

    def 'checkCoverage succeeds when no coverage is required'() {
        when:
        writeHelloWorld('nebula.hello')
        writeUnitTest(false)
        buildFile << standardBuildFile
        buildFile << '''
            jacocoCoverage {
                threshold 0.0
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('test', 'checkCoverage')
    }
}
