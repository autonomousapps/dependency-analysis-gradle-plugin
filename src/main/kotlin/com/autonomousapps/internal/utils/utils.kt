@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.util.*

// standard `all` function returns true if collection is empty!
internal inline fun <T> Collection<T>.reallyAll(predicate: (T) -> Boolean): Boolean {
  if (isEmpty()) return false
  for (element in this) if (!predicate(element)) return false
  return true
}

internal fun Sequence<MatchResult>.allItems(): List<String> =
  flatMap { matchResult ->
    val groupValues = matchResult.groupValues
    // Ignore the 0th element, as it is the entire match
    if (groupValues.isNotEmpty()) groupValues.subList(1, groupValues.size).asSequence()
    else emptySequence()
  }.toList()

internal fun ComponentIdentifier.asString(): String {
  return when (this) {
    is ProjectComponentIdentifier -> projectPath
    is ModuleComponentIdentifier -> moduleIdentifier.toString()
    else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
  }.intern()
}

internal fun ComponentIdentifier.resolvedVersion(): String? {
  return when (this) {
    is ProjectComponentIdentifier -> null
    is ModuleComponentIdentifier -> version
    else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
  }
}

internal fun DependencySet.toIdentifiers(): Set<String> = mapNotNullToSet {
  when (it) {
    is ProjectDependency -> it.dependencyProject.path
    is ModuleDependency -> "${it.group}:${it.name}"
    // Don't have enough information, so ignore it
    is SelfResolvingDependency -> null
    else -> throw GradleException("Unknown Dependency subtype: \n$it\n${it.javaClass.name}")
  }
}

// Begins with an 'L'
// followed by at least one word character
// followed by one or more word char, /, or $, in any combination
// ends with a ';'
// Not perfect, but probably close enough
internal val METHOD_DESCRIPTOR_REGEX = """L\w[\w/$]+;""".toRegex()

// TODO sync with above. Note this has a capturing group.
internal val DESC_REGEX = """L(\w[\w/$]+);""".toRegex()

// This regex matches a Java FQCN.
// https://stackoverflow.com/questions/5205339/regular-expression-matching-fully-qualified-class-names#comment5855158_5205467
internal val JAVA_FQCN_REGEX =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()
internal val JAVA_FQCN_REGEX_SLASHY =
  "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*/)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*".toRegex()


// Print dependency tree (like running the `dependencies` task).
@Suppress("unused")
internal fun printDependencyTree(dependencies: Set<DependencyResult>, level: Int = 0) {
  dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { result ->
    val resolvedComponentResult = result.selected
    println("${"  ".repeat(level)}- ${resolvedComponentResult.id}")
      printDependencyTree(resolvedComponentResult.dependencies, level + 1)
  }
}

internal fun TaskContainer.namedOrNull(name: String): TaskProvider<Task>? = try {
  named(name)
} catch (_: UnknownTaskException) {
  null
}

// copied from StringsJVM.kt
fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
  if (isNotEmpty()) {
    val firstChar = this[0]
    if (firstChar.isLowerCase()) {
      return buildString {
        val titleChar = firstChar.toTitleCase()
        if (titleChar != firstChar.toUpperCase()) {
          append(titleChar)
        } else {
          append(this@capitalizeSafely.substring(0, 1).toUpperCase(locale))
        }
        append(this@capitalizeSafely.substring(1))
      }
    }
  }
  return this
}
