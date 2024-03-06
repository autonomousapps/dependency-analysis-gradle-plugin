package com.autonomousapps.android

import com.autonomousapps.android.projects.HasJavaAndKotlinProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertAbout

final class HasJavaAndKotlinSourcesSpec extends AbstractAndroidSpec {

  // whenTaskAdded eagerly realizes all tasks and this broke with AGP 8 APIs. Here we verify that we don't fail in its
  // presence, and that we can still analyze class files from Java and Kotlin source. We're lazier now!
  // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1116
  def "doesn't fail with whenTaskAdded (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new HasJavaAndKotlinProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
