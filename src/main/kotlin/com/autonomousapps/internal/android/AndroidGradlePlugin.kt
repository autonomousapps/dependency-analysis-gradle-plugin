package com.autonomousapps.internal.android

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal interface AndroidGradlePlugin {
  fun getBundleTaskOutput(variantName: String): Provider<RegularFile>
  fun isViewBindingEnabled(): Boolean
  fun isDataBindingEnabled(): Boolean

  /**
   * The package name or "namespace" of this Android module.
   *
   * @see <a href="https://developer.android.com/reference/tools/gradle-api/4.2/com/android/build/api/variant/Variant#packageName:org.gradle.api.provider.Provider">AGP 4.2</a>
   * @see <a href="https://developer.android.com/reference/tools/gradle-api/7.0/com/android/build/api/variant/Variant#namespace">AGP 7.x</a>
   */
  fun namespace(): Provider<String>
}
