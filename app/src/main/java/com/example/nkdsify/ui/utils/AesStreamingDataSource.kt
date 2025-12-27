package com.example.nkdsify.ui.utils

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec

class AesStreamingDataSource : BaseDataSource(true) {

    private val upstream: DataSource = FileDataSource()
    private var cipherInputStream: InputStream? = null
    private var opened = false
    private var uri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        
        // В CBC режиме мы ВСЕГДА должны начинать с начала файла, чтобы прочитать IV 
        // и инициализировать Cipher, даже если плеер просит данные с середины.
        // Поэтому мы игнорируем dataSpec.position для upstream.open и делаем его сами.
        
        val upstreamDataSpec = dataSpec.buildUpon()
            .setPosition(0) // Всегда читаем с 0, чтобы получить IV
            .setLength(C.LENGTH_UNSET.toLong())
            .build()
            
        val upstreamLength = upstream.open(upstreamDataSpec)
        
        val dataSourceInputStream = object : InputStream() {
            override fun read(): Int {
                val b = ByteArray(1)
                val result = read(b, 0, 1)
                return if (result == -1) -1 else b[0].toInt() and 0xFF
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return upstream.read(b, off, len)
            }
        }

        // 1. Читаем IV (первые 16 байт)
        val iv = ByteArray(16)
        var totalIvRead = 0
        while (totalIvRead < 16) {
            val read = dataSourceInputStream.read(iv, totalIvRead, 16 - totalIvRead)
            if (read <= 0) throw Exception("Invalid IV: unexpected end of stream")
            totalIvRead += read
        }

        // 2. Инициализируем Cipher
        val cipher = Cipher.getInstance(CryptoUtils.TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, CryptoUtils.getSecretKey(), IvParameterSpec(iv))
        cipherInputStream = CipherInputStream(dataSourceInputStream, cipher)
        
        // 3. Если плеер просил данные не с 0 (Seek), "проматываем" ненужные байты
        if (dataSpec.position > 0) {
            var bytesToSkip = dataSpec.position
            val skipBuffer = ByteArray(4096)
            while (bytesToSkip > 0) {
                val toRead = minOf(bytesToSkip, skipBuffer.size.toLong()).toInt()
                val read = cipherInputStream!!.read(skipBuffer, 0, toRead)
                if (read == -1) break
                bytesToSkip -= read
            }
        }

        opened = true
        transferStarted(dataSpec)

        return if (upstreamLength != C.LENGTH_UNSET.toLong()) {
            val contentSize = upstreamLength - 16
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                contentSize - dataSpec.position
            }
        } else {
            C.LENGTH_UNSET.toLong()
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val read = cipherInputStream?.read(buffer, offset, length) ?: -1
        if (read > 0) {
            bytesTransferred(read)
        }
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (opened) {
            opened = false
            cipherInputStream?.close()
            cipherInputStream = null
            upstream.close()
            transferEnded()
        }
    }
}

class AesDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource = AesStreamingDataSource()
}
