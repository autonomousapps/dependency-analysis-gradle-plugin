package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Source(
  /** Source file path relative to project dir (e.g. `src/main/com/foo/Bar.kt`). */
  open val relativePath: String
) : Comparable<Source> {

  override fun compareTo(other: Source): Int = when (this) {
    is AndroidResSource -> if (other !is AndroidResSource) -1 else defaultCompareTo(other)
    is CodeSource -> if (other !is CodeSource) 1 else defaultCompareTo(other)
  }

  private fun defaultCompareTo(other: Source): Int = relativePath.compareTo(other.relativePath)
}

/** A single `.class` file in this project. */
@TypeLabel("code")
@JsonClass(generateAdapter = false)
data class CodeSource(
  override val relativePath: String,
  val kind: Kind,
  val className: String,
  /** Every class discovered in the bytecode of [className]. */
  val usedClasses: Set<String>,
  /** Every class discovered in the bytecode of [className], and which is exposed as part of the ABI. */
  val exposedClasses: Set<String>,
  /** Every import in this source file. */
  val imports: Set<String>
) : Source(relativePath) {

  enum class Kind {
    JAVA,
    KOTLIN,

    /** Probably generated source. */
    UNKNOWN,
  }
}

/** A single `.xml` (Android resource) file in this project. */
@TypeLabel("android_res")
@JsonClass(generateAdapter = false)
data class AndroidResSource(
  override val relativePath: String,
  val styleParentRefs: Set<StyleParentRef>,
  val attrRefs: Set<AttrRef>,
  /** Layout files have class references. */
  val usedClasses: Set<String>
) : Source(relativePath) {

  /** The parent of a style resource, e.g. "Theme.AppCompat.Light.DarkActionBar". */
  data class StyleParentRef(val styleParent: String)

  /** * Any attribute that looks like a reference to another resource. */
  data class AttrRef(val type: String, val id: String) {
    companion object {

      private val TYPE_REGEX = Regex("""@(?:.+:)?(.+)/""")
      private val ATTR_REGEX = Regex("""\?(?:.+/)?(.+)""")

      /**
       * On consumer side, only get attrs from the XML document when:
       * 1. They're not an ID (don't start with `@+id` or `@id`)
       * 2. They're not a tools namespace (don't start with `tools:`)
       * 3. They're not a data binding expression (don't start with `@{` and end with `}`)
       * 4. Their value starts with `?`, like `?themeColor`.
       * 5. Their value starts with `@`, like `@drawable/`.
       *
       * Will return `null` if the map entry doesn't match an expected pattern.
       */
      fun from(mapEntry: Map.Entry<String, String>): AttrRef? {
        if (mapEntry.isId()) return null
        if (mapEntry.isToolsAttr()) return null
        if (mapEntry.isDataBindingExpression()) return null

        val id = mapEntry.value
        return when {
          id.startsWith('?') -> AttrRef(
            type = "attr",
            id = id.attr().replace('.', '_')
          )
          TYPE_REGEX.containsMatchIn(id) -> AttrRef(
            type = id.type(),
            // @drawable/some_drawable => some_drawable
            id = id.substringAfterLast('/').replace('.', '_')
          )
          // Swipe refresh layout defines an attr (https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:swiperefreshlayout/swiperefreshlayout/src/main/res-public/values/attrs.xml;l=19):
          //   <public type="attr" name="swipeRefreshLayoutProgressSpinnerBackgroundColor" format="color"/>
          // A consumer may provide a value for this attr:
          //   <item name="swipeRefreshLayoutProgressSpinnerBackgroundColor">...</item>
          // See ResSpec.detects attr usage in res file.
          mapEntry.key == "name" -> AttrRef(
            type = "attr",
            id = id.replace('.', '_')
          )
          else -> null
        }
      }

      private fun Map.Entry<String, String>.isId() = value.startsWith("@+") || value.startsWith("@id")
      private fun Map.Entry<String, String>.isToolsAttr() = key.startsWith("tools:")
      private fun Map.Entry<String, String>.isDataBindingExpression() = value.startsWith("@{") && value.endsWith("}")

      // @drawable/some_drawable => drawable
      // @android:drawable/some_drawable => drawable
      private fun String.type(): String = TYPE_REGEX.find(this)!!.groupValues[1]

      // ?themeColor => themeColor
      // ?attr/themeColor => themeColor
      private fun String.attr(): String = ATTR_REGEX.find(this)!!.groupValues[1]
    }
  }
}
