// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class AbiInterfaceProject extends AbstractProject {

  final GradleProject gradleProject
  private final abstractProject = project('implementation', ':abstract')

  AbiInterfaceProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('impl') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [abstractProject]
        }
      }
      .withSubproject('abstract') { s ->
        s.sources = [SOURCE_PRODUCER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = []
        }
      }
      .write()
  }

  private static final Source SOURCE_PRODUCER = new Source(
    SourceType.JAVA, "IPermission", "",
    """\
      public interface IPermission {
        String name();
        boolean isAdmin();
        default Object getLoggingObject() {
          return name();
        }
      }""".stripIndent()
  )

  private static final Source SOURCE_CONSUMER = new Source(
    SourceType.JAVA, "WebPermission", "",
    """\
      public enum WebPermission implements IPermission {
        ViewData(false),
        EditData(true),
        ;
        
        private final boolean superRole;
        
        WebPermission(boolean superRole) {
          this.superRole = superRole;
        }
        
        @Override
        public boolean isAdmin() {
          return superRole;
        }
      }""".stripIndent()
  )

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final implAdvice2 = [
    Advice.ofChange(projectCoordinates(abstractProject), abstractProject.configuration, 'api'),
  ] as Set<Advice>

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':impl', implAdvice2),
    emptyProjectAdviceFor(':abstract'),
  ]
}
