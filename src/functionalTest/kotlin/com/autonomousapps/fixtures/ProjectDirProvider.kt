// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.Flags
import com.autonomousapps.internal.getAdvicePathV2
import com.autonomousapps.internal.getFinalAdvicePathV2
import com.autonomousapps.internal.utils.MOSHI
import com.autonomousapps.model.Advice
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ProjectAdvice
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import okio.source
import java.io.File

interface ProjectDirProvider {

  val projectDir: File

  fun project(moduleName: String): Module

  fun adviceFor(spec: ModuleSpec): Set<Advice> = adviceFor(spec.name)

  fun adviceFor(moduleName: String): Set<Advice> {
    return project(moduleName).dir.resolve(projectAdvicePath()).bufferRead().use { reader ->
      MOSHI.adapter(ProjectAdvice::class.java).fromJson(reader)!!.dependencyAdvice
    }
  }

  fun buildHealthFor(moduleName: String): Set<ProjectAdvice> = buildHealthForV2(moduleName)

  private fun buildHealthForV2(moduleName: String): Set<ProjectAdvice> {
    return project(moduleName).dir.resolve(buildHealthPath()).bufferRead().use { reader ->
      MOSHI.adapter(BuildHealth::class.java).fromJson(reader)!!.projectAdvice
    }
  }

  private fun projectAdvicePath() = "build/${getAdvicePathV2()}"
  private fun buildHealthPath() = "build/${getFinalAdvicePathV2()}"

  private fun File.bufferRead(): BufferedSource {
    return if (Flags.compress()) {
      GzipSource(source()).buffer()
    } else {
      source().buffer()
    }
  }
}
