// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.provider.Provider
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier

/**
 * This is different from [org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE], which has type
 * `Category` (cf `String`).
 */
internal val CATEGORY = Attribute.of("org.gradle.category", String::class.java)

private val attributeKey = Attribute.of("artifactType", String::class.java)

internal fun Configuration.artifactsFor(attrValue: String): ArtifactCollection = artifactViewFor(attrValue).artifacts

/**
 * Returns the component identifiers of the artifacts in this collection (sorted for consistency).
 * A task that writes these identifiers into its output must declare them as an [Input][org.gradle.api.tasks.Input].
 * Otherwise, the task would keep stale identifiers in its output, since a version bump with byte-identical files
 * wouldn't invalidate the file inputs.
 */
internal fun ArtifactCollection.identifiers(): Provider<List<String>> {
  return resolvedArtifacts.map { artifacts -> artifacts.map { it.id.componentIdentifier.displayName }.sorted() }
}

/** Captures things like the Gradle version catalog and Gradle API jar. */
internal fun Configuration.opaqueComponentArtifacts(): ArtifactCollection = incoming.artifactView { view ->
  view
    .componentFilter { id -> id is OpaqueComponentArtifactIdentifier }
    .lenient(true)
}.artifacts

private fun Configuration.artifactViewFor(attrValue: String): ArtifactView = incoming.artifactView {
  it.attributes.attribute(attributeKey, attrValue)
  it.lenient(true)
}

internal fun Configuration.externalArtifactsFor(attrValue: String): ArtifactCollection = externalArtifactViewFor(attrValue).artifacts

private fun Configuration.externalArtifactViewFor(attrValue: String): ArtifactView = incoming.artifactView {
  it.attributes.attribute(attributeKey, attrValue)
  it.lenient(true)
  // Only resolve external dependencies! Without this, all project dependencies will get _compiled_.
  it.componentFilter { id -> id is ModuleComponentIdentifier }
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
