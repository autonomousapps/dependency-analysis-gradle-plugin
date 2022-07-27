Release procedure for dependency-analysis-android-gradle-plugin

1. Update CHANGELOG
1. Update README if needed
1. Bump version number in `gradle.properties` to next stable version (use semantic versioning: x.y.z)
   _with_ the `-SNAPSHOT` suffix (we publish a snapshot first for smoke testing).
1. Publish the snapshot to Maven Central: `./gradlew :publishEverywhere --no-configuration-cache`
1. Remove the `-SNAPSHOT` suffix from the version name.
1. git commit -am "Prepare for release x.y.z."
1. Publish again: `./gradlew :publishEverywhere --no-configuration-cache`
(this will automatically run the tests, including smoke tests, and won't publish if any fail)
1. git tag -a vx.y.z -m "Version x.y.z"
1. Update version number `gradle.properties` to next snapshot version (x.y.z-SNAPSHOT)
1. git commit -am "Prepare next development version."
1. git push && git push --tags
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.

nb: if there are ever any issues with publishing to the Gradle Plugin Portal, open an issue on 
https://github.com/gradle/plugin-portal-requests/issues and email plugin-portal-support@gradle.com.
