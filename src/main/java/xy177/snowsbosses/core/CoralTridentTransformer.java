package xy177.snowsbosses.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class CoralTridentTransformer implements IClassTransformer {
    private static final String PREFIX = "xy177/farmersfuturedelight/";
    private static final String TRIDENT = PREFIX + "common/entity/EntityTrident";
    private static final String HOOK = "xy177/snowsbosses/content/CoralSpear";
    private static final String FACTORY = "(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        String target = transformedName.replace('.', '/');
        boolean items = target.equals(PREFIX + "common/registry/FFDItems");
        boolean item = target.equals(PREFIX + "common/item/ItemTrident");
        boolean hands = target.equals(PREFIX + "client/ClientEventHandler");
        boolean pose = target.equals(PREFIX + "client/FFDClientSwimmingHooks");
        boolean projectile = target.equals(TRIDENT);
        if (!items && !item && !hands && !pose && !projectile) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (projectile && (method.name.equals("onHit") || method.name.equals("func_184549_a"))
                    && method.desc.equals("(Lnet/minecraft/util/math/RayTraceResult;)V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (!call.owner.equals("net/minecraft/world/World") || !call.desc.equals("()Z")
                            || !(call.name.equals("isThundering") || call.name.equals("func_72911_I"))) continue;
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "channelingWeather", "(ZL" + TRIDENT + ";)Z", false));
                    method.instructions.insert(instruction, list);
                    changes++;
                }
            }
            if (projectile && method.name.equals("getLoyalty") && method.desc.equals("()I")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() != Opcodes.IRETURN) continue;
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "returnLevel", "(IL" + TRIDENT + ";)I", false));
                    method.instructions.insertBefore(instruction, list);
                    changes++;
                }
            }
            if (items && method.name.equals("isTridentItem") && method.desc.equals("(Lnet/minecraft/item/Item;)Z")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() != Opcodes.IRETURN) continue;
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "isTrident", "(ZLnet/minecraft/item/Item;)Z", false));
                    method.instructions.insertBefore(instruction, list);
                    changes++;
                }
            }
            if ((hands && method.name.equals("renderChargingTrident")) || (pose && method.name.equals("applyTridentPose"))) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof FieldInsnNode)) continue;
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.getOpcode() != Opcodes.GETSTATIC || !field.owner.equals(PREFIX + "common/registry/FFDItems")
                            || !field.name.equals("TRIDENT")) continue;
                    AbstractInsnNode next = field.getNext();
                    while (next != null && next.getOpcode() < 0) next = next.getNext();
                    if (!(next instanceof JumpInsnNode)) continue;
                    JumpInsnNode comparison = (JumpInsnNode) next;
                    if (comparison.getOpcode() != Opcodes.IF_ACMPEQ && comparison.getOpcode() != Opcodes.IF_ACMPNE) continue;
                    method.instructions.insertBefore(comparison, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "isTrident",
                            "(Lnet/minecraft/item/Item;Lnet/minecraft/item/Item;)Z", false));
                    comparison.setOpcode(comparison.getOpcode() == Opcodes.IF_ACMPEQ ? Opcodes.IFNE : Opcodes.IFEQ);
                    changes++;
                }
            }
            if (item && (method.name.equals("onPlayerStoppedUsing") || method.name.equals("func_77615_a"))) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof TypeInsnNode) || instruction.getOpcode() != Opcodes.NEW
                            || !((TypeInsnNode) instruction).desc.equals(TRIDENT)) continue;
                    AbstractInsnNode duplicate = instruction.getNext();
                    while (duplicate != null && duplicate.getOpcode() < 0) duplicate = duplicate.getNext();
                    if (duplicate == null || duplicate.getOpcode() != Opcodes.DUP) continue;
                    AbstractInsnNode constructor = duplicate.getNext();
                    while (constructor != null && !(constructor instanceof MethodInsnNode
                            && constructor.getOpcode() == Opcodes.INVOKESPECIAL
                            && ((MethodInsnNode) constructor).owner.equals(TRIDENT))) constructor = constructor.getNext();
                    if (constructor == null || !((MethodInsnNode) constructor).name.equals("<init>")
                            || !((MethodInsnNode) constructor).desc.equals(FACTORY + "V")) continue;
                    method.instructions.remove(instruction);
                    method.instructions.remove(duplicate);
                    method.instructions.set(constructor, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "projectile", FACTORY + "L" + TRIDENT + ";", false));
                    changes++;
                }
            }
        }
        if (changes == 0) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
