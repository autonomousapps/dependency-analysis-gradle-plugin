package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject

abstract class AbstractJvmSpec extends AbstractFunctionalSpec {

  protected GradleProject gradleProject = null

  /**
   * Set to `false` in a concrete class temporarily if you want to inspect the build output.
   */
  protected boolean shouldClean = true

  def cleanup() {
    if (gradleProject != null && shouldClean) {
      clean(gradleProject.rootDir)
    }
  }

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdvice(gradleProject)
  }

  String actualAdviceConsole() {
    return AdviceHelper.actualConsoleAdvice(gradleProject)
  }
}
