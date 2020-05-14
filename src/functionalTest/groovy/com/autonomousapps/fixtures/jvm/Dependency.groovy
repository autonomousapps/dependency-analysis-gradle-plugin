package com.autonomousapps.fixtures.jvm

import static com.autonomousapps.fixtures.jvm.Plugin.KOTLIN_VERSION

final class Dependency {

  final String configuration, dependency

  Dependency(String configuration, String dependency) {
    this.configuration = configuration
    this.dependency = dependency
  }

  static Dependency kotlinStdlib(String configuration) {
    return new Dependency(
      configuration, "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
    )
  }

  static Dependency kotlinStdlibJdk7(String configuration) {
    return new Dependency(
      configuration, "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION"
    )
  }

  static Dependency guava(String configuration) {
    return new Dependency(configuration, "com.google.guava:guava:28.2-jre")
  }

  static Dependency commonsMath(String configuration) {
    return new Dependency(configuration, "org.apache.commons:commons-math3:3.6.1")
  }

  static Dependency commonsIO(String configuration) {
    return new Dependency(configuration, "commons-io:commons-io:2.6")
  }

  static Dependency commonsCollections(String configuration) {
    return new Dependency(configuration, "org.apache.commons:commons-collections4:4.4")
  }

  static Dependency conscryptUber(String configuration) {
    return new Dependency(configuration, "org.conscrypt:conscrypt-openjdk-uber:2.4.0")
  }

  static Dependency okHttp(String configuration) {
    return new Dependency(configuration, "com.squareup.okhttp3:okhttp:4.6.0")
  }

  @Override
  String toString() {
    if (dependency.startsWith(':')) {
      // project dependency
      return "$configuration project('$dependency')"
    } else {
      return "$configuration '$dependency'"
    }
  }
}
