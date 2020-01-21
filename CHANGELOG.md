Dependency Analysis Plugin Changelog

# TBD 
* [Fixed] Plugin doesn't detect namespaced Android resource usage.
(nb: still does not detect non-namespaced res usage.)
* [Fixed] Plugin now works with AGP 4.0.0-alpha09.

# Version 0.15.0
* [Fixed] Plugin now works with AGP 4.0.0-alpha08.

# Version 0.14.0
* [New] Reports now indicate on which configuration a dependency was declared, if it were.
* [Fixed] Kotlin stdlib is never a candidate for unused direct dependency.
* Significantly more tests, alongside some refactoring.

# Version 0.13.1
* [Fixed] Kotlin stdlib never appears in list of used transitive dependencies.

# Version 0.13.0
* Added support for detecting use of Kotlin inline members.

# Version 0.12.1 
* Reverted modularization of models & utils.

# Version 0.12.0
* ([12](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/12)) Generate outputs into buildDir/reports directory
* Normalize annotation of input AbiAnalysisTask
* ([11](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/11)) add version in transitive dependencies
* More robustness in Functional tests
* Extract models and utils to separate Gradle project ("module")

# Version 0.11.1
* Add resolved versions to all reports
* Clean up of Runner class
* Matrix of Functional tests for different AGP and Gradle versions
* Target Kotlin at Java 8
* Add test for ABI dependency analysis
* Use Java 8
* Add unit tests to some components
* Improved logging
* Refactor of jar/ClassList analysis tasks
* Refactor Gradle Runner

# Version 0.11.0

# Version 0.10.0
* Plugin should now be applied only to root project.

# Version 0.9.0
* Add extension for Aggregate BuildHealth Task
* Improved bytecode analysis
* Switched from aggregating tasks to aggregating configurations.
* ([6](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/6)) Changed file extension on some outputs to .json.
* ([7](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/7)) Use reified types
* Added abstract base class for android-project analysis
* Introduction of perpetual semantic versioning.

# Version 0.8
* Improve analysis of pure Java projects
* Apache 2.0 license
* Improved misused-dependencies report.
* Fixed an issue that impacted IDE sync.
* ABI analysis merged
* AGP 3.5.3

# Version 0.7.1

# Version 0.7

# Version 0.6.1

# Version 0.6

# Version 0.5
* Reports in HTML
* Improved cacheability of tasks
* AGP & KGP versions bump 3.5.2 & 1.3.61

# Version 0.4
* Gradle 5 and shaded ASM dependency
* Blacklist kotlin-stdlib artifacts
* ASM 7
* kapt support
* [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) projects support
* Better byte code analysis

# Version 0.3

# Version 0.2
* Analysis of XML layouts
* Gradle 6
* Function testing introduced (but doesn't work yet)
* Simplified ClassListAnalysisTask configuration.

# Version 0.1
October 23rd, 2019. Project starts.
