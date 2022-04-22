package com.autonomousapps.model.declaration

import spock.lang.Specification

class VariantSpec extends Specification {

  def "can compute variant from configuration (#configurationName => #variant)"() {
    expect:
    Variant.of(configurationName, [
      'main', 'release', 'debug', 'test', 'testDebug', 'testRelease',
      'flavor', 'flavorRelease', 'flavorDebug', 'testFlavorRelease', 'testFlavorDebug'
    ] as Set<String>) == variant

    where:
    configurationName                  | variant
    'implementation'                   | new Variant('main', SourceSetKind.MAIN)
    'debugImplementation'              | new Variant('debug', SourceSetKind.MAIN)
    'flavorReleaseImplementation'      | new Variant('flavorRelease', SourceSetKind.MAIN)

    'api'                              | new Variant('main', SourceSetKind.MAIN)
    'releaseApi'                       | new Variant('release', SourceSetKind.MAIN)
    'flavorDebugApi'                   | new Variant('flavorDebug', SourceSetKind.MAIN)

    'compileOnly'                      | new Variant('main', SourceSetKind.MAIN)
    'debugCompileOnly'                 | new Variant('debug', SourceSetKind.MAIN)
    'flavorReleaseCompileOnly'         | new Variant('flavorRelease', SourceSetKind.MAIN)

    'runtimeOnly'                      | new Variant('main', SourceSetKind.MAIN)
    'releaseRuntimeOnly'               | new Variant('release', SourceSetKind.MAIN)
    'flavorDebugRuntimeOnly'           | new Variant('flavorDebug', SourceSetKind.MAIN)

    'annotationProcessor'              | new Variant('main', SourceSetKind.MAIN)
    'debugAnnotationProcessor'         | new Variant('debug', SourceSetKind.MAIN)
    'flavorReleaseAnnotationProcessor' | new Variant('flavorRelease', SourceSetKind.MAIN)

    'kapt'                             | new Variant('main', SourceSetKind.MAIN)
    'kaptRelease'                      | new Variant('release', SourceSetKind.MAIN)
    'kaptFlavorDebug'                  | new Variant('flavorDebug', SourceSetKind.MAIN)

    'testImplementation'               | new Variant('main', SourceSetKind.TEST)
    'testApi'                          | new Variant('main', SourceSetKind.TEST)
    'testCompileOnly'                  | new Variant('main', SourceSetKind.TEST)
    'testRuntimeOnly'                  | new Variant('main', SourceSetKind.TEST)
  }

  def "throws when no matching variant found (#configurationName)"() {
    when:
    Variant.of(configurationName, [
      'main', 'release', 'debug', 'test', 'testDebug', 'testRelease',
      'flavor', 'flavorRelease', 'flavorDebug', 'testFlavorRelease', 'testFlavorDebug'
    ] as Set<String>)

    then:
    thrown(IllegalArgumentException)

    where:
    configurationName << [
      'debugKapt', 'annotationProcessorRelease',
      'implementationDebug', 'apiRelease'
    ]
  }
}
