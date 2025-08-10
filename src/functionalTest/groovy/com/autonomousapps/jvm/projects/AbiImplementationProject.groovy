// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class AbiImplementationProject extends AbstractProject {

  final GradleProject gradleProject

  AbiImplementationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = projSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('implementation', ':anno')
          ]
        }
      }
      .withSubproject('anno') { s ->
        s.sources = annoSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private projSources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        import com.example.annotation.Annotation;
        import com.example.annotation.Clazz;
        
        @Annotation
        public class Main {
          public void usesAnnotationAsClass() {
            Class<Clazz> clazz = Clazz.class;
          }
        }""".stripIndent()
    ),
  ]

  private annoSources = [
    new Source(
      SourceType.JAVA, 'Clazz', 'com/example/annotation',
      """\
        package com.example.annotation;
        
        import static java.lang.annotation.ElementType.TYPE;
        import static java.lang.annotation.RetentionPolicy.CLASS;
        
        import java.lang.annotation.Documented;
        import java.lang.annotation.Retention;
        import java.lang.annotation.Target;
                
        @Documented
        @Retention(CLASS)
        @Target(TYPE)
        public @interface Clazz {}""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'Annotation', 'com/example/annotation',
      """\
        package com.example.annotation;
        
        import static java.lang.annotation.ElementType.TYPE;
        import static java.lang.annotation.RetentionPolicy.CLASS;
        
        import java.lang.annotation.Documented;
        import java.lang.annotation.Retention;
        import java.lang.annotation.Target;
                
        @Documented
        @Retention(CLASS)
        @Target(TYPE)
        public @interface Annotation {}""".stripIndent()
    ),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = emptyProjectAdviceFor(
    ':proj',
    ':anno',
  )
}
