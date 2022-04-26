package net.blancworks.figura.lua.api.nameplate;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

public class NamePlateCustomization {
    public String text;
    public Vec3f position;
    public Vec3f scale;
    public Boolean enabled;

    private static final String LOADING = "\u22EE\u22F0\u22EF\u22F1";

    public static Text applyCustomization(String text) {
        text = TextUtils.noBadges4U(text);
        Text ret = TextUtils.tryParseJson(text);
        return TextUtils.removeClickableObjects(ret);
    }

    public static Text getBadges(AvatarData currentData) {
        if (currentData == null) return null;

        //font
        Identifier font = (boolean) ConfigManager.Config.BADGE_AS_ICONS.value ? FiguraMod.FIGURA_FONT : Style.DEFAULT_FONT_ID;
        String badges = " ";

        if (currentData.hasAvatar()) {
            //trust
            TrustContainer tc = currentData.getTrustContainer();
            CustomModel model = currentData.model;
            if ((currentData.getComplexity() > tc.getTrust(TrustContainer.Trust.COMPLEXITY)) ||
                    (model != null && (model.animRendered > model.animMaxRender || (!model.animations.isEmpty() && model.animMaxRender == 0)))) {
                currentData.trustIssues = true;
            } else if (currentData.script != null) {
                CustomScript script = currentData.script;
                currentData.trustIssues = (script.customVCP != null && script.customVCP.hasLayers() && tc.getTrust(TrustContainer.Trust.CUSTOM_RENDER_LAYER) == 0) ||
                        (!script.nameplateCustomizations.isEmpty() && tc.getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 0) ||
                        (!script.allCustomizations.isEmpty() && tc.getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0) ||
                        (!script.customSounds.isEmpty() && tc.getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 0);
            } else {
                currentData.trustIssues = false;
            }

            //the mark
            if (!currentData.isAvatarLoaded()) {
                if ((boolean) ConfigManager.Config.BADGE_AS_ICONS.value)
                    badges += Integer.toHexString(Math.abs(FiguraMod.ticksElapsed) % 16);
                else
                    badges += LOADING.charAt(Math.abs(FiguraMod.ticksElapsed) % 4);
            }
            else if (currentData.script != null && currentData.script.scriptError)
                badges += "▲";
            else if (currentData.trustIssues)
                badges += "!";
            else if ((boolean) ConfigManager.Config.EASTER_EGGS.value && FiguraMod.IS_CHEESE)
                badges += "\uD83E\uDDC0";
            else
                badges += "△";
        }

        //special badges
        if (FiguraMod.VIP.contains(currentData.entityId))
            badges += "★";

        //return null if no badges
        if (badges.equals(" ")) return null;

        //create badges text
        return Text.literal(badges).setStyle(Style.EMPTY.withExclusiveFormatting(Formatting.WHITE).withFont(font));
    }
}
