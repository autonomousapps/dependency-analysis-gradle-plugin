package com.autonomousapps.kit

final class AndroidBlock {

  final String content

  AndroidBlock(String content) {
    this.content = content
  }

  @Override
  String toString() {
    return content
  }

  static final defaultAndroidBlock(boolean isKotlinApplied = false) {
    return new AndroidBlock(
      """\
        android {
          compileSdkVersion 29
          defaultConfig {
            applicationId "com.example"
            minSdkVersion 21
            targetSdkVersion 29
            versionCode 1
            versionName "1.0"
        }
        compileOptions {
          sourceCompatibility JavaVersion.VERSION_1_8
          targetCompatibility JavaVersion.VERSION_1_8
        }
        ${kotlinOptions(isKotlinApplied)}
        }""".stripIndent()
    )
  }

  private static String kotlinOptions(boolean isKotlinApplied) {
    if (isKotlinApplied) {
      return """\
        kotlinOptions {
          jvmTarget = "1.8"
        }
        """.stripIndent()
    } else {
      return ''
    }
  }
}
