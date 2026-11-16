package com.akkhara.keyboard

import com.akkhara.keyboard.translation.ListedModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListedModelsTest {

    @Test
    fun `the listed models are smallest first`() {
        assertEquals(
            listOf(
                "Qwen3.5 0.8B",
                "Qwen3 1.7B",
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
}
