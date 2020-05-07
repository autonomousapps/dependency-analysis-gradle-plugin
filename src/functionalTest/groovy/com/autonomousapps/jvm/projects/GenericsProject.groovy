package com.autonomousapps.jvm.projects

import com.autonomousapps.fixtures.jvm.Dependency
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.Source
import com.autonomousapps.fixtures.jvm.SourceType

/**
 * <pre>
 * import com.my.other.DependencyClass;
 * import java.util.Optional;
 *
 * public interface MyJavaClass {
 *   Optional<DependencyClass> getMyDependency();
 * }
 * </pre>
 *
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/148
 */
final class GenericsProject {

  final JvmProject jvmProject

  GenericsProject() {
    this.jvmProject = build()
  }

  private static JvmProject build() {
    def builder = new JvmProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]
    def dependencies1 = [new Dependency("implementation", ":proj-2")]
    def source1 = new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import com.example.lib.Library;
        import java.util.Optional;

        public interface Main {
          Optional<Library> getLibrary();
        }
      """.stripIndent()
    )
    def source2 = new Source(
      SourceType.JAVA, "Library", "com/example/lib",
      """\
        package com.example.lib;
        
        public class Library {
        }
      """.stripIndent()
    )

    builder.addSubproject(plugins, dependencies1, [source1], 'main', '')
    builder.addSubproject(plugins, [], [source2], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }
}
