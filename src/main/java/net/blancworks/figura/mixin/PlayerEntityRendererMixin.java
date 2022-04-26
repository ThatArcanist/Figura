package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.access.PlayerEntityRendererAccess;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> implements PlayerEntityRendererAccess {

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Unique private final ArrayList<ModelPart> figura$customizedParts = new ArrayList<>();

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    public void onRender(AbstractClientPlayerEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());
        AvatarData.setRenderingData(data, vertexConsumerProvider, this.getModel(), MinecraftClient.getInstance().getTickDelta());

        shadowRadius = 0.5f; //Vanilla shadow radius.
        //Reset this here because... Execution order.

        if (data != null && data.script != null && data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1) {
            PlayerEntityModel<AbstractClientPlayerEntity> model = this.getModel();

            figura$applyPartCustomization(VanillaModelAPI.VANILLA_HEAD, model.head, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_TORSO, model.body, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_ARM, model.leftArm, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_ARM, model.rightArm, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_LEG, model.leftLeg, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_LEG, model.rightLeg, entity);

            figura$applyPartCustomization(VanillaModelAPI.VANILLA_HAT, model.hat, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_JACKET, model.jacket, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_SLEEVE, model.leftSleeve, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_SLEEVE, model.rightSleeve, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_PANTS, model.leftPants, entity);
            figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_PANTS, model.rightPants, entity);

            if (data.script.customShadowSize != null) {
                shadowRadius = data.script.customShadowSize;
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    public void postRender(AbstractClientPlayerEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());

        if (data != null && data.script != null && data.script.isDone) {
            for (VanillaModelAPI.ModelPartTable partTable : data.script.vanillaModelPartTables) {
                if (VanillaModelAPI.isPartSpecial(partTable.accessor))
                    continue;

                partTable.updateFromPart();
            }
        }

        figura$clearAllPartCustomizations();
    }

    @Inject(at = @At("HEAD"), method = "renderArm")
    private void onRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());
        AvatarData.setRenderingData(data, vertexConsumers, this.getModel(), MinecraftClient.getInstance().getTickDelta());

        if (data == null) return;

        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getModel();

        figura$applyPartCustomization(VanillaModelAPI.VANILLA_HEAD, model.head, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_TORSO, model.body, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_ARM, model.leftArm, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_ARM, model.rightArm, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_LEG, model.leftLeg, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_LEG, model.rightLeg, entity);

        figura$applyPartCustomization(VanillaModelAPI.VANILLA_HAT, model.hat, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_JACKET, model.jacket, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_SLEEVE, model.leftSleeve, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_SLEEVE, model.rightSleeve, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_PANTS, model.leftPants, entity);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_PANTS, model.rightPants, entity);
    }

    @Inject(at = @At("RETURN"), method = "renderArm")
    private void postRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer) (Object) this;
        PlayerEntityModel<?> model = realRenderer.getModel();
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());

        if (data != null && data.model != null) {
            arm.pitch = 0;
            data.model.renderArm(matrices, data.getVCP(), light, arm, model, 1f);
        }

        figura$clearAllPartCustomizations();
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void renderFiguraLabelIfPresent(AbstractClientPlayerEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        //get metadata
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());
        if (data == null || !(boolean) Config.NAMEPLATE_MODIFICATIONS.value)
            return;

        //check entity distance
        if (this.dispatcher.getSquaredDistanceToCamera(entity) > 4096)
            return;

        //get customizations
        NamePlateCustomization custom = data.script == null ? null : data.script.nameplateCustomizations.get(NamePlateAPI.ENTITY);

        //enabled
        if (custom != null && custom.enabled != null && !custom.enabled) {
            ci.cancel();
            return;
        }

        //trust check
        boolean trust = data.getTrustContainer().getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 1;

        matrices.push();

        //pos
        Vec3f pos = new Vec3f(0f, entity.getHeight() + 0.5f, 0f);
        if (custom != null && custom.position != null && trust)
            pos.add(custom.position);

        matrices.translate(pos.getX(), pos.getY(), pos.getZ());

        //rotation
        matrices.multiply(this.dispatcher.getRotation());

        //scale
        float scale = 0.025f;
        Vec3f scaleVec = new Vec3f(-scale, -scale, scale);
        if (custom != null && custom.scale != null && trust)
            scaleVec.multiplyComponentwise(custom.scale.getX(), custom.scale.getY(), custom.scale.getZ());

        matrices.scale(scaleVec.getX(), scaleVec.getY(), scaleVec.getZ());

        //text
        Text replacement;
        if (custom != null && custom.text != null && trust) {
            replacement = NamePlateCustomization.applyCustomization(custom.text);
        } else {
            replacement = Text.literal(entity.getName().getString());
        }

        if ((boolean) Config.BADGES.value) {
            Text badges = NamePlateCustomization.getBadges(data);
            if (badges != null) ((MutableText) replacement).append(badges);
        }

        text = TextUtils.replaceInText(text, "\\b" + entity.getName().getString() + "\\b", replacement);

        // * variables * //
        boolean isSneaking = entity.isSneaky();
        boolean deadmau = "deadmau5".equals(text.getString());

        float bgOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f);
        int bgColor = (int) (bgOpacity * 0xFF) << 24;

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = this.getTextRenderer();

        //render scoreboard
        boolean hasScore = false;
        if (this.dispatcher.getSquaredDistanceToCamera(entity) < 100.0D) {
            //get scoreboard
            Scoreboard scoreboard = entity.getScoreboard();
            ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(2);
            if (scoreboardObjective != null) {
                hasScore = true;

                //render scoreboard
                ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(entity.getEntityName(), scoreboardObjective);

                Text text1 = Text.literal(Integer.toString(scoreboardPlayerScore.getScore())).append(" ").append(scoreboardObjective.getDisplayName());
                float x = -textRenderer.getWidth(text1) / 2f;
                float y = deadmau ? -10f : 0f;

                textRenderer.draw(text1, x, y, 0x20FFFFFF, false, matrix4f, vertexConsumers, !isSneaking, bgColor, light);
                if (!isSneaking)
                    textRenderer.draw(text1, x, y, -1, false, matrix4f, vertexConsumers, false, 0, light);
            }
        }

        //render name
        List<Text> textList = TextUtils.splitText(text, "\n");

        for (int i = 0; i < textList.size(); i++) {
            Text text1 = textList.get(i);
            int line = i - textList.size() + (hasScore ? 0 : 1);

            float x = -textRenderer.getWidth(text1) / 2f;
            float y = (deadmau ? -10f : 0f) + (textRenderer.fontHeight + 1.5f) * line;

            textRenderer.draw(text1, x, y, 0x20FFFFFF, false, matrix4f, vertexConsumers, !isSneaking, bgColor, light);
            if (!isSneaking)
                textRenderer.draw(text1, x, y, -1, false, matrix4f, vertexConsumers, false, 0, light);
        }

        matrices.pop();
        ci.cancel();
    }

    public void figura$applyPartCustomization(String id, ModelPart part, AbstractClientPlayerEntity entity) {
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());

        if (data != null && data.script != null && data.script.allCustomizations != null) {
            VanillaModelPartCustomization customization = data.script.allCustomizations.get(id);

            if (customization != null) {
                ((ModelPartAccess) (Object) part).figura$setPartCustomization(customization);
                figura$customizedParts.add(part);
            }
        }
    }

    public void figura$clearAllPartCustomizations() {
        for (ModelPart part : figura$customizedParts) {
            ((ModelPartAccess) (Object) part).figura$setPartCustomization(null);
        }
        figura$customizedParts.clear();
    }

    public void figura$setupTransformsPublic(AbstractClientPlayerEntity abstractClientPlayerEntity, MatrixStack matrixStack, float f, float g, float h) {
        this.setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
    }
}
