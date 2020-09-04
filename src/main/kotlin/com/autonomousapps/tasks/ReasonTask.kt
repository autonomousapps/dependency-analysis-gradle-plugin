package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ReasonableDependency
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.graph.GraphWriter
import com.autonomousapps.graph.ProducerNode
import com.autonomousapps.graph.Reason
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

abstract class ReasonTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Provides the reason for a piece of advice"

    // This task is never up to date. It always prints to console
    outputs.upToDateWhen { false }
  }

  private val projectPath = project.path
  private lateinit var query: String

  @Option(option = "id", description = "The dependency whose advice to explain")
  fun query(identifier: String) {
    this.query = identifier
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val graph: RegularFileProperty

  /**
   * [`List<Advice>`][Advice] -- the advice generated for this project. In our nomenclature here,
   * this advice is for the "consumer."
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val advice: RegularFileProperty

  /**
   * The [ReasonableDependency]s.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val reasonableDependenciesReport: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  private val reasonableDependencies by lazy {
    reasonableDependenciesReport.fromJsonSet<ReasonableDependency>()
  }

  @TaskAction fun action() {
    val outputDot = outputDot.getAndDelete()

    val graph = graph.fromJson<DependencyGraph>()
    val advice = advice.fromJsonList<Advice>()

    val queryNode = getQueryNode()
    val reason = Reason.determine(
      graph = graph,
      queryNode = queryNode,
      advice = advice
    )

    logger.quiet(reason.toString())

    logger.quiet("Reason DOT: ${outputDot.path}")
    outputDot.writeText(GraphWriter.toDot(graph, reason.path))
  }

  private fun getQueryNode(): ProducerNode {
    if (query == projectPath) {
      throw GradleException("You cannot query for the project itself.")
    }

    val reasonableDependency = reasonableDependencies.find { it.dependency.identifier == query }
      ?: error("No component matching $query found in the dependency graph")

    return ProducerNode(
      identifier = query,
      reasonableDependency = reasonableDependency
    )
  }
}
