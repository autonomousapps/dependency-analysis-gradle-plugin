// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.Flags
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier

/**
 * This is different from [org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE], which has type `Category`
 * (cf `String`).
 */
internal val CATEGORY = Attribute.of("org.gradle.category", String::class.java)

private val attributeKey = Attribute.of("artifactType", String::class.java)

internal fun Configuration.jsonArtifacts(): ArtifactCollection = artifactsFor(JSON)

internal fun Configuration.artifactsFor(attrValue: String): ArtifactCollection {
  return artifactViewFor(attrValue(attrValue)).artifacts
}

/** Internal json artifacts will be compressed to gz files when [Flags.compress] is true. */
private fun attrValue(value: String): String {
  return if (Flags.compress() && value == JSON) {
    GZ
  } else {
    value
  }
}

/** Captures things like the Gradle version catalog and Gradle API jar. */
internal fun Configuration.opaqueComponentArtifacts(): ArtifactCollection = incoming.artifactView { v ->
  v
    .componentFilter { id -> id is OpaqueComponentArtifactIdentifier }
    .lenient(true)
}.artifacts

private fun Configuration.artifactViewFor(attrValue: String): ArtifactView = incoming.artifactView { v ->
  v.attributes.attribute(attributeKey, attrValue)
  v.lenient(true)
}

internal fun Configuration.externalArtifactsFor(attrValue: String): ArtifactCollection =
  externalArtifactViewFor(attrValue).artifacts

private fun Configuration.externalArtifactViewFor(attrValue: String): ArtifactView = incoming.artifactView { v ->
  v.attributes.attribute(attributeKey, attrValue)
  v.lenient(true)
  // Only resolve external dependencies! Without this, all project dependencies will get _compiled_.
  v.componentFilter { id -> id is ModuleComponentIdentifier }
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
  const val DYLIB = "dylib"

  /** This is only available on the _runtime_ classpath. */
  const val ANDROID_ASSETS = "android-assets"
}
