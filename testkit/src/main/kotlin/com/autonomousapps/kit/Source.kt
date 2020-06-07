package com.autonomousapps.kit

class Source @JvmOverloads constructor(
  val sourceType: SourceType,
  val name: String,
  val path: String,
  val source: String,
  val sourceSet: String = "main"
) {

  override fun toString(): String {
    return source
  }
}
