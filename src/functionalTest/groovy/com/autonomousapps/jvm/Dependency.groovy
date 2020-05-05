package com.autonomousapps.jvm

final class Dependency {

  final String configuration, dependency

  Dependency(String configuration, String dependency) {
    this.configuration = configuration
    this.dependency = dependency
  }

  @Override
  String toString() {
    return "$configuration '$dependency'"
  }
}
