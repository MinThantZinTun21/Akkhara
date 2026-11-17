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
    val asking: Asking = Asking.Chat,
) {
    /** It cannot check grammar. See GLOSSARY.md, "Translates only". */
    val translatesOnly: Boolean get() = asking == Asking.TranslationOnly
}

/**
 * The model that checks grammar: [inUse] when it can, otherwise the smallest
 * model [onPhone] that can, or null when none can.
 */
fun grammarCheckModel(inUse: TranslationModel?, onPhone: List<TranslationModel>): TranslationModel? =
    inUse?.takeUnless { it.translatesOnly } ?: onPhone.filterNot { it.translatesOnly }.minByOrNull { it.sizeBytes }

/** How a model is given the prompt and the text. */
enum class Asking {
    /** The prompt as the system instruction and the text as the user's message. */
    Chat,

    /** As [Chat], with the model's thinking turned off so that only its answer comes back. */
    ChatWithoutThinking,

    /**
     * A model made only to translate (Hy-MT2): no system instruction and no
     * edited prompt, just its maker's own instruction and the text in one message.
     */
    TranslationOnly,
}

/** One file on Hugging Face, pinned to a commit, and the SHA-256 it must have (docs/adr/0005). */
data class ModelSource(val url: String, val sha256: String)

/**
 * Akkhara's fixed list of translation models (docs/adr/0005-fixed-list-of-downloadable-models.md).
 * It is never fetched from anywhere: adding a model means a new release.
 */
object ListedModels {
    val Qwen35_0_8B = TranslationModel(
        id = "qwen3.5-0.8b",
        name = "Qwen3.5 0.8B",
        sizeBytes = 964_657_152,
        source = huggingFace(
            repo = "litert-community/Qwen3.5-0.8B",
            commit = "d31d4139c495584ddf4ca78b07c92dbf7887f66d",
            file = "Qwen3.5-0.8B_int8.litertlm",
            sha256 = "d6702088983b3a997db5299558a37d2fd924a94daf2b09bd42c4e618c6f694a4",
        ),
    )

    val Qwen3_1_7B = TranslationModel(
        id = "qwen3-1.7b",
        name = "Qwen3 1.7B",
        sizeBytes = 1_754_890_240,
        source = huggingFace(
            repo = "litert-community/Qwen3-1.7B",
            commit = "c421cc3775575372ac2ca4aa1bebc97300ae36b6",
            file = "Qwen3_1.7B.litertlm",
            sha256 = "e40599625f3e446d6522674586a40566d6a3a117af608f6e9ea46ac86d326461",
        ),
        asking = Asking.ChatWithoutThinking,
    )

    val HyMT2 = TranslationModel(
        id = "hy-mt2-1.8b",
        name = "Hy-MT2 1.8B",
        sizeBytes = 1_827_065_856,
        source = huggingFace(
            repo = "litert-community/Hy-MT2-1.8B",
            commit = "5162a55929a0d9d646b4e8c21d7d2e9170142c45",
            file = "Hy-MT2-1.8B_int8.litertlm",
            sha256 = "ed88f17831b0b7e1b20ea2350e931e6aa4208d9a558f07dcc8644eb94ca4ccda",
        ),
        asking = Asking.TranslationOnly,
    )

    val Qwen35_2B = TranslationModel(
        id = "qwen3.5-2b",
        name = "Qwen3.5 2B",
        sizeBytes = 2_119_843_840,
        source = huggingFace(
            repo = "litert-community/Qwen3.5-2B",
            commit = "d03e58352b3a5bee21007490748585b86e405f34",
            file = "Qwen3.5-2B_int8.litertlm",
            sha256 = "c2438cb9c050220e667366b4beeca4daac56e693b226420674f53fc40f007c49",
        ),
    )

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

    val Qwen3_4B_Instruct = TranslationModel(
        id = "qwen3-4b-instruct-2507",
        name = "Qwen3 4B Instruct 2507",
        sizeBytes = 2_659_057_664,
        source = huggingFace(
            repo = "litert-community/Qwen3-4B-Instruct-2507",
            commit = "9dc4dd564e5c27452a8237d7725a78f228e1486a",
            file = "qwen3_4b_instruct_2507_mixed_int4.litertlm",
            sha256 = "9e48b165836256f5344d9d044930607b9c47f6ef34e27f82e96881664f3ba2fd",
        ),
    )

    val Qwen35_4B = TranslationModel(
        id = "qwen3.5-4b",
        name = "Qwen3.5 4B",
        sizeBytes = 2_754_166_784,
        source = huggingFace(
            repo = "litert-community/Qwen3.5-4B",
            commit = "f9b390cdefde5edfea1c05dd5ad91fc44b56e61c",
            file = "Qwen3.5-4B_mixed_int4.litertlm",
            sha256 = "878e5d1926ecc4930e24c62de43a7038edeeb0c57446231086b139892984a5fd",
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

    /** Smallest first, so the list reads from what fits any phone to what needs the most room. */
    val all = listOf(Qwen35_0_8B, Qwen3_1_7B, HyMT2, Qwen35_2B, Gemma4E2B, Qwen3_4B_Instruct, Qwen35_4B, Gemma4E4B)

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
    private val folder = File(context.filesDir, FOLDER)

    /** Where Android's DownloadManager puts downloaded models (docs/adr/0006). Null while shared storage is missing. */
    private val downloads: File? = context.getExternalFilesDir(null)?.let { File(it, FOLDER) }
    private val preferences = context.getSharedPreferences("akkhara", Context.MODE_PRIVATE)

    init {
        takeOverSingleModel()
        deleteOldPartialFiles()
    }

    /** Every model the app shows: the listed ones, then the imported one once there is one. */
    fun models(): List<TranslationModel> = ListedModels.all + listOfNotNull(imported())

    fun isOnPhone(model: TranslationModel): Boolean = file(model).isFile

    /** [model]'s file: in the app's own folder, or where it was downloaded. */
    fun file(model: TranslationModel): File {
        val own = fileFor(model.id)
        val downloaded = downloadedFile(model)
        return if (!own.isFile && downloaded?.isFile == true) downloaded else own
    }

    /** Where [model] is written while it is imported, until it passes its checks. */
    fun partialFile(model: TranslationModel): File = File(folder, model.id + PARTIAL)

    /** Where Android downloads [model], until it passes its checks. Null while shared storage is missing. */
    fun downloadFile(model: TranslationModel): File? = downloads?.let { File(it, downloadName(model)) }

    /** [downloadFile]'s path within the app's shared-storage folder, as DownloadManager takes it. */
    fun downloadPath(model: TranslationModel): String = "$FOLDER/${downloadName(model)}"

    private fun downloadName(model: TranslationModel) = model.id + PARTIAL

    /** Where a downloaded [model] is once it passed its checks. */
    private fun downloadedFile(model: TranslationModel): File? = downloads?.let { File(it, model.id + EXTENSION) }

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

    /** The model that checks grammar, or null while no model on the phone can. See [grammarCheckModel]. */
    fun grammarCheckModel(): TranslationModel? = grammarCheckModel(inUse(), models().filter(::isOnPhone))

    fun use(model: TranslationModel?) {
        preferences.edit {
            if (model == null) remove(KEY_IN_USE) else putString(KEY_IN_USE, model.id)
        }
    }

    /** Puts [model] on the phone from its checked [partial] file. The first model on the phone goes into use. */
    fun install(model: TranslationModel, partial: File): Boolean {
        val done = partial.renameTo(File(partial.parentFile, model.id + EXTENSION))
        if (done) inUse()
        return done
    }

    /** Deletes [model]'s file, wherever it is. If it was in use, another model on the phone takes its place. */
    fun remove(model: TranslationModel) {
        fileFor(model.id).delete()
        downloadedFile(model)?.delete()
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

    /**
     * The app's own downloads, before Android did them, left a partial file
     * when they stopped. Android starts over instead, so those go.
     */
    private fun deleteOldPartialFiles() {
        for (model in ListedModels.all) File(folder, model.id + PARTIAL).delete()
    }

    companion object {
        const val IMPORTED_ID = "imported"
        const val IMPORTED_NAME = "Imported model"
        private const val FOLDER = "translation"
        private const val EXTENSION = ".litertlm"
        private const val PARTIAL = ".partial.litertlm"
        private const val KEY_IN_USE = "model_in_use"
    }
}
