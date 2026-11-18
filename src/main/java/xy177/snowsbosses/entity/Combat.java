package xy177.snowsbosses.entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;
import xy177.snowsbosses.Balance;

public final class Combat {
    private Combat() {
    }

    public static List<Entity> nearby(Entity source, double x, double y, double z, double radius) {
        Vec3d center = new Vec3d(x, y, z);
        List<Entity> entities = source.world.getEntitiesWithinAABBExcludingEntity(source,
                new AxisAlignedBB(center, center).grow(radius));
        entities.sort(Comparator.comparingDouble(entity -> entity.getDistanceSq(x, y, z)));
        return entities;
    }

    public static float depthBonus(Entity target) {
        return target instanceof EntityLivingBase ? EnchantmentHelper.getEnchantmentLevel(
                Enchantments.DEPTH_STRIDER, ((EntityLivingBase) target).getItemStackFromSlot(EntityEquipmentSlot.FEET)) : 0;
    }

    private static Entity owner(Entity entity) {
        if (entity instanceof IEntityOwnable) return ((IEntityOwnable) entity).getOwner();
        if (entity instanceof AbstractHorse) {
            UUID id = ((AbstractHorse) entity).getOwnerUniqueId();
            return id == null ? null : entity.world.getPlayerEntityByUUID(id);
        }
        return null;
    }

    public static boolean protectedFrom(Entity target, Entity caster) {
        if (target == caster) return true;
        Entity owner = owner(target);
        UUID ownerId = target instanceof IEntityOwnable ? ((IEntityOwnable) target).getOwnerId()
                : target instanceof AbstractHorse ? ((AbstractHorse) target).getOwnerUniqueId() : null;
        if (caster != null && (caster.isOnSameTeam(target)
                || caster.getUniqueID().equals(ownerId) || owner == caster || owner != null && caster.isOnSameTeam(owner))) return true;
        if (caster != null && ownerId != null && caster.getTeam() != null && target.world.getMinecraftServer() != null) {
            com.mojang.authlib.GameProfile profile = target.world.getMinecraftServer().getPlayerProfileCache().getProfileByUUID(ownerId);
            if (profile != null && caster.getTeam().isSameTeam(target.world.getScoreboard().getPlayersTeam(profile.getName()))) return true;
        }
        if (Balance.flag("enchantment.protectAllPlayersAndPets", true)) {
            if (target instanceof EntityPlayer || owner instanceof EntityPlayer) return true;
            if (target instanceof AbstractHorse && ((AbstractHorse) target).getOwnerUniqueId() != null) return true;
            if (target instanceof IEntityOwnable && ((IEntityOwnable) target).getOwnerId() != null) return true;
        }
        return false;
    }

    public static boolean alliedWith(Entity target, Entity caster) {
        if (target == caster || caster == null || caster.isOnSameTeam(target)) return true;
        Entity targetOwner = owner(target);
        if (targetOwner == caster || targetOwner != null
                && (caster.isOnSameTeam(targetOwner) || targetOwner.isOnSameTeam(caster))) return true;
        UUID ownerId = target instanceof IEntityOwnable ? ((IEntityOwnable) target).getOwnerId()
                : target instanceof AbstractHorse ? ((AbstractHorse) target).getOwnerUniqueId() : null;
        if (caster.getUniqueID().equals(ownerId)) return true;
        if (ownerId != null && caster.getTeam() != null && target.world.getMinecraftServer() != null) {
            com.mojang.authlib.GameProfile profile = target.world.getMinecraftServer().getPlayerProfileCache().getProfileByUUID(ownerId);
            if (profile != null && caster.getTeam().isSameTeam(target.world.getScoreboard().getPlayersTeam(profile.getName()))) return true;
        }
        return false;
    }

    public static void pillar(Entity caster, double x, double y, double z, boolean enchanted) {
        PhantomPillar pillar = enchanted ? new PhantomPillar.Enchanted(caster.world) : new PhantomPillar(caster.world);
        pillar.setLocationAndAngles(Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5,
                caster.world.rand.nextFloat() * 360.0F, 0.0F);
        pillar.setCaster(caster);
        caster.world.spawnEntity(pillar);
        if (!caster.world.isRemote && caster instanceof Drowner && !caster.isRiding()
                && ((Drowner) caster).isHalfHealthCombat()) {
            caster.world.addWeatherEffect(new EntityLightningBolt(caster.world, pillar.posX, pillar.posY, pillar.posZ, true));
            for (Entity target : nearby(caster, pillar.posX, pillar.posY + 1.0, pillar.posZ, 3.0)) {
                if (!(target instanceof EntityLivingBase) || Drowner.isCreativePlayer(target) || alliedWith(target, caster)) continue;
                target.attackEntityFrom(net.minecraft.util.DamageSource.LIGHTNING_BOLT,
                        (float) Balance.number("drowner.pillarLightningDamage", 6));
            }
        }
    }
}
