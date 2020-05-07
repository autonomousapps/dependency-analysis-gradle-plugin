package com.autonomousapps.fixtures

import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin

import static com.autonomousapps.fixtures.jvm.Dependency.commonsMath
import static com.autonomousapps.fixtures.jvm.Dependency.guava

final class PostProcessingProject2 {

  final JvmProject jvmProject

  PostProcessingProject2() {
    this.jvmProject = build()
  }

  private static JvmProject build() {
    def builder = new JvmProject.Builder()

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
