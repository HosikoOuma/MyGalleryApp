package com.example.nkdsify.ui.utils

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import com.example.nkdsify.ui.utils.SecretRepository.getSecretFolder
import java.io.ByteArrayOutputStream
import java.io.File

class EncryptedImageDecoder(
    private val context: Context,
    private val source: SourceResult,
    private val originalUri: Uri // Pass the original URI from the factory
) : Decoder {

    private fun getCacheFile(originalPath: String): File {
        val cacheDir = context.cacheDir.resolve("decrypted_thumbnails")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val fileName = File(originalPath).name
        return File(cacheDir, "$fileName.decrypted")
    }

    override suspend fun decode(): DecodeResult {
        val originalFilePath = originalUri.path!!
        val cacheFile = getCacheFile(originalFilePath)

        val decryptedBytes = if (cacheFile.exists()) {
            cacheFile.readBytes()
        } else {
            val thumbFile = File(originalFilePath + ".thumb")
            val decrypted = ByteArrayOutputStream().use { outputStream ->
                thumbFile.inputStream().use { inputStream ->
                    CryptoUtils.decrypt(inputStream, outputStream)
                }
                outputStream.toByteArray()
            }
            cacheFile.writeBytes(decrypted)
            decrypted
        }

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(decryptedBytes))
        } else {
            @Suppress("DEPRECATION")
            android.graphics.BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        }

        return DecodeResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false
        )
    }

    class Factory(private val context: Context) : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // CORRECT WAY: Get the file path from the source result.
            val path = result.source.file().toString()
            val uri = File(path).toUri()

            if (uri.scheme != "file") {
                return null // Not a file URI, let other decoders handle it.
            }

            val secretFolderPath = try {
                getSecretFolder(context).path
            } catch (e: Exception) {
                return null // Could not get secret folder path
            }

            if (!path.startsWith(secretFolderPath)) {
                return null // Not our file, let other decoders handle it.
            }

            // It's our file, create the decoder and pass the original URI
            return EncryptedImageDecoder(context, result, uri)
        }
    }
}
