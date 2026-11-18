package xy177.snowsbosses.content;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Combat;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class TridentEnchantment extends Enchantment {
    public static final TridentEnchantment INSTANCE = new TridentEnchantment();

    private TridentEnchantment() {
        super(Rarity.UNCOMMON, EnumEnchantmentType.DIGGER, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        setRegistryName(SnowsBosses.MOD_ID, "spawn_trident");
        setName(SnowsBosses.MOD_ID + ".spawn_trident");
    }

    @Override
    public int getMaxLevel() {
        return Balance.ticks("enchantment.maxLevel", 3);
    }

    @Override
    public boolean isTreasureEnchantment() {
        return true;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Enchantment> event) {
        event.getRegistry().register(INSTANCE);
    }

    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        Entity target = event.getEntityLiving();
        Entity source = event.getSource().getTrueSource();
        if (target.world.isRemote || !(source instanceof EntityLivingBase) || Combat.protectedFrom(target, source)) return;
        int level = EnchantmentHelper.getEnchantmentLevel(INSTANCE, ((EntityLivingBase) source).getHeldItemMainhand());
        double offset = Balance.number("enchantment.chanceDenominatorOffset", 4.0);
        if (level > 0 && target.world.rand.nextDouble() < level / (level + offset)) {
            Combat.pillar(source, target.posX, target.posY, target.posZ, true);
        }
    }
}
