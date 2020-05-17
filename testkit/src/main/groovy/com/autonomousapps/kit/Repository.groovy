package com.autonomousapps.kit

final class Repository {

  final String repo

  Repository(String repo) {
    this.repo = repo
  }

  @Override
  String toString() {
    return repo
  }

  static List<Repository> DEFAULT_REPOS = [
    new Repository('google()'),
    new Repository('jcenter()'),
    new Repository('maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }')
  ]
}
