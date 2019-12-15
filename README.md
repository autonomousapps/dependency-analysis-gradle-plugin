# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.
1. Compute the ABI of a project, and recommend which dependencies should be on the api configuration.

# Compatibilities
Android Gradle Plugin: tested to work with AGP 3.5.x and 3.6.0-beta03 (`com.android.library` and `com.android.application` projects only).
Kotlin plugin: tested with Kotlin 1.3.x (specifically 1.3.5x-6x).
Java Library Plugin: tested with the java-library plugin bundled with Gradle 5.6 and 6.0.
Gradle: this plugin is built with Gradle 5.6.4 and is only guaranteed compatible with projects built with Gradle 5.6.4 or above.
It works with Java, Kotlin, and Kapt.

# How to use
1. Add to your project like any other Gradle plugin.
See https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis for instructions.
If you want to add it to all subprojects in your build, do this:

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
1. Run a task. E.g., `./gradlew my-project:misusedDependenciesDebug`.
Replace `Debug` with the variant you're interested in. 
1. For `java-library` projects, the task variants are based on sourceSets, so the gradle invocation would be `./gradlew :my-java-lib-project:misusedDependenciesMain` (for the `main` source set).
1. For ABI analysis, run instead `./gradlew my-project:abiAnalysisDebug` or `./gradlew my-java-lib-project:abiAnalysisMain`.

The result of this will be three files in the `my-project/build/dependency-analysis/debug` directory:
1. unused-direct-dependencies.txt
2. used-transitive-dependencies.txt
3. misused-dependencies.html (this combines the first two in a very ugly HTML report)

And, for the ABI analysis task,
4. abi.txt. This simply lists the dependencies that should be `api`
5. abi-dump.txt. This is a richer format that fully describes your project's binary API.

The names, of course, relate to the use-cases described above.

# TODO
1. Add plugin extension for user configuration.
Particularly, specify a list of variants to analyze (maybe)
1. Extend dependency analysis to non-main source sets
1. Add lifecycle tasks that aggregate across variants
1. Add root-project task that aggregates all subproject reports
