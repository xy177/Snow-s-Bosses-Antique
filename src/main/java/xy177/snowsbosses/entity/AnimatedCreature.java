package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;

public abstract class AnimatedCreature extends EntityCreature implements IAnimatable {
    private static final DataParameter<String> ANIMATION = EntityDataManager.createKey(AnimatedCreature.class, DataSerializers.STRING);
    private static final DataParameter<String> TEXTURE = EntityDataManager.createKey(AnimatedCreature.class, DataSerializers.STRING);
    private final AnimationFactory factory = new AnimationFactory(this);
    protected int action;
    protected int actionTick;

    protected AnimatedCreature(World world) {
        super(world);
        enablePersistence();
    }

    public abstract String species();

    public String modelName() {
        return species();
    }

    public String textureName() {
        String texture = dataManager.get(TEXTURE);
        return texture.isEmpty() ? species() : texture;
    }

    public void texture(String name) {
        if (!name.equals(dataManager.get(TEXTURE))) dataManager.set(TEXTURE, name);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ANIMATION, "");
        dataManager.register(TEXTURE, "");
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        attribute(SharedMonsterAttributes.MAX_HEALTH, "maxHealth", 20.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(
                this instanceof Drowner ? 0.25 : this instanceof DeepwaterPiranha ? 3 : this instanceof CopperGolem ? 0.2 : 0.3);
        attribute(SharedMonsterAttributes.ARMOR, "armor", 0.0);
        attribute(SharedMonsterAttributes.ARMOR_TOUGHNESS, "armorToughness", 0.0);
        attribute(SharedMonsterAttributes.ATTACK_DAMAGE, "attackDamage", 3.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(
                this instanceof Drowner || this instanceof DeepwaterPiranha ? 48 : this instanceof CopperGolem ? 24 : 32);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(
                this instanceof Drowner ? 0.4 : this instanceof DeepwaterPiranha ? 0.8 : this instanceof CopperGolem ? 2 : 0);
    }

    private void attribute(IAttribute attribute, String key, double fallback) {
        IAttributeInstance instance = getEntityAttribute(attribute);
        instance.setBaseValue(n(key, fallback));
    }

    protected double n(String key, double fallback) {
        return Balance.number(species() + "." + key, fallback);
    }

    public void animate(String name) {
        if (!name.equals(dataManager.get(ANIMATION))) dataManager.set(ANIMATION, name);
    }

    protected boolean hasActionAnimation() {
        return !dataManager.get(ANIMATION).isEmpty();
    }

    public String animationName() {
        return dataManager.get(ANIMATION);
    }

    protected void begin(int next, String name) {
        action = next;
        actionTick = 0;
        animate(name);
        getNavigator().clearPath();
    }

    protected void finish() {
        action = 0;
        actionTick = 0;
        animate("");
    }

    protected String idleAnimation(boolean moving) {
        return moving ? "walk" : "idle";
    }

    private String soundPrefix() {
        return this instanceof Drowner ? "entity.drowned." : this instanceof DeepwaterPiranha ? "entity.ravager."
                : this instanceof TideRaiser ? "entity.evoker." : this instanceof CopperGolem ? "entity.iron_golem." : null;
    }

    @Override
    protected net.minecraft.util.SoundEvent getAmbientSound() {
        String prefix = soundPrefix();
        return prefix == null || this instanceof CopperGolem ? null : xy177.snowsbosses.content.ModSounds.get(prefix + "ambient");
    }

    @Override
    protected net.minecraft.util.SoundEvent getHurtSound(net.minecraft.util.DamageSource source) {
        String prefix = soundPrefix();
        return prefix == null ? null : xy177.snowsbosses.content.ModSounds.get(prefix + "hurt");
    }

    @Override
    protected net.minecraft.util.SoundEvent getDeathSound() {
        String prefix = soundPrefix();
        return prefix == null ? null : xy177.snowsbosses.content.ModSounds.get(prefix + "death");
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.Block block) {
        if (this instanceof Drowner || this instanceof CopperGolem) playSound(
                xy177.snowsbosses.content.ModSounds.get(soundPrefix() + "step"), 0.15F, 1);
        else super.playStepSound(pos, block);
    }

    private <T extends IAnimatable> PlayState animation(AnimationEvent<T> event) {
        String name = dataManager.get(ANIMATION);
        boolean loop = name.isEmpty();
        if (loop) name = idleAnimation(event.isMoving());
        if (getHealth() <= 0.0F && (this instanceof Drowner || this instanceof DeepwaterPiranha)) {
            name = "death";
            loop = false;
        }
        event.getController().setAnimation(new AnimationBuilder().addAnimation(name, loop));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "main", 1, this::animation));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!world.isRemote && this instanceof IMob && world.getDifficulty() == EnumDifficulty.PEACEFUL) setDead();
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(SnowsBosses.MOD_ID, "entities/" + species());
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setInteger("Action", action);
        data.setInteger("ActionTick", actionTick);
        data.setString("Animation", dataManager.get(ANIMATION));
        data.setString("Texture", dataManager.get(TEXTURE));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        int loadedAction = data.getInteger("Action");
        if (this instanceof Drowner && loadedAction == 11) loadedAction = 0;
        int maximum = this instanceof Drowner ? 10 : 2;
        action = Math.max(0, Math.min(maximum, loadedAction));
        actionTick = Math.max(0, data.getInteger("ActionTick"));
        animate(data.getString("Animation"));
        texture(data.getString("Texture"));
    }
}
