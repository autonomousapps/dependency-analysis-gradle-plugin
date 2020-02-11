package com.autonomousapps.internal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

/**
 * A small parser to read the constant pool directly, in case it contains references
 * ASM does not support.
 * <p>
 * Adapted from http://stackoverflow.com/a/32278587/23691
 * See also http://svn.apache.org/viewvc/maven/shared/trunk/maven-dependency-analyzer/?pathrev=1717974
 * and https://github.com/autonomousapps/dependencyanalysis-gradle-plugin/blob/master/src/main/java/com/intershop/gradle/analysis/analyzer/ConstantPoolParser.java,
 * from which this was translated into Kotlin.
 */
internal object ConstantPoolParser {

    private const val HEAD = 0xcafebabe.toInt()

    // Constant pool types
    private const val CONSTANT_UTF8: Byte = 1
    private const val CONSTANT_INTEGER: Byte = 3
    private const val CONSTANT_FLOAT: Byte = 4
    private const val CONSTANT_LONG: Byte = 5
    private const val CONSTANT_DOUBLE: Byte = 6
    private const val CONSTANT_CLASS: Byte = 7
    private const val CONSTANT_STRING: Byte = 8
    private const val CONSTANT_FIELDREF: Byte = 9
    private const val CONSTANT_METHODREF: Byte = 10
    private const val CONSTANT_INTERFACEMETHODREF: Byte = 11
    private const val CONSTANT_NAME_AND_TYPE: Byte = 12
    private const val CONSTANT_METHODHANDLE: Byte = 15
    private const val CONSTANT_METHOD_TYPE: Byte = 16
    private const val CONSTANT_INVOKE_DYNAMIC: Byte = 18

    private const val OXF0 = 0xf0
    private const val OXE0 = 0xe0
    private const val OX3F = 0x3F

    internal fun getConstantPoolClassReferences(b: ByteArray): Set<String> {
        return parseConstantPoolClassReferences(ByteBuffer.wrap(b))
    }

    private fun parseConstantPoolClassReferences(buf: ByteBuffer): Set<String> {
        if (buf.order(ByteOrder.BIG_ENDIAN).int != HEAD) {
            return emptySet()
        }

        buf.char
        buf.char // minor + ver
        val classes = HashSet<Int>()
        val stringConstants = HashMap<Int, String>()

        var ix = 1
        val num: Int = buf.char.toInt()

        while (ix < num) {
            when (buf.get()) {
                CONSTANT_UTF8 -> stringConstants[ix] = decodeString(buf)
                CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHOD_TYPE -> classes.add(buf.char.toInt())
                CONSTANT_FIELDREF, CONSTANT_METHODREF, CONSTANT_INTERFACEMETHODREF, CONSTANT_NAME_AND_TYPE, CONSTANT_INVOKE_DYNAMIC -> {
                    buf.char
                    buf.char
                }
                CONSTANT_INTEGER -> buf.int
                CONSTANT_FLOAT -> buf.float
                CONSTANT_DOUBLE -> {
                    buf.double
                    ix++
                }
                CONSTANT_LONG -> {
                    buf.long
                    ix++
                }
                CONSTANT_METHODHANDLE -> {
                    buf.get()
                    buf.char
                }
                else -> throw RuntimeException("Unknown constant pool type")
            }
            ix++
        }

        return classes.mapNotNull {
            stringConstants[it]
        }.toSet()
    }

    private fun decodeString(buf: ByteBuffer): String {
        val size = buf.char.toInt()
        val oldLimit = buf.limit()

        buf.limit(buf.position() + size)
        val sb = StringBuilder(size + (size shr 1) + 16)
        while (buf.hasRemaining()) {
            val b = buf.get()
            if (b > 0) {
                sb.append(b.toChar())
            } else {
                val b2 = buf.get()
                if (b and OXF0.toByte() != OXE0.toByte()) {
                    sb.append(((b and 0x1F).toInt() shl 6 or (b2.toInt() and OX3F)).toChar())
                } else {
                    val b3: Int = buf.get().toInt()
                    sb.append(((b and 0x0F).toInt() shl 12 or (b2.toInt() and OX3F shl 6) or (b3 and OX3F)).toChar())
                }
            }
        }
        buf.limit(oldLimit)
        return sb.toString()
    }
}
