package net.mcreator.eldervale.client.model;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Werewolf beast form (placeholder model written by hand, replaceable by a Blockbench export).
 * Same conventions as Modelpaladin_shield: vanilla-style space (feet at y = 24), texture 128x128.
 * Rendered over the player by WerewolfFormRenderer while PlayerVariables.werewolf_mode is true.
 *
 * Bones: body > (head > jaw), right_arm, left_arm, tail; right_leg, left_leg.
 * Animation (walk, head look, attack swing, idle breathing) is in setupAnim.
 */
public class Modelwerewolf_form<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("eldervale", "modelwerewolf_form"), "main");

	private static final float RAD = 0.017453292F;
	private static final float BODY_LEAN = 0.3F; // hunched posture

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart tail;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public Modelwerewolf_form(ModelPart root) {
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.rightArm = this.body.getChild("right_arm");
		this.leftArm = this.body.getChild("left_arm");
		this.tail = this.body.getChild("tail");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition root = meshdefinition.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -14.0F, -3.5F, 12.0F, 14.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(76, 0).addBox(-2.5F, -3.0F, -8.0F, 5.0F, 3.0F, 4.0F).texOffs(96, 8).addBox(-4.0F, -11.0F, -1.0F, 2.0F, 3.0F, 1.0F).texOffs(96, 8)
						.mirror().addBox(2.0F, -11.0F, -1.0F, 2.0F, 3.0F, 1.0F).mirror(false),
				PartPose.offset(0.0F, -14.0F, -1.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(96, 0).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -4.0F));

		body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 24).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 15.0F, 5.0F).texOffs(24, 24).addBox(-2.5F, 13.0F, -3.0F, 5.0F, 2.0F, 1.0F), PartPose.offset(-8.5F, -12.0F, 0.0F));
		body.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-2.5F, -2.0F, -2.5F, 5.0F, 15.0F, 5.0F).texOffs(24, 24).addBox(-2.5F, 13.0F, -3.0F, 5.0F, 2.0F, 1.0F).mirror(false), PartPose.offset(8.5F, -12.0F, 0.0F));

		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(40, 24).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 9.0F), PartPose.offset(0.0F, -2.0F, 3.5F));

		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F).texOffs(24, 48).addBox(-2.5F, 10.0F, -4.5F, 5.0F, 2.0F, 2.0F), PartPose.offset(-3.5F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F).texOffs(24, 48).addBox(-2.5F, 10.0F, -4.5F, 5.0F, 2.0F, 2.0F).mirror(false),
				PartPose.offset(3.5F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		float swing = limbSwing * 0.6662F;
		float amount = Math.min(limbSwingAmount, 1.0F);
		float breathe = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;

		// every value is set absolutely each frame (nothing accumulates)
		body.xRot = BODY_LEAN;
		body.yRot = 0.0F;

		head.yRot = netHeadYaw * RAD;
		head.xRot = headPitch * RAD - BODY_LEAN;
		jaw.xRot = 0.12F + (Mth.sin(ageInTicks * 0.15F) + 1.0F) * 0.06F;

		rightLeg.xRot = Mth.cos(swing) * 1.4F * amount;
		leftLeg.xRot = Mth.cos(swing + Mth.PI) * 1.4F * amount;

		rightArm.xRot = Mth.cos(swing + Mth.PI) * 1.2F * amount - 0.15F;
		leftArm.xRot = Mth.cos(swing) * 1.2F * amount - 0.15F;
		rightArm.yRot = 0.0F;
		leftArm.yRot = 0.0F;
		rightArm.zRot = 0.1F + breathe;
		leftArm.zRot = -0.1F - breathe;

		// attack swing (right arm, real attack animation of the entity)
		float attack = entity instanceof LivingEntity living ? living.getAttackAnim(ageInTicks - living.tickCount) : 0.0F;
		if (attack > 0.0F) {
			rightArm.xRot -= Mth.sin(attack * Mth.PI) * 2.2F;
			rightArm.yRot = -Mth.sin(Mth.sqrt(attack) * Mth.PI * 2.0F) * 0.5F;
			body.yRot = Mth.sin(Mth.sqrt(attack) * Mth.PI * 2.0F) * 0.2F;
		}

		tail.xRot = -0.7F;
		tail.yRot = Mth.cos(swing) * 0.35F * amount + Mth.sin(ageInTicks * 0.08F) * 0.12F;
	}
}
