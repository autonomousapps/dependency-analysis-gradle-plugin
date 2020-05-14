@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.HasDependency
import com.autonomousapps.internal.AndroidPublicRes.Line
import com.autonomousapps.internal.advice.ComputedAdvice
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.utils.asString
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.resolvedVersion
import org.gradle.api.artifacts.component.ComponentIdentifier
import java.io.File
import java.io.Serializable
import java.lang.annotation.RetentionPolicy
import java.util.regex.Pattern

/**
 * A tuple of an identifier (project or external module) and the name of the configuration on which it is declared.
 */
data class DependencyConfiguration(
  val identifier: String,
  val configurationName: String
) : Serializable

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
   * True if this dependency contains a class that extends [java.security.Provider].
   */
  val isSecurityProvider: Boolean = false,
  /**
   * The classes declared by this library.
   */
  val classes: Set<String>
) : Comparable<Component> {

  internal constructor(artifact: Artifact, analyzedJar: AnalyzedJar) : this(
    dependency = artifact.dependency,
    isTransitive = artifact.isTransitive!!,
    isCompileOnlyAnnotations = analyzedJar.isCompileOnlyCandidate(),
    isSecurityProvider = analyzedJar.isSecurityProvider,
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

data class AndroidPublicRes(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  val dependency: Dependency,
  /**
   * A list of "lines" ([Line]) that are in a `public.txt` file.
   */
  val lines: List<Line>
) : Comparable<AndroidPublicRes> {
  constructor(componentIdentifier: ComponentIdentifier, lines: List<Line>) : this(
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion()
    ),
    lines = lines
  )

  data class Line(val type: String, val value: String)

  override fun compareTo(other: AndroidPublicRes): Int = dependency.compareTo(other.dependency)
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
      return types.efficient()
    }
  }
}

data class AnnotationProcessor(
  val dependency: Dependency,
  val processor: String,
  val supportedAnnotationTypes: Set<String>
) : Comparable<AnnotationProcessor> {

  constructor(
    processor: String,
    supportedAnnotationTypes: Set<String>,
    componentIdentifier: ComponentIdentifier,
    candidates: Set<DependencyConfiguration>
  ) : this(
    processor = processor, supportedAnnotationTypes = supportedAnnotationTypes,
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion(),
      configurationName = candidates.find { it.identifier == componentIdentifier.asString() }?.configurationName
    )
  )

  override fun compareTo(other: AnnotationProcessor): Int {
    return dependency.compareTo(other.dependency)
  }
}

internal data class ServiceLoader(
  override val dependency: Dependency,
  val providerFile: String,
  val providerClasses: Set<String>
) : HasDependency, Comparable<ServiceLoader> {

  constructor(
    providerFile: String,
    providerClasses: Set<String>,
    componentIdentifier: ComponentIdentifier,
    candidates: Set<DependencyConfiguration>
  ) : this(
    providerFile = providerFile,
    providerClasses = providerClasses,
    dependency = Dependency(
      identifier = componentIdentifier.asString(),
      resolvedVersion = componentIdentifier.resolvedVersion(),
      configurationName = candidates.find { it.identifier == componentIdentifier.asString() }?.configurationName
    )
  )

  override fun compareTo(other: ServiceLoader): Int {
    return dependency.compareTo(other.dependency)
  }
}

internal data class ConsoleReport(
  val addToApiAdvice: Set<Dependency>,
  val addToImplAdvice: Set<Dependency>,
  val removeAdvice: Set<Dependency>,
  val changeToApiAdvice: Set<Dependency>,
  val changeToImplAdvice: Set<Dependency>,
  val compileOnlyDependencies: Set<Dependency>,
  val unusedProcsAdvice: Set<Dependency>
) {

  fun isEmpty() = addToApiAdvice.isEmpty() &&
    addToImplAdvice.isEmpty() &&
    removeAdvice.isEmpty() &&
    changeToApiAdvice.isEmpty() &&
    changeToImplAdvice.isEmpty() &&
    compileOnlyDependencies.isEmpty() &&
    unusedProcsAdvice.isEmpty()

  companion object {
    fun from(computedAdvice: ComputedAdvice): ConsoleReport {
      var addToApiAdvice = emptySet<Dependency>()
      var addToImplAdvice = emptySet<Dependency>()
      var removeAdvice = emptySet<Dependency>()
      var changeToApiAdvice = emptySet<Dependency>()
      var changeToImplAdvice = emptySet<Dependency>()
      var compileOnlyDependencies = emptySet<Dependency>()
      var unusedProcsAdvice = emptySet<Dependency>()

      if (!computedAdvice.filterRemove && computedAdvice.removeAdvice.isNotEmpty()) {
        removeAdvice = computedAdvice.removeAdvice.mapToSet { it.dependency }
      }

      val addAdvices = computedAdvice.addToApiAdvice + computedAdvice.addToImplAdvice
      if (!computedAdvice.filterAdd && addAdvices.isNotEmpty()) {
        addToApiAdvice = computedAdvice.addToApiAdvice.mapToSet { it.dependency }
        addToImplAdvice = computedAdvice.addToImplAdvice.mapToSet { it.dependency }
      }

      val changeAdvices = computedAdvice.changeToApiAdvice + computedAdvice.changeToImplAdvice
      if (!computedAdvice.filterChange && changeAdvices.isNotEmpty()) {
        changeToApiAdvice = computedAdvice.changeToApiAdvice.mapToSet { it.dependency }
        changeToImplAdvice = computedAdvice.changeToImplAdvice.mapToSet { it.dependency }
      }

      if (!computedAdvice.filterCompileOnly && computedAdvice.compileOnlyDependencies.isNotEmpty()) {
        compileOnlyDependencies = computedAdvice.compileOnlyAdvice.mapToSet { it.dependency }
      }

      // TODO add filter
      if (computedAdvice.unusedProcsAdvice.isNotEmpty()) {
        unusedProcsAdvice = computedAdvice.unusedProcsAdvice.mapToSet { it.dependency }
      }

      return ConsoleReport(
        addToApiAdvice,
        addToImplAdvice,
        removeAdvice,
        changeToApiAdvice,
        changeToImplAdvice,
        compileOnlyDependencies,
        unusedProcsAdvice
      )
    }
  }
}
