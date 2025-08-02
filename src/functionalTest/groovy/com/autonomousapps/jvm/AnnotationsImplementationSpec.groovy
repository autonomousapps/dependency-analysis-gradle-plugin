package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AnnotationByDelegateProject
import com.autonomousapps.jvm.projects.AnnotationsCompileOnlyProject
import com.autonomousapps.jvm.projects.AnnotationsImplementationProject
import com.autonomousapps.jvm.projects.AnnotationsImplementationProject2
import com.autonomousapps.utils.Colors
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AnnotationsImplementationSpec extends AbstractJvmSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1210")
  def "classes used in foreign runtime-retained annotations are implementation (#gradleVersion, visibleAnnotations: #visibleAnnotations)"() {
    given:
    def project = new AnnotationsImplementationProject(visibleAnnotations)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, visibleAnnotations] << [
      gradleVersions(),
      [true, false]
    ].combinations()
  }

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1210")
  def "runtime-retained annotations are compileOnly (#gradleVersion)"() {
    given:
    def project = new AnnotationsImplementationProject2()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'consumer:reason', '--id', 'org.cthing:cthing-annotations')

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        ------------------------------------------------------------
        You asked about the dependency 'org.cthing:cthing-annotations:1.0.0'.
        There is no advice regarding this dependency.
        ------------------------------------------------------------
        
        Shortest path from :consumer to org.cthing:cthing-annotations:1.0.0 for compileClasspath:
        :consumer
        \\--- org.cthing:cthing-annotations:1.0.0
        
        There is no path from :consumer to org.cthing:cthing-annotations:1.0.0 for runtimeClasspath
        
        
        There is no path from :consumer to org.cthing:cthing-annotations:1.0.0 for testCompileClasspath
        
        
        There is no path from :consumer to org.cthing:cthing-annotations:1.0.0 for testRuntimeClasspath
        
        
        Source: main
        ------------
        * Uses (as or in an annotation) 1 class: org.cthing.annotations.PackageNonnullByDefault (implies compileOnly).
        * Provides annotations (implies compileOnly).
        
        Source: test
        ------------
        (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "classes used in compile-retained annotations are compileOnly (#gradleVersion)"() {
    given:
    def project = new AnnotationsCompileOnlyProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')
    // TODO(tsr): still need better tests for reason. Before the fix, this output was wrong. Still not fixed really.
    //, ':consumer:reason', '--id', 'org.jetbrains:annotations')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  // This test ensures that we don't suggest adding an implementation dependency on errorprone annotations due to
  // implicit usage of @CanIgnoreReturnValue, which is an api dependency provided by Guava for use of the Service class.
  def "class-retained annotations used by delegates do not need to be declared (#gradleVersion)"() {
    given:
    def project = new AnnotationByDelegateProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':consumer:reason', '--id', 'com.google.errorprone:error_prone_annotations',
    )

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        ------------------------------------------------------------
        You asked about the dependency 'com.google.errorprone:error_prone_annotations:2.28.0'.
        There is no advice regarding this dependency.
        ------------------------------------------------------------
    
        Shortest path from :consumer to com.google.errorprone:error_prone_annotations:2.28.0 for compileClasspath:
        :consumer
        \\--- com.google.guava:guava:33.3.1-jre (capabilities: [com.google.collections:google-collections])
              \\--- com.google.errorprone:error_prone_annotations:2.28.0
        
        Shortest path from :consumer to com.google.errorprone:error_prone_annotations:2.28.0 for runtimeClasspath:
        :consumer
        \\--- com.google.guava:guava:33.3.1-jre (capabilities: [com.google.collections:google-collections])
              \\--- com.google.errorprone:error_prone_annotations:2.28.0
        
        Shortest path from :consumer to com.google.errorprone:error_prone_annotations:2.28.0 for testCompileClasspath:
        :consumer
        \\--- com.google.guava:guava:33.3.1-jre (capabilities: [com.google.collections:google-collections])
              \\--- com.google.errorprone:error_prone_annotations:2.28.0
        
        Shortest path from :consumer to com.google.errorprone:error_prone_annotations:2.28.0 for testRuntimeClasspath:
        :consumer
        \\--- com.google.guava:guava:33.3.1-jre (capabilities: [com.google.collections:google-collections])
              \\--- com.google.errorprone:error_prone_annotations:2.28.0
        
        Source: main
        ------------
        * Uses (as or in an annotation) 1 class: com.google.errorprone.annotations.CanIgnoreReturnValue (implies compileOnly).
        
        Source: test
        ------------
        (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }
}
