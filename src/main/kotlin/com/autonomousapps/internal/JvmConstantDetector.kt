package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.provider.Property

internal class JvmConstantDetector(
  private val inMemoryCacheProvider: Property<InMemoryCache>,
  private val components: List<Component>,
  private val actualImports: Set<String>
) {

  fun find(): Set<Dependency> = findUsedConstantImports(findConstantImportCandidates())

  // from the upstream bytecode. Therefore "candidates" (not necessarily used)
  private fun findConstantImportCandidates(): Set<ComponentWithConstantMembers> {
    return components.map { component ->
      component.dependency to JvmConstantMemberFinder2(inMemoryCacheProvider, component).find()
    }.filterNot { (_, imports) ->
      imports.isEmpty()
    }.mapToOrderedSet { (dependency, imports) ->
      ComponentWithConstantMembers(dependency, imports)
    }
  }

  private fun findUsedConstantImports(
    constantImportCandidates: Set<ComponentWithConstantMembers>
  ): Set<Dependency> = actualImports.flatMapToOrderedSet { actualImport ->
    findUsedConstantImports(actualImport, constantImportCandidates)
  }

  /**
   * [actualImport] is, e.g.,
   * * `com.myapp.BuildConfig.DEBUG`
   * * `com.myapp.BuildConfig.*`
   *
   * TODO it's a little disturbing there can be multiple matches. An issue with this naive algorithm.
   * TODO I need to be more intelligent in source parsing. Look at actual identifiers being used and associate those with their star-imports
   */
  private fun findUsedConstantImports(
    actualImport: String, constantImportCandidates: Set<ComponentWithConstantMembers>
  ): List<Dependency> = constantImportCandidates.filter {
    it.imports.contains(actualImport)
  }.map {
    it.dependency
  }
}

/**
 * Parses bytecode looking for constant declarations.
 */
private class JvmConstantMemberFinder2(
  inMemoryCacheProvider: Property<InMemoryCache>,
  private val component: Component
) {

  private val inMemoryCache = inMemoryCacheProvider.get()

  /**
   * Returns either an empty list, if there are no constants, or a list of import candidates. E.g.:
   * ```
   * [
   *   "com.myapp.BuildConfig.*",
   *   "com.myapp.BuildConfig.DEBUG"
   * ]
   * ```
   * An import statement with either of those would import the `com.myapp.BuildConfig.DEBUG`
   * constant, contributed by the "com.myapp" module.
   */
  fun find(): Set<String> {
    val alreadyFoundConstantMembers: Set<String>? = inMemoryCache.constantMember(component.dependency.identifier)
    if (alreadyFoundConstantMembers != null) {
      return alreadyFoundConstantMembers
    }

    val ktFiles = component.ktFiles
    return component.constantFields.flatMap { (className, constantFields) ->
      if (constantFields.isNotEmpty()) {
        val fqcn = className
          .replace("/", ".")
          .replace("$", ".")
        val ktPrefix = ktFiles.find { it.fqcn == fqcn }?.name?.let {
          fqcn.removeSuffix(it)
        }

        listOf(
          // import com.myapp.BuildConfig -> BuildConfig.DEBUG
          fqcn,
          // import com.myapp.BuildConfig.* -> DEBUG
          "$fqcn.*"
        ) +
          // import com.myapp.* -> /* Kotlin file with top-level const val declarations */
          optionalStarImport(fqcn) +
          // import com.library.CONSTANT -> com.library.ConstantsKt.CONSTANT
          optionalKtImport(ktPrefix, constantFields) +
          constantFields.map { name -> "$fqcn.$name" }
      } else {
        emptyList()
      }
    }.toSet()
      .also {
        inMemoryCache.constantMembers(component.dependency.identifier, it)
      }
  }

  private fun optionalStarImport(fqcn: String): List<String> {
    return if (fqcn.contains(".")) {
      listOf("${fqcn.substring(0, fqcn.lastIndexOf("."))}.*")
    } else {
      // "fqcn" is not in a package, and so contains no dots
      // a star import makes no sense in this context
      emptyList()
    }
  }

  private fun optionalKtImport(ktPrefix: String?, constantMembers: Set<String>): List<String> {
    return ktPrefix?.let { prefix ->
      constantMembers.map { member -> "$prefix$member" }
    } ?: emptyList()
  }
}

/*
 * TODO some thoughts on an improved algo:
 * Need a data structure that includes the following import patterns from providers:
 * 1. com.myapp.MyClass                // Import of class containing constant thing -> MyClass.CONSTANT_THING
 * 2. com.myapp.MyClass.CONSTANT_THING // Direct import of constant thing -> CONSTANT_THING
 * 3. com.myapp.MyClass.*              // Star-import of all constant things in MyClass -> CONSTANT_THING_1, CONSTANT_THING_2
 * 4. com.myapp.*                      // Kotlin top-level declarations in com.myapp package -> CONSTANT_THING
 *
 * 3 and 4 (mostly 4) are problematic because they results in non-uniquely identifiable component providers of
 * constants.
 *
 * If, on the consumer side, I see one of those import patterns, I could also look for `SimpleIdentifier`s and associate
 * those with constant things provided by the providers. My data structure would need the addition of simple identifiers
 * for each class/package.
 */
