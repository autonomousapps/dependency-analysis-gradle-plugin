package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec

abstract class AbstractJvmSpec extends AbstractFunctionalSpec {

  /**
   * Set to `false` in a concrete class temporarily if you want to inspect the build output.
   */
  protected boolean shouldClean = true

  @SuppressWarnings('unused')
  def cleanup() {
    if (gradleProject != null && shouldClean) {
//      clean(gradleProject.rootDir)
    }
  }
}
