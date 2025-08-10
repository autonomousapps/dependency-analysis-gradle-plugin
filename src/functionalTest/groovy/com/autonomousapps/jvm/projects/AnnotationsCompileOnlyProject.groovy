// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

/**
 * Modeling a Gradle plugin was the only way I could (easily) figure out how to get bytecode like this:
 * <pre>
 * ClassAnalyzer#visit: com/example/ExamplePlugin$configureTask$checkTask$1$1 extends kotlin/jvm/internal/Lambda
 * - visitSource: source=/ExamplePlugin.kt debug=null
 * ClassAnalyzer#visitAnnotation: descriptor=Lkotlin/Metadata; visible=true
 * - AnnotationAnalyzer#visit: name=mv, value=(int[], [I@9948912)
 * - AnnotationAnalyzer#visit: name=k, value=(Integer, 3)
 * - AnnotationAnalyzer#visit: name=xi, value=(Integer, 48)
 * - AnnotationAnalyzer#visitArray: name=d1
 * - AnnotationAnalyzer#visit: name=null, value=(String, ...)
 * - AnnotationAnalyzer#visitArray: name=d2
 * - AnnotationAnalyzer#visit: name=null, value=(String, <anonymous>)
 * - AnnotationAnalyzer#visit: name=null, value=(String, Lorg/gradle/api/provider/Provider;)
 * - AnnotationAnalyzer#visit: name=null, value=(String, Lorg/gradle/api/file/RegularFile;)
 * - AnnotationAnalyzer#visit: name=null, value=(String, kotlin.jvm.PlatformType)
 * - AnnotationAnalyzer#visit: name=null, value=(String, Lorg/jetbrains/annotations/Nullable;)
 * - AnnotationAnalyzer#visit: name=null, value=(String, it)
 * - AnnotationAnalyzer#visit: name=null, value=(String, Lcom/example/tasks/CheckTask;)
 * - AnnotationAnalyzer#visit: name=null, value=(String, invoke)
 * </pre>
 *
 * Where in particular I needed the bytecode to contain a reference to {@code Lorg/jetbrains/annotations/Nullable;} in
 * an annotation array named "d2". The goal is for that reference to be treated as {@code compileOnly} and not a
 * required {@code implementation} dependency.
 */
final class AnnotationsCompileOnlyProject extends AbstractProject {

  final GradleProject gradleProject

  AnnotationsCompileOnlyProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = SOURCE_CONSUMER
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaGradle, Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion]
        }
      }
      .write()
  }

  private static final List<Source> SOURCE_CONSUMER = [
    Source.kotlin(
      '''\
        package com.example.consumer
  
        import org.gradle.api.*
        import org.gradle.api.file.*
        import org.gradle.api.provider.*
        import org.gradle.api.tasks.*
  
        abstract class Consumer : Plugin<Project> {
          
          private lateinit var myTask: TaskProvider<MyTask>
            
          override fun apply(project: Project): Unit = project.run {
            tasks.register("", MyTask::class.java) { t ->
              t.input.set(myTask.flatMap { it.output })
            }
          }
        }
        
        abstract class MyTask : DefaultTask() {
          
          @get:InputFile
          abstract val input: RegularFileProperty
          
          @get:OutputFile
          abstract val output: RegularFileProperty
          
          @TaskAction fun action() {}
        }
      '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
