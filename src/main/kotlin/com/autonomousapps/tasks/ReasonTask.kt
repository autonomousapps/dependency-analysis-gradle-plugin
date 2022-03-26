package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonMapSet
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.lowercase
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Declaration
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.model.intermediates.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

abstract class ReasonTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Explain how a dependency is used"
  }

  @get:Input
  abstract val projectPath: Property<String>

  // Not really optional, but we want to handle validation ourselves, rather than let Gradle do it
  @get:Optional
  @get:Input
  @set:Option(option = "id", description = "Dependency identifier you'd like to reason about")
  var id: String? = null

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val dependencyUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val annotationProcessorUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  // TODO InputDirectory of all dependencies for finding capabilities
  // TODO graph of shortest path

  private val dependencyUsages by unsafeLazy {
    dependencyUsageReport.fromJsonMapSet<String, Usage>()
  }
  private val annotationProcessorUsages by unsafeLazy {
    annotationProcessorUsageReport.fromJsonMapSet<String, Usage>()
  }
  private val projectAdvice by unsafeLazy {
    projectAdviceReport.fromJson<com.autonomousapps.model.ProjectAdvice>()
  }

  @TaskAction fun action() {
    val coord = getRequestedCoordinates()
    val usages = getUsageFor(coord.gav())
    val advice = findAdviceFor(coord.gav())
    val declaration = findDeclarationFor(coord)

    val reason = DeepThought(coord, usages, advice, declaration).computeReason()

    logger.quiet(reason)
  }

  /** Returns the requested ID as [Coordinates], even if user passed in a prefix. */
  private fun getRequestedCoordinates(): Coordinates {
    val id: String = id ?: throw InvalidUserDataException(
      """
        You must call 'reason' with the `--id` option. For example:
          ./gradlew ${projectPath.get()}:reason --id com.foo:bar:1.0
          ./gradlew ${projectPath.get()}:reason --id :other:module
          
        For external dependencies, the version string is optional.
      """.trimIndent()
    )

    // Guaranteed to find full GAV or throw
    val gav = dependencyUsages.entries.find(id::equalsKey)?.key
      ?: dependencyUsages.entries.find(id::startsWithKey)?.key
      ?: annotationProcessorUsages.entries.find(id::equalsKey)?.key
      ?: annotationProcessorUsages.entries.find(id::startsWithKey)?.key
      ?: throw InvalidUserDataException("There is no dependency with coordinates '$id' in this project.")
    return Coordinates.of(gav)
  }

  private fun getUsageFor(id: String): Set<Usage> {
    // One of these is guaranteed to be non-null
    return dependencyUsages.entries.find(id::equalsKey)?.value?.toSortedSet(Usage.BY_VARIANT)
      ?: annotationProcessorUsages.entries.find(id::equalsKey)?.value?.toSortedSet(Usage.BY_VARIANT)
      // This should really be impossible
      ?: throw InvalidUserDataException("No usage found for coordinates '$id'.")
  }

  private fun findAdviceFor(id: String): Advice? {
    // Would be null if there is no advice for the given id.
    return projectAdvice.dependencyAdvice.find { it.coordinates.gav() == id }
  }

  private fun findDeclarationFor(coordinates: Coordinates): Declaration? {
    // Would be null if the given id were not declared.
    return declarations.fromJsonSet<Declaration>().find { it.identifier == coordinates.identifier }
  }

  internal class DeepThought(
    private val coordinates: Coordinates,
    private val usages: Set<Usage>,
    private val advice: Advice?,
    private val declaration: Declaration? // TODO unused
  ) {

    fun computeReason() = buildString {
      check(advice != null || declaration != null) {
        "One of 'advice' or 'declaration' must be non-null"
      }

      // Header
      appendReproducibleNewLine("-".repeat(40))
      append("You asked about the dependency '${coordinates.gav()}'. ")
      appendReproducibleNewLine(adviceText())
      appendReproducibleNewLine("-".repeat(40))

      // Usages
      usages.forEach { usage ->
        val variant = usage.variant

        appendReproducibleNewLine()
        sourceText(variant).let { txt ->
          appendReproducibleNewLine(txt)
          appendReproducibleNewLine("-".repeat(txt.length))
        }

        val reasons = usage.reasons.filter { it !is Reason.Unused && it !is Reason.Undeclared }
        val isCompileOnly = reasons.any { it is Reason.CompileTimeAnnotations }
        reasons.forEach { reason ->
          append("""* """)
          val prefix = if (variant.kind == SourceSetKind.MAIN) "" else "test"
          appendReproducibleNewLine(reason.reason(prefix, isCompileOnly))
        }
        if (reasons.isEmpty()) {
          appendReproducibleNewLine("(no usages)")
        }
      }
    }

    private fun adviceText(): String = when {
      advice == null -> "There is no advice regarding this dependency."
      advice.isAdd() || advice.isCompileOnly() -> {
        "You have been advised to add this dependency to '${advice.toConfiguration}'."
      }
      advice.isRemove() || advice.isProcessor() -> {
        "You have been advised to remove this dependency from '${advice.fromConfiguration}'."
      }
      advice.isChange() -> {
        "You have been advised to change this dependency to '${advice.toConfiguration}' from '${advice.fromConfiguration}'."
      }
      else -> error("Unknown advice type: $advice")
    }

    private fun sourceText(variant: Variant): String = when (variant.variant) {
      Variant.VARIANT_NAME_MAIN, Variant.VARIANT_NAME_TEST -> "Source: ${variant.variant}"
      else -> "Source: ${variant.variant}, ${variant.kind.name.lowercase()}"
    }
  }
}

private fun <T> String.equalsKey(mapEntry: Map.Entry<String, T>) = mapEntry.key == this
private fun <T> String.startsWithKey(mapEntry: Map.Entry<String, T>) = mapEntry.key.startsWith(this)
