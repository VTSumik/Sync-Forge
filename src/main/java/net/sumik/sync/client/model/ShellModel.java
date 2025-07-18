package net.sumik.sync.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sumik.sync.api.shell.ShellState;
import net.sumik.sync.client.render.CustomRenderLayer;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class ShellModel<T extends AgeableListModel<?>> extends Model {
    public final T parentModel;
    private float buildProgress;
    private final VoxelModel voxelModel;

    public ShellModel(T parentModel) {
        this(parentModel, new Random());
    }

    public ShellModel(T parentModel, Random random) {
        super(RenderType::entityTranslucent);
        this.parentModel = parentModel;
        this.buildProgress = 0;
        this.voxelModel = VoxelModel.fromModel(parentModel, random);
    }

    public void setBuildProgress(float buildProgress) {
        this.buildProgress = buildProgress;
        this.voxelModel.completeness = buildProgress / ShellState.PROGRESS_PRINTING;
    }

    public void setDestructionProgress(float destructionProgress) {
        this.voxelModel.destructionProgress = destructionProgress;
    }

    public RenderType getLayer(ResourceLocation textureId) {
        if (this.isBeingPrinted() || this.isBeingDestroyed()) {
            return this.voxelModel.renderType(textureId);
        }

        if (this.isBeingPainted()) {
            float paintingProgress = (this.buildProgress - ShellState.PROGRESS_PRINTING) / ShellState.PROGRESS_PAINTING;
            float cutoutY = this.voxelModel.pivotY + this.voxelModel.sizeY * (1F - paintingProgress);
            return CustomRenderLayer.getEntityTranslucentPartiallyTextured(textureId, cutoutY);
        }

        return RenderType.entityTranslucent(textureId);
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        Model target = (this.isBeingPrinted() || this.isBeingDestroyed()) ? this.voxelModel : this.parentModel;
        target.renderToBuffer(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    private boolean isBeingPrinted() {
        return this.buildProgress >= ShellState.PROGRESS_START && this.buildProgress < ShellState.PROGRESS_PRINTING;
    }

    private boolean isBeingPainted() {
        return this.buildProgress >= ShellState.PROGRESS_PRINTING && this.buildProgress < ShellState.PROGRESS_DONE;
    }

    private boolean isBeingDestroyed() {
        return this.voxelModel.destructionProgress > 0F;
    }
}