package net.kazi.kazimod.shikigami;

public final class SummonHud {
    public static int[] position(int x,int y){
        // Preserve native gauge spacing; overflow wraps upward in two-column rows.
        if(x<4){int row=Math.floorDiv(3-x,70)+1;return new int[]{x+row*70,y-row*52};}
        return new int[]{x,y};
    }
    private SummonHud(){}
}
