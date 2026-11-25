package me.jahnen.libaums.core.fs.fat32

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Covers the fast path added 2026-11-24: a long name that already fits 8.3 cleanly must get an
 * unmangled short name (confirmed on real hardware that a mangled one, e.g. "EX0000~0.PDB" for
 * "EXPORT.PDB", breaks a real Pioneer CDJ/XDJ's direct short-name lookup), while anything that
 * genuinely needs mangling — too long, invalid chars, multiple dots, a too-long extension, or a
 * collision — must still go through the old "XXnnnn~n" scheme exactly as before.
 */
class ShortNameGeneratorTest {

    private fun generate(name: String, existing: Collection<ShortName> = emptyList()) =
        ShortNameGenerator.generateShortName(name, existing).string

    @Test
    fun `name that already fits 8_3 is not mangled`() {
        assertEquals("EXPORT.PDB", generate("EXPORT.PDB"))
        assertEquals("README", generate("README"))
        assertEquals("AB.C", generate("ab.c"))
    }

    @Test
    fun `extension longer than 3 chars is still mangled even though the base name alone would fit`() {
        // Regression: the fast path used to check the extension's length AFTER it had already
        // been truncated to 3, so a long extension (.flac, .jpeg, .html, ...) silently looked
        // like it "fit" and skipped mangling, when real Windows always mangles these.
        val result = generate("track.flac")
        assertNotEquals("TRACK.FLA", result)
        assertEquals("TR0000~0.FLA", result)
    }

    @Test
    fun `name over 8 chars is mangled`() {
        assertEquals("LO0000~0.TXT", generate("longfilename.txt"))
    }

    @Test
    fun `invalid characters are replaced and still mangled, not returned unmangled`() {
        // "a b" -> invalid space is stripped by the caller's own leading logic elsewhere, but a
        // char ShortNameGenerator itself has to replace (e.g. '+') must disqualify the fast path.
        val result = generate("a+b.txt")
        assertNotEquals("A_B.TXT", result)
        assertEquals("A_0000~0.TXT", result)
    }

    @Test
    fun `embedded dot forces mangling even if the result would otherwise fit`() {
        val result = generate("a.b.txt")
        assertNotEquals("A_B.TXT", result)
        assertEquals("A_0000~0.TXT", result)
    }

    @Test
    fun `collision with an existing short name falls back to the tilde scheme`() {
        val existing = listOf(ShortName("README", ""))
        assertEquals("RE0000~0.000", generate("README", existing))
    }

    @Test
    fun `empty or all-invalid-char name never produces an empty short name`() {
        val result = ShortNameGenerator.generateShortName("...", emptyList())
        assertNotEquals("", result.string.trim())
    }
}
