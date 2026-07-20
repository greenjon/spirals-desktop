package llm.slop.liquidlsd.parameters

import llm.slop.liquidlsd.rendering.Deck
import llm.slop.liquidlsd.rendering.Mandala
import llm.slop.liquidlsd.rendering.Mixer
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ParameterResolverTest {

    @Test
    fun testRegistrationAndPresence() {
        ParameterResolver.register(ParameterDescriptor("Test/Path", "Test", "TestOwner"))
        val descriptors = ParameterResolver.getAllDescriptors()
        assertTrue(descriptors.isNotEmpty(), "Descriptors should not be empty after registration")
        assertTrue(descriptors.any { it.path == "Test/Path" }, "Registered path should be present")
    }

    @Test
    fun testMissingParameterThrows() {
        val mixer = mockk<Mixer>()
        val deck = mockk<Deck>()
        val param = ModulatableParameter(0f)

        every { mixer.deckA } returns deck
        every { mixer.deckB } returns deck
        every { mixer.deckC } returns deck
        
        every { mixer.crossfade } returns param
        every { mixer.masterAlpha } returns param
        every { mixer.bloom } returns param
        every { mixer.xfadeSpeed } returns param
        every { mixer.queuePrev } returns param
        every { mixer.queueNext } returns param
        every { mixer.randDeckA } returns param
        every { mixer.randDeckB } returns param
        every { mixer.randDeckC } returns param
        every { mixer.randAll } returns param
        
        every { deck.fbDecay } returns param
        every { deck.fbGain } returns param
        every { deck.fbZoom } returns param
        every { deck.fbRotate } returns param
        every { deck.fbHueShift } returns param
        every { deck.fbBlur } returns param
        every { deck.fbChroma } returns param
        every { deck.fbMode } returns param

        val mandala = mockk<Mandala>()
        every { deck.source } returns mandala
        every { mandala.globalAlpha } returns param
        // Return an empty map so that getOrElse will fail for "Lobes"
        every { mandala.parameters } returns linkedMapOf()

        val ex = assertFailsWith<IllegalStateException> {
            ParameterResolver.getAllParameterPaths(mixer)
        }
        assertTrue(ex.message!!.contains("Deck A/Geometry/Lobes"), "Exception message should contain the failing path")
    }
}
