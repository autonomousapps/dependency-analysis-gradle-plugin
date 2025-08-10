// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.*

final class IncludedBuildWithAnnotationProcessorProject extends AbstractProject {

  final GradleProject gradleProject

  IncludedBuildWithAnnotationProcessorProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.settingsScript.additions = "includeBuild 'processor-build'"
      }
      .withSubproject("user-of-processor") { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            api('my.custom.processor:sub-processor1'),
            annotationProcessor('my.custom.processor:sub-processor1')
          ]
        }
        consumer.sources = [
          new Source(
            SourceType.JAVA, 'UserOfProcessor', 'com/example/user/of/processor1',
            """\
            package com.example.user.of.processor1;
            
            import com.example.included.processor1.Field;
    
            public class UserOfProcessor {
              public @Field int field;
            }""".stripIndent()
          )
        ]
      }
      .withIncludedBuild('processor-build') { second ->
        second.withRootProject { r ->
          r.gradleProperties += GradleProperties.enableConfigurationCache() + ADDITIONAL_PROPERTIES
          r.withBuildScript { bs ->
            bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinJvmNoApply]
          }
        }
        second.withSubproject('sub-processor1') { sub ->
          sub.withBuildScript { bs ->
            bs.plugins = javaLibrary
            bs.dependencies = [
              annotationProcessor('com.google.auto.service:auto-service:1.0-rc6'),
              compileOnly('com.google.auto.service:auto-service-annotations:1.0-rc6')
            ]
            bs.repositories = [
              Repository.GOOGLE,
              Repository.MAVEN_CENTRAL
            ]
            bs.group = 'my.custom.processor'
          }
          sub.sources = [
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
              }""".stripIndent()
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
              }""".stripIndent()
            )
          ]
        }
      }.write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':user-of-processor', [] as Set<Advice>)
  ]
}
