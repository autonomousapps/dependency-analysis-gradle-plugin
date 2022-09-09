package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.externalArtifactsFor
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.stream.Collectors

@Suppress("UnstableApiUsage") // resolvedArtifacts
@CacheableTask
abstract class ResolveExternalDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Resolves external dependencies for single variant."
  }

  @get:Input
  abstract val artifactIds: ListProperty<ComponentArtifactIdentifier>

  /** Output in json format for compile classpath graph. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  internal fun configureTask(
    compileClasspath: Configuration,
    runtimeClasspath: Configuration,
    jarAttr: String
  ) {
    val extractor = IdExtractor()
//    artifactIds.addAll(compileClasspath.incoming.artifacts.resolvedArtifacts.map(extractor))
//    artifactIds.addAll(runtimeClasspath.incoming.artifacts.resolvedArtifacts.map(extractor))
    artifactIds.addAll(compileClasspath.externalArtifactsFor(jarAttr).resolvedArtifacts.map(extractor))
    artifactIds.addAll(runtimeClasspath.externalArtifactsFor(jarAttr).resolvedArtifacts.map(extractor))
  }

  private class IdExtractor : Transformer<List<ComponentArtifactIdentifier>, Collection<ResolvedArtifactResult>> {
    override fun transform(artifacts: Collection<ResolvedArtifactResult>): List<ComponentArtifactIdentifier> {
      return artifacts.stream().map(ResolvedArtifactResult::getId).collect(Collectors.toList())
    }
  }

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val dependencies = artifactIds.get().map { it.componentIdentifier.toCoordinates() }

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
