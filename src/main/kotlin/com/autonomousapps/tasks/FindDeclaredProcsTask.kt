package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ANNOTATION_PROCESSOR_PATH
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.intermediates.AnnotationProcessorDependency
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import java.io.BufferedReader
import java.io.File
import java.io.Writer
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.zip.ZipFile
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.naming.OperationNotSupportedException
import javax.tools.Diagnostic
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject

/**
 * Sketch of proc algo
 * 1. Gather all APs by looking at both kapt<Variant> and annotationProcessor<Variant>
 *    configurations
 * 2. Create a ClassLoader with all of the resultant jars on the classpath
 * 3. Look through each jar for META-INF/services/javax.annotation.processing.Processor, parsing
 *    that file for any APs in the jar.
 * 4. For each AP found in step 3, create instance via reflection and invoke
 *    `getSupportedAnnotationTypes()`. It may also be necessary to invoke `init()` beforehand.
 * 5. Associate each supported annotation type with its processor.
 * 6. Parse bytecode for presence of annotation types
 */
@CacheableTask
abstract class FindDeclaredProcsTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all supported annotation types and their annotation processors"

    if (GradleVersions.isAtLeastGradle74) {
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Cannot serialize unsafeLazy")
    }
  }

  private var kaptArtifacts: ArtifactCollection? = null
  private var annotationProcessorArtifacts: ArtifactCollection? = null

  fun setKaptArtifacts(artifacts: ArtifactCollection) {
    kaptArtifacts = artifacts
  }

  fun setAnnotationProcessorArtifacts(artifacts: ArtifactCollection) {
    annotationProcessorArtifacts = artifacts
  }

  @Optional
  @Classpath
  fun getKaptArtifactFiles(): FileCollection? = kaptArtifacts?.artifactFiles

  @Optional
  @Classpath
  fun getAnnotationProcessorArtifactFiles(): FileCollection? = annotationProcessorArtifacts?.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @delegate:Transient
  private val inMemoryCache by unsafeLazy { inMemoryCacheProvider.get() }

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val kaptClassLoader = newClassLoader("for-kapt", getKaptArtifactFiles())
    val apClassLoader = newClassLoader("for-annotation-processor", getAnnotationProcessorArtifactFiles())

    val kaptProcs = procs(kaptArtifacts, kaptClassLoader)
    val annotationProcessorProcs = procs(annotationProcessorArtifacts, apClassLoader)
    val procs = kaptProcs + annotationProcessorProcs

    outputFile.bufferWriteJsonList(procs)
  }

  private fun newClassLoader(name: String, files: FileCollection?): ClassLoader? {
    val urls = files?.toList()?.map { it.toURI().toURL() }?.toTypedArray()
    return urls?.let { FirstClassLoader(name, urls, javaClass.classLoader) }
  }

  private fun procs(artifacts: ArtifactCollection?, classLoader: ClassLoader?): List<AnnotationProcessorDependency> {
    if (artifacts == null) return emptyList()

    return artifacts.mapNotNull { artifact ->
      val procs = findProcs(artifact.file)
      if (procs != null) artifact to procs else null
    }.flatMap { (artifact, procs) ->
      procs.mapNotNull { procName ->
        inMemoryCache.proc(procName) ?: procFor(artifact, procName, classLoader!!).also { proc ->
          proc?.let { inMemoryCache.procs(procName, it) }
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun procFor(
    artifact: ResolvedArtifactResult,
    procName: String,
    classLoader: ClassLoader
  ): AnnotationProcessorDependency? {
    return try {
      val procClass = classLoader.loadClass(procName) as Class<out Processor>
      val types = getSupportedAnnotationTypes(procClass)
      types?.let { AnnotationProcessorDependency(procName, it, artifact) }
    } catch (_: ClassNotFoundException) {
      logger.warn("Could not load $procName from class loader")
      null
    }
  }

  private fun findProcs(file: File): List<String>? {
    val zip = ZipFile(file)
    return zip.getEntry(ANNOTATION_PROCESSOR_PATH)?.let {
      zip.getInputStream(it).bufferedReader().use(BufferedReader::readLines)
    }
  }

  private fun <T : Processor> getSupportedAnnotationTypes(procClass: Class<T>): Set<String>? = try {
    val proc = procClass.getDeclaredConstructor().newInstance()
    logger.debug("Trying to initialize annotation processor with type ${proc.javaClass.name}")
    tryInit(proc)
    proc.supportedAnnotationTypes.toSortedSet()
  } catch (_: Throwable) {
    logger.warn("Could not reflectively access processor class ${procClass.name}")
    null
  }

  private fun <T : Processor> tryInit(proc: T) {
    try {
      proc.init(StubProcessingEnvironment())
    } catch (_: Throwable) {
      logger.debug("Could not initialize ${proc.javaClass.name}. May not be able to get supported annotation types.")
    }
  }
}

private class StubProcessingEnvironment : ProcessingEnvironment {
  override fun getElementUtils(): Elements = StubElements()

  override fun getTypeUtils(): Types = StubTypes()

  override fun getMessager(): Messager = StubMessager()

  override fun getLocale(): Locale {
    throw OperationNotSupportedException()
  }

  override fun getSourceVersion(): SourceVersion = SourceVersion.latestSupported()

  override fun getOptions(): MutableMap<String, String> = mutableMapOf()

  override fun getFiler(): Filer = StubFiler()

  private class StubElements : Elements {
    override fun hides(hider: Element?, hidden: Element?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun overrides(overrider: ExecutableElement?, overridden: ExecutableElement?, type: TypeElement?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun getName(cs: CharSequence?): Name {
      throw OperationNotSupportedException()
    }

    override fun isFunctionalInterface(type: TypeElement?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun getElementValuesWithDefaults(a: AnnotationMirror?): MutableMap<out ExecutableElement, out AnnotationValue> {
      throw OperationNotSupportedException()
    }

    override fun getBinaryName(type: TypeElement?): Name {
      throw OperationNotSupportedException()
    }

    override fun getDocComment(e: Element?): String {
      throw OperationNotSupportedException()
    }

    override fun isDeprecated(e: Element?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun getAllMembers(type: TypeElement?): MutableList<out Element> {
      throw OperationNotSupportedException()
    }

    override fun printElements(w: Writer?, vararg elements: Element?) {
      throw OperationNotSupportedException()
    }

    override fun getPackageElement(name: CharSequence?): PackageElement {
      throw OperationNotSupportedException()
    }

    override fun getTypeElement(name: CharSequence?): TypeElement {
      throw OperationNotSupportedException()
    }

    override fun getConstantExpression(value: Any?): String {
      throw OperationNotSupportedException()
    }

    override fun getPackageOf(type: Element?): PackageElement {
      throw OperationNotSupportedException()
    }

    override fun getAllAnnotationMirrors(e: Element?): MutableList<out AnnotationMirror> {
      throw OperationNotSupportedException()
    }
  }

  private class StubMessager : Messager {
    override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?) {
      throw OperationNotSupportedException()
    }

    override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?, e: Element?) {
      throw OperationNotSupportedException()
    }

    override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?, e: Element?, a: AnnotationMirror?) {
      throw OperationNotSupportedException()
    }

    override fun printMessage(
      kind: Diagnostic.Kind?,
      msg: CharSequence?,
      e: Element?,
      a: AnnotationMirror?,
      v: AnnotationValue?
    ) {
      throw OperationNotSupportedException()
    }
  }

  private class StubFiler : Filer {
    override fun createSourceFile(name: CharSequence?, vararg originatingElements: Element?): JavaFileObject {
      throw OperationNotSupportedException()
    }

    override fun createClassFile(name: CharSequence?, vararg originatingElements: Element?): JavaFileObject {
      throw OperationNotSupportedException()
    }

    override fun getResource(
      location: JavaFileManager.Location?,
      pkg: CharSequence?,
      relativeName: CharSequence?
    ): FileObject {
      throw OperationNotSupportedException()
    }

    override fun createResource(
      location: JavaFileManager.Location?,
      pkg: CharSequence?,
      relativeName: CharSequence?,
      vararg originatingElements: Element?
    ): FileObject {
      throw OperationNotSupportedException()
    }
  }

  private class StubTypes : Types {
    override fun contains(t1: TypeMirror?, t2: TypeMirror?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun boxedClass(p: PrimitiveType?): TypeElement {
      throw OperationNotSupportedException()
    }

    override fun getArrayType(componentType: TypeMirror?): ArrayType {
      throw OperationNotSupportedException()
    }

    override fun getDeclaredType(typeElem: TypeElement?, vararg typeArgs: TypeMirror?): DeclaredType {
      throw OperationNotSupportedException()
    }

    override fun getDeclaredType(
      containing: DeclaredType?,
      typeElem: TypeElement?,
      vararg typeArgs: TypeMirror?
    ): DeclaredType {
      throw OperationNotSupportedException()
    }

    override fun isAssignable(t1: TypeMirror?, t2: TypeMirror?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun asMemberOf(containing: DeclaredType?, element: Element?): TypeMirror {
      throw OperationNotSupportedException()
    }

    override fun getNullType(): NullType {
      throw OperationNotSupportedException()
    }

    override fun getWildcardType(extendsBound: TypeMirror?, superBound: TypeMirror?): WildcardType {
      throw OperationNotSupportedException()
    }

    override fun unboxedType(t: TypeMirror?): PrimitiveType {
      throw OperationNotSupportedException()
    }

    override fun isSameType(t1: TypeMirror?, t2: TypeMirror?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun getPrimitiveType(kind: TypeKind?): PrimitiveType {
      throw OperationNotSupportedException()
    }

    override fun getNoType(kind: TypeKind?): NoType {
      throw OperationNotSupportedException()
    }

    override fun isSubsignature(m1: ExecutableType?, m2: ExecutableType?): Boolean {
      throw OperationNotSupportedException()
    }

    override fun capture(t: TypeMirror?): TypeMirror {
      throw OperationNotSupportedException()
    }

    override fun erasure(t: TypeMirror?): TypeMirror {
      throw OperationNotSupportedException()
    }

    override fun asElement(t: TypeMirror?): Element {
      throw OperationNotSupportedException()
    }

    override fun directSupertypes(t: TypeMirror?): MutableList<out TypeMirror> {
      throw OperationNotSupportedException()
    }

    override fun isSubtype(t1: TypeMirror?, t2: TypeMirror?): Boolean {
      throw OperationNotSupportedException()
    }
  }
}

/**
 * Invert normal Java rules and try to load classes from this classloader before checking the parent.
 *
 * This resolves [https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/479].
 */
private class FirstClassLoader(
  name: String,
  urls: Array<URL>,
  parent: ClassLoader
) : URLClassLoader(name, urls, parent) {
  override fun loadClass(name: String): Class<*> = try {
    findLoadedClass(name) ?: findClass(name)
  } catch (_: ClassNotFoundException) {
    super.loadClass(name)
  }
}
