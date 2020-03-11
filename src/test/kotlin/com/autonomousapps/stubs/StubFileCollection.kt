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

class StubFileCollection(private val files: MutableList<File>) : FileCollection {

  constructor(vararg files: File) : this(files.toMutableList())

  override fun iterator(): MutableIterator<File> = files.iterator()

  override fun getFiles(): MutableSet<File> = TODO("stub")
  override fun contains(file: File): Boolean = TODO("stub")
  override fun getAsFileTree(): FileTree = TODO("stub")
  override fun isEmpty(): Boolean = TODO("stub")
  override fun addToAntBuilder(builder: Any, nodeName: String, type: FileCollection.AntType) = TODO("stub")
  override fun addToAntBuilder(builder: Any, nodeName: String): Any = TODO("stub")
  override fun getBuildDependencies(): TaskDependency = TODO("stub")
  override fun minus(collection: FileCollection): FileCollection = TODO("stub")
  override fun getAsPath(): String = TODO("stub")
  override fun getElements(): Provider<MutableSet<FileSystemLocation>> = TODO("stub")
  override fun filter(filterClosure: Closure<*>): FileCollection = TODO("stub")
  override fun filter(filterSpec: Spec<in File>): FileCollection = TODO("stub")
  override fun plus(collection: FileCollection): FileCollection = TODO("stub")
  override fun getSingleFile(): File = TODO("stub")
}
