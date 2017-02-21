[![Build Status](https://travis-ci.org/palantir/gradle-jacoco-coverage.svg?branch=develop)](https://travis-ci.org/palantir/gradle-jacoco-coverage)
[![Download](https://api.bintray.com/packages/palantir/releases/gradle-jacoco-coverage/images/download.svg) ](https://bintray.com/palantir/releases/gradle-jacoco-coverage/_latestVersion)

**Note: This plugin is considered obsolete as of Gradle 3.4 which supports [coverage enforcement as part of the core JaCoCo plugin](https://docs.gradle.org/3.4/release-notes.html).**

# Jacoco Coverage Gradle Plugin

Jacoco Coverage is a Gradle Plugin that provides two tasks extending the standard Gradle Jacoco plugin:
- Firstly, the `com.palantir.jacoco-coverage` plugin allows Gradle build scripts to configure minimum Java Code Coverage
thresholds for projects, packages, classes, and files.
- Secondly, the `com.palantir.jacoco-full-report` plugin adds a task that produces a Jacoco report for the combined code
coverage of the tests of all subprojects of the current project.

Release and change logs: [CHANGELOG.md](CHANGELOG.md)

Source code: [https://github.com/palantir/gradle-jacoco-coverage](https://github.com/palantir/gradle-jacoco-coverage)


## com.palantir.jacoco-coverage

### Quick start

Add the following configuration to `build.gradle`:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.palantir:jacoco-coverage:<version>'
    }
}

apply plugin: 'java'
apply plugin: 'com.palantir.jacoco-coverage'
jacocoCoverage {
    // Enforce minimum code coverage of 50% for every Java file.
    fileThreshold 0.5

    // Whitelist files named MyClass.java from coverage requirements.
    fileThreshold 0.0, "MyClass.java"
}
```

Subsequent `./gradlew build` runs will fail if the configured minimum coverage thresholds are not achieved by the
project's tests. (Note that `build` depends on `check` which depends on the `jacocoCoverage` task added by this plugin.)


### Configuration

Code coverage requirements are specified separately for the different "realms" reported by Jacoco:
- average coverage in a report, specified by the `reportThreshold` keyword
- average coverage in a package, specified by the `packageThreshold` keyword
- coverage in a class, specified by the `classThreshold` keyword
- coverage in a file, specified by the `fileThreshold` keyword

Each realm contains a number of so-called "scopes": 
- The file realm contains one scope for every source file, named by file name, e.g., "MyClass.java". Note that file
scopes for files with the same name in different packages or folders clash; in such cases coverage thresholds should be
specified in the class realm.
- The class realm contains one scope for every (top-level, inner, or anonymous) class, named by fully qualified class
name, e.g., "org/package/MyClass$1".
- The package realm contains one scope for every package, named by the package name, e.g., "org/package".
- The report realm contains exactly one scope which is usually derived from the name of the corresponding Gradle (sub-)
project.

Scopes can be specified by exact name, e.g., `"MyClass.java"`, or by a Groovy regex pattern matching an arbitrary number
of scopes, e.g., `~'org/package.*'` for all sub-packages of "org/mypackage".

#### Configuration syntax

Coverage requirements are written in terms of a realm, a scope specification, and one (or several) of the Jacoco-defined
types of coverage, e.g., lines covered or branches covered. The following syntax variations are supported:

    <realm> <threshold>
    <realm> <threshold>, <coverage type>
    <realm> <threshold>, <scope name>
    <realm> <threshold>, <coverage type>, <scope name>
    <realm> <threshold>, <scope pattern>
    <realm> <threshold>, <coverage type>, <scope pattern>

, where:
- `<realm>` is one of `fileThreshold`, `classThreshold`, `packageThreshold`, `reportThreshold`
- `<threshold>` is a Groovy `double`
- `<coverage type>` is one of `BRANCH`, `CLASS`, `COMPLEXITY`, `INSTRUCTION`, `LINE`, `METHOD`
- `<scope name>` is a Groovy string
- `<scope pattern>` is a Groovy regex pattern

Examples:
```groovy
jacocoCoverage {
    // Minimum code coverage of 50% for all scopes in the file realm (i.e., for all files) and for all coverage types.
    fileThreshold 0.5

    // Minimum 'branch' coverage of 30% for all files.
    fileThreshold 0.3, BRANCH

    // Minimum 'line' coverage of 10% for files (in any directory) whose name matches the given regular expression.
    fileThreshold 0.1, LINE, ~"(Indentation|Wrapping)\\.java"

    // Minimum 'line' coverage of 10% for files named "Indentation.java" (case-sensitive, in any directory).
    // (Note: This syntax uses exact string match against the file name while the regex syntax requires escaping.)
    fileThreshold 0.1, LINE, "Indentation.java"

    // Minimum coverage of 30% in the given class.
    classThreshold 0.3, "org/company/module/MyClass"

    // Minimum average coverage of 30% in given package.
    packageThreshold 0.3, "org/company/module"

    // Minimum average coverage of 50% in report "my-project; the report name is usually the Gradle project name.
    reportThreshold 0.5, "my-project" 

    // Scopes can be exempt from all coverage requirements by exact scope name or scope name pattern.
    fileThreshold 0.0, "MyClass.java"
    packageThreshold 0.0, "org/company/module"
    fileThreshold 0.0, ~".*Test.java"
}
```

#### Configuration semantics

Given the observed code coverage for a realm, scope, and coverage type (e.g., _line_ coverage 47% for class
`org/product/module/MyClass`), the *lowest* specified threshold matching the scope and coverage type is enforced. For
example, the specification

```groovy
jacocoCoverage {
    classThreshold 0.5, BRANCH
    classThreshold 0.3, "org/company/module/MyClass"
}
```

will enforce 50% branch coverage for every class, and 30% coverage for class `org/company/module/MyClass` for all
coverage types. In particular, branch coverage for `org/company/module/MyClass` is required to be at least 30% rather
than 50% since the lowest specified threshold dominates. This implies that specific coverage types and scopes can be
excluded from any thresholds globally, for example:

```groovy
jacocoCoverage {
    packageThreshold 0.0, BRANCH // Never enforce branch coverage at the package level
    fileThreshold 0.0, ~".*Util\\.java" // Never enforce coverage on Java files like XyzUtil.java
    packageThreshold 0.0, ~"org/thirdparty/.*" // Never enforce coverage on thirdparty package.
}
```


## com.palantir.jacoco-full-report

### Quickstart

Add the following configuration to the `build.gradle` configuration of the root project:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.palantir:jacoco-coverage:<version>'
    }
}

apply plugin: 'com.palantir.jacoco-full-report'  // Automatically applies the 'jacoco' plugin to this project.
```

Subsequent `./gradlew test jacocoFullReport` runs will generate a test report in `build/reports/jacoco/jacocoFullReport/`
that evaluates the coverage yielded by all subprojects combined. (Note that generally Jacoco reports are only generated
if the `test` task has run previously in order to generated the Jacoco execution data.)

### Configuration

The `jacocoFullReport` task is a standard `JacocoReport` task and has the same configuration options as the vanilla
[Gradle Jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html) task.

By default, Jacoco report tasks of all projects and subprojects are considered when compiling the full report. Projects
can be excluded from consideration through the `jacocoFull` extension:

```groovy
jacocoFull {
    excludeProject ":my-sub-project", ":buildSrc"
}
```

Note that `com.palantir.jacoco-full-report` and `com.palantir.jacoco-coverage` can be combined in order to enforce
coverage requirements over the combined coverage of several subprojects.
