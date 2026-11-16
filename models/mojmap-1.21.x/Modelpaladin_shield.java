// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelpaladin_shield<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "paladin_shield"), "main");
	private final ModelPart shields;
	private final ModelPart paladin_shield1;
	private final ModelPart paladin_shield3;
	private final ModelPart paladin_shield4;
	private final ModelPart paladin_shield2;

	public Modelpaladin_shield(ModelPart root) {
		this.shields = root.getChild("shields");
		this.paladin_shield1 = this.shields.getChild("paladin_shield1");
		this.paladin_shield3 = this.shields.getChild("paladin_shield3");
		this.paladin_shield4 = this.shields.getChild("paladin_shield4");
		this.paladin_shield2 = this.shields.getChild("paladin_shield2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition shields = partdefinition.addOrReplaceChild("shields", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition paladin_shield1 = shields.addOrReplaceChild("paladin_shield1", CubeListBuilder.create()
				.texOffs(38, 8).mirror().addBox(-6.0F, -33.25F, -14.75F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.mirror(false).texOffs(38, 20).mirror()
				.addBox(-4.4957F, -7.104F, -14.75F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -12.8F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -14.7F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(0.0F, 0.0F, -10.0F));

		PartDefinition cube_r1 = paladin_shield1.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(38, 28).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, -0.9076F));

		PartDefinition cube_r2 = paladin_shield1.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(38, 12).addBox(-4.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, 0.9338F));

		PartDefinition cube_r3 = paladin_shield1.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(38, 0).mirror()
						.addBox(-9.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, -1.5621F));

		PartDefinition cube_r4 = paladin_shield1.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(38, 4).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, 1.5621F));

		PartDefinition cube_r5 = paladin_shield1.addOrReplaceChild("cube_r5",
				CubeListBuilder.create().texOffs(38, 16).mirror()
						.addBox(-7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, -0.9338F));

		PartDefinition cube_r6 = paladin_shield1.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(38, 24).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, 0.9076F));

		PartDefinition paladin_shield3 = shields.addOrReplaceChild("paladin_shield3", CubeListBuilder.create()
				.texOffs(38, 8).mirror().addBox(-6.0F, -33.25F, -14.75F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.mirror(false).texOffs(38, 20).mirror()
				.addBox(-4.4957F, -7.104F, -14.75F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -12.8F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -14.7F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(37.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r7 = paladin_shield3.addOrReplaceChild("cube_r7",
				CubeListBuilder.create().texOffs(38, 28).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, -0.9076F));

		PartDefinition cube_r8 = paladin_shield3.addOrReplaceChild("cube_r8",
				CubeListBuilder.create().texOffs(38, 12).addBox(-4.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, 0.9338F));

		PartDefinition cube_r9 = paladin_shield3.addOrReplaceChild("cube_r9",
				CubeListBuilder.create().texOffs(38, 0).mirror()
						.addBox(-9.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, -1.5621F));

		PartDefinition cube_r10 = paladin_shield3.addOrReplaceChild("cube_r10",
				CubeListBuilder.create().texOffs(38, 4).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, 1.5621F));

		PartDefinition cube_r11 = paladin_shield3.addOrReplaceChild("cube_r11",
				CubeListBuilder.create().texOffs(38, 16).mirror()
						.addBox(-7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, -0.9338F));

		PartDefinition cube_r12 = paladin_shield3.addOrReplaceChild("cube_r12",
				CubeListBuilder.create().texOffs(38, 24).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, 0.9076F));

		PartDefinition paladin_shield4 = shields.addOrReplaceChild("paladin_shield4", CubeListBuilder.create()
				.texOffs(38, 8).mirror().addBox(-6.0F, -33.25F, -14.75F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.mirror(false).texOffs(38, 20).mirror()
				.addBox(-4.4957F, -7.104F, -14.75F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -12.8F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -14.7F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-9.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r13 = paladin_shield4.addOrReplaceChild("cube_r13",
				CubeListBuilder.create().texOffs(38, 28).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, -0.9076F));

		PartDefinition cube_r14 = paladin_shield4.addOrReplaceChild("cube_r14",
				CubeListBuilder.create().texOffs(38, 12).addBox(-4.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, 0.9338F));

		PartDefinition cube_r15 = paladin_shield4.addOrReplaceChild("cube_r15",
				CubeListBuilder.create().texOffs(38, 0).mirror()
						.addBox(-9.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, -1.5621F));

		PartDefinition cube_r16 = paladin_shield4.addOrReplaceChild("cube_r16",
				CubeListBuilder.create().texOffs(38, 4).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, 1.5621F));

		PartDefinition cube_r17 = paladin_shield4.addOrReplaceChild("cube_r17",
				CubeListBuilder.create().texOffs(38, 16).mirror()
						.addBox(-7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, -0.9338F));

		PartDefinition cube_r18 = paladin_shield4.addOrReplaceChild("cube_r18",
				CubeListBuilder.create().texOffs(38, 24).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, 0.9076F));

		PartDefinition paladin_shield2 = shields.addOrReplaceChild("paladin_shield2", CubeListBuilder.create()
				.texOffs(38, 8).mirror().addBox(-6.0F, -33.25F, -14.75F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.mirror(false).texOffs(38, 20).mirror()
				.addBox(-4.4957F, -7.104F, -14.75F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -12.8F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 27).mirror()
				.addBox(-9.4957F, -32.104F, -14.7F, 19.0F, 27.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(0.0F, 0.0F, 37.0F));

		PartDefinition cube_r19 = paladin_shield2.addOrReplaceChild("cube_r19",
				CubeListBuilder.create().texOffs(38, 28).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, -0.9076F));

		PartDefinition cube_r20 = paladin_shield2.addOrReplaceChild("cube_r20",
				CubeListBuilder.create().texOffs(38, 12).addBox(-4.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, 0.9338F));

		PartDefinition cube_r21 = paladin_shield2.addOrReplaceChild("cube_r21",
				CubeListBuilder.create().texOffs(38, 0).mirror()
						.addBox(-9.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, -1.5621F));

		PartDefinition cube_r22 = paladin_shield2.addOrReplaceChild("cube_r22",
				CubeListBuilder.create().texOffs(38, 4).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(9.9602F, -22.9373F, -13.75F, 0.0F, 0.0F, 1.5621F));

		PartDefinition cube_r23 = paladin_shield2.addOrReplaceChild("cube_r23",
				CubeListBuilder.create().texOffs(38, 16).mirror()
						.addBox(-7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.8556F, -11.3258F, -13.75F, 0.0F, 0.0F, -0.9338F));

		PartDefinition cube_r24 = paladin_shield2.addOrReplaceChild("cube_r24",
				CubeListBuilder.create().texOffs(38, 24).mirror()
						.addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(7.6746F, -29.4823F, -13.75F, 0.0F, 0.0F, 0.9076F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		shields.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}