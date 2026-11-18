package xy177.snowsbosses.content;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.common.registry.FFDBlocks;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.CopperGolem;
import xy177.snowsbosses.entity.Drowner;
import xy177.snowsbosses.entity.HellstoneGolem;
import xy177.snowsbosses.entity.Mechasent;
import xy177.snowsbosses.entity.TideRaiser;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class OceanEvents {
    private OceanEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (world.isRemote || !world.isThundering() || event.getPlacedBlock().getBlock() != FFDItems.effectiveBlock(FFDBlocks.LIGHTNING_ROD)) return;
        Block copper = FFDItems.effectiveBlock(FFDBlocks.WAXED_COPPER_BLOCKS[0]);
        if (copper == null || world.getBlockState(pos.down()).getBlock() != copper
                || world.getBlockState(pos.down(2)).getBlock() != copper) return;
        for (EnumFacing direction : new EnumFacing[]{EnumFacing.EAST, EnumFacing.SOUTH}) {
            BlockPos arm = pos.down().offset(direction);
            BlockPos other = pos.down().offset(direction.getOpposite());
            if (world.getBlockState(arm).getBlock() != copper || world.getBlockState(other).getBlock() != copper
                    || !world.isAirBlock(arm.down()) || !world.isAirBlock(other.down())) continue;
            CopperGolem golem = new CopperGolem(world);
            golem.setLocationAndAngles(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 0.5, world.rand.nextFloat() * 360, 0);
            if (!world.spawnEntity(golem)) return;
            for (BlockPos target : new BlockPos[]{pos, pos.down(), pos.down(2), arm, other}) world.setBlockToAir(target);
            if (event.getPlayer() instanceof EntityPlayerMP) award((EntityPlayerMP) event.getPlayer(),
                    new ResourceLocation("minecraft:adventure/summon_iron_golem"));
            world.addWeatherEffect(new EntityLightningBolt(world, pos.getX() + 0.5, pos.getY() - 2, pos.getZ() + 0.5, true));
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP)) return;
        String name = event.getEntityLiving() instanceof Drowner ? "tsd"
                : event.getEntityLiving() instanceof TideRaiser ? "utt"
                : event.getEntityLiving() instanceof HellstoneGolem ? "return_to_mist"
                : event.getEntityLiving() instanceof Mechasent ? "dissociative_recombination" : null;
        if (name != null) award((EntityPlayerMP) source, new ResourceLocation(SnowsBosses.MOD_ID, name));
    }

    private static void award(EntityPlayerMP player, ResourceLocation id) {
        Advancement advancement = player.getServerWorld().getAdvancementManager().getAdvancement(id);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        for (String criterion : progress.getRemaningCriteria()) player.getAdvancements().grantCriterion(advancement, criterion);
    }
}
