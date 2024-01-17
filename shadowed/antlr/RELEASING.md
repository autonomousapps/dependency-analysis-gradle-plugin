Release procedure for antlr

1. Update CHANGELOG
1. Update README if needed
1. `git commit -am "Prepare for antlr release x."`
1. Verify shadow jar. Run `./gradlew :antlr:shadowJar`. Manually inspect contents.
1. Publish: `./gradlew :antlr:publishToMavenCentral`
1. `git tag -a antlr-x -m "antlr version x."`
1. `git push && git push --tags`
1. (Optional) Follow instructions in console output to release from Maven Central's staging repo.
   This step is now automated via the `:promote` task, and should only be necessary if that task
   fails.
