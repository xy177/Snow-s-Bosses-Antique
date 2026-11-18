package xy177.snowsbosses.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;

public final class WorkerBeeSwarm {
    private final Mechasent owner;
    private List<MechasentWorkerBee> members = new ArrayList<>();
    private long memberTick = Long.MIN_VALUE;
    private UUID targetId;
    private int progress;
    private int pending;
    private int healthSummons;

    WorkerBeeSwarm(Mechasent owner) { this.owner = owner; }

    public List<MechasentWorkerBee> members() {
        long now = owner.world.getTotalWorldTime();
        if (memberTick != now) {
            members = owner.world.getEntitiesWithinAABB(MechasentWorkerBee.class,
                    owner.getEntityBoundingBox().grow(64, 256, 64), bee -> bee.boundTo(owner) && !bee.isDead);
            members.sort(Comparator.comparing(Entity::getUniqueID));
            memberTick = now;
        }
        return members;
    }

    public EntityLivingBase target() {
        Entity entity = targetId == null ? null : ((WorldServer) owner.world).getEntityFromUuid(targetId);
        return entity instanceof EntityLivingBase && entity.isEntityAlive() ? (EntityLivingBase) entity : null;
    }

    public boolean active() { return targetId != null; }

    public void begin(EntityLivingBase target) {
        if (targetId == null) { targetId = target.getUniqueID(); progress = 0; memberTick = Long.MIN_VALUE; owner.broadcast(9); }
    }

    public void brokenArm() { interrupt(); pending++; memberTick = Long.MIN_VALUE; owner.broadcast(owner.getRNG().nextBoolean() ? 2 : 3); }

    public float damageMultiplier() {
        int count = 0;
        for (MechasentWorkerBee bee : members()) if (bee.isEntityAlive() && bee.boundTo(owner)) count++;
        return 1 - Math.min(0.35F, count * 0.1F);
    }

    public MechasentWorkerBee healer() {
        for (MechasentWorkerBee bee : members()) if (bee.isEntityAlive() && bee.boundTo(owner) && !bee.sleeping() && bee.action() == 4) return bee;
        return null;
    }

    public boolean supported() { return healer() != null; }

    public void tick() {
        int thresholds = owner.getHealth() <= owner.getMaxHealth() / 3 ? 2 : owner.getHealth() <= owner.getMaxHealth() * 2 / 3 ? 1 : 0;
        if (thresholds > healthSummons) {
            pending += thresholds - healthSummons;
            healthSummons = thresholds;
        }
        if (pending > 0 && owner.ticksExisted % 10 == 0) {
            MechasentWorkerBee bee = new MechasentWorkerBee(owner.world);
            bee.bind(owner);
            if (bee.teleportNearOwner() && owner.world.spawnEntity(bee)) {
                pending--; memberTick = Long.MIN_VALUE;
            }
        }
        if (supported()) owner.heal(owner.getMaxHealth() * 0.1F / 200);
        if (!active()) return;
        EntityLivingBase target = target();
        if (target == null || MechasentWorkerBee.invalidLockTarget(target)) { interrupt(); return; }
        int count = 0;
        for (MechasentWorkerBee bee : members()) if (!bee.sleeping() && bee.action() == 5) count++;
        if (count == 0) { interrupt(); return; }
        progress += 2 * count + 1;
        if (progress >= 720) {
            owner.world.spawnEntity(new WorkerBeeBeam(owner.world, owner, owner.raidSpawned(), target.posX, target.posY, target.posZ));
            owner.broadcast(11);
            finishLock(false);
        }
    }

    public void interrupt() {
        finishLock(true);
    }

    private void finishLock(boolean interrupted) {
        if (interrupted && active()) owner.broadcast(10);
        targetId = null; progress = 0;
        memberTick = Long.MIN_VALUE;
        for (MechasentWorkerBee bee : new ArrayList<>(members())) if (bee.action() == 5) bee.finishAction();
    }

    public void removeAll() {
        if (owner.world.isRemote) return;
        DeadOwners data = DeadOwners.get(owner.world);
        if (data.ids.add(owner.getUniqueID())) data.markDirty();
        for (MechasentWorkerBee bee : new ArrayList<>(members())) bee.setDead();
    }

    public void write(NBTTagCompound data) {
        data.setInteger("WorkerPending", pending);
        data.setInteger("WorkerHealthSummons", healthSummons);
        data.setInteger("WorkerProgress", progress);
        if (targetId != null) data.setUniqueId("WorkerLockTarget", targetId);
    }

    public void read(NBTTagCompound data) {
        pending = Math.max(0, data.getInteger("WorkerPending"));
        healthSummons = Math.max(0, Math.min(2, data.getInteger("WorkerHealthSummons")));
        progress = Math.max(0, data.getInteger("WorkerProgress"));
        targetId = data.hasUniqueId("WorkerLockTarget") ? data.getUniqueId("WorkerLockTarget") : null;
    }

    public static boolean ownerWasRemoved(World world, UUID id) { return id != null && DeadOwners.get(world).ids.contains(id); }

    public static final class DeadOwners extends WorldSavedData {
        private final Set<UUID> ids = new HashSet<>();
        public DeadOwners(String name) { super(name); }
        static DeadOwners get(World world) {
            String name = "snows_bosses_worker_owners";
            DeadOwners data = (DeadOwners) world.getPerWorldStorage().getOrLoadData(DeadOwners.class, name);
            if (data == null) { data = new DeadOwners(name); world.getPerWorldStorage().setData(name, data); }
            return data;
        }
        @Override public void readFromNBT(NBTTagCompound data) {
            ids.clear();
            NBTTagList list = data.getTagList("Removed", 8);
            for (int i = 0; i < list.tagCount(); i++) {
                try { ids.add(UUID.fromString(list.getStringTagAt(i))); } catch (IllegalArgumentException ignored) { }
            }
        }
        @Override public NBTTagCompound writeToNBT(NBTTagCompound data) {
            NBTTagList list = new NBTTagList();
            for (UUID id : ids) list.appendTag(new NBTTagString(id.toString()));
            data.setTag("Removed", list);
            return data;
        }
    }
}
