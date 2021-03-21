package iskallia.vault.util;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nimothy.skinreloadermod.GenericCache;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.network.play.server.SPlayerListItemPacket.AddPlayerData;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SkinProfile {

    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(4);
    private static final GenericCache<String, Property> cache = new GenericCache<String, Property>();
    private static final Logger LOGGER = LogManager.getLogger();

    private String latestNickname;

    public AtomicReference<GameProfile> gameProfile = new AtomicReference<>();
    public AtomicReference<NetworkPlayerInfo> playerInfo = new AtomicReference<>();

    public String getLatestNickname() {
        return this.latestNickname;
    }

    public void updateSkin(String name) {
        if(name.equals(this.latestNickname))return;
        this.latestNickname = name;

        SERVICE.submit(() -> {
            gameProfile.set(new GameProfile(null, name));
            gameProfile.set(SkullTileEntity.updateGameProfile(gameProfile.get()));
            AddPlayerData data = new SPlayerListItemPacket().new AddPlayerData(gameProfile.get(), 0, null, null);
            playerInfo.set(new NetworkPlayerInfo(data));
            NetworkPlayerInfo player = playerInfo.get();

            Property textureProperty = Iterables.getFirst(player.getGameProfile().getProperties().get("textures"), null);

            if ( textureProperty == null )
            {
                LOGGER.info(String.format("Bypassing Cache for %s...", name));
				player.getGameProfile().getProperties().get("textures").add(updateSkinBypassCache(name));
            }
        });
    }

    public ResourceLocation getLocationSkin() {
        if (this.playerInfo == null || this.playerInfo.get() == null) {
            return DefaultPlayerSkin.getDefaultSkinLegacy();
        }

        try {
            return this.playerInfo.get().getLocationSkin();
        } catch (Exception e) {
            System.err.println("stupid! how did you even do this?");
            e.printStackTrace();
        }

        return DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    public static void updateGameProfile(GameProfile input, Consumer<GameProfile> consumer) {
        SERVICE.submit(() -> {
            GameProfile output = SkullTileEntity.updateGameProfile(input);
            consumer.accept(output);
        });
    }

    private static Property updateSkinBypassCache(String name)
    {
        AtomicReference<Property> property = new AtomicReference<>();

        if ( cache.containsKey(name) ) {
            LOGGER.info(String.format("Using Local Cache for %s...", name));
            return cache.get(name).get();
        } else {
            try {
                LOGGER.info(String.format("Bypassing Local Cache for %s...", name));
                URL profileUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                InputStreamReader profileReader = new InputStreamReader(profileUrl.openStream());
                String uuid = new JsonParser().parse(profileReader).getAsJsonObject().get("id").getAsString();

                URL textureUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                InputStreamReader textureReader = new InputStreamReader(textureUrl.openStream());
                JsonArray jsonArray;
                JsonObject textureProperty = new JsonParser().parse(textureReader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

                property.set(new Property("textures", textureProperty.get("value").getAsString(), textureProperty.get("signature").getAsString()));
                cache.put(name, property.get());
                return property.get();
            } catch ( Exception e ) {
                System.err.println("Could not get skin data from session servers!");
                LOGGER.error(String.format("Could not get skin data for %s  from session server", name), e);
                cache.put(name, null);
                return null;
            }
        }
    }

}
