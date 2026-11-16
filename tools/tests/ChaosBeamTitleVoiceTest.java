package net.kazi.kazimod.client.asriel;

/** Sound is driven by the same revealed characters used by every overlay rendering path. */
public final class ChaosBeamTitleVoiceTest {
    private static int checks;
    private static final String TEXT = "WHY WONT YOU LET ME WIN";

    public static void main(String[] args) {
        phrase(TEXT, 18);
        phrase("I CAN STILL SAVE MY FRIENDS", 22);
        ChaosBeamTitleVoiceCursor cursor = new ChaosBeamTitleVoiceCursor();
        cursor.start(TEXT, 40);
        require(!cursor.advance(TEXT, 40), "Late packets do not replay their visible prefix");
        require(cursor.advance(TEXT, 42), "Next new letter after a late packet still sounds");
        require(!cursor.advance(TEXT, 42), "Paused or repeated frame cannot restart the sample");
        cursor.start(TEXT, 0);
        require(cursor.advance(TEXT, 70), "A skipped section produces at most the newest cue");
        require(!cursor.advance(TEXT, 70), "Skipped letters never produce a follow-up backlog");
        require(!cursor.advance(TEXT, 134), "Expired text cannot produce a sound");
        cursor.clear();
        cursor.start(TEXT, 0);
        require(cursor.advance(TEXT, 2), "A new cast can speak its first letter after cleanup");
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY})
            require(!cursor.advance(TEXT, invalid), "Invalid ages stay silent");
        System.out.println("Chaos Beam title voice: " + checks + " checks passed.");
    }

    private static void phrase(String text, int expectedCues) {
        for (int fps : new int[]{10, 20, 30, 60, 144, 240}) {
            ChaosBeamTitleVoiceCursor cursor = new ChaosBeamTitleVoiceCursor();
            cursor.start(text, 0);
            int cues = 0;
            for (int frame = 0; frame <= fps * 7; frame++) {
                double age = frame * 20D / fps;
                if (cursor.advance(text, age)) {
                    cues++;
                    int visible = ChaosBeamTitleMotion.visibleCharacters(age, text.length());
                    require(text.charAt(visible - 1) != ' ', "Spaces never trigger a voice clip");
                }
                require(!cursor.advance(text, age), "Repeated HUD/render callbacks cannot duplicate a letter sound");
            }
            require(cues == expectedCues, "Every non-space letter sounds exactly once at " + fps + " FPS");
        }
    }

    private static void require(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
