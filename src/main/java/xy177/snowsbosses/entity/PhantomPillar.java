package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import xy177.snowsbosses.Balance;

public class PhantomPillar extends Entity implements IAnimatable, IEntityOwnable {
    private static final DataParameter<String> ANIMATION = EntityDataManager.createKey(PhantomPillar.class, DataSerializers.STRING);
    private final AnimationFactory factory = new AnimationFactory(this);
    private UUID casterId;
    private Entity caster;
    private int age;

    public PhantomPillar(World world) {
        super(world);
        setSize(0.2F, 0.2F);
        setNoGravity(true);
        isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
        dataManager.register(ANIMATION, "");
    }

    public String species() {
        return "phantom_trident_entity";
    }

    protected boolean enchanted() {
        return false;
    }

    public String animationName() {
        return dataManager.get(ANIMATION);
    }

    private void animate(String name) {
        if (!name.equals(dataManager.get(ANIMATION))) dataManager.set(ANIMATION, name);
    }

    public void setCaster(Entity entity) {
        caster = entity;
        casterId = entity.getUniqueID();
    }

    @Override
    public Entity getOwner() {
        return caster();
    }

    @Override
    public UUID getOwnerId() {
        return casterId;
    }

    private Entity caster() {
        if (caster == null && casterId != null && world instanceof WorldServer) {
            caster = ((WorldServer) world).getEntityFromUuid(casterId);
        }
        return caster;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        motionX = motionY = motionZ = 0;
        if (world.isRemote) return;
        age++;
        if (age == (enchanted() ? 2 : 3)) animate("spawn");
        int first = enchanted() ? 5 : 10;
        if (age == first) playSound(xy177.snowsbosses.content.ModSounds.get("item.trident.throw"), 1, 1);
        int interval = enchanted() ? 15 : 10;
        int count = enchanted() ? 3 : 4;
        if (age >= first && (age - first) % interval == 0 && (age - first) / interval < count) {
            for (Entity target : Combat.nearby(this, posX, posY + 0.6, posZ, 0.6)) {
                if (target instanceof EntityItem || Drowner.isCreativePlayer(target)) continue;
                if (enchanted()) {
                    if (casterId != null && caster() == null || Combat.protectedFrom(target, caster())) continue;
                } else if (target instanceof Drowner || target instanceof DeepwaterPiranha
                        || target instanceof TideRaiser || target instanceof PhantomPillar && !((PhantomPillar) target).enchanted()) {
                    continue;
                }
                target.attackEntityFrom(DamageSource.MAGIC, (float) Balance.number(
                        species() + ".damage", enchanted() ? 3 : 5));
            }
        }
        if (age >= 58) setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void applyEntityCollision(Entity entity) {
    }

    @Override
    public void setNoGravity(boolean ignored) {
        super.setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "main", 1, this::animation));
    }

    private <T extends IAnimatable> PlayState animation(AnimationEvent<T> event) {
        String name = animationName();
        boolean loop = name.isEmpty();
        event.getController().setAnimation(new AnimationBuilder().addAnimation(loop ? "idle" : name, loop));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound data) {
        data.setInteger("Age", age);
        data.setString("Animation", animationName());
        if (casterId != null) data.setUniqueId("Caster", casterId);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound data) {
        age = data.getInteger("Age");
        animate(data.getString("Animation"));
        if (data.hasUniqueId("Caster")) casterId = data.getUniqueId("Caster");
    }

    public static final class Enchanted extends PhantomPillar {
        public Enchanted(World world) {
            super(world);
        }

        @Override
        public String species() {
            return "enchant_phantom_trident_entity";
        }

        @Override
        protected boolean enchanted() {
            return true;
        }
    }
}
