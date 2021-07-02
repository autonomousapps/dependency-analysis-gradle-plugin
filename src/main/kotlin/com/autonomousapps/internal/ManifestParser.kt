package com.autonomousapps.internal

import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.mapToSet
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

internal class ManifestParser {

  class ParseResult(
    val packageName: String,
    val components: Map<String, Set<String>>
  )

  fun parse(manifest: File): ParseResult {
    val document = buildDocument(manifest)

    val packageName = packageName(document)
    val application = application(document)
    // "service" is enough to catch LeakCanary, and "provider" makes sense in principle. Trying not to be too aggressive.
    val services = application?.componentNames(Manifest.Component.SERVICE, packageName) ?: emptySet()
    val providers = application?.componentNames(Manifest.Component.PROVIDER, packageName) ?: emptySet()

    val componentsMapping = mutableMapOf<String, Set<String>>()
    if (services.isNotEmpty()) componentsMapping[Manifest.Component.SERVICE.mapKey] = services
    if (providers.isNotEmpty()) componentsMapping[Manifest.Component.PROVIDER.mapKey] = providers

    return ParseResult(
      packageName = packageName,
      components = componentsMapping
    )
  }

  private fun application(document: Document): Element? {
    val elements = document.getElementsByTagName("application")
    return if (elements.length > 0) {
      elements.item(0) as Element
    } else {
      null
    }
  }

  private fun packageName(document: Document): String {
    return document.getElementsByTagName("manifest").item(0)
      .attributes
      .getNamedItem("package")
      .nodeValue
  }

  private fun Element.componentNames(
    component: Manifest.Component,
    packageName: String
  ): Set<String> {
    return getElementsByTagName(component.tagName)
      .mapToSet {
        it.attributes.getNamedItem(component.attrName).nodeValue.withPackageName(
          packageName
        )
      }
  }

  private fun String.withPackageName(packageName: String): String {
    return if (startsWith(".")) {
      // item name is relative, so prefix with the package name
      "$packageName$this"
    } else {
      // item name is absolute, so use it as-is
      this
    }
  }
}