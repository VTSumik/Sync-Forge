package net.sumik.sync.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public abstract class DoubleBlockModel extends Model {
    private final Map<DoubleBlockHalf, List<ModelPart>> parts;
    protected final int textureWidth;
    protected final int textureHeight;

    public DoubleBlockModel(int textureWidth, int textureHeight) {
        this(RenderType::entityCutout, textureWidth, textureHeight);
    }

    public DoubleBlockModel(Function<ResourceLocation, RenderType> layerFactory, int textureWidth, int textureHeight) {
        super(layerFactory);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.parts = new HashMap<>();
    }

    protected ModelPart createCuboid(int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize) {
        ModelPart.Cube cube = new ModelPart.Cube(textureOffsetU, textureOffsetV, 0, 0, 0, xSize, ySize, zSize, 0, 0, 0, true, this.textureWidth, this.textureHeight, Arrays.stream(Direction.values()).collect(Collectors.toSet()));
        ModelPart part = new ModelPart(List.of(cube), Map.of());
        part.setPos(x, y, z);
        return part;
    }

    protected ModelPart createCuboid(int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize, ModelPart template) {
        ModelPart part = this.createCuboid(textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        part.copyFrom(template);
        part.setPos(x, y, z);
        return part;
    }

    protected ModelPart addCuboid(DoubleBlockHalf type, int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize) {
        ModelPart part = this.createCuboid(textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        if (!this.parts.containsKey(type)) {
            this.parts.put(type, new ArrayList<>());
        }
        this.parts.get(type).add(part);

        return part;
    }

    protected ModelPart addCuboid(DoubleBlockHalf type, int textureOffsetU, int textureOffsetV, float x, float y, float z, float xSize, float ySize, float zSize, ModelPart template) {
        ModelPart part = this.addCuboid(type, textureOffsetU, textureOffsetV, x, y, z, xSize, ySize, zSize);
        part.copyFrom(template);
        part.setPos(x, y, z);
        return part;
    }

    protected ModelPart createTemplate() {
        return new ModelPart(List.of(), Map.of());
    }

    protected ModelPart createRotationTemplate(float pitch, float yaw, float roll) {
        ModelPart template = this.createTemplate();
        template.xRot = pitch;
        template.yRot = yaw;
        template.zRot = roll;
        return template;
    }

    public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay) {
        this.renderToBuffer(matrices, vertices, light, overlay, 1F, 1F, 1F, 1F);
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        matrices.pushPose();
        this.render(DoubleBlockHalf.LOWER, matrices, vertices, light, overlay, red, green, blue, alpha);
        this.translate(matrices);
        this.render(DoubleBlockHalf.UPPER, matrices, vertices, light, overlay, red, green, blue, alpha);
        matrices.popPose();
    }

    public void render(DoubleBlockHalf type, PoseStack matrices, VertexConsumer vertices, int light, int overlay) {
        this.render(type, matrices, vertices, light, overlay, 1F, 1F, 1F, 1F);
    }

    public void render(DoubleBlockHalf type, PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        List<ModelPart> currentParts = this.parts.get(type);
        if (currentParts == null) {
            return;
        }

        for (ModelPart part : currentParts) {
            part.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        }
    }

    protected abstract void translate(PoseStack matrices);
}