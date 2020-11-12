@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.graph.*
import com.autonomousapps.internal.Location
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * This task generates a very simple project-dependencies graph that describes the direct
 * dependencies of the subproject in question.
 */
@CacheableTask
abstract class ProjectGraphTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a graph of all inter-project dependencies"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val locations: ListProperty<RegularFile>

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      locations.set(this@ProjectGraphTask.locations)
      outputJson.set(this@ProjectGraphTask.outputJson)
      outputDot.set(this@ProjectGraphTask.outputDot)
      projectPath.set(project.path)
    }
  }

  interface Parameters : WorkParameters {
    val locations: ListProperty<RegularFile>
    val outputJson: RegularFileProperty
    val outputDot: RegularFileProperty
    val projectPath: Property<String>
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<ProjectGraphTask>()

    override fun execute() {
      val outputJsonFile = parameters.outputJson.getAndDelete()
      val outputDotFile = parameters.outputDot.getAndDelete()

      val graph = DependencyGraph()
      parameters.locations.get().flatMap {
        it.fromJsonSet<Location>()
      }.filterToSet {
        it.isInteresting && it.identifier.startsWith(":")
      }.forEach {
        graph.addEdge(Edge(ProducerNode(parameters.projectPath.get()), ConsumerNode(it.identifier)))
      }

      logger.quiet("Graph JSON at ${outputJsonFile.path}")
      outputJsonFile.writeText(graph.toJson())

      logger.quiet("Graph DOT at ${outputDotFile.path}")
      outputDotFile.writeText(GraphWriter.toDot(graph))
    }
  }
}
