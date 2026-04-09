package com.secondition.creeperindustry.content.logistics.storage;

import com.secondition.creeperindustry.CIEntityTypes;
import com.secondition.creeperindustry.CIItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class StorageDiscMinecartEntity extends Minecart {
    private ItemStack discStack = new ItemStack(CIItems.STORAGE_DISC.get());

    public StorageDiscMinecartEntity(EntityType<? extends StorageDiscMinecartEntity> entityType, Level level) {
        super(entityType, level);
    }

    public StorageDiscMinecartEntity(Level level, double x, double y, double z, ItemStack discStack) {
        super(CIEntityTypes.STORAGE_DISC_MINECART.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
        setDiscStack(discStack);
    }

    public ItemStack getDiscStack() {
        return discStack.copy();
    }

    public void setDiscStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(CIItems.STORAGE_DISC.get())) {
            this.discStack = new ItemStack(CIItems.STORAGE_DISC.get());
            return;
        }

        this.discStack = stack.copyWithCount(1);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            setCustomName(stack.getHoverName());
        }
    }

    @Override
    protected Item getDropItem() {
        return CIItems.STORAGE_DISC.get();
    }

    @Override
    protected void destroy(DamageSource source) {
        kill();
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }

        ItemStack drop = getDiscStack();
        if (hasCustomName()) {
            drop.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        spawnAtLocation(drop);
    }

    @Override
    public ItemStack getPickResult() {
        return getDiscStack();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("StorageDisc", discStack.saveOptional(level().registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setDiscStack(ItemStack.parseOptional(level().registryAccess(), compound.getCompound("StorageDisc")));
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }
}
