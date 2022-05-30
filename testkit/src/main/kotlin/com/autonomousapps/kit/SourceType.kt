package com.autonomousapps.kit

enum class SourceType(
  val value: String,
  val fileExtension: String
) {
  GRADLE_GROOVY_DSL("groovy", "gradle"),
  GROOVY("groovy", "groovy"),
  JAVA("java", "java"),
  KOTLIN("kotlin", "kt"),
  SCALA("scala", "scala"),
}
