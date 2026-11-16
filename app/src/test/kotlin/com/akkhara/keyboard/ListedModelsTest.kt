package com.akkhara.keyboard

import com.akkhara.keyboard.translation.ListedModels
import com.akkhara.keyboard.translation.ListedModels.Gemma4E2B
import com.akkhara.keyboard.translation.ListedModels.HyMT2
import com.akkhara.keyboard.translation.ListedModels.Qwen35_0_8B
import com.akkhara.keyboard.translation.ListedModels.Qwen35_2B
import com.akkhara.keyboard.translation.ModelShelf
import com.akkhara.keyboard.translation.TranslationModel
import com.akkhara.keyboard.translation.grammarCheckModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListedModelsTest {

    @Test
    fun `the listed models are smallest first`() {
        assertEquals(
            listOf(
                "Qwen3.5 0.8B",
                "Qwen3 1.7B",
                "Hy-MT2 1.8B",
                "Qwen3.5 2B",
                "Gemma 4 E2B",
                "Qwen3 4B Instruct 2507",
                "Qwen3.5 4B",
                "Gemma 4 E4B",
            ),
            ListedModels.all.map { it.name },
        )
        val sizes = ListedModels.all.map { it.sizeBytes }
        assertEquals(sizes.sorted(), sizes)
    }

    @Test
    fun `every listed model is pinned to a commit with its checksum and comes from litert-community`() {
        val pinned = Regex("https://huggingface\\.co/litert-community/[^/]+/resolve/[0-9a-f]{40}/[^/]+\\.litertlm")
        for (model in ListedModels.all) {
            val source = checkNotNull(model.source) { model.name }
            assertTrue(source.url, pinned.matches(source.url))
            assertTrue(model.name, Regex("[0-9a-f]{64}").matches(source.sha256))
            assertTrue(model.name, model.sizeBytes > 500_000_000)
        }
        assertEquals(ListedModels.all.size, ListedModels.all.map { it.id }.toSet().size)
    }

    @Test
    fun `Hy-MT2 is the only listed model that translates only`() {
        assertEquals(listOf(HyMT2), ListedModels.all.filter { it.translatesOnly })
    }

    @Test
    fun `the model in use checks grammar when it can`() {
        assertEquals(Qwen35_2B, grammarCheckModel(inUse = Qwen35_2B, onPhone = listOf(Qwen35_0_8B, Qwen35_2B)))
    }

    @Test
    fun `while Hy-MT2 is in use, the smallest model on the phone that can checks grammar`() {
        assertEquals(Qwen35_0_8B, grammarCheckModel(inUse = HyMT2, onPhone = listOf(Gemma4E2B, HyMT2, Qwen35_0_8B)))
    }

    @Test
    fun `an imported model counts as one that checks grammar`() {
        val imported = TranslationModel(ModelShelf.IMPORTED_ID, ModelShelf.IMPORTED_NAME, 1_000, source = null)

        assertEquals(imported, grammarCheckModel(inUse = HyMT2, onPhone = listOf(HyMT2, imported)))
    }

    @Test
    fun `with only Hy-MT2 on the phone, nothing checks grammar`() {
        assertNull(grammarCheckModel(inUse = HyMT2, onPhone = listOf(HyMT2)))
        assertNull(grammarCheckModel(inUse = null, onPhone = emptyList()))
    }
}
