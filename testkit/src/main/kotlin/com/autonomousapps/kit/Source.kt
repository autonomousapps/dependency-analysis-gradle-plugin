package com.autonomousapps.kit

class Source @JvmOverloads constructor(
  val sourceType: SourceType,
  val name: String,
  val path: String,
  val source: String,
  val sourceSet: String = DEFAULT_SOURCE_SET,
  val forceLanguage: String? = null,
) {

  companion object {
    const val DEFAULT_SOURCE_SET = "main"
  }

  internal fun rootPath(): String {
    forceLanguage ?: return "src/${sourceSet}/${sourceType.value}"
    return "src/$sourceSet/$forceLanguage"
  }

  override fun toString(): String {
    return source
  }
}
