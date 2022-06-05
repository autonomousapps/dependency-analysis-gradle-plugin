package com.autonomousapps


import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.utils.Files
import com.autonomousapps.model.Advice
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ProjectAdvice

abstract class AdviceStrategy {
  abstract List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject)

  abstract def actualBuildHealth(GradleProject gradleProject)

  abstract def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName)

  protected static BuildHealth fromBuildHealth(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(BuildHealth)
    return adapter.fromJson(json)
  }

  protected static Set<ProjectAdvice> fromAllProjectAdviceJson(String json) {
    return fromBuildHealth(json).projectAdvice
  }

  protected static ProjectAdvice fromProjectAdvice(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(ProjectAdvice)
    return adapter.fromJson(json)
  }

  static class V2 extends AdviceStrategy {

    @Override
    def actualBuildHealth(GradleProject gradleProject) {
      File buildHealth = Files.resolveFromRoot(gradleProject, OutputPathsKt.getFinalAdvicePathV2())
      return fromAllProjectAdviceJson(buildHealth.text)
    }

    @Override
    def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName) {
      File advice = Files.resolveFromName(gradleProject, projectName, OutputPathsKt.getAggregateAdvicePathV2())
      return fromProjectAdvice(advice.text)
    }

    @Override
    List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }
  }
}
