package com.akkhara.keyboard.translation

import android.content.Context
import java.io.File

/**
 * Where the imported translation model lives: one file in the app's private
 * storage (docs/adr/0004-user-imported-translation-model.md). Nothing else in
 * the app decides this path.
 */
object TranslationModel {
    fun file(context: Context): File = File(File(context.filesDir, "translation"), "model.litertlm")

    fun isInstalled(context: Context): Boolean = file(context).isFile
}
