package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.jvm.JvmProject

abstract class AbstractJvmSpec extends AbstractFunctionalSpec {

  protected JvmProject jvmProject = null

  /**
   * Set to `false` in a concrete class temporarily if you want to inspect the build output.
   */
  protected boolean shouldClean = true

  def cleanup() {
    if (jvmProject != null && shouldClean) {
      clean(jvmProject.rootDir)
    }
  }
}
