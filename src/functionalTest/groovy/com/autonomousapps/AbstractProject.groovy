package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject extends AbstractGradleProject {

  protected final androidAppPlugin = [Plugin.androidAppPlugin]
  protected final androidLibPlugin = [Plugin.androidLibPlugin]

  protected GradleProject.Builder minimalAndroidProjectBuilder(String agpVersion) {
    return GradleProject.minimalAndroidProject(
      rootDir.toFile(),
      agpVersion
    )
  }
}
