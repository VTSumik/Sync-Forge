package net.sumik.sync.common.block.entity;

import net.sumik.sync.api.event.PlayerSyncEvents;
import net.sumik.sync.api.shell.ShellStateContainer;
import net.sumik.sync.common.block.ShellStorageBlock;
import net.sumik.sync.client.gui.ShellSelectorGUI;
import net.sumik.sync.common.config.SyncConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.sumik.sync.common.utils.BlockPosUtil;

public class ShellStorageBlockEntity extends AbstractShellContainerBlockEntity implements IEnergyStorage {
    private EntityState entityState;
    private int ticksWithoutPower;
    private int storedEnergy;
    private final BooleanAnimator connectorAnimator;
    private LazyOptional<IEnergyStorage> energyHandler;

    public ShellStorageBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SHELL_STORAGE.get(), pos, state);
        this.entityState = EntityState.NONE;
        this.connectorAnimator = new BooleanAnimator(false);
        this.energyHandler = LazyOptional.of(() -> this);
    }

    public DyeColor getIndicatorColor() {
        if (this.level != null && ShellStorageBlock.isPowered(this.getBlockState())) {
            return this.color == null ? DyeColor.LIME : this.color;
        }

        return DyeColor.RED;
    }

    @OnlyIn(Dist.CLIENT)
    public float getConnectorProgress(float tickDelta) {
        return this.getBottomPart().map(x -> ((ShellStorageBlockEntity)x).connectorAnimator.getProgress(tickDelta)).orElse(0f);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);

        SyncConfig config = SyncConfig.getInstance();
        boolean infinitePower = config.shellStorageConsumption() == 0;
        boolean isReceivingRedstonePower = !infinitePower
                && config.shellStorageAcceptsRedstone()
                && ShellStorageBlock.isEnabled(state);
        boolean hasEnergy = infinitePower ? true : this.storedEnergy > 0;
        boolean isPowered = infinitePower || isReceivingRedstonePower || hasEnergy;
        boolean shouldBeOpen = isPowered && this.getBottomPart().map(x -> x.shell == null).orElse(true);

        ShellStorageBlock.setPowered(state, world, pos, isPowered);
        ShellStorageBlock.setOpen(state, world, pos, shouldBeOpen);

        if (!infinitePower) {
            if (this.shell != null && !isPowered) {
                ++this.ticksWithoutPower;
                if (this.ticksWithoutPower >= config.shellStorageMaxUnpoweredLifespan()) {
                    this.destroyShell((ServerLevel)world, pos);
                }
            } else {
                this.ticksWithoutPower = 0;
            }
        }

        if (!infinitePower && !isReceivingRedstonePower && hasEnergy) {
            this.storedEnergy = (int) Mth.clamp(this.storedEnergy - config.shellStorageConsumption(), 0, config.shellStorageCapacity());
        }
    }

    @Override
    public void onClientTick(Level world, BlockPos pos, BlockState state) {
        super.onClientTick(world, pos, state);
        this.connectorAnimator.setValue(this.shell != null);
        this.connectorAnimator.step();
        if (this.entityState == EntityState.LEAVING || this.entityState == EntityState.CHILLING) {
            this.entityState = BlockPosUtil.hasPlayerInside(pos, world) ? this.entityState : EntityState.NONE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void onEntityCollisionClient(Entity entity, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (this.entityState == EntityState.NONE) {
            boolean isInside = BlockPosUtil.isEntityInside(entity, this.worldPosition);
            PlayerSyncEvents.ShellSelectionFailureReason failureReason = !isInside && client.player == entity ? PlayerSyncEvents.ALLOW_SHELL_SELECTION.invoker().allowShellSelection(player, this) : null;
            this.entityState = isInside || failureReason != null ? EntityState.CHILLING : EntityState.ENTERING;
            if (failureReason != null) {
                player.displayClientMessage(failureReason.toText(), true);
            }
        } else if (this.entityState != EntityState.CHILLING && client.screen == null) {
            BlockPosUtil.moveEntity(entity, this.worldPosition, state.getValue(ShellStorageBlock.FACING), this.entityState == EntityState.ENTERING);
        }

        if (this.entityState == EntityState.ENTERING && client.player == entity && client.screen == null && BlockPosUtil.isEntityInside(entity, this.worldPosition)) {
            client.setScreen(new ShellSelectorGUI(() -> this.entityState = EntityState.LEAVING, () -> this.entityState = EntityState.CHILLING));
        }
    }

    @Override
    public InteractionResult onUse(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (stack.getCount() > 0 && item instanceof DyeItem dye) {
            stack.shrink(1);
            this.color = dye.getDyeColor();
        }
        return InteractionResult.SUCCESS;
    }

    // IEnergyStorage implementation
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (SyncConfig.getInstance().shellStorageConsumption() == 0) {
            return 0;
        }

        ShellStorageBlockEntity bottom = (ShellStorageBlockEntity)this.getBottomPart().orElse(null);
        if (bottom == null) {
            return 0;
        }

        int capacity = bottom.getMaxEnergyStored();
        int maxEnergy = Mth.clamp(capacity - bottom.storedEnergy, 0, capacity);
        int inserted = Mth.clamp(maxReceive, 0, maxEnergy);

        if (!simulate) {
            bottom.storedEnergy += inserted;
        }

        return inserted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.getBottomPart().map(x -> ((ShellStorageBlockEntity)x).storedEnergy).orElse(0);
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.toIntExact(SyncConfig.getInstance().shellStorageConsumption() == 0 ? 0 : SyncConfig.getInstance().shellStorageCapacity());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return SyncConfig.getInstance().shellStorageConsumption() != 0;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ShellStateContainer.CAPABILITY) {
            return LazyOptional.of(() -> (T) this);
        }
        if (cap == ForgeCapabilities.ENERGY && this.canReceive()) {
            return this.energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.energyHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.energyHandler = LazyOptional.of(() -> this);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("storedEnergy", this.storedEnergy);
        nbt.putInt("ticksWithoutPower", this.ticksWithoutPower);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("storedEnergy");
        this.ticksWithoutPower = nbt.getInt("ticksWithoutPower");
    }

    private enum EntityState {
        NONE,
        ENTERING,
        CHILLING,
        LEAVING
    }

}