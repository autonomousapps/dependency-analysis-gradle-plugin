# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.
1. Compute the ABI of a project, and recommend which dependencies should be on the api configuration.

# Compatibilities
1. Android Gradle Plugin: tested to work with AGP 3.5.x and 3.6.0-beta03 (`com.android.library` and `com.android.application` projects only).
1. Kotlin plugin: tested with Kotlin 1.3.x (specifically 1.3.5x-6x).
1. Java Library Plugin: tested with the java-library plugin bundled with Gradle 5.6 and 6.0.
1. Gradle: this plugin is built with Gradle 5.6.4 and is only guaranteed compatible with projects built with Gradle 5.6.4 or above.
1. It works with Java, Kotlin, and Kapt.

# How to use
1. Add to your project like any other Gradle plugin.
See https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis for instructions.

If you want to add it to all subprojects in your build, you could do this:

    ```
    // root build.gradle[.kts]
    buildscript {
      dependencies {
        // Add this
        classpath "gradle.plugin.com.autonomousapps:dependency-analysis-gradle-plugin:${latest_version}"
      }
    }
    // Add or edit this
    allprojects {
        apply plugin: "com.autonomousapps.dependency-analysis"
    }
    ```

## Aggregate tasks
If you've added it to all your projects (as suggested above), then there is a task on the root project, `buildHealth`.
Running that task will execute all tasks in all projects, and then produce a pair of aggregated reports.
The path to these reports will be printed to the console.

## Per-project tasks
If you haven't added the plugin to all projects, but just a single one, follow these instructions.

1. Run a task. E.g., `./gradlew my-project:misusedDependenciesDebug`.
Replace `Debug` with the variant you're interested in. 
2. For `java-library` projects, the task variants are based on sourceSets, so the gradle invocation would be `./gradlew :my-java-lib-project:misusedDependenciesMain` (for the `main` source set).
3. For ABI analysis, run instead `./gradlew my-project:abiAnalysisDebug` or `./gradlew my-java-lib-project:abiAnalysisMain`.
(Please note, there is no ABI analysis task for a `com.android.application` project, since that would be meaningless.)

The result of this will be three files in the `my-project/build/dependency-analysis/debug` directory:
1. unused-direct-dependencies.txt
2. used-transitive-dependencies.txt
3. misused-dependencies.html (this combines the first two in a very ugly HTML report)
4. If you want to run this report across all subprojects in your project, follow the advice above under 1., and also apply this plugin to your root project.
This will add a task, `:misusedDependenciesReport`, which will run the `misusedDependencies[Debug|Main]` tasks in your subprojects, and then aggregate them into a single report.
Future releases will enable users to specify which tasks to run in subprojects.

And, for the ABI analysis task,

4. abi.txt. This simply lists the dependencies that should be `api`
5. abi-dump.txt. This is a richer format that fully describes your project's binary API.
6. If you want to run this report across all subprojects in your project, follow the advice above under 1., and also apply this plugin to your root project.
   This will add a task, `:abiReport`, which will run the `abiAnalysis[Debug|Main]` tasks in your subprojects, and then aggregate them into a single report.
   Future releases will enable users to specify which tasks to run in subprojects.

# Customizing variants to analyze
If your Android project uses flavors or custom build types, you may wish to change the default variant that is analyzed.
By default, this plugin will analyze the `debug` variant for Android, and the `main` source set for Java.
To customize this, add the following to your build script

```
dependencyAnalysis {
  setVariants("my", "custom", "variants")
}
```
Do this in the individual subproject that has non-standard variants.
If the plugin cannot find any variants by these names, it will first fallback to the defaults ("debug" and "main"), and then simply ignore that subproject.
A future release is planned that will simplify this, by allowing you to set a single extension in the root project.  

# TODO
1. Add root-project task that aggregates all subproject reports (done, except for HTML reports)
1. Add option to fail build based on various conditions.
