package com.autonomousapps.internal.utils

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal fun buildDocument(file: File): Document = DocumentBuilderFactory.newInstance()
  .newDocumentBuilder()
  .parse(file).also {
    it.documentElement.normalize()
  }
