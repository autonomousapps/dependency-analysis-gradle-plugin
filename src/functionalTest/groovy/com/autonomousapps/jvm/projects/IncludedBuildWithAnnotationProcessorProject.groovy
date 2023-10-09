package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*

final class IncludedBuildWithAnnotationProcessorProject extends AbstractProject {

  final GradleProject gradleProject

  IncludedBuildWithAnnotationProcessorProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.settingsScript.additions = """\
        includeBuild 'processor-build'
        include 'user-of-processor'
      """.stripIndent()
    }
    builder.withSubprojectInIncludedBuild('processor-build', 'sub-processor1') { secondSub ->
      secondSub.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
                new Dependency('annotationProcessor', 'com.google.auto.service:auto-service:1.0-rc6'),
                new Dependency('compileOnly', 'com.google.auto.service:auto-service-annotations:1.0-rc6'),
        ]
        bs.repositories = [
          Repository.GOOGLE,
          Repository.MAVEN_CENTRAL
        ]
        bs.additions = """\
          group = 'my.custom.processor'
        """.stripIndent()
      }
      secondSub.sources = [
        new Source(
          SourceType.JAVA, 'Field', 'com/example/included/processor1',
          """\
        package com.example.included.processor1;
        
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.FIELD})
        public @interface Field {
        }
          """.stripIndent()
        ),
        new Source(
          SourceType.JAVA, 'Processor1', 'com/example/included/processor1',
          """\
        package com.example.included.processor1;
        
        import java.util.HashSet;
        import java.util.List;
        import java.util.Set;
        import javax.annotation.processing.AbstractProcessor;
        import javax.annotation.processing.Processor;
        import javax.annotation.processing.RoundEnvironment;
        import javax.lang.model.element.TypeElement;
        
        import com.google.auto.service.AutoService;

        @AutoService(Processor.class)
        public class Processor1 extends AbstractProcessor {
          @Override
          public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
            return true;
          }

          @Override
          public Set<String> getSupportedAnnotationTypes() {
            return new HashSet<>(List.of(Field.class.getName()));
          }
        }
          """.stripIndent()
        )
      ]
    }
    builder.withSubproject("user-of-processor", userOfProcessor -> {
      userOfProcessor.withBuildScript {bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          new Dependency('api', 'my.custom.processor:sub-processor1'),
          new Dependency('annotationProcessor', 'my.custom.processor:sub-processor1'),
        ]
      }

      userOfProcessor.sources = [
        new Source(
          SourceType.JAVA, 'UserOfProcessor', 'com/example/user/of/processor1',
          """\
        package com.example.user.of.processor1;
        
        import com.example.included.processor1.Field;

        public class UserOfProcessor {
          public @Field int field;
        }
          """.stripIndent()
        )
      ]
    })

    def project = builder.build()
    project.writer().write()
    return project
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':user-of-processor', [] as Set<Advice>)
  ]
}
