package com.autonomousapps

const val FLAG_SILENT = "dependency.analysis.silent"
const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
const val FLAG_FAIL = "dependency.analysis.fail"
const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"

internal fun shouldNotBeSilent() = getSysProp(FLAG_SILENT, false)
internal fun shouldFail() = getSysProp(FLAG_FAIL, false)
internal fun shouldAnalyzeTests() = getSysProp(FLAG_TEST_ANALYSIS, true)

private fun getSysProp(name: String, default: Boolean): Boolean {
  return System.getProperty(name, default.toString())!!.toBoolean()
}
