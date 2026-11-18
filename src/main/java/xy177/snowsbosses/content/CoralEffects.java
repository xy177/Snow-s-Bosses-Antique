package xy177.snowsbosses.content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xy177.farmersfuturedelight.common.registry.FFDPotions;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.Balance;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class CoralEffects {
    private static final ResourceLocation CORAL_LUNG_ICON = new ResourceLocation(SnowsBosses.MOD_ID, "textures/mob_effect/coral_lung.png");
    public static final Potion CORAL_LUNG = new Potion(true, 0x0099FF) {
        @Override
        @SideOnly(Side.CLIENT)
        public void renderInventoryEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z) {
            renderIcon(x + 6, y + 7, 1.0F);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void renderHUDEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) {
            renderIcon(x + 3, y + 3, alpha);
        }

        @SideOnly(Side.CLIENT)
        private void renderIcon(int x, int y, float alpha) {
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(CORAL_LUNG_ICON);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
        }

        @Override
        public boolean isReady(int duration, int amplifier) {
            return true;
        }

        @Override
        public void applyAttributesModifiersToEntity(EntityLivingBase entity,
                AbstractAttributeMap attributes, int amplifier) {
            super.applyAttributesModifiersToEntity(entity, attributes, amplifier);
            entity.setAir(0);
        }

        @Override
        public void performEffect(EntityLivingBase entity, int amplifier) {
            if (!entity.world.isRemote) {
                entity.setAir((int) Math.max(-19L,
                        (long) entity.getAir() - Balance.integer("effects.airDrainPerTick", 1)));
            }
        }
    }.setPotionName("effect.snows_bosses.coral_lung").setRegistryName(SnowsBosses.MOD_ID, "coral_lung");

    private CoralEffects() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(CORAL_LUNG);
    }

    public static boolean inGroup(ItemStack stack, String name) {
        if (stack == null || stack.isEmpty()) return false;
        int id = OreDictionary.getOreID(name);
        for (int entry : OreDictionary.getOreIDs(stack)) if (entry == id) return true;
        return false;
    }

    public static boolean fullSet(EntityLivingBase entity) {
        ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        Item chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem();
        return (head.getItem() == ModItems.SEAGOLDEN_HELMET || inGroup(head, ModItems.CORAL_HELMETS))
                && (chest == ModItems.CORAL_CHESTPLATE || chest == ModItems.SEAGOLDEN_CHESTPLATE)
                && entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() == ModItems.CORAL_LEGGINGS
                && entity.getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem() == ModItems.CORAL_BOOTS;
    }

    public static void hit(EntityLivingBase target, EntityLivingBase attacker,
            int duration, boolean clearAir, boolean heal) {
        if (attacker.world.isRemote) return;
        if (heal && attacker.isEntityAlive()) attacker.heal((float) Balance.number("effects.spearHealing", 1.0));
        if (fullSet(attacker)) {
            target.addPotionEffect(new PotionEffect(CORAL_LUNG, duration, Balance.integer("effects.coralLungAmplifier", 0), false, false));
        } else if (clearAir) {
            target.setAir(0);
        }
    }

    public static void armorTick(EntityLivingBase entity) {
        if (entity.world.isRemote || !fullSet(entity)) return;
        if (!entity.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)) {
            effect(entity, MobEffects.WATER_BREATHING, Balance.integer("effects.waterBreathingDuration", 1205), "waterBreathingAmplifier");
            effect(entity, FFDPotions.effectiveDolphinsGrace(), Balance.integer("effects.dolphinsGraceDuration", 602), "dolphinsGraceAmplifier");
        }
        if (!entity.isInWater()) return;
        Item head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem();
        boolean all = head == ModItems.SEAGOLDEN_HELMET
                && entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() == ModItems.SEAGOLDEN_CHESTPLATE;
        if (all || head == ModItems.REGENERATION_CORAL_HELMET) regeneration(entity);
        if (all || head == ModItems.STRENGTH_CORAL_HELMET) effect(entity, MobEffects.STRENGTH, Balance.integer("effects.helmetBuffDuration", 3), "helmetBuffAmplifier");
        if (all || head == ModItems.HASTE_CORAL_HELMET) effect(entity, MobEffects.HASTE, Balance.integer("effects.helmetBuffDuration", 3), "helmetBuffAmplifier");
    }

    private static void regeneration(EntityLivingBase entity) {
        int duration = Balance.integer("effects.helmetBuffDuration", 3);
        int amplifier = Balance.integer("effects.helmetBuffAmplifier", 0);
        if (duration <= 0) return;
        PotionEffect active = entity.getActivePotionEffect(MobEffects.REGENERATION);
        if (active == null || active.getAmplifier() < amplifier
                || active instanceof ArmorRegenerationEffect && ((ArmorRegenerationEffect) active).fromArmor) {
            entity.addPotionEffect(new ArmorRegenerationEffect(duration, amplifier));
        }
    }

    private static final class ArmorRegenerationEffect extends PotionEffect {
        private int elapsed;
        private boolean fromArmor = true;

        private ArmorRegenerationEffect(int duration, int amplifier) {
            super(MobEffects.REGENERATION, duration, amplifier, false, false);
        }

        @Override
        public boolean onUpdate(EntityLivingBase entity) {
            if (fromArmor && getDuration() > 0
                    && MobEffects.REGENERATION.isReady(++elapsed, getAmplifier())) {
                MobEffects.REGENERATION.performEffect(entity, getAmplifier());
            }
            return super.onUpdate(entity);
        }

        @Override
        public void performEffect(EntityLivingBase entity) {
            if (!fromArmor) super.performEffect(entity);
        }

        @Override
        public void combine(PotionEffect other) {
            if (!(other instanceof ArmorRegenerationEffect) && other.getAmplifier() >= getAmplifier()) {
                fromArmor = false;
            }
            super.combine(other);
        }
    }

    private static void effect(EntityLivingBase entity, Potion potion, int duration, String amplifier) {
        if (potion != null) entity.addPotionEffect(new PotionEffect(potion, duration, Balance.integer("effects." + amplifier, 0), false, false));
    }
}
