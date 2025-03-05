// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.dependencies

import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins.KOTLIN_VERSION

object Dependencies {

  private val provider = DependencyProvider(
    kotlinVersion = KOTLIN_VERSION,
  )

  @JvmStatic
  fun dagp(configuration: String): Dependency {
    return provider.dagp(configuration)
  }

  @JvmStatic
  fun groovyStdlib(configuration: String): Dependency {
    return provider.groovyStdlib(configuration)
  }

  @JvmStatic
  fun kotlinReflect(configuration: String): Dependency {
    return provider.kotlinReflect(configuration)
  }

  @JvmStatic
  fun kotlinStdLib(configuration: String): Dependency {
    return provider.kotlinStdLib(configuration)
  }

  @JvmStatic
  fun kotlinStdlibJdk8(configuration: String): Dependency {
    return provider.kotlinStdlibJdk8(configuration)
  }

  @JvmStatic
  fun kotlinStdlibJdk7(configuration: String): Dependency {
    return provider.kotlinStdlibJdk7(configuration)
  }

  @JvmStatic
  fun kotlinTestJunit(configuration: String): Dependency {
    return provider.kotlinTestJunit(configuration)
  }

  @JvmStatic
  fun scalaStdlib(configuration: String): Dependency {
    return provider.scalaStdlib(configuration)
  }

  @JvmStatic
  fun guava(configuration: String): Dependency {
    return provider.guava(configuration)
  }

  @JvmStatic
  fun commonsMath(configuration: String): Dependency {
    return provider.commonsMath(configuration)
  }

  @JvmStatic
  fun commonsIO(configuration: String): Dependency {
    return provider.commonsIO(configuration)
  }

  @JvmStatic
  fun commonsCollections(configuration: String): Dependency {
    return provider.commonsCollections(configuration)
  }

  @JvmStatic
  fun commonsText(configuration: String): Dependency {
    return provider.commonsText(configuration)
  }

  @JvmStatic
  fun clikt(configuration: String): Dependency {
    return provider.clikt(configuration)
  }

  @JvmStatic
  fun conscryptUber(configuration: String): Dependency {
    return provider.conscryptUber(configuration)
  }

  @JvmStatic
  fun kotestAssertions(configuration: String): Dependency {
    return provider.kotestAssertions(configuration)
  }

  @JvmStatic
  fun moshi(configuration: String): Dependency {
    return provider.moshi(configuration)
  }

  @JvmStatic
  fun moshiKotlin(configuration: String): Dependency {
    return provider.moshiKotlin(configuration)
  }

  @JvmStatic
  fun moshiAdapters(configuration: String): Dependency {
    return provider.moshiAdapters(configuration)
  }

  @JvmStatic
  fun okio(configuration: String): Dependency {
    return provider.okio(configuration)
  }

  @JvmStatic
  fun okio2(configuration: String): Dependency {
    return provider.okio2(configuration)
  }

  @JvmStatic
  fun okio3(configuration: String): Dependency {
    return provider.okio3(configuration)
  }

  @JvmStatic
  fun okHttp(configuration: String): Dependency {
    return provider.okHttp(configuration)
  }

  @JvmStatic
  fun appcompat(configuration: String): Dependency {
    return provider.appcompat(configuration)
  }

  @JvmStatic
  fun androidxAnnotations(configuration: String): Dependency {
    return provider.androidxAnnotations(configuration)
  }

  @JvmStatic
  fun composeMultiplatformRuntime(configuration: String): Dependency {
    return provider.composeMultiplatformRuntime(configuration)
  }

  @JvmStatic
  fun composeMultiplatformFoundation(configuration: String): Dependency {
    return provider.composeMultiplatformFoundation(configuration)
  }

  @JvmStatic
  fun coreKtx(configuration: String): Dependency {
    return provider.coreKtx(configuration)
  }

  @JvmStatic
  fun core(configuration: String): Dependency {
    return provider.core(configuration)
  }

  @JvmStatic
  fun navUiKtx(configuration: String): Dependency {
    return provider.navUiKtx(configuration)
  }

  @JvmStatic
  fun constraintLayout(configuration: String): Dependency {
    return provider.constraintLayout(configuration)
  }

  @JvmStatic
  fun recyclerView(configuration: String): Dependency {
    return provider.recyclerView(configuration)
  }

  @JvmStatic
  fun swipeRefreshLayout(configuration: String): Dependency {
    return provider.swipeRefreshLayout(configuration)
  }

  @JvmStatic
  fun dagger(configuration: String): Dependency {
    return provider.dagger(configuration)
  }

  @JvmStatic
  fun daggerCompiler(configuration: String): Dependency {
    return provider.daggerCompiler(configuration)
  }

  @JvmStatic
  fun daggerAndroidCompiler(configuration: String): Dependency {
    return provider.daggerAndroidCompiler(configuration)
  }

  @JvmStatic
  fun firebaseAnalytics(configuration: String): Dependency {
    return provider.firebaseAnalytics(configuration)
  }

  @JvmStatic
  fun firebaseAnalyticsKtx(configuration: String): Dependency {
    return provider.firebaseAnalyticsKtx(configuration)
  }

  @JvmStatic
  fun javaxInject(configuration: String): Dependency {
    return provider.javaxInject(configuration)
  }

  @JvmStatic
  fun jakartaInject(configuration: String): Dependency {
    return provider.jakartaInject(configuration)
  }

  @JvmStatic
  fun javaxServlet(configuration: String): Dependency {
    return provider.javaxServlet(configuration)
  }

  @JvmStatic
  fun jsr305(configuration: String): Dependency {
    return provider.jsr305(configuration)
  }

  @JvmStatic
  fun kotlinxCoroutinesAndroid(configuration: String): Dependency {
    return provider.kotlinxCoroutinesAndroid(configuration)
  }

  @JvmStatic
  @JvmOverloads
  fun kotlinxCoroutinesCore(configuration: String, target: String = ""): Dependency {
    return provider.kotlinxCoroutinesCore(configuration, target)
  }

  @JvmStatic
  @JvmOverloads
  fun kotlinxCoroutinesTest(configuration: String, target: String = ""): Dependency {
    return provider.kotlinxCoroutinesTest(configuration, target)
  }

  @JvmStatic
  @JvmOverloads
  fun kotlinxImmutable(configuration: String, target: String = ""): Dependency {
    return provider.kotlinxImmutable(configuration, target)
  }

  @JvmStatic
  fun jwThreeTenAbp(configuration: String): Dependency {
    return provider.jwThreeTenAbp(configuration)
  }

  @JvmStatic
  fun mockitoCore(configuration: String): Dependency {
    return provider.mockitoCore(configuration)
  }

  @JvmStatic
  fun mockitoKotlin(configuration: String): Dependency {
    return provider.mockitoKotlin(configuration)
  }

  @JvmStatic
  fun tpCompiler(configuration: String): Dependency {
    return provider.tpCompiler(configuration)
  }

  @JvmStatic
  fun junit(configuration: String): Dependency {
    return provider.junit(configuration)
  }

  @JvmStatic
  fun timber(configuration: String): Dependency {
    return provider.timber(configuration)
  }

  @JvmStatic
  fun rxlint(configuration: String): Dependency {
    return provider.rxlint(configuration)
  }

  @JvmStatic
  fun openTelemetry(configuration: String): Dependency {
    return provider.openTelemetry(configuration)
  }

  @JvmStatic
  fun slf4j(configuration: String): Dependency {
    return provider.slf4j(configuration)
  }

  @JvmStatic
  fun slf4jTests(configuration: String): Dependency {
    return provider.slf4jTests(configuration)
  }

  @JvmStatic
  fun androidJoda(configuration: String): Dependency {
    return provider.androidJoda(configuration)
  }

  @JvmStatic
  fun jodaTimeNoTzdbClassifier(configuration: String): Dependency {
    return provider.jodaTimeNoTzdbClassifier(configuration)
  }

  @JvmStatic
  fun jodaTimeNoTzdbFeature(configuration: String): Dependency {
    return provider.jodaTimeNoTzdbFeature(configuration)
  }

  @JvmStatic
  fun antlr(): Dependency {
    return provider.antlr()
  }
}
