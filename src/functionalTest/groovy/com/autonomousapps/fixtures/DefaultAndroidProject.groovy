package com.autonomousapps.fixtures

import static com.autonomousapps.fixtures.Dependencies.*
import static com.autonomousapps.fixtures.Sources.getDEFAULT_APP_SOURCES

final class DefaultAndroidProject {

  private final String agpVersion
  private final AppSpec appSpec = new AppSpec(
      AppType.KOTLIN_ANDROID_APP,
      DEFAULT_APP_SOURCES,
      DEFAULT_APP_DEPENDENCIES
  )
  private final List<LibrarySpec> librarySpecs = [
      new LibrarySpec(
          'lib',
          LibraryType.KOTLIN_ANDROID_LIB,
          DEFAULT_LIB_DEPENDENCIES,
          LibrarySpec.defaultSources(LibraryType.KOTLIN_ANDROID_LIB)
      ),
      new LibrarySpec(
          'java_lib',
          LibraryType.JAVA_JVM_LIB,
          DEFAULT_LIB_DEPENDENCIES,
          LibrarySpec.defaultSources(LibraryType.JAVA_JVM_LIB)
      ),
      new LibrarySpec(
          'kotlin_lib',
          LibraryType.KOTLIN_JVM_LIB,
          DEFAULT_LIB_DEPENDENCIES,
          LibrarySpec.defaultSources(LibraryType.KOTLIN_JVM_LIB)
      ),
  ]

  DefaultAndroidProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(
        agpVersion,
        appSpec,
        librarySpecs,
        ''
    )
  }
}
