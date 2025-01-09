// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.Flags
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.model.ProjectAdvice

final class AllVariantsIgnoredProject extends AbstractVariantProject {

  AllVariantsIgnoredProject(String agpVersion) {
    super(agpVersion)
  }

  @Override
  protected GradleProperties getProjectGradleProperties() {
    return super.getProjectGradleProperties() + GradleProperties.of(
      "$Flags.FLAG_ANDROID_IGNORED_VARIANTS=debug,release"
    )
  }

  @Override
  Set<ProjectAdvice> expectedBuildHealth() {
    return Collections.emptySet()
  }
}
