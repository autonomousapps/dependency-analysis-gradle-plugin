package com.autonomousapps.jvm

final class Plugin {

  final String id, version
  final boolean apply

  Plugin(String id, String version = null, boolean apply = true) {
    this.id = id
    this.version = version
    this.apply = apply
  }

  @Override
  String toString() {
    String s = "id '$id'"
    if (version) {
      s += " version '$version'"
    }
    if (!apply) {
      s += ' apply false'
    }
    return s
  }
}
