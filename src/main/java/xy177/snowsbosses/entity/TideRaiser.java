package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.world.World;
import xy177.snowsbosses.content.ModSounds;

public final class TideRaiser extends AnimatedCreature implements IMob, StructureBoundBoss {
    private int cooldown;
    private double enemyX;
    private double enemyY;
    private double enemyZ;
    private String naturalStructure = "";
    private String structureUuid = "";

    public TideRaiser(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 20;
        cooldown = 80;
    }

    @Override
    public String species() {
        return "tide_raiser";
    }

    @Override
    public String naturalStructure() {
        return naturalStructure;
    }

    @Override
    public String structureUuid() {
        return structureUuid;
    }

    @Override
    public void setStructureUuid(String uuid) {
        structureUuid = uuid == null ? "" : uuid;
    }

    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, 10, false, false, null));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 10, false, false, null));
        targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
        tasks.addTask(4, new EntityAIWander(this, 1.0, 120));
        tasks.addTask(5, new EntityAILookIdle(this));
        tasks.addTask(6, new EntityAISwimming(this));
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.ILLAGER;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return source != DamageSource.FALL && source != DamageSource.DROWN && super.attackEntityFrom(source, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || !isEntityAlive()) return;
        if (cooldown < 80) cooldown++;
        EntityLivingBase target = getAttackTarget();
        if (target != null && target.isEntityAlive()) {
            double distance = getDistanceSq(target);
            getLookHelper().setLookPositionWithEntity(target, 360, 360);
            if (distance > Math.pow(3.0, 2)) getNavigator().tryMoveToEntityLiving(target, 1.2);
            else getNavigator().clearPath();
            if (action == 0 && cooldown >= 80) {
                boolean close = distance <= Math.pow(2.0, 2)
                        && Math.abs(posY - target.posY) < 0.7;
                begin(close ? 1 : 2, "attack");
                cooldown = close ? 40 : 0;
            }
            if (action != 0) cast(target);
        }
        if (action != 0) {
            getNavigator().clearPath();
            if (actionTick >= 45) finish();
            else actionTick++;
        }
    }

    private void cast(EntityLivingBase target) {
        int sound = 5;
        if (actionTick == sound) playSound(ModSounds.get("entity.evoker.prepare_summon"), 1, 1);
        double interval = 1.0;
        if (action == 2 && actionTick == 15) {
            enemyX = target.posX;
            enemyY = target.posY;
            enemyZ = target.posZ;
        }
        double x = action == 1 ? posX : enemyX;
        double y = action == 1 ? posY : enemyY;
        double z = action == 1 ? posZ : enemyZ;
        int first = (action == 1 ? 13 : 15);
        int wave = -1;
        for (int index = 0; index < (int) (action == 1 ? 5 : 3); index++) {
            if (actionTick == first + index * 2) wave = index;
        }
        if (wave < 0) return;
        int[][] offsets = wave == 0 ? new int[][]{{0, 0}}
                : wave == 1 ? new int[][]{{0, 1}, {1, 0}, {-1, 0}, {0, -1}}
                : wave == 2 ? new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}
                : wave == 3 ? new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}
                : new int[][]{{1, 2}, {-2, 1}, {-1, 2}, {2, 1}, {2, -1}, {-2, -1}, {1, -2}, {-1, -2}};
        for (int[] offset : offsets) Combat.pillar(this, x + offset[0] * interval, y, z + offset[1] * interval, false);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setInteger("CastCooldown", cooldown);
        data.setDouble("EnemyX", enemyX);
        data.setDouble("EnemyY", enemyY);
        data.setDouble("EnemyZ", enemyZ);
        if (!naturalStructure.isEmpty()) data.setString("SnowNaturalStructure", naturalStructure);
        if (!structureUuid.isEmpty()) data.setString("SnowStructureUuid", structureUuid);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        cooldown = data.getInteger("CastCooldown");
        enemyX = data.getDouble("EnemyX");
        enemyY = data.getDouble("EnemyY");
        enemyZ = data.getDouble("EnemyZ");
        naturalStructure = data.getString("SnowNaturalStructure");
        structureUuid = data.getString("SnowStructureUuid");
    }
}
