Release procedure for variant-artifacts

1. Update CHANGELOG
1. Update README if needed
1. Update the version in `build.gradle.kts`.
1. `git commit -am "chore(variant-artifacts): prepare for variant-artifacts release x.y."`
1. Publish: `./gradlew :variant-artifacts:publishToMavenCentral`
   (this will automatically run the tests, and won't publish if any fail)
1. `git tag -a variant-artifacts-x.y -m "Variant-Artifacts version x.y."`
1. `git push && git push --tags`
