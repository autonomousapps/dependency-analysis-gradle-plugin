package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsText

final class HasJavaAndKotlinProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  HasJavaAndKotlinProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { a ->
        a.manifest = libraryManifest()
        a.sources = sources
        a.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock(true)
          bs.dependencies(
            // Used by Kotlin class
            commonsCollections('api'),
            // Used by Java class
            commonsText('implementation'),
          )
          bs.withGroovy("""tasks.whenTaskAdded {}""")
        }
      }
      .write()
  }

  private List<Source> sources = [
    Source.java(
      """
      package com.example.java;

      import org.apache.commons.text.CaseUtils;

      public class JavaClass {
        public void magic() {
          CaseUtils.toCamelCase(null, false);
        }
      }
      """
    ).withPath("com.example.java", "JavaClass").build(),
    Source.kotlin(
      """
      package com.example.kotlin
      
      import org.apache.commons.collections4.Bag
      import org.apache.commons.collections4.bag.HashBag

      class KotlinClass {
        fun bag(): Bag<String> = HashBag<String>()
      }
      """
    ).withPath("com.example.kotlin", "KotlinClass").build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib')
  ]
}
