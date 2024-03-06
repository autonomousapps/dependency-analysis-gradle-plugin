// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import java.io.File

class SingleProject : ProjectDirProvider {

  private val rootSpec = RootSpec(buildScript = buildScript())

  private val rootProject = RootProject(rootSpec)

  override val projectDir: File = rootProject.projectDir

  override fun project(moduleName: String): Module {
    if (moduleName == ":") {
      return rootProject
    } else {
      error("No '$moduleName' project found!")
    }
  }

  companion object {
    private fun buildScript(): String {
      return """
        plugins {
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION}'
        }
        
        java {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        repositories {
          mavenCentral()
        }
        
        dependencies {
          api 'org.apache.commons:commons-math3:3.6.1'
          implementation 'com.google.guava:guava:28.2-jre'
        }
    """
    }

    @JvmStatic
    fun expectedAdvice() = setOf(
      Advice.ofRemove(
        ModuleCoordinates("com.google.guava:guava", "28.2-jre", GradleVariantIdentification.EMPTY),
        "implementation"
      ),
      Advice.ofRemove(
        ModuleCoordinates("org.apache.commons:commons-math3", "3.6.1", GradleVariantIdentification.EMPTY),
        "api"
      )
    )
  }
}
