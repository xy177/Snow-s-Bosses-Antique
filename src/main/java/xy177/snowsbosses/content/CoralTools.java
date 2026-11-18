package xy177.snowsbosses.content;

import com.google.common.collect.Multimap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import xy177.snowsbosses.Balance;

public final class CoralTools {
    private CoralTools() {
    }

    private static Multimap<String, AttributeModifier> attributes(Multimap<String, AttributeModifier> map,
            EntityEquipmentSlot slot, double damage, double speed) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            map.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(
                    java.util.UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf"), "Weapon modifier", damage, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(
                    java.util.UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3"), "Weapon modifier", speed, 0));
        }
        return map;
    }

    public static final class Weapon extends ItemSword {
        private final double damage;
        private final double speed;
        private final int lungDuration;
        private final boolean heal;
        private final boolean fireproof;

        public Weapon(String name, Item.ToolMaterial material, double damage, double speed,
                int lungDuration, boolean heal, boolean fireproof) {
            super(material);
            this.damage = Balance.number("items." + name + ".attackDamage", damage);
            this.speed = Balance.number("items." + name + ".attackSpeed", speed);
            this.lungDuration = Balance.integer("effects." + (heal ? "spearLungDuration"
                    : lungDuration == 80 ? "largeWeaponLungDuration" : "smallWeaponLungDuration"), lungDuration);
            this.heal = heal;
            this.fireproof = fireproof;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, net.minecraft.block.state.IBlockState state) {
            if (state.getBlock() == net.minecraft.init.Blocks.WEB) return 15;
            return super.getDestroySpeed(stack, state) > 1 ? 1.5F : 1;
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            return attributes(super.getItemAttributeModifiers(slot), slot, damage, speed);
        }

        @Override
        public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
            boolean result = super.hitEntity(stack, target, attacker);
            CoralEffects.hit(target, attacker, lungDuration, this == ModItems.CORAL_SWORD, heal);
            return result;
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (this != ModItems.CORAL_SCYTHE || player.getHeldItemMainhand().getItem() != this
                    || player.getCooldownTracker().hasCooldown(this)) {
                return new ActionResult<>(EnumActionResult.PASS, stack);
            }
            if (!world.isRemote) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                for (Entity target : world.getEntitiesWithinAABBExcludingEntity(player,
                        new net.minecraft.util.math.AxisAlignedBB(player.getPositionVector(), player.getPositionVector())
                                .grow(Balance.number("scythe.sweepRange", 4.0) + 0.5))) {
                    if (!(target instanceof EntityItem) && target.getDistanceSq(player) <= Math.pow(Balance.number("scythe.sweepRange", 4.0), 2)) {
                        target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) Balance.number("scythe.sweepDamage", 10.0));
                    }
                }
                player.getCooldownTracker().setCooldown(this, Balance.integer("scythe.cooldown", 40));
            }
            player.swingArm(EnumHand.MAIN_HAND);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return fireproof;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return new FireproofItem(world, location, stack);
        }
    }

    public static final class Axe extends ItemAxe {
        public Axe() {
            super(ModItems.material("coral_axe", 4, 1300, 9.0F, 6.0F, 18),
                    (float) Balance.number("items.coral_axe.attackDamage", 7.0),
                    (float) Balance.number("items.coral_axe.attackSpeed", -2.9));
        }

        @Override
        public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
            boolean result = super.hitEntity(stack, target, attacker);
            CoralEffects.hit(target, attacker, Balance.integer("effects.smallWeaponLungDuration", 160), true, false);
            return result;
        }
    }

    public static final class Pickaxe extends ItemPickaxe {
        public Pickaxe() {
            super(ModItems.material("coral_pickaxe", 4, 1300, 9.0F, 3.5F, 18));
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            return attributes(super.getItemAttributeModifiers(slot), slot, Balance.number("items.coral_pickaxe.attackDamage", 4.5),
                    Balance.number("items.coral_pickaxe.attackSpeed", -3.0));
        }

        @Override
        public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
            boolean result = super.hitEntity(stack, target, attacker);
            CoralEffects.hit(target, attacker, Balance.integer("effects.smallWeaponLungDuration", 160), true, false);
            return result;
        }
    }

    public static final class FireproofItem extends EntityItem {
        public FireproofItem(World world) {
            super(world);
            isImmuneToFire = true;
        }

        public FireproofItem(World world, Entity original, ItemStack stack) {
            this(world);
            NBTTagCompound data = new NBTTagCompound();
            original.writeToNBT(data);
            readFromNBT(data);
            setItem(stack);
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            return !source.isFireDamage() && super.attackEntityFrom(source, amount);
        }
    }
}
