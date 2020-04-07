package com.autonomousapps.internal.android

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal interface AndroidGradlePlugin {
  fun getBundleTaskOutput(variantName: String): Provider<RegularFile>
  fun isViewBindingEnabled(): Boolean
  fun isDataBindingEnabled(): Boolean
}
