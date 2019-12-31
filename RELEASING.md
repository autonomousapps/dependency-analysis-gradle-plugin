Release procedure for dependency-analysis-android-gradle-plugin

1. Update the changelog
1. Bump version number to next stable version (use semantic versioning: x.y.z)
1. Update README if needed
1. git commit -am "Prepare for release x.y.z." 
1. git tag -a vx.y.z -m "Version x.y.z"
1. Execute `./gradlew publishPlugins` to release to the [Plugin Portal](https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis)
This will also run the tests.
1. Update `build.gradle.kts` to next snapshot version (x.y.z-SNAPSHOT)
1. Bump `latestRelease` to stable version just published.
1. git commit -am "Prepare next development version."
1. git push && git push --tags

