package net.blancworks.figura.lua.api.nameplate;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class NamePlateAPI {
    public static final String ENTITY = "ENTITY";
    public static final String CHAT = "CHAT";
    public static final String TABLIST = "LIST";

    public static Identifier getID() {
        return new Identifier("default", "nameplate");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(ENTITY, getTableForPart(ENTITY, script));
            set(CHAT, getTableForPart(CHAT, script));
            set(TABLIST, getTableForPart(TABLIST, script));
        }};
    }

    public static LuaTable getTableForPart(String accessor, CustomScript targetScript) {
        return new LuaTable() {{
            set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).position);
                }
            });

            set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).position = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Boolean enabled = targetScript.getOrMakeNameplateCustomization(accessor).enabled;
                    return enabled == null ? NIL : LuaValue.valueOf(enabled);
                }
            });

            set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeNameplateCustomization(accessor).enabled = arg.isnil() ? null : arg.checkboolean();
                    return NIL;
                }
            });

            set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).scale);
                }
            });

            set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).scale = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("setText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String string = null;

                    if (!arg.isnil()) {
                        //no ✭ 4 u
                        string = TextUtils.noBadges4U(arg.checkjstring());

                        //allow new lines only on entity
                        if (!accessor.equals(ENTITY))
                            string = string.replaceAll("[\n\r]", " ");

                        //check if nameplate is too large
                        if (string.length() > 65535) {
                            throw new LuaError("Nameplate too long - oopsie!");
                        }
                    }

                    targetScript.getOrMakeNameplateCustomization(accessor).text = string;
                    return NIL;
                }
            });

            set("getText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    String text = targetScript.getOrMakeNameplateCustomization(accessor).text;
                    return text == null ? NIL : LuaValue.valueOf(text);
                }
            });
        }};
    }
}
