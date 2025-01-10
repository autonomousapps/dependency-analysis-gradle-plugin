Release procedure for kotlin-editor-relocated

1. Update CHANGELOG.
1. Update README if needed.
1. Verify shadow jar. Run `./gradlew :kotlin-editor-relocated:shadowJar`. Manually inspect contents.
1. Update version in `kotlin-editor-relocated/build.gradle.kts`.
1. `git commit -am "chore(kotlin-editor): prepare for kotlin-editor-relocated release x."`.
1. Publish: `./gradlew :kotlin-editor-relocated:publishToMavenCentral`.
1. `git tag -a kotlin-editor-relocated-x -m "kotlin-editor-relocated version x."`.
1. Update version number in `kotlin-editor-relocated/build.gradle.kts` to next snapshot version (x.y.z-SNAPSHOT).
1. `git commit -am "chore(kotlin-editor): prepare next development version."`
1. `git push && git push --tags`.
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.
