// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject

abstract class AbstractKmpProject extends AbstractProject {
  /**
   * KGP < 2.3 fails with this, from {@code FileCollectionMap.generatedSourceDirectories()}.
   *
   * <pre>
   * NoSuchMethodError: 'org.gradle.api.file.SourceDirectorySet org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.getGeneratedKotlin()'
   * </pre>
   *
   * Setting this value to 2.3.x essentially sets a minimum value of both Kotlin (2.3.0) and Gradle (9.4.0) for KMP
   * projects. This seems acceptable.
   */
  protected static final String KOTLIN_VERSION = '2.3.21'

  AbstractKmpProject(String kotlinVersion) {
    super(kotlinVersion)
  }

  AbstractKmpProject(String kotlinVersion, String agpVersion) {
    super(kotlinVersion, agpVersion)
  }
}
