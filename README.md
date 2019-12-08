# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.

# Compatibilities
This plugin has been tested to work with AGP 3.5.x and 3.6.0-beta03 (`com.android.library` and `com.android.application` projects).
It works with Java, Kotlin, and Kapt.

# How to use
1. Add to your project like any other Gradle plugin.
See https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis for instructions.
If you want to add it to all subprojects in your build, do this:
1. This plugin is built with Gradle 6 and may only be compatible with projects that are also built with Gradle 6.

    ```
    // root build.gradle[.kts]
    buildscript {
      dependencies {
        // Add this
        classpath "gradle.plugin.com.autonomousapps:dependency-analysis-gradle-plugin:${latest_version}"
      }
    }
    subprojects {
        apply plugin: "com.autonomousapps.dependency-analysis"
    }
    ```
1. Run the task. E.g., `./gradlew :my-project:misusedDependenciesDebug`.
Replace `Debug` with the variant you're interested in. 
1. For `java-library` projects, the task variants are based on sourceSets, so the gradle invocation would be `./gradlew :my-java-lib-project:misusedDependenciesMain` (for the `main` source set).

The result of this will be two files in the `my-project/build/dependency-analysis/debug` directory:
1. unused-direct-dependencies.txt
1. used-transitive-dependencies.txt

The names, of course, relate to the use-cases described above.

# TODO
1. ABI analysis (look at return types and parameters of public methods)
1. Add plugin extension for user configuration.
Particularly, specify a list of variants to analyze (maybe)
1. Extend dependency analysis to non-main source sets
1. Add lifecycle tasks that aggregate across variants
1. Add root-project task that aggregates all subproject reports
