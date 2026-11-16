package net.kazi.kazimod.client.asriel;

/** Original fnt_main glyph dimensions and spacing for the Chaos Beam announcements. */
final class ChaosBeamTitleFont {
    static final int TEXTURE_WIDTH = 128, TEXTURE_HEIGHT = 128, GLYPH_HEIGHT = 26;
    private static final String GLYPHS = " EHILMNOTUWYACDFRSV";

    private ChaosBeamTitleFont() { }

    static int index(char letter) { return GLYPHS.indexOf(letter); }
    static int u(int index) { return index % 8 * 16; }
    static int v(int index) { return index / 8 * 32; }
    static int glyphWidth(char letter) { return letter == 'M' || letter == 'W' ? 14 : 12; }
    static int advance(char letter) { return letter == ' ' ? 6 : glyphWidth(letter) + 2; }

    static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) width += advance(text.charAt(i));
        return width;
    }
}
