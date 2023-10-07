package com.autonomousapps

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject {

  private String className = getClass().simpleName
  protected final androidAppPlugin = [Plugin.androidAppPlugin]
  protected final androidLibPlugin = [Plugin.androidLibPlugin]

  protected GradleProject.Builder newGradleProjectBuilder() {
    return new GradleProject.Builder(defaultFile(), GradleProject.DslKind.GROOVY)
  }

  protected GradleProject.Builder minimalAndroidProjectBuilder(String agpVersion) {
    return GradleProject.minimalAndroidProject(
      defaultFile(),
      agpVersion
    )
  }

  private File defaultFile() {
    return new File("build/functionalTest/${newSlug()}")
  }

  // Very similar to what is in RootProject
  private String newSlug() {
    def worker = System.getProperty('org.gradle.test.worker') ?: ''
    if (!worker.isEmpty()) {
      worker = "-$worker"
    }
    return "$className-${UUID.randomUUID().toString().take(16)}$worker"
  }
}
