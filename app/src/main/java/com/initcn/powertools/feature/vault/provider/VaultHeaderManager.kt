package com.initcn.powertools.feature.vault.provider

import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object VaultHeaderManager {
    private const val MAGIC_NUMBER = 0x5641554C // "VAUL"
    private const val HEADER_VERSION = 1

    data class FileMetadata(val originalName: String, val mimeType: String)

    fun writeHeaderToStream(outputStream: OutputStream, name: String, mime: String) {
        val json = JSONObject().apply {
            put("v", HEADER_VERSION)
            put("n", name)
            put("m", mime)
        }.toString()

        val headerBytes = json.toByteArray(Charsets.UTF_8)
        val headerBuffer = ByteBuffer.allocate(8 + headerBytes.size)
        headerBuffer.putInt(MAGIC_NUMBER)
        headerBuffer.putInt(headerBytes.size)
        headerBuffer.put(headerBytes)

        outputStream.write(headerBuffer.array())
    }

    fun readHeaderFromStream(inputStream: InputStream): FileMetadata? {
        try {
            val magicBuffer = ByteArray(4)
            // FIX: Defensive length testing to smoothly catch unfinished streaming attempts
            var totalRead = inputStream.read(magicBuffer)
            if (totalRead != 4) return null

            val magic = ByteBuffer.wrap(magicBuffer).int
            if (magic != MAGIC_NUMBER) return null

            val lengthBuffer = ByteArray(4)
            totalRead = inputStream.read(lengthBuffer)
            if (totalRead != 4) return null
            val length = ByteBuffer.wrap(lengthBuffer).int
            if (length <= 0 || length > 10 * 1024 * 1024) return null // 10MB sanity bounds check

            val dataBuffer = ByteArray(length)
            var bytesRead = 0
            while (bytesRead < length) {
                val read = inputStream.read(dataBuffer, bytesRead, length - bytesRead)
                if (read == -1) return null // Unexpected EOF
                bytesRead += read
            }

            val json = JSONObject(String(dataBuffer, Charsets.UTF_8))
            return FileMetadata(json.getString("n"), json.getString("m"))
        } catch (_: Exception) {
            return null
        }
    }


}