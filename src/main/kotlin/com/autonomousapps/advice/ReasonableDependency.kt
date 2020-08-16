package com.autonomousapps.advice

import com.autonomousapps.internal.Component
import com.autonomousapps.internal.KtFile

/**
 * A sort of exploded [Dependency]; it includes a list of all the facilities it provides. It can be
 * "reasoned" about.
 */
data class ReasonableDependency(
  val dependency: Dependency,
  val isTransitive: Boolean,
  val isCompileOnly: Boolean,
  val isSecurityProvider: Boolean,

  // TODO these three have a lot of overlap. Model it somehow?
  val publicClasses: Set<String>?,
  val usedTransitiveClasses: Set<String>?,
  val classes: Set<String>,

  val constantFields: Map<String, Set<String>>,
  val ktFiles: Set<KtFile>,

  val providesInlineMembers: Boolean?,
  val providesConstants: Boolean?,
  val providesGeneralImports: Boolean?,
  val providesManifestComponents: Boolean?,
  val providesResByRes: Boolean?,
  val providesResBySource: Boolean?,
  val providesNativeLibs: Boolean?
) : Comparable<ReasonableDependency> {

  override fun compareTo(other: ReasonableDependency): Int = dependency.compareTo(other.dependency)

  class Builder(val dependency: Dependency) {
    var variants: Set<String> = emptySet()

    var component: Component? = null
    var usedTransitiveClasses: Set<String>? = null
    var publicClasses: Set<String>? = null
    var providesInlineMembers: Boolean? = null
    var providesConstants: Boolean? = null
    var providesGeneralImports: Boolean? = null
    var providesManifestComponents: Boolean? = null
    var providesResByRes: Boolean? = null
    var providesResBySource: Boolean? = null
    var providesNativeLibs: Boolean? = null

    fun build(): ReasonableDependency {
      val component = component ?: error("component required")
      val constantFields = component.constantFields.filter { (_, value) ->
        value.isNotEmpty()
      }

      return ReasonableDependency(
        dependency = dependency,
        isTransitive = component.isTransitive,
        isCompileOnly = component.isCompileOnlyAnnotations,
        isSecurityProvider = component.isSecurityProvider,

        publicClasses = publicClasses,
        usedTransitiveClasses = usedTransitiveClasses,
        classes = component.classes,

        constantFields = constantFields,
        ktFiles = component.ktFiles.toSet(),

        providesInlineMembers = providesInlineMembers,
        providesConstants = providesConstants,
        providesGeneralImports = providesGeneralImports,
        providesManifestComponents = providesManifestComponents,
        providesResByRes = providesResByRes,
        providesResBySource = providesResBySource,
        providesNativeLibs = providesNativeLibs
      )
    }
  }
}
