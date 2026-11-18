package xy177.snowsbosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.HellstoneEffects;

public abstract class HellstoneMob extends EntityMob implements StructureBoundBoss {
    private static final DataParameter<Integer> ACTION = EntityDataManager.createKey(HellstoneMob.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> START = EntityDataManager.createKey(HellstoneMob.class, DataSerializers.VARINT);
    private String naturalStructure = "";
    private String structureUuid = "";

    protected HellstoneMob(World world) { super(world); isImmuneToFire = true; }
    public abstract String species();
    @Override
    public String naturalStructure() { return naturalStructure; }
    @Override
    public String structureUuid() { return structureUuid; }
    @Override
    public void setStructureUuid(String uuid) { structureUuid = uuid == null ? "" : uuid; }
    protected double n(String key, double value) { return Balance.number(species() + "." + key, value); }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ACTION, this instanceof HellstoneGolem ? -1 : 0);
        dataManager.register(START, 0);
    }

    public int action() { return dataManager.get(ACTION); }
    public int actionAge() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    protected void action(int value) {
        dataManager.set(ACTION, value);
        dataManager.set(START, (int) world.getTotalWorldTime());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        boolean boss = this instanceof HellstoneGolem;
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(n("maxHealth", boss ? 300 : 16));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(boss ? 0.22 : 0.25);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(n("attackDamage", boss ? 14 : 3));
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(n("armor", boss ? 22 : 2));
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(n("armorToughness", 0));
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(boss ? 20 : 16);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(boss ? 1 : 0);
    }

    @Override
    public boolean isOnSameTeam(Entity entity) { return HellstoneEffects.allies(this, entity) || super.isOnSameTeam(entity); }
    @Override
    protected ResourceLocation getLootTable() { return new ResourceLocation(SnowsBosses.MOD_ID, "entities/" + species()); }
    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data); data.setInteger("HellstoneAction", action()); data.setInteger("HellstoneActionAge", actionAge());
        if (!naturalStructure.isEmpty()) data.setString("SnowNaturalStructure", naturalStructure);
        if (!structureUuid.isEmpty()) data.setString("SnowStructureUuid", structureUuid);
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        if (data.hasKey("HellstoneAction")) {
            dataManager.set(ACTION, Math.max(-2, Math.min(this instanceof HellstoneGolem ? 10 : 3, data.getInteger("HellstoneAction"))));
            dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("HellstoneActionAge")));
        }
        naturalStructure = data.getString("SnowNaturalStructure");
        structureUuid = data.getString("SnowStructureUuid");
    }
}
