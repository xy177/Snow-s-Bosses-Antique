package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.CommonProxy;
import xy177.snowsbosses.content.CoralTools;
import xy177.snowsbosses.entity.CopperGolem;
import xy177.snowsbosses.entity.DeepwaterPiranha;
import xy177.snowsbosses.entity.Drowner;
import xy177.snowsbosses.entity.PhantomPillar;
import xy177.snowsbosses.entity.PhantomShot;
import xy177.snowsbosses.entity.TideRaiser;
import xy177.snowsbosses.entity.StructureEyeEntity;
import xy177.snowsbosses.network.Visuals;

public final class ClientProxy extends CommonProxy {
    @Override
    public void moduleStep(boolean active) {
        Minecraft client=Minecraft.getMinecraft();
        client.addScheduledTask(()->{
            if(client.player!=null)xy177.snowsbosses.content.MechasentModuleEffects.applyStep(client.player,active);
        });
    }

    @Override
    public void registerRenderers() {
        xy177.snowsbosses.compat.InnerVoiceCompat.install();
        xy177.snowsbosses.compat.MechasentInnerVoice.register();
        MechasentKeys.register();
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.TechnicalTarget.class, TechnicalTargetRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.TechnicalPiranha.class, TechnicalPiranhaRenderer::new);
        CameraShake.register();
        BossBars.register();
        MechasentMusic.register();
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.MechasentWorkerBee.class, WorkerBeeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.WorkerBeeLaser.class, WorkerBeeRenderer.Laser::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.WorkerBeeBeam.class, WorkerBeeRenderer.Beam::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.Mechasent.class, MechasentRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.MechasentLaser.class, MechasentRenderer.Laser::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.MechasentWave.class, manager -> new net.minecraft.client.renderer.entity.Render<xy177.snowsbosses.entity.MechasentWave>(manager) {
            @Override protected net.minecraft.util.ResourceLocation getEntityTexture(xy177.snowsbosses.entity.MechasentWave entity) { return net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE; }
        });
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstoneGolem.class, manager -> new HellstoneRenderer<>(manager, "hellstone_golem"));
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstoneImp.class, manager -> new HellstoneRenderer<>(manager, "hellstone_imp"));
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstoneBomb.class, manager -> new net.minecraft.client.renderer.entity.RenderSnowball<>(manager,
                net.minecraft.item.Item.getItemFromBlock(xy177.snowsbosses.content.HellstoneContent.RAW_BLOCK), Minecraft.getMinecraft().getRenderItem()));
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstoneWave.class, manager -> new net.minecraft.client.renderer.entity.Render<xy177.snowsbosses.entity.HellstoneWave>(manager) {
            @Override protected net.minecraft.util.ResourceLocation getEntityTexture(xy177.snowsbosses.entity.HellstoneWave entity) { return net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE; }
        });
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstonePillar.class, HellstonePillarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.HellstonePillarShot.class, HellstonePillarShotRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(Drowner.class, manager -> new CreatureRenderer<>(manager, 0.5F));
        RenderingRegistry.registerEntityRenderingHandler(DeepwaterPiranha.class, manager -> new CreatureRenderer<>(manager, 0.5F));
        RenderingRegistry.registerEntityRenderingHandler(TideRaiser.class, manager -> new CreatureRenderer<>(manager, 0.5F));
        RenderingRegistry.registerEntityRenderingHandler(CopperGolem.class, manager -> new CreatureRenderer<>(manager, 0.5F));
        RenderingRegistry.registerEntityRenderingHandler(PhantomPillar.class, PhantomPillarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(PhantomPillar.Enchanted.class, PhantomPillarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(xy177.snowsbosses.entity.CoralSpearShot.class, CoralSpearRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(PhantomShot.class, manager -> new TridentRenderer<>(manager, false));
        RenderingRegistry.registerEntityRenderingHandler(PhantomShot.Red.class, manager -> new TridentRenderer<>(manager, true));
        RenderingRegistry.registerEntityRenderingHandler(StructureEyeEntity.class, StructureEyeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(CoralTools.FireproofItem.class,
                manager -> new RenderEntityItem(manager, Minecraft.getMinecraft().getRenderItem()));
    }

    @Override
    public String miningKeyTooltip() { return MechasentKeys.MINE.getDisplayName(); }

    @Override
    public String moduleKeyTooltip(boolean safe, net.minecraft.item.ItemStack stack) { return MechasentKeys.tooltip(safe,stack); }

    @Override
    public void beamSound(xy177.snowsbosses.entity.WorkerBeeBeam beam) {
        if (!beam.isSilent()) Minecraft.getMinecraft().getSoundHandler().playSound(new WorkerBeeBeamSound(beam));
    }

    @Override
    public void bossBar(Visuals.BossBarMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BossBars.update(message.id, message.visible, message.color);
            MechasentMusic.track(message.musicEntityId, message.visible);
        });
    }

    @Override
    public void visual(Visuals.Message message) {
        Minecraft client = Minecraft.getMinecraft();
        client.addScheduledTask(() -> {
            World world = client.world;
            if (world == null) return;
            if (message.kind.equals("inner_voice")) {
                xy177.snowsbosses.compat.MechasentInnerVoice.receive(client, message);
                return;
            }
            if (message.kind.equals("hellstone_totem")) {
                Entity entity = world.getEntityByID(message.entityId);
                if (entity != null) client.effectRenderer.emitParticleAtEntity(entity, net.minecraft.util.EnumParticleTypes.TOTEM, 30);
                if (entity != null) {
                    client.getSoundHandler().playSound(new net.minecraft.client.audio.PositionedSoundRecord(
                            net.minecraft.init.SoundEvents.ITEM_TOTEM_USE, net.minecraft.util.SoundCategory.PLAYERS,
                            1.0F, 1.0F, (float) entity.posX, (float) entity.posY, (float) entity.posZ));
                }
                if (entity == client.player) {
                    client.entityRenderer.displayItemActivation(new net.minecraft.item.ItemStack(xy177.snowsbosses.content.HellstoneContent.TOTEM));
                }
                return;
            }
            if (message.kind.equals("freeze")) {
                Entity entity = world.getEntityByID(message.entityId);
                if (entity != null) entity.getEntityData().setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, message.count);
                return;
            }
            if (message.kind.equals("drowner_cold")) {
                DrownerFreezeClient.setCold(message.entityId, message.count);
                return;
            }
            if (message.kind.equals("drowner_freeze")) {
                DrownerFreezeClient.start(message.entityId, message.count);
                return;
            }
            if (message.kind.equals("drowner_thaw")) {
                DrownerFreezeClient.thaw(message.entityId);
                return;
            }
            if (message.kind.equals("altar_shake")) {
                CameraShake.start(message.x, message.y, message.z, message.count);
                return;
            }
            if (message.kind.equals("altar_shake_stop")) {
                CameraShake.stop(message.x, message.y, message.z);
                return;
            }
            if (message.kind.equals("mechasent_stone")) {
                MechasentParticle.stone(world, message);
                return;
            }
            if (message.kind.equals("mechasent_teleport") || message.kind.equals("worker_bee_teleport")) {
                boolean worker = message.kind.equals("worker_bee_teleport");
                double width = worker ? 0.65 : 1.9, height = worker ? 0.5 : 6.3;
                for (int index = 0; index < message.count; index++) client.effectRenderer.addEffect(new MechasentParticle(world, "mechasent_particle",
                        message.x + width * (world.rand.nextDouble() - 0.5) * 0.6,
                        message.y + height * (0.1 + world.rand.nextDouble() * 0.8),
                        message.z + width * (world.rand.nextDouble() - 0.5) * 0.6, 0, 0.4, 0));
                return;
            }
            for (int index = 0; index < message.count; index++) {
                double x = message.x + world.rand.nextGaussian() * message.spread;
                double y = message.y + world.rand.nextGaussian() * message.spread;
                double z = message.z + world.rand.nextGaussian() * message.spread;
                double dx = world.rand.nextGaussian() * message.speed;
                double dy = world.rand.nextGaussian() * message.speed;
                double dz = world.rand.nextGaussian() * message.speed;
                if (message.kind.startsWith("mechasent_")) {
                    client.effectRenderer.addEffect(new MechasentParticle(world, message.kind, x, y, z, dx, dy, dz));
                } else if (message.kind.startsWith("hellstone_") && message.kind.endsWith("_ring")) {
                    client.effectRenderer.addEffect(new HellstoneSkillParticle(world, message.kind, x, y, z));
                } else if (message.kind.startsWith("hellstone_")) {
                    client.effectRenderer.addEffect(new HellstoneParticle(world, message.kind, x, y, z, dx, dy, dz));
                } else if (PortParticle.supports(message.kind)) {
                    client.effectRenderer.addEffect(new PortParticle(world, message.kind, x, y, z, dx, dy, dz));
                } else {
                    client.effectRenderer.addEffect(new OceanParticle(world, message.kind, x, y, z, dx, dy, dz));
                }
            }
        });
    }
}
