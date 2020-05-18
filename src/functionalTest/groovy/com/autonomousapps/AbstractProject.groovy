package com.autonomousapps

import com.autonomousapps.kit.GradleProject

abstract class AbstractProject {

  protected GradleProject.Builder newGradleProjectBuilder() {
    return new GradleProject.Builder(new File("build/functionalTest/${UUID.randomUUID()}"))
  }
}
