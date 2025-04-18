Release procedure for gradle-testkit-support

1. Update CHANGELOG
1. Update README if needed
1. Remove the `-SNAPSHOT` suffix from the version name in `gradle-testkit-support/build.gradle.kts`.
1. `git commit -am "chore(testkit): prepare for testkit-support release x.y."`
1. Publish: `./gradlew -p testkit :gradle-testkit-support:publishToMavenCentral`
   (this will automatically run the tests, and won't publish if any fail)
1. `git tag -a testkit-support-x.y -m "TestKit Support version x.y."`
1. Update version number in `testkit/gradle-testkit-support/build.gradle.kts` to next snapshot version (x.y-SNAPSHOT)
1. `git commit -am "chore(testkit): prepare next development version of testkit-support."`
1. `git push && git push --tags`
1. (Optional) Update `gradle-testkit-plugin` default version of this library and publish it.
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.
