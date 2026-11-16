package net.kazi.kazimod.client.asriel;

/** Exercise the production thirty-frame jitter and type-on clock without rendering. */
public final class ChaosBeamTitleMotionTest {
    private static int checks;
    public static void main(String[] args) {
        require(ChaosBeamTitleMotion.LAYERS==7,"Seven echo layers");
        require(ChaosBeamTitleMotion.DURATION_TICKS==134,"Title lasts 134 game ticks");
        String text="WHY WONT YOU LET ME WIN";
        int length=text.length();
        require(ChaosBeamTitleMotion.visibleCharacters(0,length)==0,"Title starts before the first character");
        require(ChaosBeamTitleMotion.visibleCharacters(4D/3D-.001,length)==0,"First character waits for the original two-frame alarm");
        for(int letter=1;letter<=length;letter++) {
            double age=4D/3D+(letter-1)*4D;
            require(ChaosBeamTitleMotion.visibleCharacters(age,length)==letter,"One new character every four ticks: expected "+letter+" at "+age+", got "+ChaosBeamTitleMotion.visibleCharacters(age,length));
            require(ChaosBeamTitleMotion.visibleCharacters(age-.001,length)==letter-1,"No early character reveal");
        }
        require(ChaosBeamTitleMotion.visibleCharacters(133.999,length)==length,"Complete text remains visible to the deadline");
        require(ChaosBeamTitleMotion.active(133.999)&&!ChaosBeamTitleMotion.active(134),"Exclusive expiry boundary");
        for(double age:new double[]{-1,134,500,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            require(!ChaosBeamTitleMotion.active(age),"Invalid and expired ages are inactive");
            require(ChaosBeamTitleMotion.visibleCharacters(age,length)==0,"Inactive clocks expose no text");
        }
        require(ChaosBeamTitleMotion.visibleCharacters(80,-4)==0,"Negative text length cannot leak a negative count");
        require(ChaosBeamTitleMotion.visibleCharacters(80,0)==0,"Empty text reveals no characters");
        float[] opacity={.9F,.7F,.5F,.4F,.3F,.2F,.1F};
        int[] spread={2,2,8,12,16,20,24};
        for(int layer=0;layer<7;layer++) {
            require(ChaosBeamTitleMotion.opacity(layer,114)==opacity[layer],"Fade starts at full layer opacity");
            require(ChaosBeamTitleMotion.opacity(layer,124)==opacity[layer]*.5F,"Halfway through fade is half opacity");
            require(ChaosBeamTitleMotion.opacity(layer,133.999)<.000001F,"Text reaches transparency before cleanup");
            require(ChaosBeamTitleMotion.opacity(layer,134)==0,"Expired text stays transparent");
            require(ChaosBeamTitleMotion.opacity(layer)==opacity[layer],"Original seven ghost opacities");
            require(Float.isFinite(ChaosBeamTitleMotion.opacity(layer)),"Opacity remains finite");
        }
        boolean animated=false,seeded=false,axesDiffer=false,fractional=false;
        for(long seed:new long[]{0,1,-1,8732145,Long.MIN_VALUE,Long.MAX_VALUE}) {
            for(int frame=0;frame<200;frame++) {
                double age=(frame+.25)/1.5D;
                double laterSameFrame=(frame+.75)/1.5D;
                for(int layer=0;layer<7;layer++) {
                    for(boolean vertical:new boolean[]{false,true}) {
                        double offset=ChaosBeamTitleMotion.offset(age,layer,vertical,seed);
                        int shift=layer==1&&vertical?-1:0;
                        require(Double.isFinite(offset),"Jitter remains finite");
                        require(offset>=-spread[layer]+shift&&offset<=spread[layer]+shift,"Jitter remains in its layer's original range");
                        require(offset==ChaosBeamTitleMotion.offset(laterSameFrame,layer,vertical,seed),"All render frames inside one thirty-FPS step agree");
                        require(offset==ChaosBeamTitleMotion.offset(age,layer,vertical,seed),"Queries are deterministic and never advance the animation");
                        animated|=offset!=ChaosBeamTitleMotion.offset((frame+1.25)/1.5D,layer,vertical,seed);
                        seeded|=offset!=ChaosBeamTitleMotion.offset(age,layer,vertical,seed+7);
                        fractional|=offset!=Math.rint(offset);
                    }
                    axesDiffer|=ChaosBeamTitleMotion.offset(age,layer,false,seed)!=ChaosBeamTitleMotion.offset(age,layer,true,seed);
                }
            }
        }
        require(animated,"Jitter changes across thirty-FPS frame steps");
        require(seeded,"Distinct announcement seeds vary jitter");
        require(axesDiffer,"Horizontal and vertical jitter are independently varied");
        require(fractional,"Original random-real jitter retains fractional offsets");
        font(text,290,12);
        String finale="I CAN STILL SAVE MY FRIENDS";
        font(finale,340,15);
        for(int letter=1;letter<=finale.length();letter++) {
            double age=4D/3D+(letter-1)*4D;
            require(ChaosBeamTitleMotion.visibleCharacters(age,finale.length())==letter,"Finale reveals each letter on its original type-on clock");
            require(ChaosBeamTitleMotion.visibleCharacters(age-.001,finale.length())==letter-1,"Finale never reveals a letter early");
        }
        require(ChaosBeamTitleMotion.visibleCharacters(133.999,finale.length())==finale.length(),"Complete finale message remains visible until expiry");
        System.out.println("Chaos Beam title motion: "+checks+" checks passed.");
    }
    private static void font(String text,int expectedWidth,int uniqueLetters) {
        require(ChaosBeamTitleFont.width(text)==expectedWidth,"Title uses its exact source pixel advance");
        require(ChaosBeamTitleFont.width("")==0,"Empty title has zero advance");
        require(ChaosBeamTitleFont.index(' ')==0,"Blank slot is the spacing glyph");
        require(ChaosBeamTitleFont.index('?')<0,"Unmapped characters never borrow another glyph");
        int cursor=0;
        java.util.Set<Integer> used=new java.util.HashSet<>();
        for(int i=0;i<text.length();i++) {
            char letter=text.charAt(i);
            int glyph=ChaosBeamTitleFont.index(letter);
            require(glyph>=0,"Every phrase character maps to the source atlas");
            int u=ChaosBeamTitleFont.u(glyph),v=ChaosBeamTitleFont.v(glyph);
            require(u>=0&&v>=0&&u+ChaosBeamTitleFont.glyphWidth(letter)<=ChaosBeamTitleFont.TEXTURE_WIDTH
                    &&v+ChaosBeamTitleFont.GLYPH_HEIGHT<=ChaosBeamTitleFont.TEXTURE_HEIGHT,"Glyph rectangle stays within atlas UV bounds");
            int expected=letter==' '?6:letter=='M'||letter=='W'?16:14;
            require(ChaosBeamTitleFont.advance(letter)==expected,"Original per-letter source advance is retained");
            require(ChaosBeamTitleFont.width(text.substring(0,i))==cursor,"Revealed prefix retains its full-title cursor position");
            cursor+=expected;
            require(ChaosBeamTitleFont.width(text.substring(0,i+1))==cursor,"Next prefix adds exactly one glyph advance");
            used.add(glyph);
        }
        require(used.size()==uniqueLetters,"All phrase characters are used without aliasing");
        require(cursor==expectedWidth,"Incremental reveal and full title have identical total advance");
    }
    private static void require(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
}
