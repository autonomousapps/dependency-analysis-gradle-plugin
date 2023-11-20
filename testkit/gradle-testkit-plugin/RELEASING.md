Release procedure for gradle-testkit-plugin

1. Update CHANGELOG
1. Update README if needed
1. Remove the `-SNAPSHOT` suffix from the version name in `gradle-testkit-plugin/build.gradle.kts`.
1. `git commit -am "Prepare for testkit-plugin release x.y."`
1. Publish: `./gradlew -p testkit :gradle-testkit-plugin:publishEverywhere`
   (this will automatically run the tests, and won't publish if any fail)
1. `git tag -a testkit-plugin-x.y -m "TestKit Plugin version x.y."`
1. Update version number in `testkit/gradle-testkit-plugin/build.gradle.kts` to next snapshot version (x.y-SNAPSHOT)
1. `git commit -am "Prepare next development version of testkit-plugin."`
1. `git push && git push --tags`
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.
