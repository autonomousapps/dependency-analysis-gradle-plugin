package com.autonomousapps.kit

public class Source @JvmOverloads constructor(
  public val sourceType: SourceType,
  public val name: String,
  public val path: String,
  public val source: String,
  public val sourceSet: String = DEFAULT_SOURCE_SET,
  public val forceLanguage: String? = null,
) {

  public companion object {
    public const val DEFAULT_SOURCE_SET: String = "main"
  }

  internal fun rootPath(): String {
    forceLanguage ?: return "src/${sourceSet}/${sourceType.value}"
    return "src/$sourceSet/$forceLanguage"
  }

  override fun toString(): String {
    return source
  }
}
