// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.runtimeOnly

final class AdvancedReflectionProject extends AbstractProject {

  final GradleProject gradleProject

  AdvancedReflectionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('aggregator') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(
            implementation(':class-lookup'),
            implementation(':utils'),
            implementation(':framework-like-spring'),
            implementation(':optional-dependency'),
          )
        }
        s.sources = aggregatorSources
      }
      .withSubproject('class-lookup') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.sources = classLookupSources
      }
      .withSubproject('utils') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.sources = utilsSources
      }
      .withSubproject('framework-like-spring') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.sources = frameworkLikeSpringSources
      }
      .withSubproject('optional-dependency') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.sources = optionalDependencySources
      }
      .write()
  }

  private List<Source> aggregatorSources = [
    Source.java(
      '''\
        package com.example.aggregator;
        
        import com.example.classlookup.ClassLookup;
        import framework.like.spring.CheckForOptionalDependency;
        
        public class Aggregator {
        
          void use() {
            new ClassLookup();
            new CheckForOptionalDependency();
          }
        }'''.stripIndent()
    ).build(),
  ]

  private List<Source> classLookupSources = [
    Source.java(
      '''\
        package com.example.classlookup;
        
        public class ClassLookup {
        
          void notUsingThis() {
            System.out.println("com.example.utils.Util");
          }
        
          public Class<?> lookup(String className) throws Exception {
            return Class.forName(className);
          }
        }'''.stripIndent()
    ).build(),
  ]

  private List<Source> utilsSources = [
    Source.java(
      '''\
        package com.example.utils;
        
        public class Util {}'''.stripIndent()
    ).build(),
  ]

  private List<Source> frameworkLikeSpringSources = [
    Source.java(
      '''\
        package framework.like.spring;
        
        public class CheckForOptionalDependency {
        
          private static final String LOG_MESSAGE = "Looking up class via reflection";
        
          public Class<?> lookup() throws Exception {
            String className = "optional.dependency.OptionalDependency";
            System.out.println(LOG_MESSAGE);
            return Class.forName(className);
          }
        }'''.stripIndent()
    ).build(),
    Source.java(
      '''\
        package framework.like.spring;

        public class CheckForOptionalDependencyUsingConstant {

          private static final String CLASS_NAME = "optional.dependency.OptionalDependency";
          private static final String LOG_MESSAGE = "Looking up class via reflection";

          public Class<?> lookup() throws Exception {
            System.out.println(LOG_MESSAGE);
            return Class.forName(CLASS_NAME);
          }
        }'''.stripIndent()
    ).build(),
  ]

  private List<Source> optionalDependencySources = [
    Source.java(
      '''\
        package optional.dependency;
        
        public class OptionalDependency {}'''.stripIndent()
    ).build(),
  ]

  Set<ExplodedJar> actualExplodedJarsForProjectAndVariant(String projectPath, String variantName) {
    return explodedJarsForProjectAndVariant(gradleProject, projectPath, variantName)
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> aggregatorAdvice = [
    Advice.ofChange(projectCoordinates(':optional-dependency'), 'implementation', 'runtimeOnly'),
    Advice.ofRemove(projectCoordinates(':utils'), 'implementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':aggregator', aggregatorAdvice),
    emptyProjectAdviceFor(':class-lookup'),
    emptyProjectAdviceFor(':utils'),
    emptyProjectAdviceFor(':framework-like-spring'),
    emptyProjectAdviceFor(':optional-dependency'),
  ]
}
