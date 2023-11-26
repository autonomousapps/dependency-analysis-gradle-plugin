package com.autonomousapps.internal.parse

import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.ManifestParser.ManifestParseException
import com.autonomousapps.internal.ManifestParser.ParseResult
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.AndroidResSource
import org.w3c.dom.Document
import java.io.File

internal class AndroidLayoutParser(
  private val projectDir: File,
  private val layouts: Set<File>
) {

  val explodedLayouts: Set<ExplodedLayout> = parseLayouts()

  private fun parseLayouts(): Set<ExplodedLayout> {
    return layouts.asSequence()
      .map { layoutFile ->
        layoutFile to buildDocument(layoutFile).getElementsByTagName("*")
          .map { it.nodeName }
          .filterToOrderedSet { JAVA_FQCN_REGEX_DOTTY.matches(it) }
      }.map { (file, classes) ->
        ExplodedLayout(
          relativePath = file.toRelativeString(projectDir),
          usedClasses = classes
        )
      }
      .toSet()
  }
}

internal class AndroidResParser(
  projectDir: File,
  resources: Iterable<File>
) {

  val androidResSource: Set<ExplodedRes> = resources
    .map { it to buildDocument(it) }
    .mapToSet { (file, doc) ->
      ExplodedRes(
        relativePath = file.toRelativeString(projectDir),
        styleParentRefs = extractStyleParentsFromResourceXml(doc),
        attrRefs = extractAttrsFromResourceXml(doc) + extractContentReferencesFromResourceXml(doc)
      )
    }

  // e.g., "Theme.AppCompat.Light.DarkActionBar"
  private fun extractStyleParentsFromResourceXml(doc: Document) =
    doc.getElementsByTagName("style").mapNotNull {
      it.attributes.getNamedItem("parent")?.nodeValue
    }.mapToSet {
      // Transform Theme.AppCompat.Light.DarkActionBar to Theme_AppCompat_Light_DarkActionBar
      it.replace('.', '_')
    }.mapToSet {
      AndroidResSource.StyleParentRef(it)
    }

  private fun extractAttrsFromResourceXml(doc: Document): Set<AndroidResSource.AttrRef> {
    return doc.attrs().mapNotNullToSet { AndroidResSource.AttrRef.from(it) }
  }

  private fun extractContentReferencesFromResourceXml(doc: Document): Set<AndroidResSource.AttrRef> {
    return doc.contentReferences().entries.mapNotNullToSet { AndroidResSource.AttrRef.from(it.key to it.value) }
  }
}

internal class AndroidManifestParser(
  private val manifests: Iterable<File>,
  private val projectDir: File,
  private val namespace: String
) {

  private val parser = ManifestParser(namespace)
  val explodedManifests: List<ExplodedManifest> = compute()

  private fun compute(): List<ExplodedManifest> {
    // If we have a namespace defined by the Android DSL, it is safe to parse the manifests immediately.
    if (namespace.isNotBlank()) return parseManifests()

    // Otherwise, parsing may result in an exception. We attempt to parse each file. If there's a failure, we catch it
    // and put it in a queue for a second attempt. One of the manifests should parse correctly. We'll get the namespace
    // from that one (probably src/main/AndroidManifest.xml).
    val parseResults = mutableListOf<Pair<File, ParseResult>>()
    val malformedManifests = mutableListOf<File>()

    for (manifest in manifests) {
      if (!manifest.exists()) continue
      try {
        parseResults += manifest to parser.parse(manifest)
      } catch (_: ManifestParseException) {
        malformedManifests += manifest
      }
    }

    val mainNamespace = parseResults.firstOrNull()?.second?.packageName.orEmpty()
    val malformedParser = ManifestParser(mainNamespace)
    for (remainder in malformedManifests) {
      parseResults += remainder to malformedParser.parse(remainder)
    }

    return parseResults.toExplodedManifests()
  }

  private fun parseManifests(): List<ExplodedManifest> = manifests
    .filter { it.exists() }
    .map { it to parser.parse(it) }
    .toExplodedManifests()

  private fun Iterable<Pair<File, ParseResult>>.toExplodedManifests(): List<ExplodedManifest> {
    return map { it.toExplodedManifest() }
  }

  private fun Pair<File, ParseResult>.toExplodedManifest(): ExplodedManifest {
    val file = first
    val parseResult = second
    val applicationName = parseResult.applicationName
    val theme = AndroidResSource.AttrRef.style(parseResult.theme)
    return ExplodedManifest(
      relativePath = file.toRelativeString(projectDir),
      applicationName = applicationName,
      theme = theme,
    )
  }
}

internal class AndroidResBuilder(private val relativePath: String) {

  val styleParentRefs = mutableSetOf<AndroidResSource.StyleParentRef>()
  val attrRefs = mutableSetOf<AndroidResSource.AttrRef>()
  val usedClasses = mutableSetOf<String>()

  fun concat(other: AndroidResBuilder): AndroidResBuilder {
    styleParentRefs.addAll(other.styleParentRefs)
    attrRefs.addAll(other.attrRefs)
    usedClasses.addAll(other.usedClasses)
    return this
  }

  fun build() = AndroidResSource(
    relativePath = relativePath,
    styleParentRefs = styleParentRefs,
    attrRefs = attrRefs,
    usedClasses = usedClasses.toSortedSet()
  )
}

internal class ExplodedLayout(
  val relativePath: String,
  val usedClasses: Set<String>
)

internal class ExplodedRes(
  val relativePath: String,
  val styleParentRefs: Set<AndroidResSource.StyleParentRef>,
  val attrRefs: Set<AndroidResSource.AttrRef>
)

internal class ExplodedManifest(
  val relativePath: String,
  val applicationName: String,
  val theme: AndroidResSource.AttrRef?
)
