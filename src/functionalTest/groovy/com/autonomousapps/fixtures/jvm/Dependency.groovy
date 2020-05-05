package com.autonomousapps.fixtures.jvm

import static com.autonomousapps.fixtures.jvm.Plugin.KOTLIN_VERSION

final class Dependency {

  final String configuration, dependency

  Dependency(String configuration, String dependency) {
    this.configuration = configuration
    this.dependency = dependency
  }

  static Dependency kotlinStdlibJdk7(String configuration) {
    return new Dependency(
      configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION"
    )
  }

  @Override
  String toString() {
    return "$configuration '$dependency'"
  }
}
