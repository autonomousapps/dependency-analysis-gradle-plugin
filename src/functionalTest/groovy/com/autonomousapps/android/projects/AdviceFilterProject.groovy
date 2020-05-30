package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.GradleProperties.JVM_ARGS
import static com.autonomousapps.kit.GradleProperties.USE_ANDROID_X

final class AdviceFilterProject extends AbstractProject {

  final GradleProject gradleProject

  private final String agpVersion
  private final String rootAdditions
  private final String appAdditions

  AdviceFilterProject(String agpVersion, String rootAdditions = '', String appAdditions = '') {
    this.agpVersion = agpVersion
    this.rootAdditions = rootAdditions
    this.appAdditions = appAdditions
    this.gradleProject = build()
  }

  AdviceFilterProject(Map<String, String> map) {
    this(
      map['agpVersion'],
      map['rootAdditions'] ?: '',
      map['appAdditions'] ?: ''
    )
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.of(JVM_ARGS, USE_ANDROID_X)
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        bs.additions = rootAdditions
      }
    }
    builder.withAndroidSubproject('app') { a ->
      a.sources = appSources
      a.withBuildScript { bs ->
        bs.plugins = androidAppPlugins
        bs.android = androidAppBlock
        bs.dependencies = appDependencies
        bs.additions = appAdditions
      }
    }
    builder.withAndroidSubproject('lib_android') { a ->
      a.manifest = AndroidManifest.defaultLib("com.example.lib")
      a.sources = libAndroidSources
      a.withBuildScript { bs ->
        bs.plugins = androidLibPlugins
        bs.android = androidLibBlock
        bs.dependencies = androidLibDependencies
      }
    }
    builder.withSubproject('lib_jvm') { a ->
      a.sources = libJvmSources
      a.withBuildScript { bs ->
        bs.plugins = jvmLibPlugins
        bs.dependencies = jvmLibDependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> androidAppPlugins = [
    Plugin.androidAppPlugin,
    Plugin.kotlinAndroidPlugin
  ]

  private List<Plugin> androidLibPlugins = [
    Plugin.androidLibPlugin,
    Plugin.kotlinAndroidPlugin
  ]

  private List<Plugin> jvmLibPlugins = [
    Plugin.kotlinPluginNoVersion,
    Plugin.kaptPlugin
  ]

  private AndroidBlock androidAppBlock = AndroidBlock.defaultAndroidAppBlock(true)
  private AndroidBlock androidLibBlock = AndroidBlock.defaultAndroidLibBlock(true)

  private List<Source> appSources = [
    new Source(
      SourceType.KOTLIN, "MainActivity", "com/example",
      """\
        package com.example
        
        import androidx.annotation.AnyThread
        import androidx.appcompat.app.AppCompatActivity
        import org.apache.commons.collections4.bag.HashBag
        
        // AppCompatActivity from APPCOMPAT is not declared, but brought in transitively from lib-android
        class MainActivity : AppCompatActivity() {
          
          // From ANDROIDX_ANNOTATIONS, which is incorrectly declared as "api"
          @AnyThread
          fun thing() {
            val bag = HashBag<String>()
            JvmLibrary().thing()
          }       
        }
      """.stripIndent()
    )
  ]

  private List<Source> libAndroidSources = [
    new Source(
      SourceType.KOTLIN, "AndroidLibrary", "com/example/lib",
      """\
        package com.example.lib

        import androidx.annotation.AnyThread
        import androidx.appcompat.app.AppCompatActivity
        import androidx.core.provider.FontRequest
              
        class AndroidLibrary {
          // FontRequest from CORE_KTX is part of the ABI
          fun abi() = FontRequest("foo", "foo", "foo", 0)
              
          // @AnyThread is from ANDROIDX_ANNOTATIONS, brought in transitively from APPCOMPAT
          @AnyThread
          fun implementation() {
            // AppCompatActivity from APPCOMPAT is an implementation dependency
            val klass = AppCompatActivity::class.java
          }
        }
      """.stripIndent()
    )
  ]

  private List<Source> libJvmSources = [
    new Source(
      SourceType.KOTLIN, "JvmLibrary", "com/example",
      """\
        package com.example

        import org.apache.commons.collections4.bag.HashBag // Direct from commons-collections
        import org.apache.commons.lang3.StringUtils // Brought in transitively from commons-text
        import org.apache.commons.io.output.NullWriter // Direct from commons-io
              
        class JvmLibrary {
          fun thing() {
            // From commons-lang
            val empty = StringUtils.isEmpty("")
            // From commons-collections
            val bag = HashBag<String>()
          }
              
          // NullWriter is part of ABI, but if method is never called, not needed by consumer
          fun nullWriter(): NullWriter {
            return NullWriter()
          }
        }
      """.stripIndent()
    )
  ]

  private List<Dependency> appDependencies = [
    Dependency.project('implementation', ':lib_android'),
    Dependency.project('implementation', ':lib_jvm'),
    Dependency.kotlinStdlibJdk7("implementation"),
    Dependency.androidxAnnotations("api"),
    Dependency.coreKtx("implementation"),
    Dependency.commonsIO("debugImplementation")
  ]

  private List<Dependency> androidLibDependencies = [
    Dependency.kotlinStdlibJdk7("implementation"),
    Dependency.appcompat("api"),
    Dependency.coreKtx("implementation"),
    Dependency.navUiKtx("implementation")
  ]

  private List<Dependency> jvmLibDependencies = [
    Dependency.kotlinStdlibJdk7("implementation"),
    Dependency.commonsText("implementation"),
    Dependency.commonsCollections("api"),
    Dependency.commonsIO("implementation"),
    Dependency.tpCompiler("kapt")
  ]

  List<BuildHealth> actualBuildHealth() {
    //noinspection UnnecessaryQualifiedReference
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  List<Advice> expectedAppAdvice(Advice... ignored = []) {
    def advice = [
      removeLibAndroid, removeCoreKtx, removeCommonsIo,
      addAppCompat, addCommonsCollections,
      changeAndroidxAnnotation
    ]
    advice.removeAll(ignored)
    return advice
  }

  List<Advice> expectedLibAndroidAdvice(Advice... ignored = []) {
    def advice = [
      removeCoreKtxAndroidLib, removeNavUiKtx,
      addAndroidxCore,
      changeAppcompat
    ]
    advice.removeAll(ignored)
    return advice
  }

  List<Advice> expectedLibJvmAdvice(Advice... ignored = []) {
    def advice = [
      removeToothpick, removeCommonsText,
      addCommonsLang,
      changeCommonsCollections, changeCommonsIo
    ]
    advice.removeAll(ignored)
    return advice
  }

  final removeLibAndroid = Advice.ofRemove(componentWithTransitives(
    dependency: dependency(
      identifier: ':lib_android',
      configurationName: 'implementation'
    ),
    usedTransitiveDependencies: [
      dependency('androidx.appcompat:appcompat'),
      dependency('org.jetbrains.kotlin:kotlin-stdlib')
    ]
  ))
  final removeCommonsIo = Advice.ofRemove(dependency(
    identifier: 'commons-io:commons-io',
    configurationName: 'debugImplementation'
  ))
  private final removeCoreKtx = Advice.ofRemove(
    componentWithTransitives(
      dependency: dependency(
        identifier: 'androidx.core:core-ktx',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [dependency('org.jetbrains.kotlin:kotlin-stdlib')] as Set<Dependency>
    )
  )
  private final addAppCompat = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency(identifier: 'androidx.appcompat:appcompat'),
      parents: [dependency(identifier: ':lib_android')]
    ),
    'implementation'
  )
  final addCommonsCollections = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency(identifier: 'org.apache.commons:commons-collections4'),
      parents: [dependency(identifier: ':lib_jvm')]
    ),
    'implementation'
  )
  final changeAndroidxAnnotation = Advice.ofChange(
    dependency(
      identifier: 'androidx.annotation:annotation',
      configurationName: 'api'
    ),
    'compileOnly'
  )
  // lib-android
  final removeNavUiKtx = Advice.ofRemove(
    componentWithTransitives(
      dependency: dependency(
        identifier: 'androidx.navigation:navigation-ui-ktx',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [
        dependency('org.jetbrains.kotlin:kotlin-stdlib'),
        dependency('org.jetbrains:annotations'),
        dependency('androidx.annotation:annotation'),
        dependency('androidx.core:core')
      ]
    )
  )
  private final removeCoreKtxAndroidLib = Advice.ofRemove(
    componentWithTransitives(
      dependency: dependency(
        identifier: 'androidx.core:core-ktx',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [
        dependency('org.jetbrains.kotlin:kotlin-stdlib'),
        dependency('org.jetbrains:annotations'),
        dependency('androidx.annotation:annotation'),
        dependency('androidx.core:core')
      ] as Set<Dependency>
    )
  )
  final addAndroidxCore = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency('androidx.core:core'),
      parents: [
        dependency('androidx.core:core-ktx'),
        dependency('androidx.navigation:navigation-ui-ktx'),
        dependency('androidx.appcompat:appcompat')
      ]
    ),
    'api'
  )
  final changeAppcompat = Advice.ofChange(
    dependency(
      identifier: 'androidx.appcompat:appcompat',
      configurationName: 'api'
    ),
    'implementation'
  )
  // lib-jvm
  final removeToothpick = Advice.ofRemove(dependency(
    identifier: 'com.github.stephanenicolas.toothpick:toothpick-compiler',
    configurationName: 'kapt'
  ))
  final removeCommonsText = Advice.ofRemove(componentWithTransitives(
    dependency:  dependency(
      identifier: 'org.apache.commons:commons-text',
      configurationName: 'implementation'
    ),
    usedTransitiveDependencies: [dependency(identifier: 'org.apache.commons:commons-lang3')]
  ))
  final addCommonsLang = Advice.ofAdd(transitiveDependency(
    dependency: dependency(identifier: 'org.apache.commons:commons-lang3'),
    parents: [dependency(identifier: 'org.apache.commons:commons-text')]
  ), 'implementation')
  final changeCommonsCollections = Advice.ofChange(dependency(
    identifier: 'org.apache.commons:commons-collections4',
    configurationName: 'api'
  ), 'implementation')
  final changeCommonsIo = Advice.ofChange(dependency(
    identifier: 'commons-io:commons-io',
    configurationName: 'implementation'
  ), 'api')
}
