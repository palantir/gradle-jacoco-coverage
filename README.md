# Jacoco Coverage Gradle Plugin

Jacoco Coverage is a Gradle Plugin that provides two tasks extending the standard Gradle Jacoco plugin:
- Firstly, the `jacoco-coverage` plugin allows Gradle build scripts to configure minimum Java Code Coverage thresholds
for projects, packages, classes, and files.
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
        threshold 0.5  // Enfore minimum code coverage of 50% for every file, class, package, and project.
        whitelist "MyClass.java"  // Exempt files named MyClass.java from coverage requirements.
    }

Subsequent `./gradlew build` runs will fail if the configured minimum coverage thresholds are not achieved by the
project's tests. (Note that `build` depends on `check` which depends on the `jacocoCoverage` task added by this plugin.)


#### Configuration

Code coverage requirements can be specified for different scopes:
- average coverage in a report
- average coverage in a package
- coverage in a class
- coverage in a file

Scopes are specified either by their exact name as it appears in the Jacoco XML report, for example package scope
`"org/product/module"`, file scope `"MyClass.java"`, class scope `"org/product/module/MyCLass"`), or by a regular
expression matching those scope names, for example `~"org.*"` for all packages or class scopes starting with `org`.

Coverage requirements are with respect to one of the Jacoco-defined types of coverage, e.g., lines covered or branches
covered.

The following examples describe the syntax:

    jacocoCoverage {
        // Minimum code coverage of 50% for all scopes (files, classes, packages, reports) and coverage types.
        threshold 0.5

        // Minimum 'branch' coverage of 30% for all scopes.
        // Available coverage types: BRANCH, CLASS, COMPLEXITY, INSTRUCTION, LINE, METHOD.
        threshold 0.3, BRANCH

        // Minimum average coverage of 30% in given package.
        threshold 0.3, "org/company/module"

        // Minimum average coverage of 50% in report "my-project; the report name is usually the Gradle project name.
        threshold 0.5, "my-project" 

        // Minimum 'line' coverage of 10% for files (in any directory) whose name matches the given regular expression.
        threshold 0.1, LINE, ~"(Indentation|Wrapping)\\.java"

        // Minimum 'line' coverage of 10% for files named "Indentation.java" (case-sensitive, in any directory).
        // (Note: This syntax uses exact string match against the file name while the regex syntax requires escaping.)
        threshold 0.1, LINE, "Indentation.java"

        // Scopes can be exempt from all coverage requirements by exact scope name or scope name pattern.
        whitelist "MyClass.java"
        whitelist "org/company/module"
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

By default, Jacoco report tasks of all projects and subprojects are considered when compiling the full report. Projects
can be excluded from consideration through the `jacocoFull` extension:

    jacocoFull {
        excludeProject ":my-sub-project", ":buildSrc"
    }