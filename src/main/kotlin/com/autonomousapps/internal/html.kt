package com.autonomousapps.internal

import org.w3c.dom.Element
import java.io.File
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// https://stackoverflow.com/a/46414605/2740621
internal fun Element.writeToFile(file: File) {
    with(TransformerFactory.newInstance().newTransformer()) {
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        setOutputProperty(OutputKeys.METHOD, "xml")
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        file.printWriter().use { writer ->
            transform(DOMSource(this@writeToFile), StreamResult(writer))
        }
    }
}