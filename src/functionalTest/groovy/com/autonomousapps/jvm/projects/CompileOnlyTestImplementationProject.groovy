package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

/**
 * The {@code testImplementation} configuration does not extend from the {@code compileOnly} configuration. So, it is
 * inaccurate to suggest removing {@code testImplementation} dependencies just because they're on the compile classpath.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#resolvable_configurations">Java configurations</a>
 */
final class CompileOnlyTestImplementationProject extends AbstractProject {

  final GradleProject gradleProject

  CompileOnlyTestImplementationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib') { s ->
        s.sources = SOURCES
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            // These two configurations are independent!
            commonsCollections('compileOnly'),
            commonsCollections('testImplementation'),
          ]
        }
      }
      .write()
  }

  private static final List<Source> SOURCES = [
    Source.java(
      '''\
        package com.example.lib;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        
        public class Lib {
          private Bag<String> newBag() {
            return new HashBag<>();
          }
        }
      '''
    )
      .withPath('com.example.lib', 'Lib')
      .build(),
    Source.java(
      '''\
        package com.example.lib;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        
        public class LibTest {
          private void test() {
            Bag<String> bag = new HashBag<>();
          }
        }
      '''
    )
      .withPath('com.example.lib', 'LibTest')
      .withSourceSet('test')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
