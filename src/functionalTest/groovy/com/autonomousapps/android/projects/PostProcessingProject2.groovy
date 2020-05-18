package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

import static com.autonomousapps.kit.Dependency.commonsMath
import static com.autonomousapps.kit.Dependency.guava

final class PostProcessingProject2 extends AbstractProject {

  final GradleProject gradleProject

  PostProcessingProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj-1') { s ->
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [guava('implementation'), commonsMath('api')]
        bs.additions = POST_TASK
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final String POST_TASK =
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
