package com.autonomousapps.kit.artifacts

import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal class FileCollector(private val predicate: (Path) -> Boolean) : SimpleFileVisitor<Path>() {

  private val matchingFiles = mutableListOf<Path>()

  val files: List<Path> get() = matchingFiles

  override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
    val result = super.visitFile(file, attrs)

    if (predicate.invoke(file)) {
      matchingFiles.add(file)
    }

    return result
  }
}
