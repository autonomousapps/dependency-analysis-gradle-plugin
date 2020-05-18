package com.autonomousapps.kit

import com.autonomousapps.kit.Plugin.Companion.KOTLIN_VERSION

class Dependency(
  val configuration: String, val dependency: String
) {

  companion object {
    /*
     * Plugin classpaths
     */

    @JvmStatic
    fun androidPlugin(version: String = "3.6.3"): Dependency {
      return Dependency("classpath", "com.android.tools.build:gradle:$version")
    }

    /*
     * Libraries
     */

    @JvmStatic
    fun kotlinStdLib(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION")
    }

    @JvmStatic
    fun kotlinStdlibJdk7(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION")
    }

    @JvmStatic
    fun guava(configuration: String): Dependency {
      return Dependency(configuration, "com.google.guava:guava:28.2-jre")
    }

    @JvmStatic
    fun commonsMath(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-math3:3.6.1")
    }

    @JvmStatic
    fun commonsIO(configuration: String): Dependency {
      return Dependency(configuration, "commons-io:commons-io:2.6")
    }

    @JvmStatic
    fun commonsCollections(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-collections4:4.4")
    }

    @JvmStatic
    fun conscryptUber(configuration: String): Dependency {
      return Dependency(configuration, "org.conscrypt:conscrypt-openjdk-uber:2.4.0")
    }

    @JvmStatic
    fun okHttp(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okhttp3:okhttp:4.6.0")
    }

    @JvmStatic
    fun appcompat(configuration: String): Dependency {
      return Dependency(configuration, "androidx.appcompat:appcompat:1.1.0")
    }

    @JvmStatic
    fun constraintLayout(configuration: String): Dependency {
      return Dependency(configuration, "androidx.constraintlayout:constraintlayout:1.1.3")
    }

    @JvmStatic
    fun kotlinxCoroutines(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5")
    }
  }

  override fun toString(): String =
    if (dependency.startsWith(':')) {
      // project dependency
      "$configuration project('$dependency')"
    } else {
      "$configuration '$dependency'"
    }
}