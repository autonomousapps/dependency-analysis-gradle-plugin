package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

import static com.autonomousapps.kit.Dependency.commonsMath
import static com.autonomousapps.kit.Dependency.guava

final class PostProcessingProject2 {

  final GradleProject gradleProject

  PostProcessingProject2() {
    this.gradleProject = build()
  }

  private static GradleProject build() {
    def builder = new GradleProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]
    def dependencies = [guava('implementation'), commonsMath('api')]

    builder.addSubproject(plugins, dependencies, [], 'main', POST_TASK)

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final String POST_TASK =
    """\
    tasks.register("postProcess", PostTask) {
      input = dependencyAnalysis.adviceOutputFor("main")
    }
        
    abstract class PostTask extends DefaultTask {
            
      @InputFile
      RegularFileProperty input
            
      @TaskAction def action() {
        println(input.get().asFile.text)
      }
    }
    """.stripIndent()
}
