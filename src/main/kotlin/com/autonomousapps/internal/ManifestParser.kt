// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.document.mapToSet
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File

internal class ManifestParser(
  /** The namespace, or package name, as set by the Android DSL. May be empty. */
  private val namespace: String
) {

  class ParseResult(
    val packageName: String,
    /** The value of the `android:name` attribute. May be empty. */
    val applicationName: String,
    /** The values of the `android:theme` attributes of application and activities. May be empty. */
    val themes: Set<String>,
    val components: Map<String, Set<String>>
  )

  /**
   * The purpose of [allComponents] is to assist in the migration from the old to the new model.
   */
  @Throws(ManifestParseException::class)
  fun parse(manifest: File, allComponents: Boolean = false): ParseResult {
    val document = buildDocument(manifest)

    val packageName = packageName(manifest, document)
    val application = application(document)
    val applicationName = application?.getAttribute("android:name") ?: ""

    val applicationTheme = application?.theme()
    val activityThemes = application?.getElementsByTagName(Manifest.Component.ACTIVITY.tagName)?.mapToSet { it.theme() }?.filterNotNull() ?: emptySet()

    val services = application?.componentNames(Manifest.Component.SERVICE, packageName) ?: emptySet()
    val providers = application?.componentNames(Manifest.Component.PROVIDER, packageName) ?: emptySet()
    val activities = application?.componentNames(Manifest.Component.ACTIVITY, packageName) ?: emptySet()
    val receivers = application?.componentNames(Manifest.Component.RECEIVER, packageName) ?: emptySet()

    val componentsMapping = mutableMapOf<String, Set<String>>()

    // "service" is enough to catch LeakCanary, and "provider" makes sense in principle. Trying not to be too aggressive.
    if (services.isNotEmpty()) componentsMapping[Manifest.Component.SERVICE.mapKey] = services
    if (providers.isNotEmpty()) componentsMapping[Manifest.Component.PROVIDER.mapKey] = providers

    if (allComponents) {
      if (activities.isNotEmpty()) componentsMapping[Manifest.Component.ACTIVITY.mapKey] = activities
      if (receivers.isNotEmpty()) componentsMapping[Manifest.Component.RECEIVER.mapKey] = receivers
    }

    return ParseResult(
      packageName = packageName,
      applicationName = applicationName,
      themes = setOfNotNull(applicationTheme) + activityThemes,
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

  // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/700
  private fun packageName(manifest: File, document: Document): String {
    return namespace.ifEmpty {
      runCatching {
        document.getElementsByTagName("manifest").item(0)
          .attributes
          .getNamedItem("package")
          .nodeValue
      }.getOrElse { t ->
        throw if (t is NullPointerException) {
          ManifestParseException(
            "${manifest.path} has no 'package' attribute. You should use 'android.namespace' to set the package name " +
              "and remove the 'package' attribute from the main manifest, since that attribute is set for removal " +
              "with AGP 8.0.",
            t
          )
        } else {
          t
        }
      }
    }
  }

  private fun Node.theme(): String? {
    return attributes?.getNamedItem("android:theme")
      ?.nodeValue?.substringAfter("@style/")
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

  internal class ManifestParseException(
    msg: String, cause: Throwable
  ) : RuntimeException(msg, cause)
}
