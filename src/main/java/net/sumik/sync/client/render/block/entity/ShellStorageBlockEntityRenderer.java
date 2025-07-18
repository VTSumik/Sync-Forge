package net.sumik.sync.client.render.block.entity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sumik.sync.Sync;
import net.sumik.sync.api.shell.ShellState;
import net.sumik.sync.client.model.AbstractShellContainerModel;
import net.sumik.sync.client.model.ShellStorageModel;
import net.sumik.sync.common.block.AbstractShellContainerBlock;
import net.sumik.sync.common.block.SyncBlocks;
import net.sumik.sync.common.block.entity.ShellStorageBlockEntity;
import net.sumik.sync.common.block.entity.ShellEntity;

@OnlyIn(Dist.CLIENT)
public class ShellStorageBlockEntityRenderer extends AbstractShellContainerBlockEntityRenderer<ShellStorageBlockEntity> {
    private static final ResourceLocation SHELL_STORAGE_TEXTURE_ID = new ResourceLocation(Sync.MOD_ID, "textures/block/shell_storage.png");
    private static final BlockState DEFAULT_STATE = SyncBlocks.SHELL_STORAGE.get().defaultBlockState()
            .setValue(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(AbstractShellContainerBlock.FACING, Direction.SOUTH)
            .setValue(AbstractShellContainerBlock.OPEN, false);

    private final ShellStorageModel model;

    public ShellStorageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new ShellStorageModel();
    }

    @Override
    protected AbstractShellContainerModel getShellContainerModel(ShellStorageBlockEntity blockEntity, BlockState blockState, float tickDelta) {
        this.model.ledColor = blockEntity.getIndicatorColor();
        this.model.connectorProgress = blockEntity.getConnectorProgress(tickDelta);
        return this.model;
    }

    @Override
    protected ShellEntity createEntity(ShellState shellState, ShellStorageBlockEntity blockEntity, float tickDelta) {
        ShellEntity entity = shellState.asEntity();
        entity.isActive = shellState.getProgress() >= ShellState.PROGRESS_DONE;
        entity.pitchProgress = entity.isActive ? blockEntity.getConnectorProgress(tickDelta) : 0;
        return entity;
    }

    @Override
    protected BlockState getDefaultState() {
        return DEFAULT_STATE;
    }

    @Override
    protected ResourceLocation getTextureId() {
        return SHELL_STORAGE_TEXTURE_ID;
    }
}