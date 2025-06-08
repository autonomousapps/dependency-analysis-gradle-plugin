package com.autonomousapps.convention.internal.kotlin

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.configureKotlin() {
  val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  val javaVersion = JavaLanguageVersion.of(versionCatalog.findVersion("java").orElseThrow().requiredVersion)

  extensions.configure(KotlinJvmProjectExtension::class.java) { k ->
    // k.compilerOptions {
    //   // In Gradle 8.12, the Kotlin language level is set to 1.8. Gradle embeds Kotlin 2.0.21.
    //   // https://docs.gradle.org/8.12.1/userguide/compatibility.html
    //   //apiVersion.set(KotlinVersion.KOTLIN_1_8)
    //
    //   // This shouldn't be necessary, I think, because we use the toolchain below.
    //   //jvmTarget.set(JvmTarget.fromTarget("11"))
    // }
    k.jvmToolchain { spec ->
      spec.languageVersion.set(javaVersion)
    }
  }
}
