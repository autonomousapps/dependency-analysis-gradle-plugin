<a alt="Latest version" href="https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis">
<img src="https://img.shields.io/maven-metadata/v.svg?label=plugin%20version&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fautonomousapps%2Fdependency-analysis%2Fcom.autonomousapps.dependency-analysis.gradle.plugin%2Fmaven-metadata.xml"/></a>

<a alt="Build Status" href="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin">
<img src="https://travis-ci.org/autonomousapps/dependency-analysis-android-gradle-plugin.svg?branch=master"/></a>

# Use cases
1. Produces an "advice" report which indicates:
    - Unused dependencies which should be removed.
    - Declared dependencies which are on the wrong configuration (api vs implementation)
    - Transitively used dependencies which ought to be declared directly, and on which configuration.
    - [Experimental] Dependencies which could be declared on the `compileOnly` configuration, as they're not required at runtime.
      This new features uses a heuristic for finding `compileOnly` candidates.
      Please see the KDoc on the `AnalyzedJar` task for details. 
    
This is printed to console in a narrative form, and also written to disk as JSON.
The JSON output has three components (see the `Advice` model class):
1. Dependency (identifier + resolved version)
1. "fromConfiguration", which is the configuration on which the dependency is currently declared.
Typically "api" or "implementation".
If this field is not present, that means it is null and the dependency is transitive.
It should be declared directly.
1. "toConfiguration", which is the configuration on which the dependency _should_ be declared.
If this field is not present, that means it is null and the dependency should be _removed_.  

# Compatibilities
1. Android Gradle Plugin: this plugin is built with AGP 3.6.1. It is tested to work with AGP 3.5.3, 3.6.1, 4.0.0-beta03, and 4.1.0-alpha04 (`com.android.library` and `com.android.application` projects only).
1. Kotlin plugin: tested with Kotlin 1.3.x (specifically 1.3.5x-7x).
1. Java Library Plugin: tested with the java-library plugin bundled with Gradle 5.6.4, 6.0.1, 6.1.1, 6.2.1, and 6.3.
1. Gradle: this plugin is built with Gradle 6.3. It is tested against Gradle 5.6.4, 6.0.1, 6.1.1, 6.2.1, and 6.3.
1. It works with Java, Kotlin, and Kapt. Both multi-module JVM and Android projects.

# Limitations
Given a multi-project build with two subprojects, A and B, and A depends on B (A --> B), the plugin will emit a false positive indicating B is unused in A (inaccurately) in the following scenario:
1. Where A only uses Android `R` references from B and those references _are not namespaced_ (you do _not_ have `android.namespacedRClass=true` in your `gradle.properties` file).

This limitation may eventually be lifted.

# How to use
The plugin is available from both the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis) and [Maven Central](https://search.maven.org/search?q=com.autonomousapps).

Add to your root project.

    plugins {
        id("com.autonomousapps.dependency-analysis") version "${latest_version}"
    }

If you prefer to resolve from Maven Central, you can add the following to your `settings.gradle`

    pluginManagement {
        repositories {
            // releases
            mavenCentral()
            // snapshots
            maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        }
    }
    
Or, if you prefer not to use the `plugins {}` syntax, you can use the legacy approach:

    buildscript {
        repositories {
            // available by default
            gradlePluginPortal()
            // releases
            mavenCentral()
            // snapshots
            maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        }
        dependencies {
            classpath "com.autonomousapps:dependency-analysis-gradle-plugin:${latest_version}"
        }
    }
    
    apply plugin: "com.autonomousapps.dependency-analysis"

## Aggregate tasks
There will be a task on the root project with the name `buildHealth`.
Running that task will execute all tasks in all projects, and then produce the advice report, aggregated across all subprojects.
The path to this report will be printed to the console.

## Customizing behavior
The plugin provides a `dependencyAnalysis {}` extension (`com.autonomousapps.DependencyAnalysisExtension`) for configuration.

### Customizing variants to analyze
If your Android project uses flavors or custom build types, you may wish to change the default variant that is analyzed.
By default, this plugin will analyze the `debug` variant for Android, and the `main` source set for Java.
To customize this, add the following to your root `build.gradle[.kts]`

    dependencyAnalysis {
      setVariants("my", "custom", "variants")
    }

If the plugin cannot find any variants by these names, it will first fallback to the defaults ("debug" and "main"), and then simply ignore the given subproject.

### Failure conditions
By default, the plugin's tasks will not fail a build upon detection of dependency issues; they simply print results to console and to disk.
If you would prefer your build to fail if there are issues, you can configure the plugin as follows:

    dependencyAnalysis {
      issues {
        // Default for all issue types is "warn"
        // Can set behavior for all issue types
        onAny { 
          fail() // or...
          warn() // or...
          ignore() 
        }
        // Or configure behavior per-type
        onUnusedDependencies { ... }
        onUsedTransitiveDependencies { ... }
        onIncorrectConfiguration { ... }
      }
    }
    
It is also possible to tell the plugin to ignore any issue relating to specified dependencies.
Both the `fail()` and `warn()` except a String varargs or `Iterable<String>`. For example:

    dependencyAnalysis {
      issues {
        onUnusedDependencies {
          fail("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "androidx.core:core-ktx")
        }
      }
    }
    
Please note that the `ignore()` method takes no argument, as it already tells the plugin to ignore everything.

If your build fails, the plugin will print the reason why to console, along with the path to the report.
Please see [Use cases](#use-cases), above, for help on understanding the report.

### Control on which projects plugin is applied
On very large projects, the plugin's default behavior of auto-applying itself to all subprojects can have major performance impacts.
To mitigate this, the plugin can be configured so that it must be _manually_ applied to each subproject of interest.

    dependencyAnalysis {
        autoApply(false) // default is true
    }

## Per-project tasks
You can also run some tasks on individual projects.

For the advice report,
1. Run the task `./gradlew my-project:adviceDebug`, where "Debug" is the variant you're interested in.
This will be "Main" for java-library projects (where the variant is based on source set name).
It will produce advice reports in the `build/reports/dependency-analysis/<variant>/` directory.

At this time, that is the only recommended task for end-users.
If you are interested in the other tasks, please run `./gradlew tasks --group dependency-analysis` or `./gradlew my-project:tasks --group dependency-analysis`  

# Flowchart
This flowchart was built with [Mermaid](https://github.com/mermaid-js/mermaid) and is experimental.
It's an attempt to provide some high-level documentation additional reference.
<!-- Please see https://github.com/mermaidjs/mermaid-live-editor/issues/23#issuecomment-520662873 for advice. -->

![Flowchart](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/blob/master/flowchart/flowchart.png)
<!--![Flowchart](https://mermaid.ink/img/eyJjb2RlIjoiZ3JhcGggVERcblVzZXIoVXNlcikgLS0-fFdhbnRzIHRvIHVuZGVyc3RhbmQgZGVwZW5kZW5jeSBpc3N1ZXN8IEFcblxuc3ViZ3JhcGggcm9vdFxuQVtBcHBseSBwbHVnaW4gdG8gcm9vdCBwcm9qZWN0XSAtLT4gUVtDb25maWd1cmUgcm9vdCBwcm9qZWN0XVxuUSAtLT58Y3JlYXRlIGV4dGVuc2lvbnwgQltkZXBlbmRlbmN5QW5hbHlzaXMgZXh0ZW5zaW9uXVxuUSAtLT4gUFtcImFkZCBsaWZlY3ljbGUgdGFza3MgKGJ1aWxkSGVhbHRoKVwiXVxuZW5kXG5cbnN1YmdyYXBoIHN1YnByb2plY3RzXG5BW0FwcGx5IHBsdWdpbiB0byByb290IHByb2plY3RdIC0tPiBEW2FwcGx5IHBsdWdpbiB0byBlYWNoIHN1YnByb2plY3RdXG5EIC0tPnxjb25maWd1cmUgYW5kcm9pZCBhcHAgcHJvamVjdHN8IEVbY29tLmFuZHJvaWQuYXBwbGljYXRpb25dXG5EIC0tPnxjb25maWd1cmUgYW5kcm9pZCBsaWJyYXJ5IHByb2plY3RzfCBGW2NvbS5hbmRyb2lkLmxpYnJhcnldXG5EIC0tPnxjb25maWd1cmUgamF2YSBsaWJyYXJ5IHByb2plY3RzfCBHW2phdmEtbGlicmFyeV1cbmVuZFxuXG5zdWJncmFwaCBwcm9qZWN0XG5FIC0tPnxwZXIgdmFyaWFudHwgSFthbmFseXplIGRlcGVuZGVuY2llc11cblxuRiAtLT58cGVyIHZhcmlhbnR8IEhcbkcgLS0-fHBlciBzb3VyY2Ugc2V0fCBIXG5IIC0tPnxhcnRpZmFjdHNSZXBvcnRUYXNrfCBJW1wicmVwb3J0OiBhbGwgZGVwZW5kZW5jaWVzLCBpbmNsdWRpbmcgdHJhbnNpdGl2ZXMsIHdpdGggYXJ0aWZhY3RzXCJdXG5JIC0tPnxkZXBlbmRlbmN5UmVwb3J0VGFza3wgSltcImFzc29jaWF0ZSBhbGwgZGVwZW5kZW5jaWVzIHdpdGggdGhlaXIgZGVjbGFyZWQgY2xhc3Nlc1wiXVxuSSAtLT58aW5saW5lVGFza3wgS1tcInJlcG9ydDogYWxsIHVzZWQgS290bGluIGlubGluZSBtZW1iZXJzXCJdXG5IIC0tPnxhbmFseXplQ2xhc3Nlc1Rhc2t8IExbcmVwb3J0OiBhbGwgY2xhc3NlcyB1c2VkIGJ5IHByb2plY3RdXG5KIC0tPnxhYmlBbmFseXNpc1Rhc2t8IE5bcmVwb3J0OiBBQkldXG5KIC0tPnxtaXN1c2VkRGVwZW5kZW5jaWVzVGFza3wgTVtyZXBvcnQ6IG1pc3VzZWQgZGVwZW5kZW5jaWVzXVxuTCAtLT58bWlzdXNlZERlcGVuZGVuY2llc1Rhc2t8IE1cbksgLS0-fG1pc3VzZWREZXBlbmRlbmNpZXNUYXNrfCBNXG5lbmRcblxuc3ViZ3JhcGggXCJsaWZlY3ljbGUgdGFza3NcIlxuTiAtLT58bWF5YmVBZGRBcnRpZmFjdHwgU3thZGQgYXJ0aWZhY3Qgb25jZX1cbk0gLS0-fG1heWJlQWRkQXJ0aWZhY3R8IFNcblMgLS0-fGFkZCByZXBvcnQgdG98IGFiaVJlcG9ydChjb25mOiBhYmlSZXBvcnQpXG5TIC0tPnxhZGQgcmVwb3J0IHRvfCBkZXBSZXBvcnQoY29uZjogZGVwZW5kZW5jeVJlcG9ydClcblAgLS0-fGNvbnN1bWV8IGFiaVJlcG9ydFxuUCAtLT58Y29uc3VtZXwgZGVwUmVwb3J0XG5lbmRcblxuXG5cblxuIiwibWVybWFpZCI6eyJ0aGVtZSI6ImRlZmF1bHQifX0)-->
