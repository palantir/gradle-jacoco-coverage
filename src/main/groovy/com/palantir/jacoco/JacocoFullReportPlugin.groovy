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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer

/**
 * Adds a 'jacocoFullReport' task to the current project that produces a Jacoco report for code coverage of the tests
 * of all subprojects of the current project. This report can be useful when different subprojects test different parts
 * of a project -- as is often a case when integration tests are factored out into separate subprojects.
 */
public class JacocoFullReportPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.plugins.apply(JacocoPlugin)
        project.evaluationDependsOnChildren()
        JacocoReport fullReportTask = project.tasks.create("jacocoFullReport", JacocoReport)
        project.afterEvaluate({ p ->
            fullReportTask.configure {
                reports.xml.enabled = true
                reports.html.enabled = true

                // Find all projects except for those below "buildSrc".
                def coreProjects = project.subprojects.findAll({ !it.path.startsWith(':buildSrc') }) + [project]
                // Find all JacocoReport tasks except for the jacocoFullReport task we're creating here.
                def reportTasks = coreProjects.collect {
                    it.tasks.withType(JacocoReport).findAll { it != fullReportTask }
                }.flatten()
                project.logger.info("Setting up jacocoFullReport for report tasks: " + reportTasks)

                // Implement fix mentioned in Gradle Source: https://github.com/gradle/gradle/blob/master/subprojects/jacoco/src/main/groovy/org/gradle/testing/jacoco/tasks/JacocoReport.groovy
                setOnlyIf {
                    executionData.any { it.exists() }
                }
                doFirst {
                    executionData = project.files(executionData.findAll { it.exists() })
                }

                // Filter for nulls since some JacacoReport tasks may have no classDirectories or sourceDirectories
                // configured, for example if there are no tests for a subproject.
                executionData reportTasks.executionData
                classDirectories = new UnionFileCollection(reportTasks.collect { it.classDirectories }.findAll {
                    it != null
                })
                sourceDirectories = new UnionFileCollection(reportTasks.collect { it.sourceDirectories }.findAll {
                    it != null
                })
            }
        })
    }
}
