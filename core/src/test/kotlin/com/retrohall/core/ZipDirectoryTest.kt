package com.retrohall.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.StandardOpenOption
import java.nio.channels.FileChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipDirectoryTest {
    private fun zipFile(entries: Map<String, ByteArray>, comment: String? = null): File {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                comment?.let { zip.setComment(it) }
                entries.forEach { (name, data) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        return File.createTempFile("zipdir", ".zip").apply {
            deleteOnExit()
            writeBytes(bytes)
        }
    }

    private fun list(file: File, limit: Int = 256) =
        FileChannel.open(file.toPath(), StandardOpenOption.READ).use { ZipDirectory.listEntries(it, limit) }

    @Test
    fun `lists entries in order`() {
        val file = zipFile(mapOf("a/" to ByteArray(0), "a/game.gba" to ByteArray(4096) { it.toByte() }, "readme.txt" to "hi".toByteArray()))
        assertEquals(listOf("a/", "a/game.gba", "readme.txt"), list(file))
    }

    @Test
    fun `handles archive comments and unicode names`() {
        val file = zipFile(mapOf("魂斗罗.nes" to ByteArray(16)), comment = "packed by someone")
        assertEquals(listOf("魂斗罗.nes"), list(file))
    }

    @Test
    fun `respects the limit`() {
        val file = zipFile((1..10).associate { "f$it.bin" to ByteArray(1) })
        assertEquals(listOf("f1.bin", "f2.bin", "f3.bin"), list(file, limit = 3))
    }

    @Test
    fun `rejects files that are not zips`() {
        val file = File.createTempFile("notzip", ".zip").apply {
            deleteOnExit()
            writeBytes(ByteArray(1000) { 7 })
        }
        assertNull(list(file))
    }
}
