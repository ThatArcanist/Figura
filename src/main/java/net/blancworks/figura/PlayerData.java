package net.blancworks.figura;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Responsible for storing all the data associated with the player on this client.
 */
public class PlayerData {
    private static TextureManager textureManager;

    //ID of the player
    public UUID playerId;

    //The custom model associated with the player
    public CustomModel model;
    //The custom texture for the custom model
    public FiguraTexture texture;
    //The custom script for the model.
    public CustomScript script;

    //Vanilla model for the player, in case we need it for something.
    public PlayerEntityModel<?> vanillaModel;

    //Extra textures for the model (like emission)
    public final List<FiguraTexture> extraTextures = new ArrayList<>();

    public PlayerEntity lastEntity;
    public PlayerListEntry playerListEntry;

    //The last hash code of the avatar.
    public String lastHash = "";
    //True if the model needs to be re-loaded due to a hash mismatch.
    public boolean isInvalidated = false;

    private Identifier trustIdentifier;

    public Text playerName;

    public boolean isLocalAvatar = true;

    public boolean hasPopup = false;

    public static final int FILESIZE_WARNING_THRESHOLD = 76800;
    public static final int FILESIZE_LARGE_THRESHOLD = 102400;

    public VertexConsumerProvider getVCP() {
        if (script != null && script.customVCP != null) return script.customVCP;
        else if (FiguraMod.vertexConsumerProvider != null) return FiguraMod.vertexConsumerProvider;
        else return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    public Identifier getTrustIdentifier() {
        if (trustIdentifier == null)
            trustIdentifier = new Identifier("player", playerId.toString());
        return trustIdentifier;
    }

    public static TextureManager getTextureManager() {
        if (textureManager == null)
            textureManager = MinecraftClient.getInstance().getTextureManager();
        return textureManager;
    }

    /**
     * Writes to the NBT this player data.
     *
     * @param nbt the nbt to write to
     * @return {@code true} if the player data was written into the NBT, otherwise {@code false}
     */
    public boolean writeNbt(NbtCompound nbt) {
        //You cannot save a model that is incomplete.
        if (!hasAvatar())
            return false;

        //Put ID.
        nbt.putUuid("id", playerId);

        //Put Model.
        if (model != null)
            nbt.put("model", model.modelNbt);

        //Put Texture.
        if (texture != null) {
            NbtCompound textureNbt = new NbtCompound();
            texture.writeNbt(textureNbt);
            nbt.put("texture", textureNbt);
        }

        //Put Script.
        if (script != null) {
            NbtCompound scriptNbt = new NbtCompound();
            script.toNBT(scriptNbt);
            nbt.put("script", scriptNbt);

            try {
                if (!script.customSounds.isEmpty()) {
                    nbt.put("sounds", writeCustomSoundsNBT());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!extraTextures.isEmpty()) {
            NbtList texList = new NbtList();

            for (FiguraTexture extraTexture : extraTextures) {
                NbtCompound extraTextureNbt = new NbtCompound();
                extraTexture.writeNbt(extraTextureNbt);
                texList.add(extraTextureNbt);
            }

            nbt.put("exTexs", texList);
        }

        return true;
    }

    /**
     * Reads a player data from the given NBT.
     *
     * @param nbt the nbt to read
     */
    public void readNbt(NbtCompound nbt) {
        if (!nbt.contains("id")) return;
        playerId = nbt.getUuid("id");

        model = null;
        texture = null;
        script = null;

        extraTextures.clear();

        try {
            //Create model on main thread.
            NbtCompound modelNbt = (NbtCompound) nbt.get("model");

            //Load model on off-thread.
            if (modelNbt != null) {
                FiguraMod.doTask(() -> {
                    try {
                        model = new CustomModel(modelNbt, this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //Create texture on main thread
            NbtCompound textureNbt = (NbtCompound) nbt.get("texture");

            //Load texture, if any
            if (textureNbt != null) {
                texture = new FiguraTexture();
                texture.id = new Identifier("figura", playerId.toString());
                getTextureManager().registerTexture(texture.id, texture);
                FiguraMod.doTask(() -> texture.readNbt(textureNbt));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("script")) {
                NbtCompound scriptNbt = (NbtCompound) nbt.get("script");

                if (scriptNbt != null) FiguraMod.doTask(() -> {
                    try {
                        script = new CustomScript();
                        script.fromNBT(this, scriptNbt);

                        if (nbt.contains("sounds")) {
                            readCustomSoundsNBT(nbt.getCompound("sounds"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("exTexs")) {
                NbtList textureList = (NbtList) nbt.get("exTexs");

                if (textureList != null) {
                    for (NbtElement element : textureList) {
                        FiguraTexture newTexture = new FiguraTexture();
                        newTexture.id = new Identifier("figura", playerId.toString() + newTexture.type.toString());
                        getTextureManager().registerTexture(newTexture.id, newTexture);
                        extraTextures.add(newTexture);

                        FiguraMod.doTask(() -> newTexture.readNbt((NbtCompound) element));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Returns the file size, in bytes.
    public long getFileSize() {
        try {
            NbtCompound writtenNbt = new NbtCompound();
            this.writeNbt(writtenNbt);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(out);

            NbtIo.writeCompressed(writtenNbt, w);
            return w.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    //Ticks from client.
    public void tick() {
        if (this.playerId == null)
            return;

        if (this.isInvalidated)
            PlayerDataManager.clearPlayer(playerId);

        vanillaModel = ((PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
        lastEntity = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getPlayerByUuid(this.playerId) : null;

        FiguraMod.currentPlayer = (AbstractClientPlayerEntity) lastEntity;

        NewFiguraNetworkManager.subscribe(playerId);

        if (lastEntity != null) {
            playerName = lastEntity.getName();

            if (script != null) {
                try {
                    script.setPlayerEntity();
                    script.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadFromNbt(DataInputStream input) throws Exception {
        NbtCompound nbt = NbtIo.readCompressed(input);
        loadFromNbt(nbt);
    }

    public void loadFromNbt(NbtCompound tag) {
        this.readNbt(tag);
        getFileSize();
    }

    public TrustContainer getTrustContainer() {
        return PlayerTrustManager.getContainer(getTrustIdentifier());
    }

    public boolean canRenderCustomLayers() {
        boolean ret = getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_RENDER_LAYER) == 1;

        if (FabricLoader.getInstance().isModLoaded("iris"))
            return ret && net.coderbot.iris.Iris.getCurrentPack().isEmpty();

        return ret;
    }

    //Saves this playerdata to the cache.
    public void saveToCache() {
        //We run this as a task to make sure all the previous load operations are done (since those are all also tasks)
        FiguraMod.doTask(() -> {
            Path destinationPath = FiguraMod.getModContentDirectory().resolve("cache");

            String[] splitID = this.playerId.toString().split("-");

            for (int i = 0; i < splitID.length; i++) {
                if (i != splitID.length - 1)
                    destinationPath = destinationPath.resolve(splitID[i]);
            }

            Path nbtFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".nbt");
            Path hashFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".hsh");

            try {
                NbtCompound targetTag = new NbtCompound();
                this.writeNbt(targetTag);

                if (!Files.exists(nbtFilePath.getParent()))
                    Files.createDirectories(nbtFilePath.getParent());
                NbtIo.writeCompressed(targetTag, new FileOutputStream(nbtFilePath.toFile()));
                Files.writeString(hashFilePath, this.lastHash);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean hasAvatar() {
        return model != null || script != null;
    }

    public boolean isAvatarLoaded() {
        return (model == null || model.isDone) && (script == null || script.isDone) && (texture == null || texture.isDone);
    }

    private NbtElement writeCustomSoundsNBT() {
        NbtCompound nbt = new NbtCompound();

        for (String key : script.customSounds.keySet()) {
            script.customSounds.get(key).writeNbt(nbt);
        }

        return nbt;
    }

    private void readCustomSoundsNBT(NbtCompound nbt) {
        nbt.getKeys().forEach(key -> FiguraSoundManager.registerCustomSound(script, key, nbt.getByteArray(key), false));
    }

    public void clearData() {
        extraTextures.clear();

        if (script != null) {
            script.clearSounds();
            script.clearPings();
        } else {
            FiguraSoundManager.getChannel().stopSound(playerId);
        }
    }
}
