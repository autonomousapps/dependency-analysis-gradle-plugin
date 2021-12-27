package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

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
    return minimalAndroidProjectBuilder(agpVersion).tap {
      withRootProject { root ->
        root.withBuildScript { buildScript ->
          buildScript.additions = rootAdditions
        }
      }
      withAndroidSubproject('app') { app ->
        app.sources = appSources
        app.withBuildScript { script ->
          script.plugins = androidAppPlugins
          script.android = androidAppBlock
          script.dependencies = appDependencies
          script.additions = appAdditions
        }
      }
      withAndroidSubproject('lib_android') { lib ->
        lib.manifest = AndroidManifest.defaultLib("com.example.lib")
        lib.sources = libAndroidSources
        lib.withBuildScript { script ->
          script.plugins = androidLibPlugins
          script.android = androidLibBlock
          script.dependencies = androidLibDependencies
        }
      }
      withSubproject('lib_jvm') { lib ->
        lib.sources = libJvmSources
        lib.withBuildScript { script ->
          script.plugins = jvmLibPlugins
          script.dependencies = jvmLibDependencies
        }
      }
    }.build().tap {
      writer().write()
    }
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
    project('implementation', ':lib_android'),
    project('implementation', ':lib_jvm'),
    kotlinStdLib("implementation"),
    androidxAnnotations("api"),
    coreKtx("implementation"),
    commonsIO("debugImplementation")
  ]

  private List<Dependency> androidLibDependencies = [
    kotlinStdLib("api"),
    appcompat("api"),
    coreKtx("implementation"),
    navUiKtx("implementation")
  ]

  private List<Dependency> jvmLibDependencies = [
    kotlinStdLib("api"),
    commonsText("implementation"),
    commonsCollections("api"),
    commonsIO("implementation"),
    tpCompiler("kapt")
  ]

  @SuppressWarnings(['GroovyAssignabilityCheck', 'UnnecessaryQualifiedReference'])
  List<ComprehensiveAdvice> actualBuildHealth() {
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
    usedTransitiveDependencies: [dependency('androidx.appcompat:appcompat', '1.1.0')]
  ))
  final removeCommonsIo = Advice.ofRemove(dependency(
    identifier: 'commons-io:commons-io',
    resolvedVersion: '2.6',
    configurationName: 'debugImplementation'
  ))
  private final removeCoreKtx = Advice.ofRemove(
    componentWithTransitives(
      dependency: dependency(
        identifier: 'androidx.core:core-ktx',
        resolvedVersion: '1.1.0',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [] as Set<Dependency>
    )
  )
  private final addAppCompat = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency(identifier: 'androidx.appcompat:appcompat', resolvedVersion: '1.1.0'),
      parents: [dependency(identifier: ':lib_android')]
    ),
    'implementation'
  )
  final addCommonsCollections = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency(identifier: 'org.apache.commons:commons-collections4', resolvedVersion: '4.4'),
      parents: [dependency(identifier: ':lib_jvm')]
    ),
    'implementation'
  )
  final changeAndroidxAnnotation = Advice.ofChange(
    dependency(
      identifier: 'androidx.annotation:annotation',
      resolvedVersion: '1.1.0',
      configurationName: 'api'
    ),
    'compileOnly'
  )
  // lib-android
  final removeNavUiKtx = Advice.ofRemove(
    componentWithTransitives(
      dependency: dependency(
        identifier: 'androidx.navigation:navigation-ui-ktx',
        resolvedVersion: '2.1.0',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [
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
        resolvedVersion: '1.1.0',
        configurationName: 'implementation'
      ),
      usedTransitiveDependencies: [
        dependency('org.jetbrains:annotations'),
        dependency('androidx.annotation:annotation'),
        dependency('androidx.core:core')
      ] as Set<Dependency>
    )
  )
  final addAndroidxCore = Advice.ofAdd(
    transitiveDependency(
      dependency: dependency('androidx.core:core', '1.1.0'),
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
      resolvedVersion: '1.1.0',
      configurationName: 'api'
    ),
    'implementation'
  )
  // lib-jvm
  final removeToothpick = Advice.ofRemove(dependency(
    identifier: 'com.github.stephanenicolas.toothpick:toothpick-compiler',
    resolvedVersion: '3.1.0',
    configurationName: 'kapt'
  ))
  final removeCommonsText = Advice.ofRemove(componentWithTransitives(
    dependency: dependency(
      identifier: 'org.apache.commons:commons-text',
      resolvedVersion: '1.8',
      configurationName: 'implementation'
    ),
    usedTransitiveDependencies: [dependency(
      identifier: 'org.apache.commons:commons-lang3',
      resolvedVersion: '3.9'
    )]
  ))
  final addCommonsLang = Advice.ofAdd(transitiveDependency(
    dependency: dependency(
      identifier: 'org.apache.commons:commons-lang3',
      resolvedVersion: '3.9'
    ),
    parents: [dependency(identifier: 'org.apache.commons:commons-text')]
  ), 'implementation')
  final changeCommonsCollections = Advice.ofChange(dependency(
    identifier: 'org.apache.commons:commons-collections4',
    resolvedVersion: '4.4',
    configurationName: 'api'
  ), 'implementation')
  final changeCommonsIo = Advice.ofChange(dependency(
    identifier: 'commons-io:commons-io',
    resolvedVersion: '2.6',
    configurationName: 'implementation'
  ), 'api')
}
