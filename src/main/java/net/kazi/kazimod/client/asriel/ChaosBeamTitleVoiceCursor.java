package net.kazi.kazimod.client.asriel;

/** Consume each newly displayed letter once, regardless of frame rate or overlapping HUD callbacks. */
final class ChaosBeamTitleVoiceCursor {
    private int revealed;

    void start(String text, double age) {
        // A late packet must not replay the letters already revealed by the shared clock.
        revealed = ChaosBeamTitleMotion.visibleCharacters(age, text.length());
    }

    boolean advance(String text, double age) {
        int visible = ChaosBeamTitleMotion.visibleCharacters(age, text.length());
        if (visible <= revealed) return false;
        revealed = visible;
        // If rendering skips several letters, speak only the newest one rather than a burst.
        return !Character.isWhitespace(text.charAt(visible - 1));
    }

    void clear() { revealed = 0; }
}
