package com.autonomousapps

import com.autonomousapps.kit.GradleProject

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject {

  private String className = getClass().simpleName

  protected GradleProject.Builder newGradleProjectBuilder() {
    return new GradleProject.Builder(defaultFile())
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
