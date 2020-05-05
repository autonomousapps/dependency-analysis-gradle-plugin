package com.autonomousapps.fixtures.jvm

final class Source {

  final String name, path, source
  final SourceType sourceType

  Source(SourceType sourceType, String name, String path, String source) {
    this.sourceType = sourceType
    this.name = name
    this.path = path
    this.source = source
  }
}
