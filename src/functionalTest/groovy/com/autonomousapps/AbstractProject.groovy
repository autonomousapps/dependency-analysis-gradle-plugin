package com.autonomousapps

import com.autonomousapps.kit.GradleProject

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject {

  protected GradleProject.Builder newGradleProjectBuilder() {
    return new GradleProject.Builder(defaultFile())
  }

  protected GradleProject.Builder minimalAndroidProjectBuilder(String agpVersion) {
    return GradleProject.minimalAndroidProject(
      defaultFile(),
      agpVersion
    )
  }

  private static File defaultFile() {
    return new File("build/functionalTest/${UUID.randomUUID()}")
  }
}
