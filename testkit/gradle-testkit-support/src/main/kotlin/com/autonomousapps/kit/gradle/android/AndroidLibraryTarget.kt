// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.gradle.kotlin.CompilerJvmTarget
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * In an Android KMP library:
 * ```
 * kotlin {
 *   // This block
 *   androidLibrary {
 *     namespace = "com.example.kmpfirstlib"
 *     compileSdk = 33
 *     minSdk = 24
 *
 *     withJava() // enable java compilation support
 *
 *     // "host" tests are unit tests
 *     withHostTest {
 *       isIncludeAndroidResources = true
 *     }
 *
 *     // instrumented tests
 *     withDeviceTest {
 *       instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 *       execution = "HOST"
 *     }
 *
 *     compilerOptions.configure {
 *       jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
 *     }
 *   }
 * }
 * ```
 *
 * @see <a href="https://developer.android.com/kotlin/multiplatform/plugin">Android KMP library configuration</a>
 */
public class AndroidLibraryTarget(
  private val namespace: String,
  private val compileSdk: Int,
  private val minSdk: Int,
  private val withJava: Boolean,
  private val hostTest: AndroidLibraryHostTest?,
  private val compilerJvmTarget: CompilerJvmTarget?,
  // TODO(tsr): withDeviceTest { ... }
) : Element.Block {

  override val name: String = "androidLibrary"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line { it.append("namespace = ").appendQuoted(namespace) }
    s.line { it.append("compileSdk = ").append(compileSdk) }
    s.line { it.append("minSdk = ").append(minSdk) }

    if (withJava) s.line { it.append("withJava()") }

    hostTest?.render(s)
    compilerJvmTarget?.render(s)
  }

  public class Builder {
    public var namespace: String? = null
    public var compileSdk: Int? = null
    public var minSdk: Int? = null
    public var withJava: Boolean = false
    public var compileTarget: Int? = null

    private var hostTestBuilder: AndroidLibraryHostTest.Builder? = null

    public fun withJava() {
      withJava = true
    }

    public fun withHostTest() {
      hostTestBuilder = AndroidLibraryHostTest.Builder()
    }

    public fun withHostTest(block: (AndroidLibraryHostTest.Builder) -> Unit) {
      val hostTestBuilder = hostTestBuilder ?: AndroidLibraryHostTest.Builder()
      block(hostTestBuilder)
      this.hostTestBuilder = hostTestBuilder
    }

    public fun build(): AndroidLibraryTarget {
      val namespace = requireNotNull(namespace) { "'namespace' must not be null" }
      val compileSdk = requireNotNull(compileSdk) { "'compileSdk' must not be null" }
      val minSdk = requireNotNull(minSdk) { "'minSdk' must not be null" }
      val jvmTarget = compileTarget?.let { CompilerJvmTarget(it) }

      return AndroidLibraryTarget(
        namespace = namespace,
        compileSdk = compileSdk,
        minSdk = minSdk,
        withJava = withJava,
        hostTest = hostTestBuilder?.build(),
        compilerJvmTarget = jvmTarget,
      )
    }
  }
}
