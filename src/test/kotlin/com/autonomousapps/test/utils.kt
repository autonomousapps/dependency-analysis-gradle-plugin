package com.autonomousapps.test

import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
import com.google.common.graph.ElementOrder
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipOutputStream

fun Any.fileFromResource(resourcePath: String): File = pathFromResource(resourcePath).toFile()

fun Any.pathFromResource(resourcePath: String): Path = Paths.get(uriFromResource(resourcePath))

fun Any.uriFromResource(resourcePath: String): URI = urlFromResource(resourcePath).toURI()

fun Any.urlFromResource(resourcePath: String): URL = javaClass.classLoader.getResource(resourcePath) ?: error("No resource at '$resourcePath'")

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
  variant: String = "debug",
  buildType: String? = null,
  flavor: String? = null,
  kind: SourceSetKind = SourceSetKind.MAIN,
  reasons: Set<Reason> = emptySet()
) = Usage(
  buildType = buildType,
  flavor = flavor,
  variant = Variant(variant, kind),
  bucket = bucket,
  reasons = reasons
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
