package de.rezeptbuch.app.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet Rezeptfotos im privaten App-Speicher (`files/photos`).
 * In der Datenbank wird nur der Dateiname gespeichert.
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val photoDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun fileFor(fileName: String): File = File(photoDir, fileName)

    /** Erzeugt eine Ziel-URI im Cache für die Kamera-App (ActivityResultContracts.TakePicture). */
    fun createCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Kopiert ein Bild (Galerie oder Kamera) verkleinert und korrekt gedreht in den Fotoordner.
     * @return neuer Dateiname
     */
    suspend fun importPhoto(source: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / (sample * 2) >= MAX_EDGE) sample *= 2

        val bitmap = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Bild konnte nicht gelesen werden")

        val rotation = resolver.openInputStream(source)?.use(::readRotation) ?: 0f
        val rotated = if (rotation != 0f) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
        } else {
            bitmap
        }

        val name = "${UUID.randomUUID()}.jpg"
        fileFor(name).outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    }

    /** Speichert rohe Bilddaten aus einem Backup unter neuem Namen. */
    fun saveRaw(input: InputStream, extension: String = "jpg"): String {
        val name = "${UUID.randomUUID()}.$extension"
        fileFor(name).outputStream().use { input.copyTo(it) }
        return name
    }

    fun delete(fileName: String) {
        fileFor(fileName).delete()
    }

    fun deleteAll() {
        photoDir.listFiles()?.forEach { it.delete() }
    }

    fun clearCameraCache() {
        File(context.cacheDir, "camera").listFiles()?.forEach { it.delete() }
    }

    private fun readRotation(stream: InputStream): Float =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    private companion object {
        const val MAX_EDGE = 1600
    }
}
