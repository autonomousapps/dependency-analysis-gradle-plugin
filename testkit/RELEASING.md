Release procedure for testkit

1. Update CHANGELOG
1. Update README if needed
1. Remove the `-SNAPSHOT` suffix from the version name in `testkit/build.gradle.kts`.
1. git commit -am "Prepare for testkit release x.y."
1. Publish again: `./gradlew :testkit:publishToMavenCentral --no-configuration-cache`
   (this will automatically run the tests, including smoke tests, and won't publish if any fail)
1. git tag -a testkit-x.y -m "Testkit version x.y."
1. Update version number in `testkit/build.gradle.kts` to next snapshot version (x.y-SNAPSHOT)
1. git commit -am "Prepare next development version of testkit."
1. git push && git push --tags
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.

nb: if there are ever any issues with publishing to the Gradle Plugin Portal, open an issue on
https://github.com/gradle/plugin-portal-requests/issues and email plugin-portal-support@gradle.com.
