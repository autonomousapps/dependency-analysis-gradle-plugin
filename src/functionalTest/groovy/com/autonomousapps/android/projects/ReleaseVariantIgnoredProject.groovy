package com.autonomousapps.android.projects

import com.autonomousapps.Flags
import com.autonomousapps.kit.GradleProperties
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

class ReleaseVariantIgnoredProject extends AbstractVariantProject {

  ReleaseVariantIgnoredProject(String agpVersion) {
    super(agpVersion)
  }

  private Set<Advice> appAdvice = [
    Advice.ofRemove(
      moduleCoordinates('org.apache.commons:commons-math3', '3.6.1'), 'debugImplementation'
    )
  ]

  @Override
  protected GradleProperties getProjectGradleProperties() {
    return super.getProjectGradleProperties() + GradleProperties.of(
      "$Flags.FLAG_ANDROID_IGNORED_VARIANTS=release"
    )
  }

  @Override
  Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdviceForDependencies(':app', appAdvice)
    ]
  }
}
