// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.test

import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind
import com.google.common.graph.ElementOrder
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipOutputStream

fun Any.fileFromResource(resourcePath: String): File = pathFromResource(resourcePath).toFile()

fun Any.pathFromResource(resourcePath: String): Path = Paths.get(uriFromResource(resourcePath))

fun Any.uriFromResource(resourcePath: String): URI = urlFromResource(resourcePath).toURI()

fun Any.urlFromResource(resourcePath: String): URL =
  javaClass.classLoader.getResource(resourcePath) ?: error("No resource at '$resourcePath'")

fun Any.textFromResource(resourcePath: String): String {
  return streamFromResource(resourcePath).bufferedReader().use(BufferedReader::readText)
}

fun Any.streamFromResource(resourcePath: String): InputStream {
  return javaClass.classLoader.getResourceAsStream(resourcePath) ?: error("No resource at '$resourcePath'")
}

fun walkFileTree(path: Path, predicate: (Path) -> Boolean = { true }): Set<File> {
  val files = mutableSetOf<File>()
  Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
    override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
      if (predicate.invoke(path)) {
        files.add(path.toFile())
      }
      return super.visitFile(path, attrs)
    }
  })

  return files
}

fun Path.emptyZipFile(): Path = resolve("${System.currentTimeMillis()}.zip").apply {
  Files.newOutputStream(this).use { fos ->
    ZipOutputStream(fos).run {
      close()
    }
  }
}

internal fun usage(
  bucket: Bucket,
  sourceKind: SourceKind,
  buildType: String? = null,
  flavor: String? = null,
  reasons: Set<Reason> = emptySet()
) = Usage(
  buildType = buildType,
  flavor = flavor,
  sourceKind = sourceKind,
  bucket = bucket,
  reasons = reasons,
)

@Suppress("UnstableApiUsage") // Guava
internal fun <N : Any> graphOf(vararg pairs: Pair<N, N>): ImmutableGraph<N> {
  val builder: ImmutableGraph.Builder<N> = GraphBuilder.directed()
    .allowsSelfLoops(false)
    .incidentEdgeOrder(ElementOrder.stable<N>())
    .immutable()

  pairs.forEach { (from, to) ->
    builder.putEdge(from, to)
  }
  return builder.build()
}
