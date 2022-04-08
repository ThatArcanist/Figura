package net.blancworks.figura.lua.api.math;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.utils.ColorUtils;
import net.blancworks.figura.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.awt.*;

public class VectorAPI {
    private static LuaTable globalLuaTable;
    private static boolean initialized;

    public static Identifier getID() {
        return new Identifier("default", "vectors");
    }

    public static LuaTable getForScript(CustomScript script) {
        if (!initialized) {
            updateGlobalTable();
            initialized = true;
        }

        return globalLuaTable;
    }

    public static void updateGlobalTable() {
        globalLuaTable = new LuaTable() {{
            set("of", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaVector.of(arg.checktable());
                }
            });

            set("lerp", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    LuaVector a = LuaVector.checkOrNew(arg1);
                    LuaVector b = LuaVector.checkOrNew(arg2);
                    return lerp(a, b, arg3.checknumber().tofloat());
                }
            });

            set("rgbToINT", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector rgb = LuaVector.checkOrNew(arg);
                    return LuaValue.valueOf(intFromRGB(rgb));
                }
            });

            set("intToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int i = arg.checkint();
                    return RGBfromInt(i);
                }
            });

            set("rgbToHSV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector rgb = LuaVector.checkOrNew(arg);
                    return toHSV(rgb);
                }
            });

            set("hsvToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector hsv = LuaVector.checkOrNew(arg);
                    return toRGB(hsv);
                }
            });

            set("worldToPart", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return toModelSpace(vec);
                }
            });

            set("worldToCameraPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return toCameraSpace(vec);
                }
            });

            set("getVector", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int n = arg.checkint();
                    return new LuaVector(new float[n]);
                }
            });

            set("rotateWithQuaternion", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    return rotateWithQuaternion(LuaVector.checkOrNew(arg1), LuaVector.checkOrNew(arg2));
                }
            });

            set("rotateAroundAxis", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue vector, LuaValue axis, LuaValue degrees) {
                    return rotateAroundAxis(LuaVector.checkOrNew(vector), LuaVector.checkOrNew(axis), degrees.checknumber());
                }
            });

            set("axisAngleToEuler", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue luaAxis, LuaValue luaAngle) {
                    Vec3f axis = LuaVector.checkOrNew(luaAxis).asV3f();
                    float angle = luaAngle.checknumber().tofloat();
                    return LuaVector.of(MathUtils.quaternionToEulerXYZ(new Quaternion(axis, angle, true)));
                }
            });

            set("toQuaternion", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Quaternion q = Quaternion.fromEulerXyzDegrees(LuaVector.checkOrNew(arg).asV3f());
                    return LuaVector.of(new Vector4f(q.getX(), q.getY(), q.getZ(), q.getW()));
                }
            });

            set("fromQuaternion", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return LuaVector.of(MathUtils.quaternionToEulerXYZ(new Quaternion(vec.x(), vec.y(), vec.z(), vec.w())));
                }
            });

            set("worldToScreenSpace", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaVector.of(MathUtils.worldToScreenSpace(LuaVector.checkOrNew(arg).asV3f()));
                }
            });
        }};
    }

    public static LuaVector lerp(LuaVector first, LuaVector second, float delta) {
        int n = Math.max(first._size(), second._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = MathHelper.lerp(delta, first._get(i + 1), second._get(i + 1));
        }
        return new LuaVector(vals);
    }

    public static LuaVector toHSV(LuaVector rgb) {
        Vec3f color = rgb.asV3f();
        float[] hsb = Color.RGBtoHSB((int) (color.getX() * 255), (int) (color.getY() * 255), (int) (color.getZ() * 255), null);
        return new LuaVector(hsb);
    }

    public static LuaVector toRGB(LuaVector hsv) {
        return (LuaVector) LuaVector.of(ColorUtils.hsvToRGB(hsv.asV3f()));
    }

    public static LuaVector RGBfromInt(int rgb) {
        return (LuaVector) LuaVector.of(ColorUtils.intToRGB(rgb));
    }

    public static int intFromRGB(LuaVector rgb) {
        return ColorUtils.rgbToInt(rgb.asV3f());
    }

    public static LuaValue rotateWithQuaternion(LuaVector vector, LuaVector rotation) {
        Quaternion quat = Quaternion.fromEulerXyzDegrees(vector.asV3f());
        Quaternion rot = Quaternion.fromEulerXyzDegrees(rotation.asV3f());
        quat.hamiltonProduct(rot);

        //we cant use the quaternion to euler from the quaternion class because NaN and weird rotations
        return LuaVector.of(MathUtils.quaternionToEulerXYZ(quat));
    }

    public static LuaValue rotateAroundAxis(LuaVector vector, LuaVector axis, LuaNumber degrees) {
        Vec3f normalizedAxis = axis.asV3f();
        normalizedAxis.normalize();
        Quaternion rotatorQuat = new Quaternion(normalizedAxis, degrees.tofloat(), true);
        Quaternion rotatorQuatConj = new Quaternion(rotatorQuat);
        rotatorQuatConj.conjugate();
        Quaternion vectorQuat = new Quaternion(vector.x(), vector.y(), vector.z(), 0);
        rotatorQuat.hamiltonProduct(vectorQuat);
        rotatorQuat.hamiltonProduct(rotatorQuatConj);
        return LuaVector.of(new Vec3f(rotatorQuat.getX(), rotatorQuat.getY(), rotatorQuat.getZ()));
    }

    public static LuaValue toCameraSpace(LuaVector vec) {
        Matrix3f transformMatrix = new Matrix3f(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
        transformMatrix.invert();
        Vec3f target = new Vec3f(vec.x(), vec.y(), vec.z());
        target.subtract(new Vec3f(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()));
        target.transform(transformMatrix);
        target.set(-target.getX(), target.getY(), target.getZ());
        return LuaVector.of(target);
    }

    private static final LuaVector[] MODEL_SPACE_FACTORS = new LuaVector[7];

    public static LuaVector toModelSpace(LuaVector vec) {
        return vec._mul(MODEL_SPACE_FACTORS[vec._size()]); // Multiplies the vector by the correct size vector in the factors array
    }

    static {
        // Store seven vectors with sizes ranging from 0 to 6, as factors for converting from world to model space
        for (int i = 0; i < 7; i++) {
            // Create a float array with size i
            float[] vals = new float[i];
            // Iterate through this array
            for (int j = 0; j < i; j++) {
                if(j == 0) vals[j] = -16; // Set the x value to -16
                else if(j == 1) vals[j] = -16; // Set the y value to -16
                else vals[j] = 16; // Set all other values to 16
            }
            MODEL_SPACE_FACTORS[i] = new LuaVector(vals); // Put this vector to the factors array
        }
    }
}
