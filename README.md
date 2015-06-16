# Jacoco Coverage Gradle Plugin

Jacoco Coverage is a Gradle Plugin that provides two tasks extending the standard Gradle Jacoco plugin:
- Firstly, the `jacoco-coverage` plugin allows Gradle build scripts to configure minimum Java Code Coverage thresholds
on a per-project, per-file, and per-coverage-type basis.
- Secondly, the `jacoco-full-report` plugin adds a task that produces a Jacoco report for the combined code coverage of
the tests of all subprojects of the current project.

Release and change logs: [CHANGELOG.md](CHANGELOG.md)

Source code: [https://github.com/palantir/gradle-jacoco-coverage](https://github.com/palantir/gradle-jacoco-coverage)


## jacoco-coverage

#### Quick start

Add the following configuration to `build.gradle`:

    buildscript {
        dependencies {
            classpath 'com.palantir:jacoco-coverage:<version>'
        }
    }
    
    apply plugin: 'jacoco-coverage'
    jacocoCoverage {
        threshold 0.5  // Enfore minimum code coverage of 50% across all files.
        whitelist "MyClass.java"  // Exempt MyClass from coverage requirements.
    }

Subsequent `./gradlew build` runs will fail if the configured minimum coverage thresholds are not achieved by the
project's tests. (Note that `build` depends on `check` which depends on the `jacocoCoverage` task added by this plugin.)


#### Configuration

Code coverage requirements can be specified for a project as a whole, for individual files, and for particular
Jacoco-defined types of coverage, e.g., lines covered or branches covered. The following example describes the syntax:

    jacocoCoverage {
        // Minimum code coverage of 50% across all files and coverage types.
        threshold 0.5

        // Minimum 'branch' coverage of 30% across all files.
        // Available coverage types: BRANCH, CLASS, COMPLEXITY, INSTRUCTION, LINE, METHOD.
        threshold 0.3, BRANCH

        // Minimum 'line' coverage of 10% for file (in any directory) whose name matches the given regular expression.
        threshold 0.1, LINE, ~"(Indentation|Wrapping)\\.java"

        // Minimum 'line' coverage of 10% for files named "Indentation.java" (case-sensitive, in any directory).
        // (Note: This syntax uses exact string match against the file name while the regex syntax requires escaping.)
        threshold 0.1, LINE, "Indentation.java"

        // Files can be exempt from any coverage requirements by exact file name or file name pattern.
        whitelist "MyClass.java"
        whitelist ~".*Test.java"
    }

## jacoco-full-report

#### Quickstart

Add the following configuration to the `build.gradle` configuration of the root project:

    buildscript {
        dependencies {
            classpath 'com.palantir:jacoco-coverage:<version>'
        }
    }

    apply plugin: 'jacoco-full-report'  // Automatically applies the 'jacoco' plugin to this project.

Subsequent `./gradle test jacocoFullReport` runs will generate a test report in `build/reports/jacoco/jacocoFullReport/`
that evaluates the coverage yielded by all subprojects combined. (Note that generally Jacoco reports are only generated
if the `test` task has run previously in order to generated the Jacoco execution data.)

#### Configuration

The `jacocoFullReport` task is a standard `JacocoReport` task and has the same configuration options as the vanilla
[Gradle Jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html) task.
