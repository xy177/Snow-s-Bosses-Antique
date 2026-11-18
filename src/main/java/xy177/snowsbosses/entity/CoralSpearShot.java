package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.entity.EntityTrident;

public final class CoralSpearShot extends EntityTrident {
    private static final DataParameter<ItemStack> STACK = EntityDataManager.createKey(CoralSpearShot.class, DataSerializers.ITEM_STACK);

    public CoralSpearShot(World world) {
        super(world);
    }

    public CoralSpearShot(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower, stack);
        dataManager.set(STACK, super.getTridentStack());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STACK, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getTridentStack() {
        ItemStack stack = dataManager.get(STACK);
        return world.isRemote && !stack.isEmpty() ? stack.copy() : super.getTridentStack();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        dataManager.set(STACK, super.getTridentStack());
    }
}
