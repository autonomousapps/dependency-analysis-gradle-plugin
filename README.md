# Use cases
1. Produce a report of unused direct dependencies.
1. Produce a report of used transitive dependencies.

# How to use
Since this is spike code, I haven't published it yet. If you want to give it a try, do the following:
1. Clone this repo locally.
1. Open a project you want to test it on.
1. Open the `settings.gradle[.kts]` file and add this:
```
includeBuild("path/to/dependency-analysis-plugin")
```
4. Open the subproject/module you want to add this plugin to and add this:
```
// my-project/build.gradle[.kts]
plugins {
    id("com.autonomousapps.dependency-analysis")
}
```
nb: this will _not_ work with the old form of plugin application.
Specifically, `apply plugin: "com.autonomousapps.dependency-analysis"` will fail.

5. Run a task of interest. E.g., `./gradlew :my-project:misusedDependenciesDebug`. 
Replace `Debug` with the variant you're interested in. 

The result of this will be two files in the `my-project/build/dependency-analysis/debug` directory:
1. unused-direct-dependencies.txt
1. used-transitive-dependencies.txt

The names, of course, relate to the use-cases described above.

# TODO
1. ABI analysis (look at return types and parameters of public methods)
1. Add plugin extension for user configuration.
Particularly, specify a list of variants to analyze (maybe)
1. Extend functionality to vanilla (non-Android) Java/Kotlin projects.