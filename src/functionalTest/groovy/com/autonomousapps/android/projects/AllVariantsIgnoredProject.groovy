package com.autonomousapps.android.projects

import com.autonomousapps.Flags
import com.autonomousapps.kit.GradleProperties
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

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
