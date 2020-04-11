@file:Suppress("UnstableApiUsage")

package com.autonomousapps.stubs

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskDependency
import java.io.File
import javax.naming.OperationNotSupportedException

class StubFileCollection(private val files: MutableList<File>) : FileCollection {

  constructor(vararg files: File) : this(files.toMutableList())

  override fun iterator(): MutableIterator<File> = files.iterator()

  override fun getFiles(): MutableSet<File> = throw OperationNotSupportedException("stub")
  override fun contains(file: File): Boolean = throw OperationNotSupportedException("stub")
  override fun getAsFileTree(): FileTree = throw OperationNotSupportedException("stub")
  override fun isEmpty(): Boolean = throw OperationNotSupportedException("stub")
  override fun addToAntBuilder(builder: Any, nodeName: String, type: FileCollection.AntType) =
    throw OperationNotSupportedException("stub")
  override fun addToAntBuilder(builder: Any, nodeName: String): Any =
    throw OperationNotSupportedException("stub")
  override fun getBuildDependencies(): TaskDependency =
    throw OperationNotSupportedException("stub")
  override fun minus(collection: FileCollection): FileCollection =
    throw OperationNotSupportedException("stub")
  override fun getAsPath(): String = throw OperationNotSupportedException("stub")
  override fun getElements(): Provider<MutableSet<FileSystemLocation>> =
    throw OperationNotSupportedException("stub")
  override fun filter(filterClosure: Closure<*>): FileCollection =
    throw OperationNotSupportedException("stub")
  override fun filter(filterSpec: Spec<in File>): FileCollection =
    throw OperationNotSupportedException("stub")
  override fun plus(collection: FileCollection): FileCollection =
    throw OperationNotSupportedException("stub")
  override fun getSingleFile(): File = throw OperationNotSupportedException("stub")
}
