package com.autonomousapps.kit.gradle.dependencies

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.gradle.Dependency

class DependencyProvider(
  private val kotlinVersion: String,
) {

  private val pluginUnderTestVersion = AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION

  /*
   * Common configurations. TODO(tsr): add later.
   */

  /*
   * Frequently-used dependencies.
   */

  fun dagp(configurationName: String): Dependency {
    return Dependency(
      configurationName,
      "com.autonomousapps:dependency-analysis-gradle-plugin:$pluginUnderTestVersion"
    )
  }

  fun groovyStdlib(configurationName: String): Dependency {
    return Dependency(configurationName, "org.codehaus.groovy:groovy-all:2.4.15")
  }

  fun kotlinReflect(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  }

  fun kotlinStdLib(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  }

  fun kotlinStdlibJdk8(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  }

  fun kotlinStdlibJdk7(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
  }

  fun kotlinTestJunit(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
  }

  fun scalaStdlib(configuration: String): Dependency {
    return Dependency(configuration, "org.scala-lang:scala-library:2.13.1")
  }

  fun guava(configuration: String): Dependency {
    return Dependency(configuration, "com.google.guava:guava:28.2-jre")
  }

  fun commonsMath(configuration: String): Dependency {
    return Dependency(configuration, "org.apache.commons:commons-math3:3.6.1")
  }

  fun commonsIO(configuration: String): Dependency {
    return Dependency(configuration, "commons-io:commons-io:2.6")
  }

  fun commonsCollections(configuration: String): Dependency {
    return Dependency(configuration, "org.apache.commons:commons-collections4:4.4")
  }

  fun commonsText(configuration: String): Dependency {
    return Dependency(configuration, "org.apache.commons:commons-text:1.8")
  }

  fun clikt(configuration: String): Dependency {
    return Dependency(configuration, "com.github.ajalt.clikt:clikt:3.4.2")
  }

  fun conscryptUber(configuration: String): Dependency {
    return Dependency(configuration, "org.conscrypt:conscrypt-openjdk-uber:2.4.0")
  }

  fun kotestAssertions(configuration: String): Dependency {
    return Dependency(configuration, "io.kotest:kotest-assertions-core:4.6.0")
  }

  fun moshi(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.moshi:moshi:1.14.0")
  }

  fun moshiKotlin(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.moshi:moshi-kotlin:1.14.0")
  }

  fun moshiAdapters(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.moshi:moshi-adapters:1.14.0")
  }

  fun okio(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.okio:okio:2.6.0")
  }

  fun okio2(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.okio:okio:2.6.0")
  }

  fun okio3(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.okio:okio:3.0.0")
  }

  fun okHttp(configuration: String): Dependency {
    return Dependency(configuration, "com.squareup.okhttp3:okhttp:4.6.0")
  }

  fun appcompat(configuration: String): Dependency {
    return Dependency(configuration, "androidx.appcompat:appcompat:1.1.0")
  }

  fun androidxAnnotations(configuration: String): Dependency {
    return Dependency(configuration, "androidx.annotation:annotation:1.1.0")
  }

  fun composeMultiplatformRuntime(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.compose.runtime:runtime:1.0.1")
  }

  fun composeMultiplatformFoundation(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.compose.foundation:foundation:1.0.1")
  }

  fun coreKtx(configuration: String): Dependency {
    return Dependency(configuration, "androidx.core:core-ktx:1.1.0")
  }

  fun core(configuration: String): Dependency {
    return Dependency(configuration, "androidx.core:core:1.1.0")
  }

  fun navUiKtx(configuration: String): Dependency {
    return Dependency(configuration, "androidx.navigation:navigation-ui-ktx:2.1.0")
  }

  fun constraintLayout(configuration: String): Dependency {
    return Dependency(configuration, "androidx.constraintlayout:constraintlayout:1.1.3")
  }

  fun recyclerView(configuration: String): Dependency {
    return Dependency(configuration, "androidx.recyclerview:recyclerview:1.3.2")
  }

  fun swipeRefreshLayout(configuration: String): Dependency {
    return Dependency(configuration, "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
  }

  fun dagger(configuration: String): Dependency {
    return Dependency(configuration, "com.google.dagger:dagger:2.44.2")
  }

  fun daggerCompiler(configuration: String): Dependency {
    return Dependency(configuration, "com.google.dagger:dagger-compiler:2.44.2")
  }

  fun daggerAndroidCompiler(configuration: String): Dependency {
    return Dependency(configuration, "com.google.dagger:dagger-android-processor:2.44.2")
  }

  fun firebaseAnalytics(configuration: String): Dependency {
    return Dependency(configuration, "com.google.firebase:firebase-analytics:17.6.0")
  }

  fun firebaseAnalyticsKtx(configuration: String): Dependency {
    return Dependency(configuration, "com.google.firebase:firebase-analytics-ktx:21.0.0")
  }

  fun javaxInject(configuration: String): Dependency {
    return Dependency(configuration, "javax.inject:javax.inject:1")
  }

  fun jakartaInject(configuration: String): Dependency {
    return Dependency(configuration, "jakarta.inject:jakarta.inject-api:2.0.1")
  }

  fun javaxServlet(configuration: String): Dependency {
    return Dependency(configuration, "javax.servlet:javax.servlet-api:3.0.1")
  }

  fun jsr305(configuration: String): Dependency {
    return Dependency(configuration, "com.google.code.findbugs:jsr305:3.0.2")
  }

  fun kotlinxCoroutinesAndroid(configuration: String): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
  }

  @JvmOverloads
  fun kotlinxCoroutinesCore(configuration: String, target: String = ""): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-core$target:1.6.0")
  }

  @JvmOverloads
  fun kotlinxCoroutinesTest(configuration: String, target: String = ""): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-test$target:1.6.0")
  }

  @JvmOverloads
  fun kotlinxImmutable(configuration: String, target: String = ""): Dependency {
    return Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-collections-immutable$target:0.3.5")
  }

  fun jwThreeTenAbp(configuration: String): Dependency {
    return Dependency(configuration, "com.jakewharton.threetenabp:threetenabp:1.2.4")
  }

  fun mockitoCore(configuration: String): Dependency {
    return Dependency(configuration, "org.mockito.kotlin:mockito-core:4.0.0")
  }

  fun mockitoKotlin(configuration: String): Dependency {
    return Dependency(configuration, "org.mockito.kotlin:mockito-kotlin:4.0.0")
  }

  fun tpCompiler(configuration: String): Dependency {
    return Dependency(configuration, "com.github.stephanenicolas.toothpick:toothpick-compiler:3.1.0")
  }

  fun junit(configuration: String): Dependency {
    return Dependency(configuration, "junit:junit:4.13")
  }

  fun timber(configuration: String): Dependency {
    return Dependency(configuration, "com.jakewharton.timber:timber:4.7.1")
  }

  fun rxlint(configuration: String): Dependency {
    return Dependency(configuration, "nl.littlerobots.rxlint:rxlint:1.7.6")
  }

  fun openTelemetry(configuration: String): Dependency {
    return Dependency(configuration, "io.opentelemetry:opentelemetry-extension-annotations:1.11.0")
  }

  fun slf4j(configuration: String): Dependency {
    return Dependency(configuration, "org.slf4j:slf4j-api:2.0.3")
  }

  fun slf4jTests(configuration: String): Dependency {
    return Dependency(configuration, "org.slf4j:slf4j-api:2.0.3:tests")
  }

  fun androidJoda(configuration: String): Dependency {
    return Dependency(configuration, "net.danlew:android.joda:2.10.7.2")
  }

  fun jodaTimeNoTzdbClassifier(configuration: String): Dependency {
    return Dependency(configuration, "joda-time:joda-time:2.10.7:no-tzdb")
  }

  fun jodaTimeNoTzdbFeature(configuration: String): Dependency {
    return Dependency(configuration, "joda-time:joda-time:2.10.7", capability = "joda-time:joda-time-no-tzdb")
  }

  fun antlr(): Dependency {
    return Dependency("antlr", "org.antlr:antlr4:4.8-1")
  }
}
