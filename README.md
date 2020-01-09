<a alt="Latest version" href="https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis">
<img src="https://img.shields.io/maven-metadata/v.svg?label=gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fautonomousapps%2Fdependency-analysis%2Fcom.autonomousapps.dependency-analysis.gradle.plugin%2Fmaven-metadata.xml"/></a>

<a alt="Build Status" href="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin">
<img src="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin.svg?branch=master"/></a>


# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.
1. Compute the ABI of a project, and recommend which dependencies should be on the api configuration.

# Compatibilities
1. Android Gradle Plugin: tested to work with AGP 3.5.3 and 3.6.0-rc01  (`com.android.library` and `com.android.application` projects only).
   1. 4.0.0-alpha08 tests currently are not passing.
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

# Flowchart
This flowchart was built with [Mermaid](https://github.com/mermaid-js/mermaid) and is experimental. It's an attempt to provide some high-level documentation for potential contributors.
<!-- Please see https://github.com/mermaidjs/mermaid-live-editor/issues/23#issuecomment-520662873 for advice. -->

![Flowchart](https://mermaid.ink/img/eyJjb2RlIjoiZ3JhcGggVERcblVzZXIoVXNlcikgLS0-fFdhbnRzIHRvIHVuZGVyc3RhbmQgZGVwZW5kZW5jeSBpc3N1ZXN8IEFcblxuc3ViZ3JhcGggcm9vdFxuQVtBcHBseSBwbHVnaW4gdG8gcm9vdCBwcm9qZWN0XSAtLT4gUVtDb25maWd1cmUgcm9vdCBwcm9qZWN0XVxuUSAtLT58Y3JlYXRlIGV4dGVuc2lvbnwgQltkZXBlbmRlbmN5QW5hbHlzaXMgZXh0ZW5zaW9uXVxuUSAtLT4gUFtcImFkZCBsaWZlY3ljbGUgdGFza3MgKGJ1aWxkSGVhbHRoKVwiXVxuZW5kXG5cbnN1YmdyYXBoIHN1YnByb2plY3RzXG5BW0FwcGx5IHBsdWdpbiB0byByb290IHByb2plY3RdIC0tPiBEW2FwcGx5IHBsdWdpbiB0byBlYWNoIHN1YnByb2plY3RdXG5EIC0tPnxjb25maWd1cmUgYW5kcm9pZCBhcHAgcHJvamVjdHN8IEVbY29tLmFuZHJvaWQuYXBwbGljYXRpb25dXG5EIC0tPnxjb25maWd1cmUgYW5kcm9pZCBsaWJyYXJ5IHByb2plY3RzfCBGW2NvbS5hbmRyb2lkLmxpYnJhcnldXG5EIC0tPnxjb25maWd1cmUgamF2YSBsaWJyYXJ5IHByb2plY3RzfCBHW2phdmEtbGlicmFyeV1cbmVuZFxuXG5zdWJncmFwaCBwcm9qZWN0XG5FIC0tPnxwZXIgdmFyaWFudHwgSFthbmFseXplIGRlcGVuZGVuY2llc11cblxuRiAtLT58cGVyIHZhcmlhbnR8IEhcbkcgLS0-fHBlciBzb3VyY2Ugc2V0fCBIXG5IIC0tPnxhcnRpZmFjdHNSZXBvcnRUYXNrfCBJW1wicmVwb3J0OiBhbGwgZGVwZW5kZW5jaWVzLCBpbmNsdWRpbmcgdHJhbnNpdGl2ZXMsIHdpdGggYXJ0aWZhY3RzXCJdXG5JIC0tPnxkZXBlbmRlbmN5UmVwb3J0VGFza3wgSltcImFzc29jaWF0ZSBhbGwgZGVwZW5kZW5jaWVzIHdpdGggdGhlaXIgZGVjbGFyZWQgY2xhc3Nlc1wiXVxuSSAtLT58aW5saW5lVGFza3wgS1tcInJlcG9ydDogYWxsIHVzZWQgS290bGluIGlubGluZSBtZW1iZXJzXCJdXG5IIC0tPnxhbmFseXplQ2xhc3Nlc1Rhc2t8IExbcmVwb3J0OiBhbGwgY2xhc3NlcyB1c2VkIGJ5IHByb2plY3RdXG5KIC0tPnxhYmlBbmFseXNpc1Rhc2t8IE5bcmVwb3J0OiBBQkldXG5KIC0tPnxtaXN1c2VkRGVwZW5kZW5jaWVzVGFza3wgTVtyZXBvcnQ6IG1pc3VzZWQgZGVwZW5kZW5jaWVzXVxuTCAtLT58bWlzdXNlZERlcGVuZGVuY2llc1Rhc2t8IE1cbksgLS0-fG1pc3VzZWREZXBlbmRlbmNpZXNUYXNrfCBNXG5lbmRcblxuc3ViZ3JhcGggXCJsaWZlY3ljbGUgdGFza3NcIlxuTiAtLT58bWF5YmVBZGRBcnRpZmFjdHwgU3thZGQgYXJ0aWZhY3Qgb25jZX1cbk0gLS0-fG1heWJlQWRkQXJ0aWZhY3R8IFNcblMgLS0-fGFkZCByZXBvcnQgdG98IGFiaVJlcG9ydChjb25mOiBhYmlSZXBvcnQpXG5TIC0tPnxhZGQgcmVwb3J0IHRvfCBkZXBSZXBvcnQoY29uZjogZGVwZW5kZW5jeVJlcG9ydClcblAgLS0-fGNvbnN1bWV8IGFiaVJlcG9ydFxuUCAtLT58Y29uc3VtZXwgZGVwUmVwb3J0XG5lbmRcblxuXG5cblxuIiwibWVybWFpZCI6eyJ0aGVtZSI6ImRlZmF1bHQifX0)
