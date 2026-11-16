package net.kazi.kazimod.entities.projectiles;

public final class BabylonHitCooldownTest {
    public static void main(String[] args) {
        sharedWindow();
        repeatedSuccessfulHit();
        System.out.println("Babylon hit cooldown checks passed.");
    }

    private static void sharedWindow() {
        BabylonHitCooldown<Object> cooldown = new BabylonHitCooldown<>();
        Object firstEnemy = new Object();
        Object secondEnemy = new Object();
        int firstSword = 1;
        int secondSword = 2;
        int thirdSword = 3;

        check(!cooldown.blocks(firstEnemy, firstSword, 100), "Initial hit must be allowed");
        cooldown.record(firstEnemy, firstSword, 100);
        for (long tick = 100; tick < 110; tick++) {
            check(!cooldown.blocks(firstEnemy, firstSword, tick), "Native rules handle the same sword");
            check(cooldown.blocks(firstEnemy, secondSword, tick), "Second sword must be blocked at " + tick);
            check(cooldown.blocks(firstEnemy, thirdSword, tick), "Third sword must be blocked at " + tick);
            if (tick == 104) {
                check(!cooldown.blocks(secondEnemy, secondSword, tick), "Second enemy can be hit independently");
                cooldown.record(secondEnemy, secondSword, tick);
            }
        }

        check(!cooldown.blocks(firstEnemy, secondSword, 110), "Blocked attempts must not extend the deadline");
        check(!cooldown.blocks(firstEnemy, thirdSword, 110), "Checking availability must not start a new window");
        cooldown.record(firstEnemy, secondSword, 110);
        check(cooldown.blocks(firstEnemy, firstSword, 110), "The previous sword now counts as a different sword");
        check(!cooldown.blocks(firstEnemy, secondSword, 110), "Native rules handle the new successful sword");
        check(cooldown.blocks(secondEnemy, thirdSword, 113), "Second enemy keeps its own deadline");
        check(!cooldown.blocks(secondEnemy, thirdSword, 114), "Second enemy's window expires independently");
        check(cooldown.blocks(firstEnemy, thirdSword, 119), "A later hit protects the first enemy through tick 119");
        check(!cooldown.blocks(firstEnemy, thirdSword, 120), "The refreshed window expires at tick 120");
    }

    private static void repeatedSuccessfulHit() {
        BabylonHitCooldown<Object> cooldown = new BabylonHitCooldown<>();
        Object enemy = new Object();
        cooldown.record(enemy, 1, 100);
        check(!cooldown.blocks(enemy, 1, 106), "The same sword may attempt another hit");
        cooldown.record(enemy, 1, 106);
        check(cooldown.blocks(enemy, 2, 115), "A repeat success refreshes protection from other swords");
        check(!cooldown.blocks(enemy, 2, 116), "Protection ends ten ticks after the repeat success");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
