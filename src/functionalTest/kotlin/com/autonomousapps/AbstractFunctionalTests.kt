package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.fixtures.WORKSPACE
import com.autonomousapps.utils.TestMatrix
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.BeforeTest

abstract class AbstractFunctionalTests {

  protected val agpVersion = System.getProperty("com.autonomousapps.agpversion")
      ?: error("Must supply an AGP version")
  protected val testMatrix = TestMatrix(agpVersion)

  @BeforeTest fun cleanWorkspace() {
    // Same as androidProject.projectDir, but androidProject has not been instantiated yet
    FileUtils.deleteDirectory(File(WORKSPACE))
  }

  protected fun cleanup(projectDirProvider: ProjectDirProvider) {
    FileUtils.deleteDirectory(projectDirProvider.projectDir)
  }
}
