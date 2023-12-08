package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Capability : Comparable<Capability> {
  override fun compareTo(other: Capability): Int = javaClass.simpleName.compareTo(other.javaClass.simpleName)

  /**
   * This is for the JVM world, where sometimes multiple Jar files make up one component.
   * Subclasses implement this to merge details in a useful way.
   * It's implemented in all subclasses for completeness, although some situations might never occur in Android.
   */
  abstract fun merge(other: Capability): Capability
}

@TypeLabel("linter")
@JsonClass(generateAdapter = false)
data class AndroidLinterCapability(
  val lintRegistry: String,
  /** True if this dependency contains _only_ an Android lint jar/registry. */
  val isLintJar: Boolean,
) : Capability() {

  override fun merge(other: Capability): Capability {
    return AndroidLinterCapability(lintRegistry, isLintJar && (other as AndroidLinterCapability).isLintJar)
  }
}

@TypeLabel("manifest")
@JsonClass(generateAdapter = false)
data class AndroidManifestCapability(
  val componentMap: Map<Component, Set<String>>,
) : Capability() {

  enum class Component(val mapKey: String) {
    ACTIVITY("activities"),
    SERVICE("services"),
    RECEIVER("receivers"),
    PROVIDER("providers");

    companion object {
      internal fun of(mapKey: String): Component {
        return values().find {
          it.mapKey == mapKey
        } ?: error("Could not find Manifest.Component for $mapKey")
      }
    }
  }

  override fun merge(other: Capability): Capability {
    return AndroidManifestCapability(componentMap + (other as AndroidManifestCapability).componentMap)
  }
}

@TypeLabel("asset")
@JsonClass(generateAdapter = false)
data class AndroidAssetCapability(
  val assets: List<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return AndroidAssetCapability(assets + (other as AndroidAssetCapability).assets)
  }
}

@TypeLabel("res")
@JsonClass(generateAdapter = false)
data class AndroidResCapability(
  val rImport: String,
  val lines: List<Line>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class Line(val type: String, val value: String)

  override fun merge(other: Capability): Capability {
    return AndroidResCapability(
      rImport + (other as AndroidResCapability).rImport,
      lines + other.lines
    )
  }
}

@TypeLabel("proc")
@JsonClass(generateAdapter = false)
data class AnnotationProcessorCapability(
  val processor: String,
  val supportedAnnotationTypes: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return AnnotationProcessorCapability(
      processor, // other.processor ?
      supportedAnnotationTypes + (other as AnnotationProcessorCapability).supportedAnnotationTypes
    )
  }
}

@TypeLabel("class")
@JsonClass(generateAdapter = false)
data class ClassCapability(
  val classes: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return ClassCapability(classes + (other as ClassCapability).classes)
  }
}

@TypeLabel("const")
@JsonClass(generateAdapter = false)
data class ConstantCapability(
  /** Map of fully-qualified class names to constant field names. */
  val constants: Map<String, Set<String>>,
  /** Kotlin classes with top-level declarations. */
  val ktFiles: Set<KtFile>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return ConstantCapability(
      constants + (other as ConstantCapability).constants,
      ktFiles + other.ktFiles
    )
  }
}

@TypeLabel("inferred")
@JsonClass(generateAdapter = false)
data class InferredCapability(
  /**
   * True if this dependency contains only annotations that are only needed at compile-time (`CLASS` and `SOURCE` level
   * retention policies). False otherwise.
   */
  val isCompileOnlyAnnotations: Boolean,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return InferredCapability(isCompileOnlyAnnotations && (other as InferredCapability).isCompileOnlyAnnotations)
  }
}

@TypeLabel("inline")
@JsonClass(generateAdapter = false)
data class InlineMemberCapability(
  val inlineMembers: Set<InlineMember>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class InlineMember(
    val packageName: String,
    val inlineMembers: Set<String>,
  ) : Comparable<InlineMember> {
    override fun compareTo(other: InlineMember): Int = compareBy(InlineMember::packageName)
      .thenBy(LexicographicIterableComparator()) { it.inlineMembers }
      .compare(this, other)
  }

  override fun merge(other: Capability): Capability {
    return InlineMemberCapability(inlineMembers + (other as InlineMemberCapability).inlineMembers)
  }
}

@TypeLabel("typealias")
@JsonClass(generateAdapter = false)
data class TypealiasCapability(
  val typealiases: Set<Typealias>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class Typealias(
    val packageName: String,
    val typealiases: Set<Alias>,
  ) : Comparable<Typealias> {

    override fun compareTo(other: Typealias): Int = compareBy(Typealias::packageName)
      .thenBy(LexicographicIterableComparator()) { it.typealiases }
      .compare(this, other)

    @JsonClass(generateAdapter = false)
    data class Alias(
      val name: String,
      val expandedType: String,
    ) : Comparable<Alias> {
      override fun compareTo(other: Alias): Int = compareBy(Alias::name)
        .thenComparing(Alias::expandedType)
        .compare(this, other)
    }
  }

  override fun merge(other: Capability): Capability {
    return TypealiasCapability(typealiases + (other as TypealiasCapability).typealiases)
  }
}

@TypeLabel("native")
@JsonClass(generateAdapter = false)
data class NativeLibCapability(
  val fileNames: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return NativeLibCapability(fileNames + (other as NativeLibCapability).fileNames)
  }
}

@TypeLabel("service_loader")
@JsonClass(generateAdapter = false)
data class ServiceLoaderCapability(
  val providerFile: String,
  val providerClasses: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return ServiceLoaderCapability(
      providerFile + (other as ServiceLoaderCapability).providerFile, providerClasses + other.providerClasses
    )
  }
}

@TypeLabel("security_provider")
@JsonClass(generateAdapter = false)
data class SecurityProviderCapability(
  val securityProviders: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return SecurityProviderCapability(securityProviders + (other as SecurityProviderCapability).securityProviders)
  }
}
