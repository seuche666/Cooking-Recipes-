package de.rezeptbuch.app.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.rezeptbuch.app.BuildConfig
import de.rezeptbuch.app.data.backup.model.BackupFile
import de.rezeptbuch.app.data.backup.model.BackupIngredient
import de.rezeptbuch.app.data.backup.model.BackupNote
import de.rezeptbuch.app.data.backup.model.BackupRecipe
import de.rezeptbuch.app.data.backup.model.BackupStep
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.StepEntity
import de.rezeptbuch.app.data.photo.PhotoStorage
import de.rezeptbuch.app.domain.model.ExportResult
import de.rezeptbuch.app.domain.model.ImportMode
import de.rezeptbuch.app.domain.model.ImportResult
import de.rezeptbuch.app.domain.model.IngredientUnit
import de.rezeptbuch.app.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val photoStorage: PhotoStorage,
) : BackupRepository {

    private val dao = db.recipeDao()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // neuere Felder einer späteren Minor-Version ignorieren
        encodeDefaults = true
    }

    // ---------------------------------------------------------------- Export

    override suspend fun export(target: Uri, onProgress: (Float) -> Unit): ExportResult = withContext(Dispatchers.IO) {
        val recipes = dao.getAllRecipesWithDetails()
        val photos = recipes.mapNotNull { r -> r.recipe.photoFileName?.takeIf { photoStorage.fileFor(it).exists() } }
        val totalSteps = (photos.size + 1).toFloat()

        val backup = BackupFile(
            formatVersion = BackupFormat.CURRENT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = Instant.now().toString(),
            recipes = recipes.map { r ->
                val photo = r.recipe.photoFileName?.takeIf { it in photos }
                BackupRecipe(
                    uuid = r.recipe.uuid,
                    title = r.recipe.title,
                    photo = photo?.let { BackupFormat.IMAGE_DIR + it },
                    baseServings = r.recipe.baseServings,
                    cookTimeMinutes = r.recipe.cookTimeMinutes,
                    createdAt = r.recipe.createdAt,
                    updatedAt = r.recipe.updatedAt,
                    ingredients = r.ingredients.sortedBy { it.position }
                        .map { BackupIngredient(it.amount, it.unit.name, it.name) },
                    steps = r.steps.sortedBy { it.position }.map { BackupStep(it.text, it.timerSeconds) },
                    notes = r.notes.map { BackupNote(it.text, it.createdAt, it.updatedAt) },
                )
            },
        )

        try {
            val out = context.contentResolver.openOutputStream(target, "wt")
                ?: throw BackupException("Speicherort konnte nicht geöffnet werden.")
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(BackupFormat.JSON_ENTRY))
                zip.write(json.encodeToString(BackupFile.serializer(), backup).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                onProgress(1 / totalSteps)

                photos.forEachIndexed { index, name ->
                    zip.putNextEntry(ZipEntry(BackupFormat.IMAGE_DIR + name))
                    photoStorage.fileFor(name).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    onProgress((index + 2) / totalSteps)
                }
            }
        } catch (e: IOException) {
            throw BackupException("Backup konnte nicht geschrieben werden: ${e.localizedMessage}", e)
        }
        ExportResult(recipeCount = recipes.size, photoCount = photos.size)
    }

    // ---------------------------------------------------------------- Import

    override suspend fun import(source: Uri, mode: ImportMode, onProgress: (Float) -> Unit): ImportResult =
        withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                // Phase 1 (0–50 %): ZIP lesen, Bilder in temporären Ordner entpacken
                val (backup, images) = readZip(source, tempDir) { onProgress(it * 0.5f) }

                // Phase 2: Bilder in den Fotoordner übernehmen (neue Dateinamen → keine Kollisionen)
                val newPhotoNames = mutableMapOf<String, String>()
                backup.recipes.mapNotNull { it.photo }.distinct().forEach { path ->
                    images[path]?.let { file ->
                        newPhotoNames[path] = file.inputStream().use { photoStorage.saveRaw(it, file.extension.ifBlank { "jpg" }) }
                    }
                }
                onProgress(0.6f)

                val oldPhotos = if (mode == ImportMode.REPLACE) dao.getAllPhotoFileNames() else emptyList()
                val replacedPhotos = mutableListOf<String>()
                var imported = 0
                var updated = 0
                var skipped = 0

                try {
                    db.withTransaction {
                        if (mode == ImportMode.REPLACE) dao.deleteAll()
                        backup.recipes.forEachIndexed { index, r ->
                            val photoName = r.photo?.let { newPhotoNames[it] }
                            // Bei REPLACE greift das nur für doppelte UUIDs innerhalb des Backups
                            val existing = dao.findByUuid(r.uuid)
                            when {
                                existing == null -> {
                                    insertRecipe(r, photoName)
                                    imported++
                                }
                                r.updatedAt > existing.updatedAt -> {
                                    dao.deleteRecipe(existing.id)
                                    existing.photoFileName?.let(replacedPhotos::add)
                                    insertRecipe(r, photoName)
                                    updated++
                                }
                                else -> {
                                    photoName?.let(photoStorage::delete) // nicht benötigtes Bild verwerfen
                                    skipped++
                                }
                            }
                            onProgress(0.6f + 0.4f * (index + 1) / backup.recipes.size.coerceAtLeast(1))
                        }
                    }
                } catch (e: Exception) {
                    newPhotoNames.values.forEach(photoStorage::delete) // Rollback der Bilddateien
                    throw e
                }
                // Erst nach erfolgreicher Transaktion alte Bilder löschen
                (oldPhotos + replacedPhotos).forEach(photoStorage::delete)
                onProgress(1f)
                ImportResult(imported = imported, updated = updated, skipped = skipped)
            } catch (e: BackupException) {
                throw e
            } catch (e: SerializationException) {
                throw BackupException("Die Backup-Datei ist beschädigt oder hat ein unbekanntes Format.", e)
            } catch (e: IllegalArgumentException) {
                throw BackupException("Die Backup-Datei ist beschädigt oder hat ein unbekanntes Format.", e)
            } catch (e: IOException) {
                throw BackupException("Datei konnte nicht gelesen werden: ${e.localizedMessage}", e)
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private suspend fun insertRecipe(r: BackupRecipe, photoName: String?) {
        val now = System.currentTimeMillis()
        val id = dao.insertRecipe(
            RecipeEntity(
                uuid = r.uuid.ifBlank { UUID.randomUUID().toString() },
                title = r.title.ifBlank { "Unbenanntes Rezept" },
                photoFileName = photoName,
                baseServings = r.baseServings.coerceIn(1, 99),
                cookTimeMinutes = r.cookTimeMinutes,
                createdAt = r.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = r.updatedAt.takeIf { it > 0 } ?: now,
            ),
        )
        dao.insertIngredients(
            r.ingredients.mapIndexed { i, ing ->
                IngredientEntity(recipeId = id, position = i, amount = ing.amount, unit = IngredientUnit.fromName(ing.unit), name = ing.name)
            },
        )
        dao.insertSteps(r.steps.mapIndexed { i, s -> StepEntity(recipeId = id, position = i, text = s.text, timerSeconds = s.timerSeconds) })
        dao.insertNotes(
            r.notes.map {
                NoteEntity(
                    recipeId = id,
                    text = it.text,
                    createdAt = it.createdAt.takeIf { t -> t > 0 } ?: now,
                    updatedAt = it.updatedAt.takeIf { t -> t > 0 } ?: now,
                )
            },
        )
    }

    /**
     * Liest das ZIP vollständig. Einträge werden nur als Schlüssel verwendet und nie als Pfad
     * interpretiert (Schutz vor „Zip Slip“). Bilder landen unter zufälligen Namen in [tempDir].
     */
    private fun readZip(source: Uri, tempDir: File, onProgress: (Float) -> Unit): Pair<BackupFile, Map<String, File>> {
        val totalSize = querySize(source)
        val input = context.contentResolver.openInputStream(source)
            ?: throw BackupException("Datei konnte nicht geöffnet werden.")
        val counting = CountingInputStream(input.buffered())
        var jsonText: String? = null
        val images = mutableMapOf<String, File>()

        ZipInputStream(counting).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == BackupFormat.JSON_ENTRY -> jsonText = zip.readLimited(MAX_JSON_BYTES).toString(Charsets.UTF_8)
                    name.startsWith(BackupFormat.IMAGE_DIR) -> {
                        val ext = name.substringAfterLast('.', "jpg").take(5).filter { it.isLetterOrDigit() }
                        val file = File(tempDir, "${UUID.randomUUID()}.$ext")
                        file.outputStream().use { zip.copyTo(it) }
                        images[name] = file
                    }
                }
                if (totalSize > 0) onProgress((counting.count.toFloat() / totalSize).coerceIn(0f, 1f))
                entry = zip.nextEntry
            }
        }

        val text = jsonText ?: throw BackupException("Keine gültige Backup-Datei: „${BackupFormat.JSON_ENTRY}“ fehlt.")
        val raw: JsonObject = json.parseToJsonElement(text).jsonObject
        val migrated = BackupMigrations.migrate(raw)
        onProgress(1f)
        return json.decodeFromJsonElement(BackupFile.serializer(), migrated) to images
    }

    private fun querySize(uri: Uri): Long =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) throw BackupException("Backup-Daten sind zu groß.")
        }
        return out.toByteArray()
    }

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var count = 0L
            private set

        override fun read(): Int = super.read().also { if (it >= 0) count++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            super.read(b, off, len).also { if (it > 0) count += it }
    }

    private companion object {
        const val MAX_JSON_BYTES = 50 * 1024 * 1024
    }
}
