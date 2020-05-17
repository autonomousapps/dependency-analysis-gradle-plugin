package com.autonomousapps.kit

final class AndroidLayout {

  final String filename
  final String content

  AndroidLayout(String filename, String content) {
    this.filename = filename
    this.content = content
  }

  @Override
  String toString() {
    return content
  }
}
