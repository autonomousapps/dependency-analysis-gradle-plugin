// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.parse.AndroidResParser
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Source(
  /** Source file path relative to project dir (e.g. `src/main/com/foo/Bar.kt`). */
  open val relativePath: String,
) : Comparable<Source> {

  override fun compareTo(other: Source): Int = when (this) {
    is AndroidAssetSource -> {
      when (other) {
        is AndroidAssetSource -> defaultCompareTo(other)
        is AndroidResSource -> 1
        is CodeSource -> 1
      }
    }

    is AndroidResSource -> {
      when (other) {
        is AndroidAssetSource -> -1
        is AndroidResSource -> defaultCompareTo(other)
        is CodeSource -> 1
      }
    }

    is CodeSource -> {
      when (other) {
        is AndroidAssetSource -> -1
        is AndroidResSource -> -1
        is CodeSource -> defaultCompareTo(other)
      }
    }
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
  val imports: Set<String>,
) : Source(relativePath) {

  enum class Kind {
    JAVA,
    KOTLIN,
    GROOVY,
    SCALA,

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
  val usedClasses: Set<String>,
) : Source(relativePath) {

  @JsonClass(generateAdapter = false)
  /** The parent of a style resource, e.g. "Theme.AppCompat.Light.DarkActionBar". */
  data class StyleParentRef(val styleParent: String) : Comparable<StyleParentRef> {
    override fun compareTo(other: StyleParentRef): Int = styleParent.compareTo(other.styleParent)
  }

  /** * Any attribute that looks like a reference to another resource. */
  @JsonClass(generateAdapter = false)
  data class AttrRef(val type: String, val id: String) : Comparable<AttrRef> {

    override fun compareTo(other: AttrRef): Int = compareBy<AttrRef>(
      { it.type },
      { it.id }
    ).compare(this, other)

    companion object {

      /**
       * This will match references to resources `@[<package_name>:]<resource_type>/<resource_name>`:
       *
       * - `@drawable/foo`
       * - `@android:drawable/foo`
       *
       * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources#ResourcesFromXml">Accessing resources from XML</a>
       */
      private val TYPE_REGEX = Regex("""@(\w+:)?(?<type>\w+)/(\w+)""")

      /**
       * TODO(tsr): this regex is too permissive. I only want `@+id/...`, but I lazily just copied the above with a
       *  small tweak.
       *
       * This will match references to resources `@+[<package_name>:]<resource_type>/<resource_name>`:
       *
       * - `@+drawable/foo`
       * - `@+android:drawable/foo`
       *
       * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources#ResourcesFromXml">Accessing resources from XML</a>
       */
      private val NEW_ID_REGEX = Regex("""@\+(\w+:)?(?<type>\w+)/(\w+)""")

      /**
       * This will match references to style attributes `?[<package_name>:][<resource_type>/]<resource_name>`:
       *
       * - `?foo`
       * - `?attr/foo`
       * - `?android:foo`
       * - `?android:attr/foo`
       *
       * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources#ReferencesToThemeAttributes">Referencing style attributes</a>
       */
      private val ATTR_REGEX = Regex("""\?(\w+:)?(\w+/)?(?<attr>\w+)""")

      fun style(name: String): AttrRef? = if (name.isBlank()) null else AttrRef("style", name)


      internal fun from(mapEntry: Pair<String, String>, container: AndroidResParser.Container) {
        if (mapEntry.isNewId()) {
          newId(mapEntry)?.let { container.newIds += it }
        }

        from(mapEntry)?.let { container.attrRefs += it }
      }

      /**
       * On consumer side, only get attrs from the XML document when:
       * 1. They're not a new ID (don't start with `@+id`)
       * 2. They're not a tools namespace (don't start with `tools:`)
       * 3. They're not a data binding expression (don't start with `@{` and end with `}`)
       * 4. Their value starts with `?`, like `?themeColor`.
       * 5. Their value starts with `@`, like `@drawable/`.
       *
       * Will return `null` if the map entry doesn't match an expected pattern.
       */
      fun from(mapEntry: Pair<String, String>): AttrRef? {
        if (mapEntry.isNewId()) return null
        if (mapEntry.isToolsAttr()) return null
        if (mapEntry.isDataBindingExpression()) return null

        val id = mapEntry.second
        return when {
          ATTR_REGEX.matchEntire(id) != null -> AttrRef(
            type = "attr",
            id = id.attr().replace('.', '_')
          )

          TYPE_REGEX.matchEntire(id) != null -> AttrRef(
            type = id.type(),
            // @drawable/some_drawable => some_drawable
            id = id.substringAfterLast('/').replace('.', '_')
          )
          // Swipe refresh layout defines an attr (https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:swiperefreshlayout/swiperefreshlayout/src/main/res-public/values/attrs.xml;l=19):
          //   <public type="attr" name="swipeRefreshLayoutProgressSpinnerBackgroundColor" format="color"/>
          // A consumer may provide a value for this attr:
          //   <item name="swipeRefreshLayoutProgressSpinnerBackgroundColor">...</item>
          // See ResSpec.detects attr usage in res file.
          mapEntry.first == "name" -> AttrRef(
            type = "attr",
            id = id.replace('.', '_')
          )

          else -> null
        }
      }

      /**
       * Returns an [AttrRef] when [AttrRef.type] is a new id ("@+id"), so that we can strip references to that id in
       * the current res file being analyzed. Such references are local, not from a dependency.
       */
      fun newId(mapEntry: Pair<String, String>): AttrRef? {
        if (!mapEntry.isNewId()) return null

        val id = mapEntry.second
        return when {
          NEW_ID_REGEX.matchEntire(id) != null -> AttrRef(
            type = "id",
            // @drawable/some_drawable => some_drawable
            id = id.substringAfterLast('/').replace('.', '_')
          )

          else -> null
        }
      }

      private fun Pair<String, String>.isNewId() = second.startsWith("@+id")
      private fun Pair<String, String>.isToolsAttr() = first.startsWith("tools:")
      private fun Pair<String, String>.isDataBindingExpression() = first.startsWith("@{") && first.endsWith("}")

      // @drawable/some_drawable => drawable
      // @android:drawable/some_drawable => drawable
      private fun String.type(): String = TYPE_REGEX.find(this)!!.groups["type"]!!.value

      // ?themeColor => themeColor
      // ?attr/themeColor => themeColor
      private fun String.attr(): String = ATTR_REGEX.find(this)!!.groups["attr"]!!.value
    }
  }
}

@TypeLabel("android_assets")
@JsonClass(generateAdapter = false)
data class AndroidAssetSource(
  override val relativePath: String,
) : Source(relativePath)
