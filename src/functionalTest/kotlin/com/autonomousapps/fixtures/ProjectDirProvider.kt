// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.internal.getAdvicePathV2
import com.autonomousapps.internal.getFinalAdvicePathV2
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.Advice
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ProjectAdvice
import java.io.File

interface ProjectDirProvider {

  val projectDir: File

  fun project(moduleName: String): Module

  fun adviceFor(spec: ModuleSpec): Set<Advice> = adviceFor(spec.name)

  fun adviceFor(moduleName: String): Set<Advice> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getAdvicePathV2()}")
      .readText()
      .fromJson<ProjectAdvice>()
      .dependencyAdvice
  }

  fun removeAdviceFor(spec: ModuleSpec): Set<String> {
    return removeAdviceFor(spec.name)
  }

  fun removeAdviceFor(moduleName: String): Set<String> {
    return adviceFor(moduleName).asSequence()
      .filter { it.isRemove() }
      .map { it.coordinates.identifier }
      .toSortedSet()
  }

  fun buildHealthFor(spec: ModuleSpec): Set<ProjectAdvice> = buildHealthFor(spec.name)

  fun buildHealthFor(moduleName: String): Set<ProjectAdvice> {
    return buildHealthForV2(moduleName)
  }

  private fun buildHealthForV2(moduleName: String): Set<ProjectAdvice> {
    val module = project(moduleName)
    return module.dir
      .resolve(buildHealthPath())
      .readText()
      .fromJson<BuildHealth>()
      .projectAdvice
  }

  private fun buildHealthPath() = "build/${getFinalAdvicePathV2()}"
}
