package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repository

import static com.autonomousapps.kit.gradle.Dependency.*

final class PostProcessingProject extends AbstractProject {

  final GradleProject gradleProject

  PostProcessingProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withBuildSrc { s ->
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.repositories = [Repository.MAVEN_LOCAL, Repository.MAVEN_CENTRAL]
        bs.dependencies = [dagp('implementation')]
      }
      s.sources = [buildSrcSource()]
    }
    builder.withRootProject { s ->
      s.withBuildScript { bs ->
        bs.plugins = [
          Plugin.of(Plugin.dagpId),
          Plugin.kotlinPlugin(Plugin.KOTLIN_VERSION, false)
        ]
      }
    }
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

  private final Source buildSrcSource() {
    new Source(
      SourceType.JAVA, "PostTask", "",
      """\
      import com.autonomousapps.AbstractPostProcessingTask;
      import org.gradle.api.tasks.TaskAction;
      
      public abstract class PostTask extends AbstractPostProcessingTask {
        @TaskAction public void action() {
          System.out.println(projectAdvice());
        }
      }""".stripIndent()
    )
  }

  private final String POST_TASK =
    """\
    def postProcess = tasks.register("postProcess", PostTask)
    dependencyAnalysis.registerPostProcessingTask(postProcess)""".stripIndent()
}
