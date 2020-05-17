package com.autonomousapps.android.projects

import com.autonomousapps.fixtures.AndroidLayout
import com.autonomousapps.fixtures.AndroidProject
import com.autonomousapps.fixtures.AppSpec
import com.autonomousapps.fixtures.AppType
import com.autonomousapps.fixtures.LibrarySpec
import com.autonomousapps.fixtures.LibraryType
import com.autonomousapps.fixtures.RootSpec

import static com.autonomousapps.fixtures.Dependencies.getDEFAULT_APP_DEPENDENCIES
import static com.autonomousapps.fixtures.Dependencies.getDEFAULT_LIB_DEPENDENCIES
import static com.autonomousapps.fixtures.Sources.getDEFAULT_APP_SOURCES

final class DefaultAndroidProject {

  DefaultAndroidProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(rootSpec, appSpec, librarySpecs)
  }

  private final String agpVersion
  private final AppSpec appSpec = new AppSpec(
    AppType.KOTLIN_ANDROID_APP,
    DEFAULT_APP_SOURCES,
    [] as Set<AndroidLayout>,
    DEFAULT_APP_DEPENDENCIES
  )
  private final List<LibrarySpec> librarySpecs = [
    new LibrarySpec(
      'lib',
      LibraryType.KOTLIN_ANDROID_LIB,
      false,
      DEFAULT_LIB_DEPENDENCIES,
      LibrarySpec.defaultSources(LibraryType.KOTLIN_ANDROID_LIB)
    ),
    new LibrarySpec(
      'java_lib',
      LibraryType.JAVA_JVM_LIB,
      false,
      DEFAULT_LIB_DEPENDENCIES,
      LibrarySpec.defaultSources(LibraryType.JAVA_JVM_LIB)
    ),
    new LibrarySpec(
      'kotlin_lib',
      LibraryType.KOTLIN_JVM_LIB,
      false,
      DEFAULT_LIB_DEPENDENCIES,
      LibrarySpec.defaultSources(LibraryType.KOTLIN_JVM_LIB)
    ),
  ]
  private final RootSpec rootSpec = new RootSpec(
    librarySpecs, "", RootSpec.defaultGradleProperties(), agpVersion,
    RootSpec.defaultSettingsScript(agpVersion, librarySpecs), RootSpec.defaultBuildScript(agpVersion, "")
  )
}
