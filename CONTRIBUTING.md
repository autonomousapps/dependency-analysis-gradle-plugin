This file is meant to help contributors working on the project.

# Building dependency-analysis-android-gradle-plugin
```
./gradlew clean build
```

# Running tests 

## Unit Tests
```
./gradlew test
```

## Quick Functional Tests
```
./gradlew functionalTest -DfuncTest.quick
```

## Functional Tests
```
./gradlew functionalTest
```

# Publishing a snapshot to the local maven repo
```
./gradlew publishToMavenLocal -x signDependencyAnalysisPluginPluginMarkerMavenPublication -x signPluginPublication
```
