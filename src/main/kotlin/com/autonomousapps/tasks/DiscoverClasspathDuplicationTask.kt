package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.ProjectVariant
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.TreeSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CacheableTask
abstract class DiscoverClasspathDuplicationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  internal fun description(name: String) {
    description = "Discovers duplicates on the $name classpath"
    classpathName.set(name)
  }

  private lateinit var classpath: ArtifactCollection

  fun setClasspath(artifacts: ArtifactCollection) {
    this.classpath = artifacts
  }

  @Classpath
  fun getClasspath(): FileCollection = classpath.artifactFiles

  @get:Input
  abstract val classpathName: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val syntheticProject: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val project = syntheticProject.fromJson<ProjectVariant>()
    val duplicates = ClasspathAnalyzer(project, classpathName.get(), classpath).duplicates()

    output.writeText(duplicates.toJson())
  }

  internal class ClasspathAnalyzer(
    private val project: ProjectVariant,
    private val classpathName: String,
    artifacts: ArtifactCollection,
  ) {

    // map of class files to dependencies that contain them
    private val duplicatesMap = newSetMultimap<String, Coordinates>()

    init {
      artifacts
        .filterNonGradle()
        .filter { it.file.name.endsWith(".jar") }
        .forEach(::inspectJar)
    }

    fun duplicates(): Set<DuplicateClass> {
      return duplicatesMap.asMap()
        // Find all class files provided by more than one dependency
        .filterValues { it.size > 1 }
        // only warn about duplicates if it's about a class that's actually used.
        .filterKeys {
          // println("USED CLASSES: ${project.usedClasses}")
          it.replace('/', '.').removeSuffix(".class") in project.usedClasses
        }
        .mapTo(TreeSet()) { (classReference, dependency) ->
          DuplicateClass(
            variant = project.variant,
            classpathName = classpathName,
            classReference = classReference,
            dependencies = dependency.toSortedSet(),
          )
        }
    }

    private fun inspectJar(artifact: ResolvedArtifactResult) {
      val zip = ZipFile(artifact.file)

      val coordinates = artifact.toCoordinates()

      // Create multimap of classname to [dependencies]
      zip.asSequenceOfClassFiles()
        .map(ZipEntry::getName)
        .forEach { duplicatesMap.put(it, coordinates) }
    }
  }
}
