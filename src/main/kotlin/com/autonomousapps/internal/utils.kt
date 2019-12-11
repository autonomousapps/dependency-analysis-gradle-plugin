@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import java.util.*

internal fun String.capitalize() = substring(0, 1).toUpperCase(Locale.ROOT) + substring(1)

internal fun ComponentIdentifier.asString(): String {
    return when (this) {
        is ProjectComponentIdentifier -> projectPath
        is ModuleComponentIdentifier -> moduleIdentifier.toString()
        // OpaqueComponentArtifactIdentifier implements ComponentArtifactIdentifier, ComponentIdentifier
//        is ComponentArtifactIdentifier -> toString()
        else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
    }
}