package com.autonomousapps

import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipOutputStream

internal fun walkFileTree(path: Path, predicate: (Path) -> Boolean = { true }): Set<File> {
    val files = mutableSetOf<File>()
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (predicate.invoke(path)) {
                files.add(path.toFile())
            }
            return super.visitFile(path, attrs)
        }
    })

    return files
}

internal fun TemporaryFolder.emptyZipFile() = newFile("${System.currentTimeMillis()}.zip").apply {
    FileOutputStream(this).use { fos ->
        ZipOutputStream(fos).run {
            close()
        }
    }
}
