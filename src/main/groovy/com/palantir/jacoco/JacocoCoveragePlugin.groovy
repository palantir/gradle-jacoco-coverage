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
import org.gradle.api.tasks.TaskContainer
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer

/**
 * A wrapper for the standard Jacoco Gradle plugin that allows users to specify minimum coverage requirements individual
 * files and coverage types.
 */
public class JacocoCoveragePlugin implements Plugin<Project> {

    /**
     * Applies this plugin to the given {@code project} by first applying the Gradle Jacoco plugin and then registering
     * a 'checkCoverage' task that verifies the set of coverage rules registered in {@link JacocoCoverageExtension}.
     */
    @Override
    public void apply(Project project) {
        project.apply(Collections.singletonMap("plugin", "jacoco"))
        project.getExtensions().create("jacocoCoverage", JacocoCoverageExtension.class)
        project.afterEvaluate({ p ->
            TaskContainer tasks = p.getTasks()
            JacocoCoverageTask checkCoverage = tasks.create("checkCoverage", JacocoCoverageTask.class)
            tasks.findByName("check").dependsOn(checkCoverage)
            tasks.withType(JacocoReport.class, { report ->
                checkCoverage.dependsOn(report)
                JacocoReportsContainer reportsContainer = report.getReports()
                reportsContainer.getXml().setEnabled(true)
                reportsContainer.getHtml().setEnabled(true)
                reportsContainer.getCsv().setEnabled(true)
                checkCoverage.doFirst({ task ->
                    File xmlReportFile = report.reports.xml.getDestination()
                    if (xmlReportFile.canRead()) {
                        def xmlReport = JacocoCoverageTask.parseReport(xmlReportFile.newInputStream())
                        checkCoverage.setCoverage(checkCoverage.extractCoverage(xmlReport))
                    } else {
                        project.logger.info("Skipping ${JacocoCoveragePlugin.simpleName} since no Jacoco coverage " +
                            "report was found in: ${xmlReportFile}")
                    }
                })
            })
        })
    }
}
