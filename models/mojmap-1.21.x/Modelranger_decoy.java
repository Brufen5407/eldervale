// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelranger_decoy<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "ranger_decoy"), "main");
	private final ModelPart base;
	private final ModelPart right_leg;
	private final ModelPart left_leg;
	private final ModelPart torso;
	private final ModelPart waist_straw;
	private final ModelPart neck_straw;
	private final ModelPart head;
	private final ModelPart right_arm;
	private final ModelPart left_arm;
	private final ModelPart arrow;

	public Modelranger_decoy(ModelPart root) {
		this.base = root.getChild("base");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
		this.torso = root.getChild("torso");
		this.waist_straw = root.getChild("waist_straw");
		this.neck_straw = root.getChild("neck_straw");
		this.head = root.getChild("head");
		this.right_arm = root.getChild("right_arm");
		this.left_arm = root.getChild("left_arm");
		this.arrow = root.getChild("arrow");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition base = partdefinition.addOrReplaceChild("base",
				CubeListBuilder.create().texOffs(24, 25)
						.addBox(-6.0F, -3.0F, -2.5F, 12.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(28, 54)
						.addBox(-2.0F, -3.0F, -6.5F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(28, 54)
						.addBox(-2.0F, -3.0F, 2.5F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 41)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 27)
						.addBox(-2.5F, 5.0F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offset(2.5F, 12.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(0, 41)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 27)
						.addBox(-2.5F, 5.0F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.5F, 12.0F, 0.0F));

		PartDefinition torso = partdefinition.addOrReplaceChild("torso",
				CubeListBuilder.create().texOffs(30, 0)
						.addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(36, 47)
						.addBox(-3.0F, -11.5F, -3.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 54)
						.addBox(-4.5F, -5.5F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(40, 33)
						.addBox(-3.5F, -5.5F, -3.5F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition waist_straw = partdefinition.addOrReplaceChild("waist_straw", CubeListBuilder.create()
				.texOffs(24, 16).addBox(-5.0F, -3.0F, -3.0F, 10.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 11.5F, 0.0F));

		PartDefinition neck_straw = partdefinition.addOrReplaceChild("neck_straw", CubeListBuilder.create()
				.texOffs(0, 33).addBox(-4.0F, -2.0F, -3.0F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 13)
						.addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-4.0F, -7.0F, -3.5F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(24, 13)
						.addBox(-1.0F, -5.0F, -4.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(40, 40)
						.addBox(-2.0F, -10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -2.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(28, 33)
						.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 47)
						.addBox(-2.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, 2.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(28, 33)
						.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 47)
						.addBox(-2.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition arrow = partdefinition.addOrReplaceChild("arrow",
				CubeListBuilder.create().texOffs(50, 33)
						.addBox(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(58, 0)
						.addBox(-0.5F, -1.5F, -8.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.0F, 3.5F, -3.0F, -0.0873F, -0.2443F, 0.0698F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		waist_straw.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		neck_straw.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		arrow.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}