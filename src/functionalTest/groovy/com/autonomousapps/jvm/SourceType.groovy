package com.autonomousapps.jvm

enum SourceType {
  JAVA('java', 'java'), KOTLIN('kotlin', 'kt')

  final String value, fileExtension

  SourceType(String value, String fileExtension) {
    this.value = value
    this.fileExtension = fileExtension
  }
}
