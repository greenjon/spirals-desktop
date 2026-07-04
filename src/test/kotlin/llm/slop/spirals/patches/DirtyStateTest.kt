package llm.slop.spirals.patches

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import llm.slop.spirals.rendering.Deck
import llm.slop.spirals.rendering.Mixer
import llm.slop.spirals.models.DeckPatchDto
import llm.slop.spirals.models.GlobalPatchDto
import llm.slop.spirals.models.toDto
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirtyStateTest {

    @BeforeTest
    fun setup() {
        mockkStatic("llm.slop.spirals.models.PatchModelsKt")
        PatchManager.activePresetA = null
        PatchManager.activePresetB = null
        PatchManager.activePresetC = null
        PatchManager.cachedDtoA = null
        PatchManager.cachedDtoB = null
        PatchManager.cachedDtoC = null
        PatchManager.cachedGlobalDto = null
    }

    @Test
    fun testDeckDirtyState() {
        val mixer = mockk<Mixer>()
        val deck = mockk<Deck>()
        
        every { mixer.deckA } returns deck
        every { mixer.deckB } returns mockk()
        every { mixer.deckC } returns mockk()

        // Initial state should not be dirty because cachedDto is null
        assertFalse(PatchManager.isDeckDirty(deck, mixer))
        
        // "Load" a preset by setting cachedDto
        val initialDto = mockk<DeckPatchDto>()
        every { initialDto.name } returns "TestPreset"
        PatchManager.cachedDtoA = initialDto
        PatchManager.activePresetA = "TestPreset"
        
        // Mock toDto to return something EQUAL to initialDto
        every { deck.toDto(any(), any()) } returns initialDto
        
        // Now it should NOT be dirty
        assertFalse(PatchManager.isDeckDirty(deck, mixer))
        
        // Change a parameter (mock toDto to return something different)
        val modifiedDto = mockk<DeckPatchDto>()
        every { modifiedDto.name } returns "TestPreset"
        every { deck.toDto(any(), any()) } returns modifiedDto
        
        // Now it SHOULD be dirty
        assertTrue(PatchManager.isDeckDirty(deck, mixer))
    }

    @Test
    fun testGlobalDirtyState() {
        val mixer = mockk<Mixer>()
        
        val initialDto = mockk<GlobalPatchDto>()
        every { initialDto.name } returns "Untitled Project"
        
        every { mixer.toDto(any()) } returns initialDto
        PatchManager.initializeDefault(mixer)
        
        // Initial state should not be dirty
        assertFalse(PatchManager.isGlobalPatchDirty(mixer))
        
        // Change something
        val modifiedDto = mockk<GlobalPatchDto>()
        every { modifiedDto.name } returns "Untitled Project"
        every { mixer.toDto(any()) } returns modifiedDto
        
        // Now it SHOULD be dirty
        assertTrue(PatchManager.isGlobalPatchDirty(mixer))
    }

    @Test
    fun testRangeDirtyState() {
        // Use real DTO objects to test the equals() method properly
        val realInitial = llm.slop.spirals.models.ParameterDto(0.5f, 0.1f, 0.9f, false, emptyList())
        val realOther = llm.slop.spirals.models.ParameterDto(0.7f, 0.1f, 0.9f, false, emptyList())

        // Verify equals logic directly: since min != max, baseValue 0.5 should equal 0.7
        kotlin.test.assertEquals(realInitial, realOther)
        
        val mixer = mockk<Mixer>()
        val deck = mockk<Deck>()
        every { mixer.deckA } returns deck
        every { mixer.deckB } returns mockk()
        every { mixer.deckC } returns mockk()

        val globalAlpha = llm.slop.spirals.models.ParameterDto(1f, 0f, 1f, false, emptyList())

        // Use real DeckPatchDto objects
        val cachedDeckDto = DeckPatchDto(
            name = "Test",
            visualSourceType = "Mandala",
            parameters = mapOf("Lobes" to realInitial),
            feedbackParameters = emptyMap(),
            globalAlpha = globalAlpha
        )
        
        PatchManager.cachedDtoA = cachedDeckDto
        PatchManager.activePresetA = "Test"

        val currentDeckDto = DeckPatchDto(
            name = "Test",
            visualSourceType = "Mandala",
            parameters = mapOf("Lobes" to realOther),
            feedbackParameters = emptyMap(),
            globalAlpha = globalAlpha
        )

        every { deck.toDto(any(), any()) } returns currentDeckDto

        // Should NOT be dirty because the difference in baseValue is ignored for ranges
        assertFalse(PatchManager.isDeckDirty(deck, mixer))
    }
}
