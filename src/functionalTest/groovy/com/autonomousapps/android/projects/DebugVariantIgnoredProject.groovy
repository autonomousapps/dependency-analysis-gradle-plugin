// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.Flags
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

final class DebugVariantIgnoredProject extends AbstractVariantProject {

  DebugVariantIgnoredProject(String agpVersion) {
    super(agpVersion)
  }

  private Set<Advice> appAdvice = [
    Advice.ofRemove(
      moduleCoordinates('org.apache.commons:commons-collections4', '4.4'), 'implementation'
    ),
  ]

  @Override
  protected GradleProperties getProjectGradleProperties() {
    return super.getProjectGradleProperties() + GradleProperties.of(
      "$Flags.FLAG_ANDROID_IGNORED_VARIANTS=debug"
    )
  }

  @Override
  Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdviceForDependencies(':app', appAdvice)
    ]
  }
}
