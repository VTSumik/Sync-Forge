package net.sumik.sync.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class GeneratedTextureManager {
    private static final Map<TextureGenerator, ResourceLocation[]> GENERATED_TEXTURES = new HashMap<>();
    private static final ResourceLocation[] EMPTY_TEXTURES = new ResourceLocation[0];

    public static ResourceLocation[] getTextures(TextureGenerator generator) {
        ResourceLocation[] textures = GENERATED_TEXTURES.get(generator);
        if (textures == null) {
            if (RenderSystem.isOnRenderThreadOrInit()) {
                textures = genTextures(generator, GENERATED_TEXTURES.size());
                GENERATED_TEXTURES.put(generator, textures);
            } else {
                RenderSystem.recordRenderCall(() -> GENERATED_TEXTURES.put(generator, genTextures(generator, GENERATED_TEXTURES.size())));
                textures = EMPTY_TEXTURES;
            }
        }
        return textures;
    }

    private static ResourceLocation[] genTextures(TextureGenerator generator, int generatorId) {
        RenderSystem.assertOnRenderThreadOrInit();

        int textureCounter = -1;
        String format = generator.getClass().getSimpleName().toLowerCase() + "_" + generatorId + "_";
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        List<ResourceLocation> textures = new ArrayList<>();

        for (AbstractTexture texture : (Iterable<AbstractTexture>)generator.generateTextures()::iterator) {
            ResourceLocation id = new ResourceLocation("__dynamic", format + (++textureCounter));
            textureManager.register(id, texture);
            textures.add(id);
        }

        return textures.toArray(new ResourceLocation[0]);
    }
}