package com.autonomousapps.model.declaration

import spock.lang.Specification

class BucketSpec extends Specification {

  def "can compute bucket from configuration (#configurationName => #bucket)"() {
    expect:
    Bucket.of(configurationName) == bucket

    where:
    configurationName                  | bucket
    'implementation'                   | Bucket.IMPL
    'debugImplementation'              | Bucket.IMPL
    'releaseFlavorImplementation'      | Bucket.IMPL

    'api'                              | Bucket.API
    'releaseApi'                       | Bucket.API
    'debugFlavorApi'                   | Bucket.API

    'compileOnly'                      | Bucket.COMPILE_ONLY
    'debugCompileOnly'                 | Bucket.COMPILE_ONLY
    'releaseFlavorCompileOnly'         | Bucket.COMPILE_ONLY

    'runtimeOnly'                      | Bucket.RUNTIME_ONLY
    'releaseRuntimeOnly'               | Bucket.RUNTIME_ONLY
    'debugFlavorRuntimeOnly'           | Bucket.RUNTIME_ONLY

    'annotationProcessor'              | Bucket.ANNOTATION_PROCESSOR
    'debugAnnotationProcessor'         | Bucket.ANNOTATION_PROCESSOR
    'releaseFlavorAnnotationProcessor' | Bucket.ANNOTATION_PROCESSOR

    'kapt'                             | Bucket.ANNOTATION_PROCESSOR
    'kaptRelease'                      | Bucket.ANNOTATION_PROCESSOR
    'kaptFlavorDebug'                  | Bucket.ANNOTATION_PROCESSOR

    'testImplementation'               | Bucket.IMPL
    'testApi'                          | Bucket.API
    'testCompileOnly'                  | Bucket.COMPILE_ONLY
    'testRuntimeOnly'                  | Bucket.RUNTIME_ONLY
  }

  def "throws when no matching bucket found (#configurationName)"() {
    when:
    Bucket.of(configurationName)

    then:
    thrown(IllegalArgumentException)

    where:
    configurationName << [
      'debugKapt', 'annotationProcessorRelease',
      'implementationDebug', 'apiRelease'
    ]
  }
}
