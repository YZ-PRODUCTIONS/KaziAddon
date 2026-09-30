package net.kazi.kazimod.models.abilities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.kazi.kazimod.entities.MalevolentShrineEntity;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


public class MalevolentShrineModel extends EntityModel<MalevolentShrineEntity> {
    private float construction = 1.0F;

    public void setConstruction(float progress) { this.construction = progress; }

	private final ModelRenderer bone;
	private final ModelRenderer cube_r1;
	private final ModelRenderer cube_r2;
	private final ModelRenderer cube_r3;
	private final ModelRenderer cube_r4;
	private final ModelRenderer cube_r5;
	private final ModelRenderer cube_r6;
	private final ModelRenderer cube_r7;
	private final ModelRenderer cube_r8;
	private final ModelRenderer cube_r9;
	private final ModelRenderer cube_r10;
	private final ModelRenderer cube_r11;
	private final ModelRenderer cube_r12;
	private final ModelRenderer cube_r13;
	private final ModelRenderer cube_r14;
	private final ModelRenderer cube_r15;
	private final ModelRenderer BullSkull17;
	private final ModelRenderer cube_r16;
	private final ModelRenderer BullSkull18;
	private final ModelRenderer cube_r17;
	private final ModelRenderer BullSkull19;
	private final ModelRenderer cube_r18;
	private final ModelRenderer BullSkull20;
	private final ModelRenderer cube_r19;
	private final ModelRenderer BullSkull21;
	private final ModelRenderer cube_r20;
	private final ModelRenderer BullSkull22;
	private final ModelRenderer cube_r21;
	private final ModelRenderer BullSkull23;
	private final ModelRenderer cube_r22;
	private final ModelRenderer BullSkull24;
	private final ModelRenderer cube_r23;
	private final ModelRenderer bone2;
	private final ModelRenderer cube_r24;
	private final ModelRenderer cube_r25;
	private final ModelRenderer cube_r26;
	private final ModelRenderer cube_r27;
	private final ModelRenderer cube_r28;
	private final ModelRenderer cube_r29;
	private final ModelRenderer cube_r30;
	private final ModelRenderer cube_r31;
	private final ModelRenderer cube_r32;
	private final ModelRenderer cube_r33;
	private final ModelRenderer cube_r34;
	private final ModelRenderer cube_r35;
	private final ModelRenderer cube_r36;
	private final ModelRenderer BullSkull;
	private final ModelRenderer cube_r37;
	private final ModelRenderer BullSkull2;
	private final ModelRenderer cube_r38;
	private final ModelRenderer BullSkull3;
	private final ModelRenderer cube_r39;
	private final ModelRenderer BullSkull4;
	private final ModelRenderer cube_r40;
	private final ModelRenderer BullSkull5;
	private final ModelRenderer cube_r41;
	private final ModelRenderer BullSkull6;
	private final ModelRenderer cube_r42;
	private final ModelRenderer BullSkull7;
	private final ModelRenderer cube_r43;
	private final ModelRenderer BullSkull8;
	private final ModelRenderer cube_r44;
	private final ModelRenderer BullSkull9;
	private final ModelRenderer cube_r45;
	private final ModelRenderer BullSkull10;
	private final ModelRenderer cube_r46;
	private final ModelRenderer BullSkull11;
	private final ModelRenderer cube_r47;
	private final ModelRenderer BullSkull12;
	private final ModelRenderer cube_r48;
	private final ModelRenderer BullSkull13;
	private final ModelRenderer cube_r49;
	private final ModelRenderer BullSkull14;
	private final ModelRenderer cube_r50;
	private final ModelRenderer BullSkull15;
	private final ModelRenderer cube_r51;
	private final ModelRenderer BullSkull16;
	private final ModelRenderer cube_r52;
	private final ModelRenderer bone3;
	private final ModelRenderer bone4;
	private final ModelRenderer cube_r53;
	private final ModelRenderer bone5;
	private final ModelRenderer bone6;
	private final ModelRenderer cube_r54;
	private final ModelRenderer bone7;
	private final ModelRenderer cube_r55;
	private final ModelRenderer bone8;
	private final ModelRenderer cube_r56;
	private final ModelRenderer bone9;
	private final ModelRenderer top;
	private final ModelRenderer cube_r57;
	private final ModelRenderer cube_r58;
	private final ModelRenderer cube_r59;
	private final ModelRenderer cube_r60;
	private final ModelRenderer cube_r61;
	private final ModelRenderer cube_r62;
	private final ModelRenderer cube_r63;
	private final ModelRenderer cube_r64;
	private final ModelRenderer cube_r65;
	private final ModelRenderer cube_r66;
	private final ModelRenderer cube_r67;
	private final ModelRenderer cube_r68;
	private final ModelRenderer cube_r69;
	private final ModelRenderer cube_r70;
	private final ModelRenderer cube_r71;
	private final ModelRenderer cube_r72;
	private final ModelRenderer cube_r73;
	private final ModelRenderer cube_r74;
	private final ModelRenderer cube_r75;
	private final ModelRenderer cube_r76;
	private final ModelRenderer cube_r77;
	private final ModelRenderer cube_r78;
	private final ModelRenderer cube_r79;
	private final ModelRenderer cube_r80;
	private final ModelRenderer cube_r81;
	private final ModelRenderer cube_r82;
	private final ModelRenderer bb_main;
	private final ModelRenderer cube_r83;
	private final ModelRenderer cube_r84;

	public MalevolentShrineModel() {
		texWidth = 128;
		texHeight = 128;

		bone = new ModelRenderer(this);
		bone.setPos(-10.9477F, 22.3315F, -21.5251F);
		bone.texOffs(74, 90).addBox(3.9477F, -14.3315F, 10.5251F, 14.0F, 2.0F, 4.0F, 0.0F, false);
		bone.texOffs(0, 21).addBox(-0.0523F, -14.3315F, 14.5251F, 22.0F, 2.0F, 14.0F, 0.0F, false);
		bone.texOffs(0, 0).addBox(1.9477F, -1.3315F, 12.5251F, 18.0F, 3.0F, 18.0F, 0.0F, false);
		bone.texOffs(0, 89).addBox(3.9477F, -14.3315F, 28.4451F, 14.0F, 2.0F, 4.0F, 0.0F, false);
		bone.texOffs(0, 89).addBox(3.9477F, -16.3315F, 28.4451F, 14.0F, 2.0F, 4.0F, 0.0F, false);

		cube_r1 = new ModelRenderer(this);
		cube_r1.setPos(16.1684F, -9.1817F, 25.5251F);
		bone.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, 0.0F, 2.618F);
		cube_r1.texOffs(0, 12).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r1.texOffs(0, 12).addBox(-2.0F, -2.0F, -9.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r2 = new ModelRenderer(this);
		cube_r2.setPos(18.7665F, -10.6817F, 21.5251F);
		bone.addChild(cube_r2);
		setRotationAngle(cube_r2, 0.0F, 0.0F, 2.618F);
		cube_r2.texOffs(50, 92).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F, 0.0F, false);

		cube_r3 = new ModelRenderer(this);
		cube_r3.setPos(7.9477F, -2.8315F, 21.5251F);
		bone.addChild(cube_r3);
		setRotationAngle(cube_r3, 0.0F, 0.0F, -0.2618F);
		cube_r3.texOffs(0, 12).addBox(-4.1F, -2.9F, -5.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r3.texOffs(0, 21).addBox(-4.1F, -2.9F, 3.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r3.texOffs(50, 92).addBox(-6.1F, -2.9F, -5.0F, 2.0F, 4.0F, 10.0F, 0.0F, false);

		cube_r4 = new ModelRenderer(this);
		cube_r4.setPos(5.7957F, -3.4899F, 21.5251F);
		bone.addChild(cube_r4);
		setRotationAngle(cube_r4, 0.0F, 0.0F, 0.5236F);
		cube_r4.texOffs(0, 0).addBox(-5.1F, -6.9F, 3.0F, 5.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r4.texOffs(0, 6).addBox(-5.1F, -6.9F, -5.0F, 5.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r4.texOffs(26, 92).addBox(-7.1F, -6.9F, -5.0F, 2.0F, 4.0F, 10.0F, 0.0F, false);

		cube_r5 = new ModelRenderer(this);
		cube_r5.setPos(16.0431F, -3.0557F, 25.5251F);
		bone.addChild(cube_r5);
		setRotationAngle(cube_r5, -3.1416F, 0.0F, -2.8798F);
		cube_r5.texOffs(0, 12).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r6 = new ModelRenderer(this);
		cube_r6.setPos(16.0431F, -3.0557F, 17.5251F);
		bone.addChild(cube_r6);
		setRotationAngle(cube_r6, -3.1416F, 0.0F, -2.8798F);
		cube_r6.texOffs(0, 21).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r7 = new ModelRenderer(this);
		cube_r7.setPos(22.6751F, -0.761F, 21.5251F);
		bone.addChild(cube_r7);
		setRotationAngle(cube_r7, -3.1416F, 0.0F, -2.8798F);
		cube_r7.texOffs(50, 92).addBox(3.0F, -2.5F, -5.0F, 2.0F, 4.0F, 10.0F, 0.0F, false);

		cube_r8 = new ModelRenderer(this);
		cube_r8.setPos(7.3066F, -9.5664F, 26.9697F);
		bone.addChild(cube_r8);
		setRotationAngle(cube_r8, -1.5708F, 1.2654F, 1.5708F);
		cube_r8.texOffs(0, 21).addBox(-2.5F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r9 = new ModelRenderer(this);
		cube_r9.setPos(14.3066F, -9.7168F, 27.4466F);
		bone.addChild(cube_r9);
		setRotationAngle(cube_r9, -1.5708F, -1.2654F, -1.5708F);
		cube_r9.texOffs(0, 21).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r10 = new ModelRenderer(this);
		cube_r10.setPos(11.8066F, -7.2595F, 22.9787F);
		bone.addChild(cube_r10);
		setRotationAngle(cube_r10, 1.5708F, -1.2654F, -1.5708F);
		cube_r10.texOffs(88, 96).addBox(7.0F, -3.0F, -3.5F, 2.0F, 4.0F, 9.0F, 0.0F, false);

		cube_r11 = new ModelRenderer(this);
		cube_r11.setPos(17.8066F, -3.4705F, 15.1301F);
		bone.addChild(cube_r11);
		setRotationAngle(cube_r11, 1.5708F, -1.2654F, -1.5708F);
		cube_r11.texOffs(64, 92).addBox(-1.0589F, -1.461F, 2.5F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r12 = new ModelRenderer(this);
		cube_r12.setPos(10.8066F, -3.4705F, 15.1301F);
		bone.addChild(cube_r12);
		setRotationAngle(cube_r12, 1.5708F, -1.2654F, -1.5708F);
		cube_r12.texOffs(0, 95).addBox(-1.0589F, -1.461F, 2.5F, 3.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r12.texOffs(98, 38).addBox(-3.0589F, -1.461F, -4.5F, 2.0F, 4.0F, 9.0F, 0.0F, false);

		cube_r13 = new ModelRenderer(this);
		cube_r13.setPos(7.3232F, -9.8998F, 15.57F);
		bone.addChild(cube_r13);
		setRotationAngle(cube_r13, 1.5708F, 1.3526F, -1.5598F);
		cube_r13.texOffs(0, 21).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r14 = new ModelRenderer(this);
		cube_r14.setPos(14.3066F, -9.0428F, 14.8654F);
		bone.addChild(cube_r14);
		setRotationAngle(cube_r14, 1.5708F, -1.3526F, 1.5708F);
		cube_r14.texOffs(0, 21).addBox(-1.5F, -1.0F, -1.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r15 = new ModelRenderer(this);
		cube_r15.setPos(10.8066F, -9.4452F, 13.0506F);
		bone.addChild(cube_r15);
		setRotationAngle(cube_r15, -1.5708F, 1.3526F, -1.5708F);
		cube_r15.texOffs(88, 96).addBox(-0.3589F, -3.0F, -4.5F, 2.0F, 4.0F, 9.0F, 0.0F, false);

		BullSkull17 = new ModelRenderer(this);
		BullSkull17.setPos(22.8955F, 0.0F, 9.0F);
		bone.addChild(BullSkull17);
		setRotationAngle(BullSkull17, 0.0F, -0.829F, 0.0F);
		BullSkull17.texOffs(24, 95).addBox(-1.0233F, -0.4431F, -2.3077F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r16 = new ModelRenderer(this);
		cube_r16.setPos(4.5817F, -1.2331F, 0.0173F);
		BullSkull17.addChild(cube_r16);
		setRotationAngle(cube_r16, -0.829F, 0.0F, 0.0F);
		cube_r16.texOffs(0, 101).addBox(-3.675F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r16.texOffs(84, 113).addBox(-6.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull18 = new ModelRenderer(this);
		BullSkull18.setPos(14.5817F, 3.7669F, 0.0173F);
		BullSkull17.addChild(BullSkull18);
		BullSkull18.texOffs(0, 109).addBox(-19.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r17 = new ModelRenderer(this);
		cube_r17.setPos(-14.0F, -5.0F, 0.0F);
		BullSkull18.addChild(cube_r17);
		setRotationAngle(cube_r17, -0.829F, 0.0F, 0.0F);
		cube_r17.texOffs(64, 98).addBox(-3.675F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r17.texOffs(68, 98).addBox(-6.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull19 = new ModelRenderer(this);
		BullSkull19.setPos(14.5817F, 3.7669F, 0.0173F);
		BullSkull17.addChild(BullSkull19);
		BullSkull19.texOffs(14, 95).addBox(-7.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r18 = new ModelRenderer(this);
		cube_r18.setPos(-2.0F, -5.0F, 0.0F);
		BullSkull19.addChild(cube_r18);
		setRotationAngle(cube_r18, -0.829F, 0.0F, 0.0F);
		cube_r18.texOffs(92, 96).addBox(-3.675F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r18.texOffs(56, 38).addBox(-6.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull20 = new ModelRenderer(this);
		BullSkull20.setPos(0.0F, 0.0F, 0.0F);
		BullSkull19.addChild(BullSkull20);
		BullSkull20.texOffs(9, 52).addBox(-11.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r19 = new ModelRenderer(this);
		cube_r19.setPos(-6.0F, -5.0F, 0.0F);
		BullSkull20.addChild(cube_r19);
		setRotationAngle(cube_r19, -0.829F, 0.0F, 0.0F);
		cube_r19.texOffs(0, 87).addBox(-3.675F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r19.texOffs(32, 89).addBox(-6.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull21 = new ModelRenderer(this);
		BullSkull21.setPos(-16.2677F, 0.0F, 20.1183F);
		BullSkull17.addChild(BullSkull21);
		setRotationAngle(BullSkull21, 0.0F, 1.6581F, 0.0F);
		BullSkull21.texOffs(55, 108).addBox(-1.0233F, -0.4431F, -2.3077F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r20 = new ModelRenderer(this);
		cube_r20.setPos(1.967F, -1.2331F, -29.8685F);
		BullSkull21.addChild(cube_r20);
		setRotationAngle(cube_r20, -0.829F, 0.0F, 0.0F);
		cube_r20.texOffs(16, 51).addBox(-1.0603F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r20.texOffs(110, 92).addBox(-3.8503F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull22 = new ModelRenderer(this);
		BullSkull22.setPos(11.967F, 3.7669F, -29.8685F);
		BullSkull21.addChild(BullSkull22);
		BullSkull22.texOffs(108, 13).addBox(-16.9903F, -4.21F, 27.5608F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r21 = new ModelRenderer(this);
		cube_r21.setPos(-14.0F, -5.0F, 0.0F);
		BullSkull22.addChild(cube_r21);
		setRotationAngle(cube_r21, -0.829F, 0.0F, 0.0F);
		cube_r21.texOffs(68, 59).addBox(-1.0603F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r21.texOffs(4, 101).addBox(-3.8503F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull23 = new ModelRenderer(this);
		BullSkull23.setPos(11.967F, 3.7669F, -29.8685F);
		BullSkull21.addChild(BullSkull23);
		BullSkull23.texOffs(108, 8).addBox(-4.9903F, -4.21F, 27.5608F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r22 = new ModelRenderer(this);
		cube_r22.setPos(-2.0F, -5.0F, 0.0F);
		BullSkull23.addChild(cube_r22);
		setRotationAngle(cube_r22, -0.829F, 0.0F, 0.0F);
		cube_r22.texOffs(68, 11).addBox(-1.0603F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r22.texOffs(12, 14).addBox(-3.8503F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull24 = new ModelRenderer(this);
		BullSkull24.setPos(0.0F, 0.0F, 0.0F);
		BullSkull23.addChild(BullSkull24);
		BullSkull24.texOffs(107, 106).addBox(-8.9903F, -4.21F, 27.5608F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r23 = new ModelRenderer(this);
		cube_r23.setPos(-6.0F, -5.0F, 0.0F);
		BullSkull24.addChild(cube_r23);
		setRotationAngle(cube_r23, -0.829F, 0.0F, 0.0F);
		cube_r23.texOffs(56, 42).addBox(-1.0603F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r23.texOffs(81, 110).addBox(-3.8503F, -23.1742F, 20.6556F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		bone2 = new ModelRenderer(this);
		bone2.setPos(10.9562F, 1.23F, 21.561F);
		bone.addChild(bone2);
		bone2.texOffs(24, 106).addBox(5.9915F, -13.5615F, -7.0359F, 1.0F, 11.0F, 1.0F, 0.0F, false);
		bone2.texOffs(12, 37).addBox(-7.0085F, -13.5615F, 5.9641F, 1.0F, 11.0F, 1.0F, 0.0F, false);
		bone2.texOffs(16, 37).addBox(5.9915F, -13.5615F, 5.9641F, 1.0F, 11.0F, 1.0F, 0.0F, false);
		bone2.texOffs(41, 59).addBox(-4.1285F, -14.8715F, -5.5359F, 8.0F, 12.0F, 11.0F, 0.0F, false);
		bone2.texOffs(14, 0).addBox(-7.0085F, -13.5615F, -7.0359F, 1.0F, 11.0F, 1.0F, 0.0F, false);

		cube_r24 = new ModelRenderer(this);
		cube_r24.setPos(-7.008F, -12.5615F, -6.5141F);
		bone2.addChild(cube_r24);
		setRotationAngle(cube_r24, 0.0F, -0.8727F, 0.0F);
		cube_r24.texOffs(42, 65).addBox(-2.0F, 0.0F, -0.8218F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(10, 49).addBox(18.7995F, -1.0F, -2.4718F, 3.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(56, 46).addBox(18.7995F, 0.0F, -2.4718F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(72, 92).addBox(18.7995F, 2.0F, -2.4718F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(12, 20).addBox(-2.5005F, 0.0F, -0.8218F, 0.0F, 4.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(98, 38).addBox(20.4995F, 3.0F, -2.9218F, 2.0F, 2.0F, 2.0F, 0.0F, false);
		cube_r24.texOffs(12, 28).addBox(21.4995F, 0.0F, -2.4718F, 0.0F, 4.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(0, 55).addBox(-3.0F, -1.0F, -0.8218F, 3.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r24.texOffs(96, 8).addBox(-1.0F, 2.0F, -0.8218F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r25 = new ModelRenderer(this);
		cube_r25.setPos(6.9911F, -12.5615F, -6.5141F);
		bone2.addChild(cube_r25);
		setRotationAngle(cube_r25, 0.0F, 0.8727F, 0.0F);
		cube_r25.texOffs(0, 33).addBox(-21.7995F, -1.0F, -2.4718F, 3.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(16, 13).addBox(2.5005F, 0.0F, -0.8218F, 0.0F, 4.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(10, 12).addBox(0.0F, -1.0F, -0.8218F, 3.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(12, 24).addBox(-21.4995F, 0.0F, -2.4718F, 0.0F, 4.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(8, 33).addBox(-20.7995F, 0.0F, -2.4718F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(31, 95).addBox(-19.7995F, 2.0F, -2.4718F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(47, 95).addBox(0.0F, 2.0F, -0.8218F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r25.texOffs(68, 63).addBox(0.0F, 0.0F, -0.8218F, 2.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r26 = new ModelRenderer(this);
		cube_r26.setPos(-4.5317F, -13.0615F, 6.3992F);
		bone2.addChild(cube_r26);
		setRotationAngle(cube_r26, 0.0F, 0.0436F, 0.0F);
		cube_r26.texOffs(54, 14).addBox(-1.5F, -0.5F, -0.5F, 6.0F, 2.0F, 1.0F, 0.0F, false);
		cube_r26.texOffs(4, 87).addBox(-1.5F, 1.5F, -0.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r27 = new ModelRenderer(this);
		cube_r27.setPos(-8.3008F, -8.5615F, 8.7202F);
		bone2.addChild(cube_r27);
		setRotationAngle(cube_r27, 0.0F, -2.3562F, 0.0F);
		cube_r27.texOffs(98, 38).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);

		cube_r28 = new ModelRenderer(this);
		cube_r28.setPos(-4.9647F, -13.0615F, 18.3868F);
		bone2.addChild(cube_r28);
		setRotationAngle(cube_r28, 0.0F, 0.0436F, 0.0F);
		cube_r28.texOffs(21, 95).addBox(-1.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r29 = new ModelRenderer(this);
		cube_r29.setPos(-7.5085F, -13.0615F, -18.5359F);
		bone2.addChild(cube_r29);
		setRotationAngle(cube_r29, 0.0F, 1.5708F, 0.0F);
		cube_r29.texOffs(101, 96).addBox(-24.5F, -0.5F, 0.5F, 12.0F, 2.0F, 1.0F, 0.0F, false);

		cube_r30 = new ModelRenderer(this);
		cube_r30.setPos(4.5148F, -13.0615F, 6.3992F);
		bone2.addChild(cube_r30);
		setRotationAngle(cube_r30, 0.0F, -0.0436F, 0.0F);
		cube_r30.texOffs(54, 92).addBox(0.5F, 1.5F, -0.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r30.texOffs(40, 92).addBox(-4.5F, -0.5F, -0.5F, 6.0F, 2.0F, 1.0F, 0.0F, false);

		cube_r31 = new ModelRenderer(this);
		cube_r31.setPos(-8.3687F, -8.5615F, -8.6364F);
		bone2.addChild(cube_r31);
		setRotationAngle(cube_r31, 0.0F, 2.3126F, 0.0F);
		cube_r31.texOffs(98, 38).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);

		cube_r32 = new ModelRenderer(this);
		cube_r32.setPos(4.9477F, -13.0615F, 18.3868F);
		bone2.addChild(cube_r32);
		setRotationAngle(cube_r32, 0.0F, -0.0436F, 0.0F);
		cube_r32.texOffs(6, 64).addBox(0.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r33 = new ModelRenderer(this);
		cube_r33.setPos(4.9041F, -13.0615F, 7.3858F);
		bone2.addChild(cube_r33);
		setRotationAngle(cube_r33, 0.0F, -0.0436F, 0.0F);
		cube_r33.texOffs(67, 16).addBox(0.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		cube_r34 = new ModelRenderer(this);
		cube_r34.setPos(3.9041F, -13.0615F, 6.3858F);
		bone2.addChild(cube_r34);
		setRotationAngle(cube_r34, 0.0F, -0.0436F, 0.0F);
		cube_r34.texOffs(0, 91).addBox(0.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r34.texOffs(58, 31).addBox(-4.5F, -0.5F, -13.5F, 6.0F, 2.0F, 1.0F, 0.0F, false);

		cube_r35 = new ModelRenderer(this);
		cube_r35.setPos(7.4915F, -13.0615F, -18.5359F);
		bone2.addChild(cube_r35);
		setRotationAngle(cube_r35, 0.0F, -1.5708F, 0.0F);
		cube_r35.texOffs(98, 33).addBox(12.5F, -0.5F, 0.5F, 12.0F, 2.0F, 1.0F, 0.0F, false);

		cube_r36 = new ModelRenderer(this);
		cube_r36.setPos(8.3518F, -8.5615F, -8.6364F);
		bone2.addChild(cube_r36);
		setRotationAngle(cube_r36, 0.0F, 0.9163F, 0.0F);
		cube_r36.texOffs(98, 38).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);

		BullSkull = new ModelRenderer(this);
		BullSkull.setPos(8.4771F, -1.2331F, 10.0173F);
		bone.addChild(BullSkull);
		BullSkull.texOffs(107, 69).addBox(-0.605F, 0.79F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r37 = new ModelRenderer(this);
		cube_r37.setPos(0.0F, 0.0F, 0.0F);
		BullSkull.addChild(cube_r37);
		setRotationAngle(cube_r37, -0.829F, 0.0F, 0.0F);
		cube_r37.texOffs(56, 113).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r37.texOffs(60, 113).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull2 = new ModelRenderer(this);
		BullSkull2.setPos(0.0F, 0.0F, 0.0F);
		BullSkull.addChild(BullSkull2);
		BullSkull2.texOffs(106, 87).addBox(-4.605F, 0.79F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r38 = new ModelRenderer(this);
		cube_r38.setPos(-4.0F, 0.0F, 0.0F);
		BullSkull2.addChild(cube_r38);
		setRotationAngle(cube_r38, -0.829F, 0.0F, 0.0F);
		cube_r38.texOffs(113, 51).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r38.texOffs(113, 55).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull3 = new ModelRenderer(this);
		BullSkull3.setPos(0.0F, 0.0F, 0.0F);
		BullSkull.addChild(BullSkull3);
		BullSkull3.texOffs(106, 64).addBox(7.395F, 0.79F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r39 = new ModelRenderer(this);
		cube_r39.setPos(8.0F, 0.0F, 0.0F);
		BullSkull3.addChild(cube_r39);
		setRotationAngle(cube_r39, -0.829F, 0.0F, 0.0F);
		cube_r39.texOffs(13, 113).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r39.texOffs(17, 113).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull4 = new ModelRenderer(this);
		BullSkull4.setPos(0.0F, 0.0F, 0.0F);
		BullSkull3.addChild(BullSkull4);
		BullSkull4.texOffs(48, 106).addBox(3.395F, 0.79F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r40 = new ModelRenderer(this);
		cube_r40.setPos(4.0F, 0.0F, 0.0F);
		BullSkull4.addChild(cube_r40);
		setRotationAngle(cube_r40, -0.829F, 0.0F, 0.0F);
		cube_r40.texOffs(52, 112).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r40.texOffs(9, 113).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull5 = new ModelRenderer(this);
		BullSkull5.setPos(4.4183F, 1.2331F, 22.9827F);
		BullSkull3.addChild(BullSkull5);
		setRotationAngle(BullSkull5, 0.0F, 3.1416F, 0.0F);
		BullSkull5.texOffs(38, 106).addBox(-1.0233F, -0.4431F, -2.3077F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r41 = new ModelRenderer(this);
		cube_r41.setPos(-0.4183F, -1.2331F, 0.0173F);
		BullSkull5.addChild(cube_r41);
		setRotationAngle(cube_r41, -0.829F, 0.0F, 0.0F);
		cube_r41.texOffs(107, 111).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r41.texOffs(111, 111).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull6 = new ModelRenderer(this);
		BullSkull6.setPos(9.5817F, 3.7669F, 0.0173F);
		BullSkull5.addChild(BullSkull6);
		BullSkull6.texOffs(28, 106).addBox(-14.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r42 = new ModelRenderer(this);
		cube_r42.setPos(-14.0F, -5.0F, 0.0F);
		BullSkull6.addChild(cube_r42);
		setRotationAngle(cube_r42, -0.829F, 0.0F, 0.0F);
		cube_r42.texOffs(48, 111).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r42.texOffs(111, 99).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull7 = new ModelRenderer(this);
		BullSkull7.setPos(9.5817F, 3.7669F, 0.0173F);
		BullSkull5.addChild(BullSkull7);
		BullSkull7.texOffs(101, 99).addBox(-2.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r43 = new ModelRenderer(this);
		cube_r43.setPos(-2.0F, -5.0F, 0.0F);
		BullSkull7.addChild(cube_r43);
		setRotationAngle(cube_r43, -0.829F, 0.0F, 0.0F);
		cube_r43.texOffs(111, 42).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r43.texOffs(44, 111).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull8 = new ModelRenderer(this);
		BullSkull8.setPos(0.0F, 0.0F, 0.0F);
		BullSkull7.addChild(BullSkull8);
		BullSkull8.texOffs(78, 101).addBox(-6.605F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r44 = new ModelRenderer(this);
		cube_r44.setPos(-6.0F, -5.0F, 0.0F);
		BullSkull8.addChild(cube_r44);
		setRotationAngle(cube_r44, -0.829F, 0.0F, 0.0F);
		cube_r44.texOffs(111, 38).addBox(1.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r44.texOffs(40, 111).addBox(-1.465F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull9 = new ModelRenderer(this);
		BullSkull9.setPos(-19.5817F, -3.7669F, 12.9827F);
		BullSkull8.addChild(BullSkull9);
		setRotationAngle(BullSkull9, 0.0F, 1.5708F, 0.0F);
		BullSkull9.texOffs(107, 79).addBox(-1.0233F, -0.4431F, -2.3077F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r45 = new ModelRenderer(this);
		cube_r45.setPos(-15.4183F, -1.2331F, 0.0173F);
		BullSkull9.addChild(cube_r45);
		setRotationAngle(cube_r45, -0.829F, 0.0F, 0.0F);
		cube_r45.texOffs(77, 110).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r45.texOffs(36, 111).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull10 = new ModelRenderer(this);
		BullSkull10.setPos(-5.4183F, 3.7669F, 0.0173F);
		BullSkull9.addChild(BullSkull10);
		BullSkull10.texOffs(14, 100).addBox(0.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r46 = new ModelRenderer(this);
		cube_r46.setPos(-14.0F, -5.0F, 0.0F);
		BullSkull10.addChild(cube_r46);
		setRotationAngle(cube_r46, -0.829F, 0.0F, 0.0F);
		cube_r46.texOffs(69, 110).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r46.texOffs(73, 110).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull11 = new ModelRenderer(this);
		BullSkull11.setPos(-5.4183F, 3.7669F, 0.0173F);
		BullSkull9.addChild(BullSkull11);
		BullSkull11.texOffs(85, 98).addBox(12.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r47 = new ModelRenderer(this);
		cube_r47.setPos(-2.0F, -5.0F, 0.0F);
		BullSkull11.addChild(cube_r47);
		setRotationAngle(cube_r47, -0.829F, 0.0F, 0.0F);
		cube_r47.texOffs(103, 109).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r47.texOffs(65, 110).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull12 = new ModelRenderer(this);
		BullSkull12.setPos(0.0F, 0.0F, 0.0F);
		BullSkull11.addChild(BullSkull12);
		BullSkull12.texOffs(98, 51).addBox(8.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r48 = new ModelRenderer(this);
		cube_r48.setPos(-6.0F, -5.0F, 0.0F);
		BullSkull12.addChild(cube_r48);
		setRotationAngle(cube_r48, -0.829F, 0.0F, 0.0F);
		cube_r48.texOffs(95, 109).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r48.texOffs(99, 109).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull13 = new ModelRenderer(this);
		BullSkull13.setPos(9.4183F, -3.7669F, 23.9827F);
		BullSkull11.addChild(BullSkull13);
		setRotationAngle(BullSkull13, 0.0F, 3.1416F, 0.0F);
		BullSkull13.texOffs(107, 74).addBox(-1.0233F, -0.4431F, -2.3077F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r49 = new ModelRenderer(this);
		cube_r49.setPos(-15.4183F, -1.2331F, 0.0173F);
		BullSkull13.addChild(cube_r49);
		setRotationAngle(cube_r49, -0.829F, 0.0F, 0.0F);
		cube_r49.texOffs(91, 109).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r49.texOffs(32, 111).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull14 = new ModelRenderer(this);
		BullSkull14.setPos(-5.4183F, 3.7669F, 0.0173F);
		BullSkull13.addChild(BullSkull14);
		BullSkull14.texOffs(78, 96).addBox(0.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r50 = new ModelRenderer(this);
		cube_r50.setPos(-14.0F, -5.0F, 0.0F);
		BullSkull14.addChild(cube_r50);
		setRotationAngle(cube_r50, -0.829F, 0.0F, 0.0F);
		cube_r50.texOffs(28, 111).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r50.texOffs(87, 109).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull15 = new ModelRenderer(this);
		BullSkull15.setPos(-5.4183F, 3.7669F, 0.0173F);
		BullSkull13.addChild(BullSkull15);
		BullSkull15.texOffs(50, 95).addBox(12.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r51 = new ModelRenderer(this);
		cube_r51.setPos(-2.0F, -5.0F, 0.0F);
		BullSkull15.addChild(cube_r51);
		setRotationAngle(cube_r51, -0.829F, 0.0F, 0.0F);
		cube_r51.texOffs(14, 109).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r51.texOffs(18, 109).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		BullSkull16 = new ModelRenderer(this);
		BullSkull16.setPos(0.0F, 0.0F, 0.0F);
		BullSkull15.addChild(BullSkull16);
		BullSkull16.texOffs(40, 95).addBox(8.395F, -4.21F, -2.325F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r52 = new ModelRenderer(this);
		cube_r52.setPos(-6.0F, -5.0F, 0.0F);
		BullSkull16.addChild(cube_r52);
		setRotationAngle(cube_r52, -0.829F, 0.0F, 0.0F);
		cube_r52.texOffs(104, 56).addBox(16.325F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		cube_r52.texOffs(10, 109).addBox(13.535F, -1.14F, 0.465F, 1.0F, 3.0F, 1.0F, 0.0F, false);

		bone3 = new ModelRenderer(this);
		bone3.setPos(10.9477F, 22.3315F, -21.5251F);


		bone4 = new ModelRenderer(this);
		bone4.setPos(-10.9562F, 1.23F, 21.561F);
		bone3.addChild(bone4);


		cube_r53 = new ModelRenderer(this);
		cube_r53.setPos(-3.9041F, -13.0615F, 6.3858F);
		bone4.addChild(cube_r53);
		setRotationAngle(cube_r53, 0.0F, 0.0436F, 0.0F);
		cube_r53.texOffs(54, 11).addBox(-1.5F, -0.5F, -13.5F, 6.0F, 2.0F, 1.0F, 0.0F, false);

		bone5 = new ModelRenderer(this);
		bone5.setPos(10.9477F, 22.3315F, -21.5251F);


		bone6 = new ModelRenderer(this);
		bone6.setPos(-10.9562F, 1.23F, 21.561F);
		bone5.addChild(bone6);


		cube_r54 = new ModelRenderer(this);
		cube_r54.setPos(-3.9041F, -13.0615F, 6.3858F);
		bone6.addChild(cube_r54);
		setRotationAngle(cube_r54, 0.0F, 0.0436F, 0.0F);
		cube_r54.texOffs(48, 65).addBox(-1.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		bone7 = new ModelRenderer(this);
		bone7.setPos(10.9477F, 22.3315F, -21.5251F);


		cube_r55 = new ModelRenderer(this);
		cube_r55.setPos(-16.2866F, -2.4405F, 29.5036F);
		bone7.addChild(cube_r55);
		setRotationAngle(cube_r55, -1.5708F, 1.2217F, -1.5708F);
		cube_r55.texOffs(0, 95).addBox(0.53F, -2.1265F, 7.98F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		bone8 = new ModelRenderer(this);
		bone8.setPos(-10.9562F, 1.23F, 21.561F);
		bone7.addChild(bone8);


		cube_r56 = new ModelRenderer(this);
		cube_r56.setPos(-4.9041F, -13.0615F, 7.3858F);
		bone8.addChild(cube_r56);
		setRotationAngle(cube_r56, 0.0F, 0.0436F, 0.0F);
		cube_r56.texOffs(60, 6).addBox(-1.5F, 1.5F, -13.5F, 1.0F, 1.0F, 1.0F, 0.0F, false);

		bone9 = new ModelRenderer(this);
		bone9.setPos(10.9477F, 22.3315F, -21.5251F);


		top = new ModelRenderer(this);
		top.setPos(10.9477F, 21.3315F, -21.5251F);


		cube_r57 = new ModelRenderer(this);
		cube_r57.setPos(-16.9367F, -15.1386F, 11.5604F);
		top.addChild(cube_r57);
		setRotationAngle(cube_r57, 0.6611F, 0.0F, 0.0F);
		cube_r57.texOffs(56, 1).addBox(-8.0F, -0.5F, -4.5F, 6.0F, 0.0F, 9.0F, 0.0F, false);
		cube_r57.texOffs(56, 1).addBox(13.9779F, -0.5F, -4.5F, 6.0F, 0.0F, 9.0F, 0.0F, true);

		cube_r58 = new ModelRenderer(this);
		cube_r58.setPos(-8.9367F, -15.1394F, 11.5598F);
		top.addChild(cube_r58);
		setRotationAngle(cube_r58, 0.6611F, 0.0F, 0.0F);
		cube_r58.texOffs(55, 1).addBox(-10.0F, -0.5F, -4.5F, 16.0F, 1.0F, 9.0F, 0.0F, true);

		cube_r59 = new ModelRenderer(this);
		cube_r59.setPos(-20.0199F, -20.3159F, 30.4425F);
		top.addChild(cube_r59);
		setRotationAngle(cube_r59, 2.1336F, -0.6418F, 2.2781F);
		cube_r59.texOffs(2, 71).addBox(-1.5F, -1.5156F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, true);

		cube_r60 = new ModelRenderer(this);
		cube_r60.setPos(-22.4529F, -19.9796F, 13.0559F);
		top.addChild(cube_r60);
		setRotationAngle(cube_r60, 1.0374F, 0.9874F, 2.2033F);
		cube_r60.texOffs(2, 71).addBox(-1.5F, 0.0F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, true);

		cube_r61 = new ModelRenderer(this);
		cube_r61.setPos(-19.8841F, -20.4037F, 27.1303F);
		top.addChild(cube_r61);
		setRotationAngle(cube_r61, 2.4348F, -0.9541F, 1.8221F);
		cube_r61.texOffs(2, 71).addBox(-2.5F, 0.0F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, true);

		cube_r62 = new ModelRenderer(this);
		cube_r62.setPos(-5.8395F, -20.7013F, 21.3897F);
		top.addChild(cube_r62);
		setRotationAngle(cube_r62, 0.0F, 1.5708F, 0.7854F);
		cube_r62.texOffs(43, 39).addBox(-5.5F, -1.0F, -7.5F, 11.0F, 1.0F, 19.0F, 0.0F, true);

		cube_r63 = new ModelRenderer(this);
		cube_r63.setPos(-21.0324F, -20.4611F, 15.8398F);
		top.addChild(cube_r63);
		setRotationAngle(cube_r63, 0.7727F, 0.8237F, 1.8691F);
		cube_r63.texOffs(2, 71).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 0.0F, 8.0F, 0.0F, true);

		cube_r64 = new ModelRenderer(this);
		cube_r64.setPos(3.3877F, -18.4872F, 7.1956F);
		top.addChild(cube_r64);
		setRotationAngle(cube_r64, 0.9828F, -0.9642F, -2.1373F);
		cube_r64.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r65 = new ModelRenderer(this);
		cube_r65.setPos(-5.8402F, -20.7006F, 10.3897F);
		top.addChild(cube_r65);
		setRotationAngle(cube_r65, 0.0F, 1.5708F, 0.7854F);
		cube_r65.texOffs(57, 3).addBox(-24.5F, -1.0F, 4.5F, 8.0F, 1.0F, 7.0F, 0.0F, true);
		cube_r65.texOffs(57, 3).addBox(-5.5F, -1.0F, 4.5F, 9.0F, 1.0F, 7.0F, 0.0F, true);

		cube_r66 = new ModelRenderer(this);
		cube_r66.setPos(-4.7305F, -26.5897F, 26.0711F);
		top.addChild(cube_r66);
		setRotationAngle(cube_r66, 1.5955F, 0.0271F, -2.3579F);
		cube_r66.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r67 = new ModelRenderer(this);
		cube_r67.setPos(-4.2885F, -26.8456F, 20.0829F);
		top.addChild(cube_r67);
		setRotationAngle(cube_r67, 1.5931F, 0.0292F, -2.4452F);
		cube_r67.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r68 = new ModelRenderer(this);
		cube_r68.setPos(0.5574F, -19.9796F, 13.0559F);
		top.addChild(cube_r68);
		setRotationAngle(cube_r68, 1.0374F, -0.9874F, -2.2033F);
		cube_r68.texOffs(2, 71).addBox(-3.5F, 0.0F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, false);

		cube_r69 = new ModelRenderer(this);
		cube_r69.setPos(-17.4969F, -26.8505F, 23.0809F);
		top.addChild(cube_r69);
		setRotationAngle(cube_r69, -1.5931F, 0.0292F, -0.6964F);
		cube_r69.texOffs(0, 79).addBox(-3.5F, -3.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);
		cube_r69.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);
		cube_r69.texOffs(0, 79).addBox(-3.5F, 3.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);
		cube_r69.texOffs(0, 79).addBox(-3.5F, 6.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r70 = new ModelRenderer(this);
		cube_r70.setPos(3.4591F, -18.8343F, 34.0282F);
		top.addChild(cube_r70);
		setRotationAngle(cube_r70, -1.964F, -0.781F, 0.709F);
		cube_r70.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, true);

		cube_r71 = new ModelRenderer(this);
		cube_r71.setPos(-7.6859F, -17.7264F, 29.3897F);
		top.addChild(cube_r71);
		setRotationAngle(cube_r71, 0.0F, 1.5708F, 0.0F);
		cube_r71.texOffs(43, 38).addBox(1.5F, -0.5F, -12.2646F, 13.0F, 1.0F, 18.0F, 0.0F, false);
		cube_r71.texOffs(68, 59).addBox(1.5F, -5.5F, -6.2646F, 13.0F, 5.0F, 6.0F, 0.0F, false);

		cube_r72 = new ModelRenderer(this);
		cube_r72.setPos(-10.9504F, -15.2538F, 30.6125F);
		top.addChild(cube_r72);
		setRotationAngle(cube_r72, -2.4367F, 0.0F, -3.1416F);
		cube_r72.texOffs(56, 2).addBox(-8.0F, -0.5F, -4.0F, 16.0F, 1.0F, 8.0F, 0.0F, false);

		cube_r73 = new ModelRenderer(this);
		cube_r73.setPos(-16.0553F, -20.7006F, 10.3897F);
		top.addChild(cube_r73);
		setRotationAngle(cube_r73, 0.0F, -1.5708F, -0.7854F);
		cube_r73.texOffs(57, 2).addBox(16.5F, -1.0F, 4.5F, 8.0F, 1.0F, 7.0F, 0.0F, false);
		cube_r73.texOffs(59, 3).addBox(-3.5F, -1.0F, 4.5F, 9.0F, 1.0F, 7.0F, 0.0F, false);

		cube_r74 = new ModelRenderer(this);
		cube_r74.setPos(-25.2831F, -18.4872F, 7.1956F);
		top.addChild(cube_r74);
		setRotationAngle(cube_r74, -1.0155F, -0.9275F, -0.9639F);
		cube_r74.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r75 = new ModelRenderer(this);
		cube_r75.setPos(-2.0113F, -20.4037F, 27.1303F);
		top.addChild(cube_r75);
		setRotationAngle(cube_r75, 2.4348F, 0.9541F, -1.8221F);
		cube_r75.texOffs(2, 71).addBox(-2.5F, 0.0F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, false);

		cube_r76 = new ModelRenderer(this);
		cube_r76.setPos(-1.8756F, -20.3159F, 30.4425F);
		top.addChild(cube_r76);
		setRotationAngle(cube_r76, 2.1336F, 0.6418F, -2.2781F);
		cube_r76.texOffs(2, 71).addBox(-3.5F, -1.5156F, -4.0F, 5.0F, 0.0F, 8.0F, 0.0F, false);

		cube_r77 = new ModelRenderer(this);
		cube_r77.setPos(-0.8631F, -20.4611F, 15.8398F);
		top.addChild(cube_r77);
		setRotationAngle(cube_r77, 0.7727F, -0.8237F, -1.8691F);
		cube_r77.texOffs(2, 71).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 0.0F, 8.0F, 0.0F, false);

		cube_r78 = new ModelRenderer(this);
		cube_r78.setPos(-4.3985F, -26.8505F, 23.0809F);
		top.addChild(cube_r78);
		setRotationAngle(cube_r78, 1.5931F, -0.0144F, -2.4462F);
		cube_r78.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r79 = new ModelRenderer(this);
		cube_r79.setPos(-16.056F, -20.7013F, 21.3897F);
		top.addChild(cube_r79);
		setRotationAngle(cube_r79, 0.0F, -1.5708F, -0.7854F);
		cube_r79.texOffs(43, 39).addBox(-5.5F, -1.0F, -7.5F, 11.0F, 1.0F, 19.0F, 0.0F, false);

		cube_r80 = new ModelRenderer(this);
		cube_r80.setPos(-4.1784F, -26.8407F, 17.0849F);
		top.addChild(cube_r80);
		setRotationAngle(cube_r80, 1.5931F, 0.0292F, -2.4452F);
		cube_r80.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r81 = new ModelRenderer(this);
		cube_r81.setPos(-25.3546F, -18.8343F, 34.0282F);
		top.addChild(cube_r81);
		setRotationAngle(cube_r81, -1.964F, 0.781F, -0.709F);
		cube_r81.texOffs(0, 79).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 10.0F, 0.0F, false);

		cube_r82 = new ModelRenderer(this);
		cube_r82.setPos(0.0496F, -14.9855F, 31.6971F);
		top.addChild(cube_r82);
		setRotationAngle(cube_r82, -0.7047F, 0.0F, 0.0F);
		cube_r82.texOffs(57, 1).addBox(-3.0F, 0.0F, -5.0F, 7.0F, 0.0F, 8.0F, 0.0F, true);
		cube_r82.texOffs(57, 1).addBox(-25.9946F, 0.0F, -5.0F, 7.0F, 0.0F, 8.0F, 0.0F, false);

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);


		cube_r83 = new ModelRenderer(this);
		cube_r83.setPos(-3.3589F, -4.9221F, 6.1142F);
		bb_main.addChild(cube_r83);
		setRotationAngle(cube_r83, -1.5708F, 1.2217F, -1.5708F);
		cube_r83.texOffs(64, 92).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r84 = new ModelRenderer(this);
		cube_r84.setPos(-5.3389F, -4.109F, 7.9786F);
		bb_main.addChild(cube_r84);
		setRotationAngle(cube_r84, -1.5708F, 1.2217F, -1.5708F);
		cube_r84.texOffs(98, 38).addBox(-1.47F, -2.1265F, 0.98F, 2.0F, 4.0F, 9.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(MalevolentShrineEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.construction = entity.getConstruction();
    }

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.renderPart(this.bb_main, 0.0f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.bone, 0.08f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.bone3, 0.2f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.bone5, 0.32f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.bone7, 0.44f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.bone9, 0.56f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderPart(this.top, 0.7f, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

	private void renderPart(ModelRenderer part, float start, MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        float progress = Math.max(0.0f, Math.min(1.0f, (this.construction - start) / 0.3f));
        if (progress <= 0.0f) {
            return;
        }
        progress = progress * progress * (3.0f - 2.0f * progress);
        stack.pushPose();
        stack.translate(0.0, (double)((1.0f - progress) * 2.0f), 0.0);
        stack.scale(1.0f, Math.max(0.02f, progress), 1.0f);
        part.render(stack, buffer, light, overlay, red, green, blue, alpha);
        stack.popPose();
    }

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}
