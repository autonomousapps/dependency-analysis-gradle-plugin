@file:Suppress("SpellCheckingInspection")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.graph.DominanceTree
import com.autonomousapps.graph.DominanceTreeWriter
import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.FileUtils
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.PhysicalArtifact
import com.autonomousapps.model.ProjectCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class ComputeDominatorTreeTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Computes a dominator view of the dependency graph"
  }

  @get:Input
  abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val physicalArtifacts: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val graphView: RegularFileProperty

  @get:OutputFile
  abstract val outputTxt: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    val outputTxt = outputTxt.getAndDelete()
    val outputDot = outputDot.getAndDelete()

    val artifactMap = physicalArtifacts.fromJsonSet<PhysicalArtifact>().associate { (coord, file) ->
      coord to file
    }
    val graphView = graphView.fromJson<DependencyGraphView>()
    val project = ProjectCoordinates(projectPath.get(), "")

    val tree = DominanceTree(graphView.graph, project)
    val nodeWriter = BySize(
      files = artifactMap,
      tree = tree,
      root = project
    )
    val writer: DominanceTreeWriter<Coordinates> = DominanceTreeWriter(
      root = project,
      tree = tree,
      nodeWriter = nodeWriter,
    )

    outputTxt.writeText(writer.string)
    outputDot.writeText(GraphWriter.toDot(tree.dominanceGraph))
  }

  private class BySize(
    private val files: Map<Coordinates, File>,
    private val tree: DominanceTree<Coordinates>,
    root: Coordinates
  ) : DominanceTreeWriter.NodeWriter<Coordinates> {

    private val sizes = mutableMapOf<Coordinates, Long>()
    private val reachableNodes = mutableMapOf<Coordinates, Set<Coordinates>>()

    private fun getSize(node: Coordinates): Long = sizes.computeIfAbsent(node) { treeSizeOf(it) }

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
      getSize(right).compareTo(getSize(left))
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

      builder.append(node.gav())
      return builder.toString()
    }
  }
}
