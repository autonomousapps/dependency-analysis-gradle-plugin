package com.autonomousapps.android.projects

import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

class AllVariantsProject extends AbstractVariantProject {

  AllVariantsProject(String agpVersion) {
    super(agpVersion)
  }

  private Set<Advice> appAdvice = [
    Advice.ofChange(
      moduleCoordinates('org.apache.commons:commons-collections4', '4.4'), 'implementation', 'debugImplementation'
    ),
    Advice.ofRemove(
      moduleCoordinates('org.apache.commons:commons-math3', '3.6.1'), 'debugImplementation'
    )
  ]

  @Override
  Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdviceForDependencies(':app', appAdvice)
    ]
  }
}
