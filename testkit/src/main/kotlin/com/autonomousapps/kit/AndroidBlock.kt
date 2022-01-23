package com.autonomousapps.kit

class AndroidBlock(val content: String) {

  override fun toString(): String = content

  companion object {
    @JvmStatic
    fun defaultAndroidAppBlock(isKotlinApplied: Boolean): AndroidBlock {
      return AndroidBlock("""
        |android {
        |  compileSdkVersion 29
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
      """.trimMargin())
    }

    @JvmOverloads
    @JvmStatic
    fun defaultAndroidLibBlock(isKotlinApplied: Boolean = false): AndroidBlock {
      return AndroidBlock("""
        |android {
        |  compileSdkVersion 29
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
    }

    private fun kotlinOptions(isKotlinApplied: Boolean): String {
      return if (isKotlinApplied) {
        "kotlinOptions {\n    jvmTarget = \"1.8\"\n  }"
      } else {
        ""
      }
    }
  }
}
