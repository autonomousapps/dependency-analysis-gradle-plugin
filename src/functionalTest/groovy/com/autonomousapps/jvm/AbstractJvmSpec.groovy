package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject

abstract class AbstractJvmSpec extends AbstractFunctionalSpec {

  /**
   * Set to `false` in a concrete class temporarily if you want to inspect the build output.
   */
  protected boolean shouldClean = true

  @SuppressWarnings('unused')
  def cleanup() {
    if (gradleProject != null && shouldClean) {
      clean(gradleProject.rootDir)
    }
  }
}
