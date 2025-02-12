// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects


import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

/**
 * https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1346.
 */
final class AndroidTransformProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidTransformProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer', 'com.example.consumer') { consumer ->
        consumer.manifest = libraryManifest('com.example.consumer')
        consumer.sources = consumerSources
        consumer.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(true, 'com.example.consumer')

          bs.dependencies(
            project('api', ':producer'),
          )
          bs.withGroovy(TRANSFORM_TASK)
        }
      }
      .withSubproject('producer') { producer ->
        producer.sources = producerSources
        producer.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion]
        }
      }
      .write()
  }

  private static TRANSFORM_TASK =
    """\
    import com.android.build.api.artifact.ScopedArtifact
    import com.android.build.api.variant.ScopedArtifacts
    import java.io.FileInputStream
    import java.io.FileOutputStream
    import java.nio.file.Files
    import java.nio.file.Paths
    import java.util.zip.ZipEntry
    import java.util.zip.ZipFile
    import java.util.zip.ZipOutputStream
    
    abstract class TransformTask extends DefaultTask {
      @PathSensitive(PathSensitivity.RELATIVE)
      @InputFiles
      abstract ListProperty<RegularFile> getAllJars();
    
      @PathSensitive(PathSensitivity.RELATIVE)
      @InputFiles
      abstract ListProperty<Directory> getAllDirs();
    
      @OutputFile
      abstract RegularFileProperty getOutput();
    
      @TaskAction
      void transform() {
        def outputFile = output.get().asFile
        def outputStream = new ZipOutputStream(new FileOutputStream(outputFile))
        try {
          allJars.get().forEach { jar ->
            addJarToZip(jar.asFile, outputStream)
          }
          allDirs.get().forEach { dir ->
            addDirectoryToZip(dir.asFile, outputStream, dir.asFile.path)
          }
        } finally {
          outputStream.close()
        }
    
        println("Copying \${allJars.get()} and \${allDirs.get()} into \${output.get()}")
        println("Resulting jar file contents:")
        def zipFile = new ZipFile(outputFile)
        try {
          zipFile.entries().each {
            println(it.name)
          }
        } finally {
          zipFile.close()
        }
      }
    
      void addJarToZip(File file, ZipOutputStream zipOut) {
        def zipFile = new ZipFile(file)
        zipFile.entries().each {
          def zipEntry = new ZipEntry(it.name)
          zipOut.putNextEntry(zipEntry)
          def inputStream = zipFile.getInputStream(it)
          try {
            inputStream.transferTo(zipOut)
          } finally {
            inputStream.close()
          }
        }
      }
    
      void addDirectoryToZip(File directory, ZipOutputStream zipOut, String basePath) {
        Files.walk(directory.toPath()).forEach {
          def file = it.toFile()
          if (file.isFile()) {
            def fileInputStream = new FileInputStream(file)
            try {
              def zipEntry = new ZipEntry(Paths.get(basePath).relativize(file.toPath()).toString())
              zipOut.putNextEntry(zipEntry)
              fileInputStream.transferTo(zipOut)
              zipOut.closeEntry()
            } finally {
              fileInputStream.close()
            }
          }
        }
      }
    }
    
    androidComponents {
      onVariants(selector().all()) { variant ->
        variant.artifacts
          .forScope(ScopedArtifacts.Scope.PROJECT)
          .use(tasks.register("transformTask\${variant.name.capitalize()}", TransformTask.class))
          .toTransform(ScopedArtifact.CLASSES.INSTANCE,  { it.getAllJars() }, { it.getAllDirs() }, { it.getOutput() })
      }
    }
    """.stripIndent()

  private List<Source> consumerSources = [
    Source.kotlin(
      """
      package com.example.consumer
      
      import com.example.producer.Producer
      
      class Consumer : Producer() {
        override fun produce(): String {
          return "Hello, world!"
        }
      }
      """.stripIndent()
    )
      .withPath('com.example.consumer', 'Consumer')
      .build()
  ]

  private List<Source> producerSources = [
    Source.kotlin(
      """
      package com.example.producer
      
      abstract class Producer {
        abstract fun produce(): String
      }
      """.stripIndent()
    )
      .withPath('com.example.producer', 'Producer')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth =
    emptyProjectAdviceFor(':consumer', ':producer')
}
