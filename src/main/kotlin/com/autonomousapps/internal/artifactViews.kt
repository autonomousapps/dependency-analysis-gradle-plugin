package com.autonomousapps.internal

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category

/**
 * This is different than [org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE], which has type
 * `Category` (cf `String`).
 */
internal val CATEGORY = Attribute.of("org.gradle.category", String::class.java)

private val attributeKey = Attribute.of("artifactType", String::class.java)

internal fun Configuration.artifactsFor(attrValue: String): ArtifactCollection = artifactViewFor(attrValue).artifacts

private fun Configuration.artifactViewFor(attrValue: String): ArtifactView = incoming.artifactView {
  attributes.attribute(attributeKey, attrValue)
  lenient(true)
}

internal fun Configuration.externalArtifactsFor(attrValue: String): ArtifactCollection = externalArtifactViewFor(attrValue).artifacts

private fun Configuration.externalArtifactViewFor(attrValue: String): ArtifactView = incoming.artifactView {
  attributes.attribute(attributeKey, attrValue)
  lenient(true)
  // Only resolve external dependencies! Without this, all project dependencies will get _compiled_.
  componentFilter { id -> id is ModuleComponentIdentifier }
}

/**
 * Returns true if any of the variants are a kind of platform.
 */
internal fun ResolvedDependencyResult.isJavaPlatform(): Boolean = selected.variants.any { variant ->
  val category = variant.attributes.getAttribute(CATEGORY)
  category == Category.REGULAR_PLATFORM || category == Category.ENFORCED_PLATFORM
}

internal object ArtifactAttributes {
  const val ANDROID_CLASSES_JAR = "android-classes"
  const val ANDROID_JNI = "android-jni"
  const val ANDROID_LINT = "android-lint"

  /** This is only available on the _runtime_ classpath. */
  const val ANDROID_ASSETS = "android-assets"
}
