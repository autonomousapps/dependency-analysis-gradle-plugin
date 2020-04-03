@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.utils.asString
import com.autonomousapps.internal.utils.resolvedVersion
import org.gradle.api.artifacts.component.ComponentIdentifier
import java.io.File
import java.io.Serializable
import java.lang.annotation.RetentionPolicy
import java.util.regex.Pattern

/**
 * A tuple of an identifier (project or external module) and the name of the configuration on which it is declared.
 *
 * TODO: this might be temporary. The intent is that this information make its way into a `Dependency` or something.
 */
data class DependencyConfiguration(
  val identifier: String,
  val configurationName: String
) : Serializable

/**
 * Basically a tuple of [identifier] and [resolvedVersion] (and optionally the [configurationName] on which this
 * dependency is declared). `resolvedVersion` will be null for project dependencies, and `configurationName` will be
 * null for (at least) transitive dependencies.
 *
 * For equality purposes, this class only cares about its `identifier`. No other property matters.
 */
data class Dependency(
  /**
   * In group:artifact form. E.g.,
   * 1. "javax.inject:javax.inject"
   * 2. ":my-project"
   */
  val identifier: String,
  /**
   * Resolved version. Will be null for project dependencies.
   */
  val resolvedVersion: String? = null,
  /**
   * The configuration on which this dependency was declared, or null if none found.
   */
  val configurationName: String? = null
) : Comparable<Dependency> {

  constructor(componentIdentifier: ComponentIdentifier) : this(
    identifier = componentIdentifier.asString(),
    resolvedVersion = componentIdentifier.resolvedVersion()
  )

  /*
   * These overrides all basically say that we don't care about the resolved version for our algorithms. End-users
   * might care, which is why we include it anyway.
   */

  override fun compareTo(other: Dependency): Int = identifier.compareTo(other.identifier)

  override fun toString(): String {
    return if (resolvedVersion != null) {
      "$identifier:$resolvedVersion"
    } else {
      identifier
    }
  }

  /**
   * We only care about the [identifier] for equality comparisons.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Dependency

    if (identifier != other.identifier) return false

    return true
  }

  override fun hashCode(): Int = identifier.hashCode()
}

/**
 * Primarily used as a pointer to a [file] on disk; a physical artifact.
 */
data class Artifact(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
   */
  var isTransitive: Boolean? = null,
  /**
   * Physical artifact on disk; a jar file.
   */
  var file: File
) {
  constructor(
    componentIdentifier: ComponentIdentifier,
    file: File,
    candidates: Set<DependencyConfiguration>
  ) : this(
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion(),
      configurationName = candidates.find { it.identifier == componentIdentifier.asString() }?.configurationName
    ),
    file = file
  )
}

/**
 * A library or project, along with the set of classes declared by this component.
 */
data class Component(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
   */
  val isTransitive: Boolean,
  /**
   * True if this dependency contains only annotation that are only needed at compile-time (`CLASS` and `SOURCE` level
   * retention policies). False otherwise.
   */
  val isCompileOnlyAnnotations: Boolean = false,
  /**
   * The classes declared by this library.
   */
  val classes: Set<String>
) : Comparable<Component> {

  internal constructor(artifact: Artifact, analyzedJar: AnalyzedJar) : this(
    dependency = artifact.dependency,
    isTransitive = artifact.isTransitive!!,
    isCompileOnlyAnnotations = analyzedJar.isCompileOnlyCandidate(),
    classes = analyzedJar.classNames()
  )

  override fun compareTo(other: Component): Int = dependency.compareTo(other.dependency)
}

/**
 * Represents a "mis-used" transitive dependency. The [dependency] represents the unique name
 * (see [Dependency.identifier]), and the [usedTransitiveClasses] are the class members of the dependency that are used
 * directly (which "shouldn't" be).
 */
data class TransitiveComponent(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * These are class members of this dependency that are used directly by the project in question. They have leaked
   * onto the classpath (either unintentionally or by design). Unintentional leakage is usually the result of use of
   * the `compile` configuration (or Maven scope); cf the `api` configuration, which "leaks" by design.
   */
  val usedTransitiveClasses: Set<String>
)

/**
 * Represents a dependency ([Dependency.identifier]) that is declared in the `dependencies {}` block of a build script.
 * This dependency is unused and has zero or more transitive dependencies that _are_ used ([usedTransitiveDependencies])
 */
data class UnusedDirectComponent(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * If this direct dependency has any transitive dependencies that are used, they will be in this set.
   *
   * In group:artifact form. E.g.,
   * 1. "javax.inject:javax.inject"
   * 2. ":my-project"
   */
  val usedTransitiveDependencies: MutableSet<Dependency>
)

data class ComponentWithInlineMembers(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * A set of imports that indicates a possible use of an inline member from this component ([dependency]).
   */
  val imports: Set<String>
) : Comparable<ComponentWithInlineMembers> {
  override fun compareTo(other: ComponentWithInlineMembers): Int = dependency.compareTo(other.dependency)
}

data class ComponentWithConstantMembers(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * A set of imports that indicates a possible use of a constant member from this component ([dependency]).
   */
  val imports: Set<String>
) : Comparable<ComponentWithConstantMembers> {
  override fun compareTo(other: ComponentWithConstantMembers): Int = dependency.compareTo(other.dependency)
}

data class Imports(
  val sourceType: SourceType,
  val imports: Set<String>
)

enum class SourceType {
  JAVA, KOTLIN
}

data class Res(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * An import that indicates a possible use of an Android resource from this component ([dependency]).
   */
  val import: String
) {
  constructor(componentIdentifier: ComponentIdentifier, import: String) : this(
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion()
    ),
    import = import
  )
}

/**
 * Metadata from an Android manifest.
 */
data class Manifest(
  /**
   * The package name per `<manifest package="...">`.
   */
  val packageName: String,
  /**
   * True if the manifest contains Android components (Activity, Service, BroadcastReceiver, ContentProvider).
   */
  val hasComponents: Boolean,
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency
) : Comparable<Manifest> {
  constructor(packageName: String, hasComponents: Boolean, componentIdentifier: ComponentIdentifier) : this(
    packageName = packageName,
    hasComponents = hasComponents,
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion()
    )
  )

  override fun compareTo(other: Manifest): Int {
    return dependency.compareTo(other.dependency)
  }
}

data class Advice(
  /**
   * The dependency that ought to be modified in some way.
   */
  val dependency: Dependency,
  /**
   * The current configuration on which the dependency has been declared. Will be null for transitive dependencies.
   */
  val fromConfiguration: String? = null,
  /**
   * The configuration on which the dependency _should_ be declared. Will be null if the dependency is unused and
   * therefore ought to be removed.
   */
  val toConfiguration: String? = null
) : Comparable<Advice> {

  companion object {
    fun add(dependency: Dependency, toConfiguration: String) =
      Advice(dependency, fromConfiguration = null, toConfiguration = toConfiguration)

    fun remove(dependency: Dependency) =
      Advice(dependency, fromConfiguration = dependency.configurationName, toConfiguration = null)

    fun change(dependency: Dependency, toConfiguration: String) =
      Advice(dependency, fromConfiguration = dependency.configurationName, toConfiguration = toConfiguration)

    fun compileOnly(dependency: Dependency, toConfiguration: String) =
      Advice(dependency, fromConfiguration = dependency.configurationName, toConfiguration = toConfiguration)
  }

  override fun compareTo(other: Advice): Int {
    // TODO I'd like to make this comparison more robust
    return dependency.compareTo(other.dependency)
  }

  /**
   * `compileOnly` dependencies are special. If they are so declared, we assume the user knows what they're doing and
   * do not recommend changing them. We also don't recommend _adding_ a compileOnly dependency that is only included
   * transitively (to be less annoying).
   *
   * So, an advice is "compileOnly-advice" only if it is a compileOnly candidate and is declared on a different
   * configuration.
   */
  fun isCompileOnly() = toConfiguration?.endsWith("compileOnly", ignoreCase = true) == true

  /**
   * An advice is "add-advice" if it is undeclared and used, AND is not `compileOnly`.
   */
  fun isAdd() = fromConfiguration == null && !isCompileOnly()

  /**
   * An advice is "remove-advice" if it is declared and not used, AND is not `compileOnly`.
   */
  fun isRemove() = toConfiguration == null && !isCompileOnly()

  /**
   * An advice is "change-advice" if it is declared and used (but is on the wrong configuration), AND is not
   * `compileOnly`.
   */
  fun isChange() = fromConfiguration != null && toConfiguration != null && !isCompileOnly()
}

data class AnalyzedClass(
  val className: String,
  val superClassName: String?,
  val retentionPolicy: RetentionPolicy?,
  /**
   * Ignoring constructors and static initializers. Suc a class will not prejudice the compileOnly algorithm against
   * declaring the containing jar "annotations-only". See for example `org.jetbrains.annotations.ApiStatus`. This
   * outer class only exists as a sort of "namespace" for the annotations it contains.
   */
  val hasNoMembers: Boolean,
  val access: Access,
  val methods: Set<Method>,
  val innerClasses: Set<String>
) : Comparable<AnalyzedClass> {

  constructor(
    className: String,
    superClassName: String?,
    retentionPolicy: String?,
    isAnnotation: Boolean,
    hasNoMembers: Boolean,
    access: Access,
    methods: Set<Method>,
    innerClasses: Set<String>
  ) : this(className, superClassName,
    fromString(retentionPolicy, isAnnotation), hasNoMembers, access, methods, innerClasses
  )

  companion object {
    fun fromString(name: String?, isAnnotation: Boolean): RetentionPolicy? = when {
      RetentionPolicy.CLASS.name == name -> RetentionPolicy.CLASS
      RetentionPolicy.SOURCE.name == name -> RetentionPolicy.SOURCE
      RetentionPolicy.RUNTIME.name == name -> RetentionPolicy.RUNTIME
      // Default if RetentionPolicy is not specified.
      isAnnotation -> RetentionPolicy.CLASS
      else -> null
    }
  }

  override fun compareTo(other: AnalyzedClass): Int = className.compareTo(other.className)
}

enum class Access {
  PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE;

  companion object {
    fun fromInt(access: Int): Access {
      return when {
        isPublic(access) -> PUBLIC
        isProtected(access) -> PROTECTED
        isPrivate(access) -> PRIVATE
        isPackagePrivate(access) -> PACKAGE_PRIVATE
        else -> throw IllegalArgumentException("Access <$access> is an unknown value")
      }
    }

    private fun isPackagePrivate(access: Int): Boolean =
      !isPublic(access) && !isPrivate(access) && !isProtected(access)

    private fun isPublic(access: Int): Boolean = access and Opcodes.ACC_PUBLIC != 0

    private fun isPrivate(access: Int): Boolean = access and Opcodes.ACC_PRIVATE != 0

    private fun isProtected(access: Int): Boolean = access and Opcodes.ACC_PROTECTED != 0
  }
}

data class Method internal constructor(val types: Set<String>) {

  constructor(descriptor: String) : this(findTypes(descriptor))

  companion object {
    private val DESCRIPTOR = Pattern.compile("L(.+?);")

    private fun findTypes(descriptor: String): Set<String> {
      val types = sortedSetOf<String>()
      val m = DESCRIPTOR.matcher(descriptor)
      while (m.find()) {
        types.add(m.group(1))
      }
      return types
    }
  }
}