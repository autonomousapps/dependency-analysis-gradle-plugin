package com.autonomousapps.kit.gradle

/**
 * The `android` block, for use by projects build with the Android Gradle Plugin.
 * ```
 * // build.gradle[.kts]
 * android {
 *   ...
 * }
 * ```
 */
class AndroidBlock(
  val content: String,
) {

  override fun toString(): String = content

  companion object {
    @JvmOverloads
    @JvmStatic
    fun defaultAndroidAppBlock(
      isKotlinApplied: Boolean = false,
      namespace: String? = null,
    ): AndroidBlock = AndroidBlock(
      """
      |android {
      |  ${namespace?.let { "namespace '$it'" }}
      |  compileSdkVersion 33
      |  defaultConfig {
      |    applicationId "com.example"
      |    minSdkVersion 21
      |    targetSdkVersion 29
      |    versionCode 1
      |    versionName "1.0"
      |  }
      |  compileOptions {
      |    sourceCompatibility JavaVersion.VERSION_1_8
      |    targetCompatibility JavaVersion.VERSION_1_8
      |  }
      |  ${kotlinOptions(isKotlinApplied)}
      |}
    """.trimMargin()
    )

    @JvmOverloads
    @JvmStatic
    fun defaultAndroidLibBlock(
      isKotlinApplied: Boolean = false,
      namespace: String? = null,
    ): AndroidBlock = AndroidBlock("""
      |android {
      |  ${namespace?.let { "namespace '$it'" } ?: ""}
      |  compileSdkVersion 33
      |  defaultConfig {
      |    minSdkVersion 21
      |    targetSdkVersion 29
      |    versionCode 1
      |    versionName "1.0"
      |  }
      |  compileOptions {
      |    sourceCompatibility JavaVersion.VERSION_1_8
      |    targetCompatibility JavaVersion.VERSION_1_8
      |  }
      |  ${kotlinOptions(isKotlinApplied)}
      |}
    """.trimMargin())

    private fun kotlinOptions(isKotlinApplied: Boolean): String {
      return if (isKotlinApplied) {
        "kotlinOptions {\n    jvmTarget = \"1.8\"\n  }"
      } else {
        ""
      }
    }
  }
}
