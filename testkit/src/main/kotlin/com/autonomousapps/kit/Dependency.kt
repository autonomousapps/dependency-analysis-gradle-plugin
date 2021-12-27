package com.autonomousapps.kit

import com.autonomousapps.kit.Plugin.Companion.KOTLIN_VERSION

class Dependency @JvmOverloads constructor(
  val configuration: String,
  private val dependency: String,
  private val ext: String? = null
) {

  private val isProject = dependency.startsWith(":")

  val identifier = if (isProject) dependency else dependency.substringBeforeLast(":")
  val version = if (isProject) null else dependency.substringAfterLast(":")

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
    fun project(configuration: String, path: String): Dependency {
      return Dependency(configuration, path)
    }

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
    fun commonsText(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-text:1.8")
    }

    @JvmStatic
    fun conscryptUber(configuration: String): Dependency {
      return Dependency(configuration, "org.conscrypt:conscrypt-openjdk-uber:2.4.0")
    }

    @JvmStatic
    fun kotestAssertions(configuration: String): Dependency {
      return Dependency(configuration, "io.kotest:kotest-assertions-core-jvm:4.6.0")
    }

    @JvmStatic
    fun moshi(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi:1.11.0")
    }

    @JvmStatic
    fun moshiKotlin(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi-kotlin:1.11.0")
    }

    @JvmStatic
    fun moshiAdapters(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi-adapters:1.11.0")
    }

    @JvmStatic
    fun okio(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okio:okio:1.17.5")
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
    fun androidxAnnotations(configuration: String): Dependency {
      return Dependency(configuration, "androidx.annotation:annotation:1.1.0")
    }

    @JvmStatic
    fun coreKtx(configuration: String): Dependency {
      return Dependency(configuration, "androidx.core:core-ktx:1.1.0")
    }

    @JvmStatic
    fun core(configuration: String): Dependency {
      return Dependency(configuration, "androidx.core:core:1.1.0")
    }

    @JvmStatic
    fun navUiKtx(configuration: String): Dependency {
      return Dependency(configuration, "androidx.navigation:navigation-ui-ktx:2.1.0")
    }

    @JvmStatic
    fun constraintLayout(configuration: String): Dependency {
      return Dependency(configuration, "androidx.constraintlayout:constraintlayout:1.1.3")
    }

    @JvmStatic
    fun dagger(configuration: String): Dependency {
      return Dependency(configuration, "com.google.dagger:dagger:2.38.1")
    }

    @JvmStatic
    fun daggerCompiler(configuration: String): Dependency {
      return Dependency(configuration, "com.google.dagger:dagger-compiler:2.38.1")
    }

    @JvmStatic
    fun firebaseAnalytics(configuration: String): Dependency {
      return Dependency(configuration, "com.google.firebase:firebase-analytics:17.6.0")
    }

    @JvmStatic
    fun javaxInject(configuration: String): Dependency {
      return Dependency(configuration, "javax.inject:javax.inject:1")
    }

    @JvmStatic
    fun kotlinxCoroutinesAndroid(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5")
    }

    @JvmStatic
    fun kotlinxCoroutinesCore(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    }

    @JvmStatic
    fun jwThreeTenAbp(configuration: String): Dependency {
      return Dependency(configuration, "com.jakewharton.threetenabp:threetenabp:1.2.4")
    }

    @JvmStatic
    fun tpCompiler(configuration: String): Dependency {
      return Dependency(configuration, "com.github.stephanenicolas.toothpick:toothpick-compiler:3.1.0")
    }

    @JvmStatic
    fun junit(configuration: String): Dependency {
      return Dependency(configuration, "junit:junit:4.13")
    }

    @JvmStatic
    fun timber(configuration: String): Dependency {
      return Dependency(configuration, "com.jakewharton.timber:timber:4.7.1")
    }

    @JvmStatic
    fun rxlint(configuration: String): Dependency {
      return Dependency(configuration, "nl.littlerobots.rxlint:rxlint:1.7.6")
    }

    @JvmStatic
    fun antlr(): Dependency {
      return Dependency("antlr", "org.antlr:antlr4:4.8-1")
    }
  }

  override fun toString(): String =
    when {
      // project dependency
      dependency.startsWith(':') -> "$configuration(project('$dependency'))"
      // function call
      dependency.endsWith("()") -> "$configuration($dependency)"
      // normal dependency
      else -> {
        // normal external dependencies
        if (ext == null) "$configuration('$dependency')"
        // flat dependencies, e.g. in a libs/ dir
        else "$configuration(name: '$dependency', ext: '$ext')"
      }
    }
}
