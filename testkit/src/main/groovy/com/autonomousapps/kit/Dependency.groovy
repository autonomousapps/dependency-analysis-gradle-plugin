package com.autonomousapps.kit

import static com.autonomousapps.kit.Plugin.KOTLIN_VERSION

final class Dependency {

  final String configuration, dependency

  Dependency(String configuration, String dependency) {
    this.configuration = configuration
    this.dependency = dependency
  }

  /*
   * Plugin classpaths
   */

  static Dependency androidPlugin(String version = '3.6.3') {
    new Dependency('classpath', "com.android.tools.build:gradle:$version")
  }

  /*
   * Libraries
   */

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
    return new Dependency(configuration,"com.squareup.okhttp3:okhttp:4.6.0")
  }

  static Dependency appcompat(String configuration) {
    return new Dependency(configuration, "androidx.appcompat:appcompat:1.1.0")
  }

  static Dependency constraintLayout(String configuration) {
    return new Dependency(configuration, "androidx.constraintlayout:constraintlayout:1.1.3")
  }

  static Dependency kotlinxCoroutines(String configuration) {
    return new Dependency(configuration, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5")
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
