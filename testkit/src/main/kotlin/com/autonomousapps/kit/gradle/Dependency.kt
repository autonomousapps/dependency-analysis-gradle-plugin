package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.gradle.Plugin.Companion.KOTLIN_VERSION
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Dependency @JvmOverloads constructor(
  public val configuration: String,
  private val dependency: String,
  private val ext: String? = null,
  private val capability: String? = null,
  private val isVersionCatalog: Boolean = false,
) : Element.Line {

  private val isProject = dependency.startsWith(":")

  public val identifier: String = if (isProject) dependency else dependency.substringBeforeLast(":")
  public val version: String? = if (isProject) null else dependency.substringAfterLast(":")

  override fun render(scribe: Scribe): String = scribe.line { s ->
    val text = when {
      // project dependency
      dependency.startsWith(':') -> "$configuration project('$dependency')"
      // function call
      dependency.endsWith("()") -> "$configuration $dependency"
      // Some kind of custom notation
      !dependency.contains(":") -> "$configuration $dependency"
      // version catalog reference
      isVersionCatalog -> "$configuration $dependency"

      // normal dependency
      else -> {
        // normal external dependencies
        if (ext == null) "$configuration '$dependency'"
        // flat dependencies, e.g. in a libs/ dir
        else "$configuration(name: '$dependency', ext: '$ext')"
      }
    }.let {
      when {
        // Note: 'testFixtures("...")' is a shorthand for 'requireCapabilities("...-test-fixtures")'
        capability == "test-fixtures" -> {
          it.replace("$configuration ", "$configuration testFixtures(") + ")"
        }

        capability != null -> {
          if (it.startsWith("$configuration ")) {
            it.replace("$configuration ", "$configuration(") +
              ") { capabilities { requireCapabilities('$capability') } }"
          } else {
            "$it { capabilities { requireCapabilities('$capability') } }"
          }
        }

        else -> it
      }
    }

    s.append(text)
  }

  override fun toString(): String {
    error("don't call toString()")
  }

  public companion object {

    @JvmStatic
    public fun api(dependency: String): Dependency {
      return Dependency("api", dependency)
    }

    @JvmStatic
    public fun compileOnly(dependency: String): Dependency {
      return Dependency("compileOnly", dependency)
    }

    @JvmStatic
    public fun compileOnlyApi(dependency: String): Dependency {
      return Dependency("compileOnlyApi", dependency)
    }

    @JvmStatic
    public fun implementation(dependency: String): Dependency {
      return Dependency("implementation", dependency)
    }

    @JvmStatic
    public fun runtimeOnly(dependency: String): Dependency {
      return Dependency("runtimeOnly", dependency)
    }

    @JvmStatic
    public fun testCompileOnly(dependency: String): Dependency {
      return Dependency("testCompileOnly", dependency)
    }

    @JvmStatic
    public fun testImplementation(dependency: String): Dependency {
      return Dependency("testImplementation", dependency)
    }

    @JvmStatic
    public fun testRuntimeOnly(dependency: String): Dependency {
      return Dependency("testRuntimeOnly", dependency)
    }

    /*
     * Plugin classpaths
     */

    @JvmStatic
    public fun androidPlugin(version: String = "3.6.3"): Dependency {
      return Dependency("classpath", "com.android.tools.build:gradle:$version")
    }

    /*
     * Libraries
     */

    @JvmStatic
    public fun versionCatalog(configuration: String, ref: String): Dependency {
      return Dependency(
        configuration = configuration,
        dependency = ref,
        isVersionCatalog = true
      )
    }

    @JvmStatic
    public fun dagp(configuration: String): Dependency {
      val version = System.getProperty("com.autonomousapps.pluginversion")
      return Dependency(
        configuration,
        "com.autonomousapps:dependency-analysis-gradle-plugin:$version"
      )
    }

    @JvmStatic
    public fun project(configuration: String, path: String): Dependency {
      return Dependency(configuration, path)
    }

    @JvmStatic
    public fun project(configuration: String, path: String, capability: String): Dependency {
      return Dependency(configuration, path, capability = capability)
    }

    @JvmStatic
    public fun groovyStdlib(configuration: String): Dependency {
      return Dependency(configuration, "org.codehaus.groovy:groovy-all:2.4.15")
    }

    @JvmStatic
    public fun kotlinStdLib(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION")
    }

    @JvmStatic
    public fun kotlinStdlibJdk8(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    }

    @JvmStatic
    public fun kotlinStdlibJdk7(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION")
    }

    @JvmStatic
    public fun kotlinTestJunit(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION")
    }

    @JvmStatic
    public fun scalaStdlib(configuration: String): Dependency {
      return Dependency(configuration, "org.scala-lang:scala-library:2.13.1")
    }

    @JvmStatic
    public fun guava(configuration: String): Dependency {
      return Dependency(configuration, "com.google.guava:guava:28.2-jre")
    }

    @JvmStatic
    public fun commonsMath(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-math3:3.6.1")
    }

    @JvmStatic
    public fun commonsIO(configuration: String): Dependency {
      return Dependency(configuration, "commons-io:commons-io:2.6")
    }

    @JvmStatic
    public fun commonsCollections(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-collections4:4.4")
    }

    @JvmStatic
    public fun commonsText(configuration: String): Dependency {
      return Dependency(configuration, "org.apache.commons:commons-text:1.8")
    }

    @JvmStatic
    public fun clikt(configuration: String): Dependency {
      return Dependency(configuration, "com.github.ajalt.clikt:clikt:3.4.2")
    }

    @JvmStatic
    public fun conscryptUber(configuration: String): Dependency {
      return Dependency(configuration, "org.conscrypt:conscrypt-openjdk-uber:2.4.0")
    }

    @JvmStatic
    public fun kotestAssertions(configuration: String): Dependency {
      return Dependency(configuration, "io.kotest:kotest-assertions-core:4.6.0")
    }

    @JvmStatic
    public fun moshi(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi:1.14.0")
    }

    @JvmStatic
    public fun moshiKotlin(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi-kotlin:1.14.0")
    }

    @JvmStatic
    public fun moshiAdapters(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.moshi:moshi-adapters:1.14.0")
    }

    @JvmStatic
    public fun okio(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okio:okio:2.6.0")
    }

    @JvmStatic
    public fun okio2(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okio:okio:2.6.0")
    }

    @JvmStatic
    public fun okio3(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okio:okio:3.0.0")
    }

    @JvmStatic
    public fun okHttp(configuration: String): Dependency {
      return Dependency(configuration, "com.squareup.okhttp3:okhttp:4.6.0")
    }

    @JvmStatic
    public fun appcompat(configuration: String): Dependency {
      return Dependency(configuration, "androidx.appcompat:appcompat:1.1.0")
    }

    @JvmStatic
    public fun androidxAnnotations(configuration: String): Dependency {
      return Dependency(configuration, "androidx.annotation:annotation:1.1.0")
    }

    @JvmStatic
    public fun composeMultiplatformRuntime(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.compose.runtime:runtime:1.0.1")
    }

    @JvmStatic
    public fun composeMultiplatformFoundation(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.compose.foundation:foundation:1.0.1")
    }

    @JvmStatic
    public fun coreKtx(configuration: String): Dependency {
      return Dependency(configuration, "androidx.core:core-ktx:1.1.0")
    }

    @JvmStatic
    public fun core(configuration: String): Dependency {
      return Dependency(configuration, "androidx.core:core:1.1.0")
    }

    @JvmStatic
    public fun navUiKtx(configuration: String): Dependency {
      return Dependency(configuration, "androidx.navigation:navigation-ui-ktx:2.1.0")
    }

    @JvmStatic
    public fun constraintLayout(configuration: String): Dependency {
      return Dependency(configuration, "androidx.constraintlayout:constraintlayout:1.1.3")
    }

    @JvmStatic
    public fun swipeRefreshLayout(configuration: String): Dependency {
      return Dependency(configuration, "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    }

    @JvmStatic
    public fun dagger(configuration: String): Dependency {
      return Dependency(configuration, "com.google.dagger:dagger:2.44.2")
    }

    @JvmStatic
    public fun daggerCompiler(configuration: String): Dependency {
      return Dependency(configuration, "com.google.dagger:dagger-compiler:2.44.2")
    }

    @JvmStatic
    public fun daggerAndroidCompiler(configuration: String): Dependency {
      return Dependency(configuration, "com.google.dagger:dagger-android-processor:2.44.2")
    }

    @JvmStatic
    public fun firebaseAnalytics(configuration: String): Dependency {
      return Dependency(configuration, "com.google.firebase:firebase-analytics:17.6.0")
    }

    @JvmStatic
    public fun firebaseAnalyticsKtx(configuration: String): Dependency {
      return Dependency(configuration, "com.google.firebase:firebase-analytics-ktx:21.0.0")
    }

    @JvmStatic
    public fun javaxInject(configuration: String): Dependency {
      return Dependency(configuration, "javax.inject:javax.inject:1")
    }

    @JvmStatic
    public fun jakartaInject(configuration: String): Dependency {
      return Dependency(configuration, "jakarta.inject:jakarta.inject-api:2.0.1")
    }

    @JvmStatic
    public fun javaxServlet(configuration: String): Dependency {
      return Dependency(configuration, "javax.servlet:javax.servlet-api:3.0.1")
    }

    @JvmStatic
    public fun jsr305(configuration: String): Dependency {
      return Dependency(configuration, "com.google.code.findbugs:jsr305:3.0.2")
    }

    @JvmStatic
    public fun kotlinxCoroutinesAndroid(configuration: String): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    }

    @JvmStatic
    @JvmOverloads
    public fun kotlinxCoroutinesCore(configuration: String, target: String = ""): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-core$target:1.6.0")
    }

    @JvmStatic
    @JvmOverloads
    public fun kotlinxCoroutinesTest(configuration: String, target: String = ""): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-test$target:1.6.0")
    }

    @JvmStatic
    @JvmOverloads
    public fun kotlinxImmutable(configuration: String, target: String = ""): Dependency {
      return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-collections-immutable$target:0.3.5")
    }

    @JvmStatic
    public fun jwThreeTenAbp(configuration: String): Dependency {
      return Dependency(configuration, "com.jakewharton.threetenabp:threetenabp:1.2.4")
    }

    @JvmStatic
    public fun mockitoCore(configuration: String): Dependency {
      return Dependency(configuration, "org.mockito.kotlin:mockito-core:4.0.0")
    }

    @JvmStatic
    public fun mockitoKotlin(configuration: String): Dependency {
      return Dependency(configuration, "org.mockito.kotlin:mockito-kotlin:4.0.0")
    }

    @JvmStatic
    public fun tpCompiler(configuration: String): Dependency {
      return Dependency(configuration, "com.github.stephanenicolas.toothpick:toothpick-compiler:3.1.0")
    }

    @JvmStatic
    public fun junit(configuration: String): Dependency {
      return Dependency(configuration, "junit:junit:4.13")
    }

    @JvmStatic
    public fun timber(configuration: String): Dependency {
      return Dependency(configuration, "com.jakewharton.timber:timber:4.7.1")
    }

    @JvmStatic
    public fun rxlint(configuration: String): Dependency {
      return Dependency(configuration, "nl.littlerobots.rxlint:rxlint:1.7.6")
    }

    @JvmStatic
    public fun openTelemetry(configuration: String): Dependency {
      return Dependency(configuration, "io.opentelemetry:opentelemetry-extension-annotations:1.11.0")
    }

    @JvmStatic
    public fun slf4j(configuration: String): Dependency {
      return Dependency(configuration, "org.slf4j:slf4j-api:2.0.3")
    }

    @JvmStatic
    public fun slf4jTests(configuration: String): Dependency {
      return Dependency(configuration, "org.slf4j:slf4j-api:2.0.3:tests")
    }

    @JvmStatic
    public fun androidJoda(configuration: String): Dependency {
      return Dependency(configuration, "net.danlew:android.joda:2.10.7.2")
    }

    @JvmStatic
    public fun jodaTimeNoTzdbClassifier(configuration: String): Dependency {
      return Dependency(configuration, "joda-time:joda-time:2.10.7:no-tzdb")
    }

    @JvmStatic
    public fun jodaTimeNoTzdbFeature(configuration: String): Dependency {
      return Dependency(configuration, "joda-time:joda-time:2.10.7", capability = "joda-time:joda-time-no-tzdb")
    }

    @JvmStatic
    public fun antlr(): Dependency {
      return Dependency("antlr", "org.antlr:antlr4:4.8-1")
    }

    @JvmStatic
    public fun raw(configuration: String, dependency: String): Dependency {
      check(!dependency.contains(":")) { "Not meant for normal dependencies. Was '$dependency'." }
      return Dependency(configuration, dependency)
    }
  }
}
