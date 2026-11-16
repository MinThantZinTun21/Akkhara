package com.akkhara.keyboard.translation

import android.content.Context
import androidx.core.content.edit
import java.io.File

/**
 * A translation model the app can have on the phone: one of Akkhara's listed
 * models, or the one the user imported. See GLOSSARY.md, "Translation model".
 */
data class TranslationModel(
    val id: String,
    val name: String,
    /** The file's size in bytes; for the imported model, the size of the copy on the phone. */
    val sizeBytes: Long,
    /** Where a listed model downloads from; null for the imported model. */
    val source: ModelSource?,
)

/** One file on Hugging Face, pinned to a commit, and the SHA-256 it must have (docs/adr/0005). */
data class ModelSource(val url: String, val sha256: String)

/**
 * Akkhara's fixed list of translation models (docs/adr/0005-fixed-list-of-downloadable-models.md).
 * It is never fetched from anywhere: adding a model means a new release.
 */
object ListedModels {
    val Gemma4E2B = TranslationModel(
        id = "gemma-4-e2b",
        name = "Gemma 4 E2B",
        sizeBytes = 2_588_147_712,
        source = huggingFace(
            repo = "litert-community/gemma-4-E2B-it-litert-lm",
            commit = "2d74ac41a33d7da82f39f7d5720a04c68514a103",
            file = "gemma-4-E2B-it.litertlm",
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        ),
    )

    val Gemma4E4B = TranslationModel(
        id = "gemma-4-e4b",
        name = "Gemma 4 E4B",
        sizeBytes = 3_659_530_240,
        source = huggingFace(
            repo = "litert-community/gemma-4-E4B-it-litert-lm",
            commit = "f254b5ba6e36f7d89cc7892e268e9877bc20f0a0",
            file = "gemma-4-E4B-it.litertlm",
            sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
        ),
    )

    val all = listOf(Gemma4E2B, Gemma4E4B)

    private fun huggingFace(repo: String, commit: String, file: String, sha256: String) =
        ModelSource("https://huggingface.co/$repo/resolve/$commit/$file", sha256)
}

/**
 * The translation models on the phone and the one in use: each has its own
 * file in the app's private storage, and the model in use is kept with the
 * settings. Nothing else in the app decides where a model lives.
 *
 * The keyboard and the app screen run in the same process, so a change made on
 * the screen is seen by the next translation.
 */
class ModelShelf(context: Context) {
    private val folder = File(context.filesDir, "translation")
    private val preferences = context.getSharedPreferences("akkhara", Context.MODE_PRIVATE)

    init {
        takeOverSingleModel()
    }

    /** Every model the app shows: the listed ones, then the imported one once there is one. */
    fun models(): List<TranslationModel> = ListedModels.all + listOfNotNull(imported())

    fun isOnPhone(model: TranslationModel): Boolean = file(model).isFile

    fun file(model: TranslationModel): File = fileFor(model.id)

    /** Where [model] is written while it downloads or is imported, until it passes its checks. */
    fun partialFile(model: TranslationModel): File = File(folder, model.id + PARTIAL)

    /**
     * The model the keyboard translates with, or null while there is none on
     * the phone. When the chosen one is gone, the first model on the phone
     * takes its place.
     */
    fun inUse(): TranslationModel? {
        val onPhone = models().filter(::isOnPhone)
        val chosen = preferences.getString(KEY_IN_USE, null)
        return onPhone.firstOrNull { it.id == chosen } ?: onPhone.firstOrNull().also { use(it) }
    }

    fun use(model: TranslationModel?) {
        preferences.edit {
            if (model == null) remove(KEY_IN_USE) else putString(KEY_IN_USE, model.id)
        }
    }

    /** Puts [model] on the phone from its checked [partial] file. The first model on the phone goes into use. */
    fun install(model: TranslationModel, partial: File): Boolean {
        val done = partial.renameTo(file(model))
        if (done) inUse()
        return done
    }

    /** Deletes [model]'s file. If it was in use, another model on the phone takes its place. */
    fun remove(model: TranslationModel) {
        file(model).delete()
        inUse()
    }

    private fun imported(): TranslationModel? {
        val file = fileFor(IMPORTED_ID)
        return if (file.isFile) TranslationModel(IMPORTED_ID, IMPORTED_NAME, file.length(), source = null) else null
    }

    private fun fileFor(id: String) = File(folder, id + EXTENSION)

    /**
     * Before the list of models there was one file, always Gemma 4 E2B. It
     * becomes Gemma 4 E2B's file instead of being downloaded again.
     */
    private fun takeOverSingleModel() {
        val single = File(folder, "model$EXTENSION")
        val e2b = file(ListedModels.Gemma4E2B)
        if (single.isFile && !e2b.exists()) single.renameTo(e2b)
    }

    companion object {
        const val IMPORTED_ID = "imported"
        const val IMPORTED_NAME = "Imported model"
        private const val EXTENSION = ".litertlm"
        private const val PARTIAL = ".partial.litertlm"
        private const val KEY_IN_USE = "model_in_use"
    }
}
