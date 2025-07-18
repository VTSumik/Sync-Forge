package net.sumik.sync.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sumik.sync.api.shell.ShellState;
import net.sumik.sync.client.model.ShellModel;
import net.sumik.sync.common.block.entity.ShellEntity;

@OnlyIn(Dist.CLIENT)
public class ShellEntityRenderer extends PlayerRenderer {
    private final ShellModel<PlayerModel<AbstractClientPlayer>> shellModel;

    public ShellEntityRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, slim);
        this.shellModel = new ShellModel<>(this.getModel());
        this.shadowRadius = 0;
        this.shadowStrength = 0;
    }

    @Override
    public void render(AbstractClientPlayer player, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);

        if (player instanceof ShellEntity shell && !shell.isActive) {
            float progress = shell.getState().getProgress();

            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            this.scale(player, poseStack, partialTicks);
            poseStack.translate(0.0D, -1.501F, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

            this.applyStateToModel(this.shellModel, shell.getState());
            VertexConsumer vertexConsumer = this.getVertexConsumerForPartiallyTexturedEntity(shell, progress, this.shellModel.getLayer(player.getSkinTextureLocation()), bufferSource);
            this.shellModel.renderToBuffer(poseStack, vertexConsumer, packedLight, getOverlayCoords(player, 0), 1F, 1F, 1F, 1F);

            if (progress >= ShellState.PROGRESS_DONE) {
                for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer : this.layers) {
                    layer.render(poseStack, bufferSource, packedLight, player, 0, 0, partialTicks, 0, 0, 0);
                }
            }
        } else {
            Direction direction = Direction.fromYRot(yaw);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            if (direction == Direction.WEST || direction == Direction.EAST) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            }

            float maxPitch = player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 15 : 5;
            float pitch = maxPitch * (player instanceof ShellEntity shell ? shell.pitchProgress : 0);
            player.setXRot(pitch);
            player.xRotO = pitch;
            super.render(player, yaw, partialTicks, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    @SuppressWarnings("unused")
    private VertexConsumer getVertexConsumerForPartiallyTexturedEntity(ShellEntity shell, float progress, RenderType baseLayer, MultiBufferSource bufferSource) {
        return bufferSource.getBuffer(baseLayer);
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player) {
        return player.shouldShowName() && super.shouldShowName(player);
    }

    protected void applyStateToModel(ShellModel<PlayerModel<AbstractClientPlayer>> model, ShellState state) {
        var animalModel = model.parentModel;
        animalModel.head.setRotation(0, 0, 0);
        animalModel.body.setRotation(0, 0, 0);
        animalModel.rightArm.setRotation(0, 0, 0);
        animalModel.leftArm.setRotation(0, 0, 0);
        animalModel.rightLeg.setRotation(0, 0, 0);
        animalModel.leftLeg.setRotation(0, 0, 0);
        model.parentModel.young = false;
        model.setBuildProgress(state.getProgress());
    }
}