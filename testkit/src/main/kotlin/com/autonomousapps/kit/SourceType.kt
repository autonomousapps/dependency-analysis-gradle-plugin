package com.autonomousapps.kit

enum class SourceType(
  val value: String,
  val fileExtension: String
) {
  JAVA("java", "java"),
  KOTLIN("kotlin", "kt"),
  GROOVY("groovy", "gradle")
}
