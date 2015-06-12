# Jacoco Coverage Gradle Plugin

Jacoco Coverage is a Gradle Plugin that allows Gradle build scripts to configure minimum Java Code Coverage thresholds
on a per-project, per-file, and per-coverage-type basis.

- Release and change logs: [CHANGELOG.md](CHANGELOG.md)
- Source code: [https://github.com/palantir/gradle-jacoco-coverage](https://github.com/palantir/gradle-jacoco-coverage)


## Quick start

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

Subsequent `./gradlew build --info` runs will fail if the configured minimum coverage thresholds are not achieved by the
project's tests.


## Configuration

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
