// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("SpellCheckingInspection")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.graph.DependencySizeTree
import com.autonomousapps.graph.DominanceTree
import com.autonomousapps.graph.DominanceTreeDataWriter
import com.autonomousapps.graph.DominanceTreeWriter
import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
public abstract class ComputeDominatorTreeTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes a dominator view of the dependency graph"
  }

  @get:Input
  public abstract val buildPath: Property<String>

  @get:Input
  public abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val physicalArtifacts: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val graphView: RegularFileProperty

  @get:OutputFile
  public abstract val outputTxt: RegularFileProperty

  @get:OutputFile
  public abstract val outputDot: RegularFileProperty

  @get:OutputFile
  public abstract val outputJson: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) { spec ->
      spec.buildPath.set(buildPath)
      spec.projectPath.set(projectPath)
      spec.physicalArtifacts.set(physicalArtifacts)
      spec.graphView.set(graphView)
      spec.outputTxt.set(outputTxt)
      spec.outputDot.set(outputDot)
      spec.outputJson.set(outputJson)
    }
  }

  public interface Parameters : WorkParameters {
    public val buildPath: Property<String>
    public val projectPath: Property<String>
    public val physicalArtifacts: RegularFileProperty
    public val graphView: RegularFileProperty
    public val outputTxt: RegularFileProperty
    public val outputDot: RegularFileProperty
    public val outputJson: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      compute(
        buildPath = parameters.buildPath,
        projectPath = parameters.projectPath,
        outputTxt = parameters.outputTxt,
        outputDot = parameters.outputDot,
        outputJson = parameters.outputJson,
        physicalArtifacts = parameters.physicalArtifacts,
        graphView = parameters.graphView,
      )
    }

    private fun compute(
      buildPath: Property<String>,
      projectPath: Property<String>,
      outputTxt: RegularFileProperty,
      outputDot: RegularFileProperty,
      outputJson: RegularFileProperty,
      physicalArtifacts: RegularFileProperty,
      graphView: RegularFileProperty,
    ) {
      val outputTxt = outputTxt.getAndDelete()
      val outputDot = outputDot.getAndDelete()
      val outputJson = outputJson.getAndDelete()

      val artifactMap = physicalArtifacts.fromJsonSet<PhysicalArtifact>().associate { (coord, file) ->
        coord to file
      }

      val graphView = graphView.fromJson<DependencyGraphView>()
      val project = ProjectCoordinates(projectPath.get(), GradleVariantIdentification(setOf("ROOT"), emptyMap()), ":")
      val tree = DominanceTree(graphView.graph, project)

      val nodeWriter = BySize(
        files = artifactMap,
        tree = tree,
        root = project
      )
      val dominanceTreeWriter: DominanceTreeWriter<Coordinates> = DominanceTreeWriter(
        root = project,
        tree = tree,
        nodeWriter = nodeWriter,
      )
      val dataWriter = DominanceTreeDataWriter(
        root = project,
        tree = tree,
        nodeWriter = nodeWriter,
      )
      val graphWriter = GraphWriter(buildPath.get())

      outputTxt.writeText(dominanceTreeWriter.string)
      outputDot.writeText(graphWriter.toDot(tree.dominanceGraph))
      outputJson.bufferWriteParameterizedJson<DependencySizeTree<String>, String>(
        dataWriter.sizeTree.map { it.identifier } // we only really care about the identitfiers
      )
    }
  }

  private class BySize(
    private val files: Map<Coordinates, File>,
    private val tree: DominanceTree<Coordinates>,
    root: Coordinates,
  ) : DominanceTreeWriter.NodeWriter<Coordinates> {

    private val sizes = mutableMapOf<Coordinates, Long>()
    private val reachableNodes = mutableMapOf<Coordinates, Set<Coordinates>>()

    override fun getTreeSize(node: Coordinates): Long = sizes.computeIfAbsent(node) { treeSizeOf(it) }

    override fun getSize(node: Coordinates): Long? = files[node]?.length()

    private fun reachableNodes(node: Coordinates) = reachableNodes.computeIfAbsent(node) {
      tree.dominanceGraph.reachableNodes(node, excludeSelf = false)
    }

    private fun treeSizeOf(node: Coordinates): Long = reachableNodes(node)
      .mapNotNull { files[it] }
      .sumOf { it.length() }

    // Get the scale (bytes, KB, MB, ...) for printing.
    private val scale = reachableNodes(root)
      .mapNotNull { files[it] }
      .sumOf { it.length() }
      .let { FileUtils.getScale(it) }

    private val comparator = Comparator<Coordinates> { left, right ->
      // nb: right.compareTo(left) is intentional. Sorted descending.
      getTreeSize(right).compareTo(getTreeSize(left))
    }

    override fun comparator(): Comparator<Coordinates> = comparator

    override fun toString(node: Coordinates): String {
      val builder = StringBuilder()

      var printedTotalSize = false
      val subs = reachableNodes(node)
      if ((subs - node).isNotEmpty()) {
        val totalSize = subs
          .mapNotNull { files[it] }
          .sumOf { it.length() }

        printedTotalSize = true
        builder.append(FileUtils.byteCountToDisplaySize(totalSize, scale)).append(' ')
      }

      files[node]
        ?.length()
        ?.let {
          if (printedTotalSize) builder.append('(')
          builder.append(FileUtils.byteCountToDisplaySize(it, scale))
          if (printedTotalSize) builder.append(')')
          builder.append(' ')
        }

      val preferredCoordinatesNotation = if (node is IncludedBuildCoordinates) {
        node.resolvedProject
      } else {
        node
      }
      builder.append(preferredCoordinatesNotation.gav())
      return builder.toString()
    }
  }
}
