package com.autonomousapps

const val FLAG_SILENT = "dependency.analysis.silent"
const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
const val FLAG_FAIL = "dependency.analysis.fail"

internal fun shouldNotBeSilent(): Boolean {
  val silent = System.getProperty(FLAG_SILENT, "false")
  return !silent!!.toBoolean()
}

internal fun shouldFail(): Boolean {
  val shouldFail = System.getProperty(FLAG_FAIL, "false")
  return shouldFail!!.toBoolean()
}
