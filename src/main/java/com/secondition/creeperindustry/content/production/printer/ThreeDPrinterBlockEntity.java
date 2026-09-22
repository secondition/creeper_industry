package com.secondition.creeperindustry.content.production.printer;

import com.secondition.creeperindustry.*;
import com.secondition.creeperindustry.content.production.biosphere.*;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Bounded official-template construction. Uses adjacent inventories; never replaces occupied cells.
 */
public class ThreeDPrinterBlockEntity extends BlockEntity {
    private int size = 3, index;
    private String template = "";
    private UUID owner;
    private boolean paused;
    private List<BiosphereStructure.Cell> plan;

    public ThreeDPrinterBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.THREE_D_PRINTER.get(), pos, state);
    }

    public void interact(Player player, ItemStack held) {
        if (level == null || level.isClientSide() || !Container.stillValidBlockEntity(this, player))
            return;
        if (player.isShiftKeyDown()) {
            if (!template.isEmpty()) {
                paused = !paused;
                status(player, "paused", paused);
                setChanged();
                return;
            }
            size = size == 3 ? 5 : size == 5 ? 7 : 3;
            status(player, "size", size);
            setChanged();
            return;
        }
        if (held.getItem() instanceof BlockItem item && item.getBlock() instanceof BiosphereBlock) {
            if (!template.isEmpty()) {
                status(player, "busy", index);
                return;
            }
            template = BuiltInRegistries.BLOCK.getKey(item.getBlock()).toString();
            owner = player.getUUID();
            index = 0;
            paused = false;
            plan = null;
            setChanged();
            status(player, "started", size);
            return;
        }
        status(
                player,
                template.isEmpty() ? "help" : paused ? "paused" : "progress",
                template.isEmpty() ? size : index);
    }

    private void status(Player player, String key, Object value) {
        player.displayClientMessage(
                Component.translatable("message.creeper_industry.printer." + key, value), false);
    }

    private List<BiosphereStructure.Cell> plan() {
        if (plan != null) return plan;
        ResourceLocation id = ResourceLocation.tryParse(template);
        if (id == null || !(BuiltInRegistries.BLOCK.get(id) instanceof BiosphereBlock block))
            return List.of();
        Direction facing = getBlockState().getValue(ThreeDPrinterBlock.FACING);
        BlockPos controller = worldPosition.relative(facing.getOpposite(), 2).above();
        plan =
                BiosphereStructure.cells(
                        controller,
                        block.defaultBlockState().setValue(BiosphereBlock.FACING, facing),
                        size);
        return plan;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server) || paused || template.isEmpty() || owner == null)
            return;
        Player player = server.getPlayerByUUID(owner);
        if (player == null) return;
        List<BiosphereStructure.Cell> cells = plan();
        if (cells.isEmpty()) {
            template = "";
            setChanged();
            return;
        }
        int budget = 4;
        while (budget-- > 0 && index < cells.size()) {
            var cell = cells.get(index);
            BlockPos pos = cell.pos();
            if (!level.isInWorldBounds(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.hasChunkAt(pos)
                    || !server.mayInteract(player, pos)
                    || !player.mayBuild()) return;
            BlockState existing = level.getBlockState(pos);
            if (existing.equals(cell.state()) || (cell.state().isAir() && existing.isAir())) {
                index++;
                setChanged();
                continue;
            }
            if (cell.state().isAir() || !existing.isAir()) {
                if (level.getGameTime() % 100 == 0) status(player, "blocked", pos.toShortString());
                return;
            }
            Item material = cell.state().getBlock().asItem();
            Slot materialSlot = find(material), fuelSlot = find(Items.GUNPOWDER);
            if (materialSlot == null || fuelSlot == null) {
                if (level.getGameTime() % 100 == 0)
                    status(player, "materials", new ItemStack(material).getHoverName());
                return;
            }
            if (!level.setBlock(pos, cell.state(), Block.UPDATE_ALL)) return;
            materialSlot.take();
            fuelSlot.take();
            index++;
            setChanged();
        }
        if (index >= cells.size()) {
            template = "";
            plan = null;
            status(player, "complete", size);
            setChanged();
        }
    }

    private Slot find(Item item) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof Container inventory)) continue;
            int[] slots =
                    inventory instanceof WorldlyContainer sided
                            ? sided.getSlotsForFace(direction.getOpposite())
                            : java.util.stream.IntStream.range(0, inventory.getContainerSize())
                                    .toArray();
            for (int slot : slots) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.is(item)
                        && (!(inventory instanceof WorldlyContainer sided)
                                || sided.canTakeItemThroughFace(
                                        slot, stack, direction.getOpposite())))
                    return new Slot(inventory, slot);
            }
        }
        return null;
    }

    private record Slot(Container inventory, int index) {
        void take() {
            inventory.removeItem(index, 1);
            inventory.setChanged();
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("size", size);
        tag.putInt("index", index);
        tag.putString("template", template);
        tag.putBoolean("paused", paused);
        if (owner != null) tag.putUUID("owner", owner);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        size = BiosphereStructure.SIZES.contains(tag.getInt("size")) ? tag.getInt("size") : 3;
        index = Math.clamp(tag.getInt("index"), 0, size * size * size);
        template = tag.getString("template");
        paused = tag.getBoolean("paused");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        plan = null;
    }
}
