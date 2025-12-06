// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts.processing

import com.autonomousapps.internal.utils.Files
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.model.internal.KtFile
import java.io.File

internal class DirectoryArtifact(val dir: File) : Artifact {

  init {
    check(dir.isDirectory) { "Expected directory. Was '${dir.absolutePath}'" }
  }

  override fun hasKotlinClasses(): Boolean = ktFiles().isNotEmpty()

  override fun ktFiles(): Set<KtFile> = KtFile.fromDirectory(dir)

  override fun asSequenceOfClassFiles(): Sequence<ClassFile> =
    dir.asSequenceOfClassFiles().map { classFile ->
      ClassFile(
        inputStreamSupplier = { classFile.inputStream() },
        packagePath = Files.asPackagePath(classFile)
      )
    }
}
