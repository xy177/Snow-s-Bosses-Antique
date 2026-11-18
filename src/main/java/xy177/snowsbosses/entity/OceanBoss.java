package xy177.snowsbosses.entity;

import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import xy177.snowsbosses.network.Visuals;

public abstract class OceanBoss extends AnimatedCreature implements IMob {
    private final BossInfoServer bar;

    protected OceanBoss(World world, BossInfo.Overlay overlay) {
        super(world);
        bar = new BossInfoServer(new TextComponentTranslation("entity.snows_bosses." + species() + ".name"),
                BossInfo.Color.BLUE, overlay);
    }

    @Override
    public void setPortal(net.minecraft.util.math.BlockPos pos) {
    }

    @Override
    public net.minecraft.entity.Entity changeDimension(int dimension, net.minecraftforge.common.util.ITeleporter teleporter) {
        return null;
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bar.addPlayer(player);
        if (this instanceof Drowner) Visuals.bossBar(player, bar.getUniqueId(), true, -1, getEntityId());
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bar.removePlayer(player);
        if (this instanceof Drowner) Visuals.bossBar(player, bar.getUniqueId(), false, -1, getEntityId());
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        bar.setPercent(getHealth() / getMaxHealth());
        bar.setName(getDisplayName());
    }

    @Override
    protected void onDeathUpdate() {
        int delay = (this instanceof Drowner ? 50 : 40);
        if (++deathTime >= delay) {
            deathTime = 19;
            super.onDeathUpdate();
        }
    }

    @Override
    public void setDead() {
        if (bar != null) bar.setVisible(false);
        super.setDead();
    }

    @Override
    public boolean isOnSameTeam(Entity entity) {
        return isBossPartner(entity) || super.isOnSameTeam(entity);
    }

    boolean isBossPartner(Entity entity) {
        return this instanceof Drowner && entity instanceof DeepwaterPiranha
                || this instanceof DeepwaterPiranha && entity instanceof Drowner;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return !isBossPartner(source.getTrueSource()) && source != DamageSource.FALL
                && source != DamageSource.DROWN && super.attackEntityFrom(source, amount);
    }
}
