<a alt="Latest version" href="https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis">
<img src="https://img.shields.io/maven-metadata/v.svg?label=plugin%20version&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fautonomousapps%2Fdependency-analysis%2Fcom.autonomousapps.dependency-analysis.gradle.plugin%2Fmaven-metadata.xml"/></a>

<a alt="Build Status" href="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin">
<img src="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin.svg?branch=master"/></a>


# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.
1. Compute the ABI of a project, and recommend which dependencies should be on the api configuration.

# Compatibilities
1. Android Gradle Plugin: tested to work with AGP 3.5.3, 3.6.0-rc01, and 4.0.0-alpha07 (`com.android.library` and `com.android.application` projects only).
1. Kotlin plugin: tested with Kotlin 1.3.x (specifically 1.3.5x-6x).
1. Java Library Plugin: tested with the java-library plugin bundled with Gradle 5.6.4 and 6.0.1.
1. Gradle: this plugin is built with Gradle 5.6.4 and is only guaranteed compatible with projects built with Gradle 5.6.4 or above.
1. It works with Java, Kotlin, and Kapt.

# How to use
Add to your root project.
See https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis for instructions.

    plugins {
        id("com.autonomousapps.dependency-analysis") version "${latest_version}"
    }

## Aggregate tasks
There will be a task on the root project with the name `buildHealth`.
Running that task will execute all tasks in all projects, and then produce a pair of aggregated reports.
The path to these reports will be printed to the console.

## Per-project tasks
You can also run some tasks on individual projects.

1. Run a task. E.g., `./gradlew my-project:misusedDependenciesDebug`.
Replace `Debug` with the variant you're interested in. 
2. For `java-library` projects, the task variants are based on sourceSets, so the gradle invocation would be `./gradlew :my-java-lib-project:misusedDependenciesMain` (for the `main` source set).
3. For ABI analysis, run instead `./gradlew my-project:abiAnalysisDebug` or `./gradlew my-java-lib-project:abiAnalysisMain`.
(Please note, there is no ABI analysis task for a `com.android.application` project, since that would be meaningless.)

The result of this will be three files in the `my-project/build/dependency-analysis/debug` directory:
1. unused-direct-dependencies.json
2. used-transitive-dependencies.json
3. misused-dependencies.html (this combines the first two in a very ugly HTML report)
4. If you want to run this report across all subprojects in your project, follow the advice above under 1., and also apply this plugin to your root project.
This will add a task, `:misusedDependenciesReport`, which will run the `misusedDependencies[Debug|Main]` tasks in your subprojects, and then aggregate them into a single report.
Future releases will enable users to specify which tasks to run in subprojects.

And, for the ABI analysis task,

4. abi.json. This simply lists the dependencies that should be `api`
5. abi-dump.txt. This is a richer format that fully describes your project's binary API.
6. If you want to run this report across all subprojects in your project, follow the advice above under 1., and also apply this plugin to your root project.
   This will add a task, `:abiReport`, which will run the `abiAnalysis[Debug|Main]` tasks in your subprojects, and then aggregate them into a single report.
   Future releases will enable users to specify which tasks to run in subprojects.

# Customizing variants to analyze
If your Android project uses flavors or custom build types, you may wish to change the default variant that is analyzed.
By default, this plugin will analyze the `debug` variant for Android, and the `main` source set for Java.
To customize this, add the following to your root `build.gradle[.kts]`

    dependencyAnalysis {
      setVariants("my", "custom", "variants")
    }

If the plugin cannot find any variants by these names, it will first fallback to the defaults ("debug" and "main"), and then simply ignore the given subproject.

# TODO
1. Add root-project task that aggregates all subproject reports (done, except for HTML reports)
1. Add option to fail build based on various conditions.
