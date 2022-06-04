package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.AndroidResSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.w3c.dom.Document
import java.io.File
import javax.inject.Inject

/**
 * TODO this kdoc is out of date.
 *
 * This task takes two inputs:
 * 1. Android res files declared by this project (xml)
 * 2. artifacts of type "android-public-res" (public.txt)
 *
 * We can parse the first for elements that might be present in the second. For example, if we have
 * ```
 * <resources>
 *   <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
 * </resources>
 * ```
 * we can expect to find, in public.txt, this line, associated with the dependency that supplies it (in this case
 * `'androidx.appcompat:appcompat'`):
 * ```
 * style Theme_AppCompat_Light_DarkActionBar
 * ```
 */
@CacheableTask
abstract class XmlSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all resources references in this project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidLocalRes: ConfigurableFileCollection

  /** Android layout XML files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val layoutFiles: ConfigurableFileCollection

  /** AndroidManifest.xml files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val manifestFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  internal fun layouts(files: List<File>) {
    for (file in files) {
      layoutFiles.from(
        objects.fileTree().from(file)
          .matching {
            // At this point in the filtering, there's a mix of directories and files
            // Can't filter on file extension
            include { it.path.contains("layout") }
          }.files
          // At this point, we have only files. It is safe to filter on extension. We
          // only want XML files.
          .filter { it.extension == "xml" }
      )
    }
  }

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(XmlSourceExploderWorkAction::class.java) {
      projectDir.set(layout.projectDirectory)
      androidRes.setFrom(androidLocalRes)
      layouts.setFrom(layoutFiles)
      manifests.setFrom(manifestFiles)
      output.set(this@XmlSourceExploderTask.output)
    }
  }

  interface XmlSourceExploderParameters : WorkParameters {
    val projectDir: DirectoryProperty
    val androidRes: ConfigurableFileCollection
    val layouts: ConfigurableFileCollection
    val manifests: ConfigurableFileCollection
    val output: RegularFileProperty
  }

  abstract class XmlSourceExploderWorkAction : WorkAction<XmlSourceExploderParameters> {

    private val builders = mutableMapOf<String, AndroidResBuilder>()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val projectDir = parameters.projectDir.get().asFile
      val explodedLayouts = AndroidLayoutParser(
        layouts = parameters.layouts.files,
        projectDir = projectDir
      ).explodedLayouts
      val explodedResources = AndroidResParser(
        resources = parameters.androidRes,
        projectDir = projectDir
      ).androidResSource

      val explodedManifests = AndroidManifestParser(
        manifests = parameters.manifests,
        projectDir = projectDir
      ).explodedManifest

      explodedLayouts.forEach { explodedLayout ->
        builders.merge(
          explodedLayout.relativePath,
          AndroidResBuilder(explodedLayout.relativePath).apply {
            usedClasses.addAll(explodedLayout.usedClasses)
          },
          AndroidResBuilder::concat
        )
      }
      explodedResources.forEach { explodedRes ->
        builders.merge(
          explodedRes.relativePath,
          AndroidResBuilder(explodedRes.relativePath).apply {
            styleParentRefs.addAll(explodedRes.styleParentRefs)
            attrRefs.addAll(explodedRes.attrRefs)
          },
          AndroidResBuilder::concat
        )
      }
      explodedManifests.forEach { explodedManifest ->
        builders.merge(
          explodedManifest.relativePath,
          AndroidResBuilder(explodedManifest.relativePath).apply {
            usedClasses.add(explodedManifest.applicationName)
          },
          AndroidResBuilder::concat
        )
      }

      val androidResSource = builders.values.asSequence()
        .map { it.build() }
        .toSet()

      output.writeText(androidResSource.toJson())
    }
  }
}

private class AndroidLayoutParser(
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

private class AndroidResParser(
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
    return doc.attrs().entries.mapNotNullToSet { AndroidResSource.AttrRef.from(it) }
  }

  private fun extractContentReferencesFromResourceXml(doc: Document): Set<AndroidResSource.AttrRef> {
    return doc.contentReferences().entries.mapNotNullToSet { AndroidResSource.AttrRef.from(it) }
  }
}

private class AndroidManifestParser(
  manifests: Iterable<File>,
  projectDir: File
) {

  private val parser = ManifestParser()

  val explodedManifest: List<ExplodedManifest> = manifests
    .filter { it.exists() }
    .map { file ->
      val applicationName = parser.parse(file).applicationName
      ExplodedManifest(
        relativePath = file.toRelativeString(projectDir),
        applicationName = applicationName
      )
    }
    .filter { it.applicationName.isNotEmpty() }
}

private class AndroidResBuilder(private val relativePath: String) {

  // TODO sort these
  val styleParentRefs = mutableSetOf<AndroidResSource.StyleParentRef>()
  val attrRefs = mutableSetOf<AndroidResSource.AttrRef>()
  val usedClasses = mutableSetOf<String>()

  fun concat(other: AndroidResBuilder): AndroidResBuilder {
    styleParentRefs.addAll(other.styleParentRefs)
    attrRefs.addAll(other.attrRefs)
    usedClasses.addAll(other.usedClasses)
    return this
  }

  fun build(): AndroidResSource {
    return AndroidResSource(
      relativePath = relativePath,
      styleParentRefs = styleParentRefs,
      attrRefs = attrRefs,
      usedClasses = usedClasses
    )
  }
}

private class ExplodedLayout(
  val relativePath: String,
  val usedClasses: Set<String>
)

private class ExplodedRes(
  val relativePath: String,
  val styleParentRefs: Set<AndroidResSource.StyleParentRef>,
  val attrRefs: Set<AndroidResSource.AttrRef>
)

private class ExplodedManifest(
  val relativePath: String,
  val applicationName: String
)
