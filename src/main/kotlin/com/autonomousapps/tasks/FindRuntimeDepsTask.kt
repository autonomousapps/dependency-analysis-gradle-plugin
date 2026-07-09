// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.intermediates.RuntimeDepsReport
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class FindRuntimeDepsTask : DefaultTask() {

  init {
    description = "Scans source and resources for runtime dependency references (Spring DI, Liquibase)"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val sourceFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val resourceFiles: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    val output = output.getAndDelete()
    val runtimeClasses = mutableSetOf<String>()
    val componentScanPackages = mutableSetOf<String>()

    // Scan source files for Spring annotations
    sourceFiles.files
      .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
      .forEach { file ->
        scanSourceFile(file, runtimeClasses, componentScanPackages)
      }

    // Scan resource files for Liquibase/Spring XML references
    resourceFiles.files
      .filter { it.isFile && (it.extension == "xml" || it.extension == "yaml" || it.extension == "yml") }
      .forEach { file ->
        scanResourceFile(file, runtimeClasses)
      }

    val report = RuntimeDepsReport(
      projectPath = projectPath.get(),
      runtimeReferencedClasses = runtimeClasses,
      componentScanPackages = componentScanPackages,
    )
    output.bufferWriteJson(report)
  }

  internal companion object {
    internal val IMPORT_PATTERN = Regex("""import\s+([\w.]+);?""")
    internal val PACKAGE_PATTERN = Regex("""package\s+([\w.]+)""")
    internal val COMPONENT_SCAN_PATTERN = Regex("""@ComponentScan\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
    internal val BEAN_METHOD_PATTERN = Regex("""@Bean[^}]*?(?:public|protected)?\s+([\w.<>]+)\s+\w+\s*\(""")
    internal val IMPORT_ANNOTATION_PATTERN = Regex("""@Import\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
    internal val STRING_LITERAL_PATTERN = Regex(""""([^"]+)"""")
    internal val CLASS_REF_PATTERN = Regex("""(\w+)\.class""")
    internal val LIQUIBASE_XML_PATTERN = Regex("""customChange\s+class\s*=\s*"([\w.]+)"""")
    internal val LIQUIBASE_YAML_PATTERN = Regex("""class:\s*([\w.]+)""")
    internal val SPRING_XML_BEAN_PATTERN = Regex("""<bean[^>]+class\s*=\s*"([\w.]+)"""")

    internal fun scanSourceFileContent(
      content: String,
      runtimeClasses: MutableSet<String>,
      componentScanPackages: MutableSet<String>,
    ) {
      val imports = IMPORT_PATTERN.findAll(content).map { it.groupValues[1] }.toList()
      val packageName = PACKAGE_PATTERN.find(content)?.groupValues?.get(1) ?: ""

      // 1. @ComponentScan
      COMPONENT_SCAN_PATTERN.findAll(content).forEach { match ->
        val value = match.groupValues[1]
        STRING_LITERAL_PATTERN.findAll(value).forEach { strMatch ->
          componentScanPackages.add(strMatch.groupValues[1])
        }
        CLASS_REF_PATTERN.findAll(value).forEach { classMatch ->
          val className = classMatch.groupValues[1]
          val fqcn = resolveClass(className, imports, packageName)
          if (fqcn != null) {
            componentScanPackages.add(fqcn.substringBeforeLast('.'))
          }
        }
      }

      // 2. @Bean return types
      BEAN_METHOD_PATTERN.findAll(content).forEach { match ->
        val returnType = match.groupValues[1].trim()
        if (returnType.isNotBlank() && returnType != "void") {
          val fqcn = resolveClass(returnType, imports, packageName)
          if (fqcn != null) {
            runtimeClasses.add(fqcn)
          }
        }
      }

      // 3. @Import classes
      IMPORT_ANNOTATION_PATTERN.findAll(content).forEach { match ->
        val value = match.groupValues[1]
        CLASS_REF_PATTERN.findAll(value).forEach { classMatch ->
          val className = classMatch.groupValues[1]
          val fqcn = resolveClass(className, imports, packageName)
          if (fqcn != null) {
            runtimeClasses.add(fqcn)
          }
        }
      }
    }

    internal fun scanResourceFileContent(content: String, runtimeClasses: MutableSet<String>) {
      LIQUIBASE_XML_PATTERN.findAll(content).forEach { match ->
        runtimeClasses.add(match.groupValues[1])
      }
      LIQUIBASE_YAML_PATTERN.findAll(content).forEach { match ->
        runtimeClasses.add(match.groupValues[1])
      }
      SPRING_XML_BEAN_PATTERN.findAll(content).forEach { match ->
        runtimeClasses.add(match.groupValues[1])
      }
    }

    internal fun resolveClass(simpleName: String, imports: List<String>, packageName: String): String? {
      if (simpleName.contains('.')) return simpleName
      val matchingImport = imports.find { it.endsWith(".$simpleName") }
      if (matchingImport != null) return matchingImport
      return if (packageName.isNotBlank()) "$packageName.$simpleName" else null
    }
  }

  private fun scanSourceFile(
    file: File,
    runtimeClasses: MutableSet<String>,
    componentScanPackages: MutableSet<String>,
  ) {
    scanSourceFileContent(file.readText(), runtimeClasses, componentScanPackages)
  }

  private fun scanResourceFile(file: File, runtimeClasses: MutableSet<String>) {
    scanResourceFileContent(file.readText(), runtimeClasses)
  }
}
