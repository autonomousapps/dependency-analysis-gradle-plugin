package com.autonomousapps.kit

/**
 * Represents the various languages supported by this library.
 */
public enum class SourceType(
  public val value: String,
  public val fileExtension: String
) {
  GRADLE_GROOVY_DSL("groovy", "gradle"),
  GRADLE_KOTLIN_DSL("kotlin", "gradle.kts"),

  GROOVY("groovy", "groovy"),
  JAVA("java", "java"),
  KOTLIN("kotlin", "kt"),
  SCALA("scala", "scala"),
}
