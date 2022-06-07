package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.access.InGameHudAccess;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.gui.NewActionWheel;
import net.blancworks.figura.gui.PlayerPopup;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin implements InGameHudAccess {

    @Shadow private Text title;
    @Shadow private Text subtitle;
    @Shadow private Text overlayMessage;
    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At ("HEAD"), method = "render")
    public void preRender(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!AvatarDataManager.panic && FiguraMod.PLAYER_POPUP_BUTTON.isPressed())
            PlayerPopup.render(matrices);
    }

    @Inject(at = @At ("RETURN"), method = "render")
    public void postRender(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!AvatarDataManager.panic && FiguraMod.ACTION_WHEEL_BUTTON.isPressed()) {
            /*
            if ((boolean) Config.NEW_ACTION_WHEEL.value)
                NewActionWheel.render(matrices);
            else
            */
                ActionWheel.render(matrices);
        }

        //render hud parts
        Entity entity = MinecraftClient.getInstance().getCameraEntity();
        if (entity != null) {
            AvatarData data = entity instanceof PlayerEntity ? AvatarDataManager.getDataForPlayer(entity.getUuid()) : AvatarDataManager.getDataForEntity(entity);
            if (data != null && data.model != null)
                data.model.renderHudParts(matrices);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean onPlayerListRender(KeyBinding keyPlayerList) {
        return keyPlayerList.isPressed() || PlayerPopup.miniEnabled;
    }

    @Inject(at = @At ("HEAD"), method = "renderCrosshair", cancellable = true)
    private void renderCrosshair(MatrixStack matrices, CallbackInfo ci) {
        if (AvatarDataManager.panic) return;

        if (FiguraMod.ACTION_WHEEL_BUTTON.isPressed() && (ActionWheel.enabled || NewActionWheel.enabled))
            ci.cancel();

        //do not render crosshair
        AvatarData currentData = AvatarDataManager.localPlayer;
        if (currentData != null && currentData.script != null && !currentData.script.crossHairEnabled)
            ci.cancel();
    }

    @ModifyArgs(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V",
                    ordinal = 0
            )
    )
    private void renderCrosshairDrawTexture(Args args) {
        //set crosshair offset
        AvatarData currentData = AvatarDataManager.localPlayer;
        if (!AvatarDataManager.panic && currentData != null && currentData.script != null && currentData.script.crossHairPos != null) {
            args.set(1, (int) ((int) args.get(1) + currentData.script.crossHairPos.x));
            args.set(2, (int) ((int) args.get(2) + currentData.script.crossHairPos.y));
        }
    }

    @ModifyArgs(method = "onChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ClientChatListener;onChatMessage(Lnet/minecraft/network/message/MessageType;Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSender;)V"))
    private void onChatMessage(Args args) {
        if (this.client.player == null || !(boolean) Config.CHAT_MODIFICATIONS.value)
            return;

        for (UUID uuid : this.client.player.networkHandler.getPlayerUuids()) {
            //get player
            PlayerListEntry player = this.client.player.networkHandler.getPlayerListEntry(uuid);
            if (player == null)
                continue;

            //get metadata
            AvatarData data = AvatarDataManager.getDataForPlayer(uuid);
            if (data == null)
                return;

            //apply customization
            Text replacement;
            Text message = args.get(1);
            NamePlateCustomization custom = data.script == null ? null : data.script.nameplateCustomizations.get(NamePlateAPI.CHAT);
            if (custom != null && custom.text != null && data.getTrustContainer().getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 1) {
                replacement = NamePlateCustomization.applyCustomization(custom.text.replaceAll("\n|\\\\n", ""));
            } else {
                replacement = Text.literal(player.getProfile().getName());
            }

            if ((boolean) Config.BADGES.value) {
                Text badges = NamePlateCustomization.getBadges(data);
                if (badges != null) ((MutableText) replacement).append(badges);
            }

            args.set(1, TextUtils.replaceInText(message, "\\b" + player.getProfile().getName() + "\\b", replacement));
        }
    }

    @Override
    public Text getTitle() {
        return title;
    }

    @Override
    public Text getSubtitle() {
        return subtitle;
    }

    @Override
    public Text getOverlayMessage() {
        return overlayMessage;
    }
}
