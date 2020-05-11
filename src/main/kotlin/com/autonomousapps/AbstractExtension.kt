@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

abstract class AbstractExtension(private val objects: ObjectFactory) {

  private val adviceOutputs = mutableMapOf<String, RegularFileProperty>()

  internal fun storeAdviceOutput(variantName: String, provider: Provider<RegularFile>) {
    val output = objects.fileProperty().also {
      it.set(provider)
    }
    adviceOutputs[variantName] = output
  }

  fun adviceOutputFor(variantName: String): RegularFileProperty? {
    return adviceOutputs[variantName]
  }
}
