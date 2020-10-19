package com.autonomousapps.internal

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.attributes.Attribute

private val attributeKey: Attribute<String> = Attribute.of("artifactType", String::class.java)

internal fun ResolvableDependencies.artifactViewFor(attrValue: String): ArtifactView = artifactView {
  attributes.attribute(attributeKey, attrValue)
}
