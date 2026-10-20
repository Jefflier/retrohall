package com.retrohall.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel

/**
 * Lists the entries of a zip archive by reading only its central directory at the end of the
 * file. Scanning a folder of large arcade sets this way avoids streaming every archive.
 */
object ZipDirectory {
    private const val END_OF_DIRECTORY = 0x06054b50
    private const val DIRECTORY_ENTRY = 0x02014b50
    private const val END_RECORD_SIZE = 22
    private const val MAX_COMMENT = 0xFFFF

    /** Returns up to [limit] entry names, or null when the data is not a readable zip archive. */
    fun listEntries(channel: SeekableByteChannel, limit: Int = 256): List<String>? {
        val size = channel.size()
        if (size < END_RECORD_SIZE) return null

        val tailSize = minOf(size, (END_RECORD_SIZE + MAX_COMMENT).toLong()).toInt()
        val tail = read(channel, size - tailSize, tailSize) ?: return null
        val endOffset = (tailSize - END_RECORD_SIZE downTo 0).firstOrNull { tail.getInt(it) == END_OF_DIRECTORY }
            ?: return null

        val entryCount = tail.getShort(endOffset + 10).toInt() and 0xFFFF
        val directorySize = tail.getInt(endOffset + 12).toLong() and 0xFFFFFFFFL
        val directoryOffset = tail.getInt(endOffset + 16).toLong() and 0xFFFFFFFFL
        // ZIP64 archives store 0xFFFFFFFF here; they are rare for ROMs and not supported.
        if (directoryOffset + directorySize > size || directorySize > Int.MAX_VALUE) return null

        val directory = read(channel, directoryOffset, directorySize.toInt()) ?: return null
        val names = ArrayList<String>(minOf(entryCount, limit))
        var position = 0
        while (names.size < minOf(entryCount, limit) && position + 46 <= directory.limit()) {
            if (directory.getInt(position) != DIRECTORY_ENTRY) return null
            val flags = directory.getShort(position + 8).toInt() and 0xFFFF
            val nameLength = directory.getShort(position + 28).toInt() and 0xFFFF
            val extraLength = directory.getShort(position + 30).toInt() and 0xFFFF
            val commentLength = directory.getShort(position + 32).toInt() and 0xFFFF
            if (position + 46 + nameLength > directory.limit()) return null

            val nameBytes = ByteArray(nameLength)
            directory.position(position + 46)
            directory.get(nameBytes)
            // Bit 11 marks UTF-8 names; legacy archives use CP437, which matches ASCII for ROM names.
            val charset = if (flags and 0x0800 != 0) Charsets.UTF_8 else Charsets.ISO_8859_1
            names += String(nameBytes, charset)
            position += 46 + nameLength + extraLength + commentLength
        }
        return names
    }

    private fun read(channel: SeekableByteChannel, offset: Long, length: Int): ByteBuffer? {
        val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(offset)
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) return null
        }
        buffer.flip()
        return buffer
    }
}
