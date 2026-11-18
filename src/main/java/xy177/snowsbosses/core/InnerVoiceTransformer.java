package xy177.snowsbosses.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class InnerVoiceTransformer implements IClassTransformer {
    private static final String MANAGER = "xy177/innervoiceantique/client/profile/VoiceProfileManager";
    private static final String TARGET = MANAGER + "$TextTarget";
    private static final String HOOK = "xy177/snowsbosses/compat/InnerVoiceCompat";

    private static MethodNode eventTarget() {
        String event = "xy177/snowsbosses/compat/MechasentInnerVoice$Event";
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "snowsBossesEventTarget",
                "(Lnet/minecraft/client/Minecraft;)Ljava/util/Optional;", null, null);
        InsnList code = method.instructions;
        LabelNode present = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xy177/snowsbosses/compat/MechasentInnerVoice", "event",
                "(Lnet/minecraft/client/Minecraft;)L" + event + ";", false));
        code.add(new VarInsnNode(Opcodes.ASTORE, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, present));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(present);
        code.add(new FrameNode(Opcodes.F_APPEND, 1, new Object[]{event}, 0, null));
        code.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.NEW, TARGET));
        code.add(new InsnNode(Opcodes.DUP));
        String[] names = {"key", "lines", "mainColor", "accentColor"};
        String[] descriptors = {"Ljava/lang/String;", "Ljava/util/List;", "I", "I"};
        for (int index = 0; index < names.length; index++) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, event, names[index], descriptors[index]));
        }
        code.add(new InsnNode(Opcodes.ICONST_0));
        for (String name : new String[]{"x", "y", "z"}) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, event, name, "D"));
        }
        code.add(new LdcInsnNode(Double.NaN));
        code.add(new InsnNode(Opcodes.DCONST_1));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.LCONST_0));
        for (String name : new String[]{"duration", "elapsed"}) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, event, name, "J"));
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, TARGET, "<init>",
                "(Ljava/lang/String;Ljava/util/List;IIZDDDDDIJJJ)V", false));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false));
        code.add(new InsnNode(Opcodes.ARETURN));
        method.maxLocals = 2;
        method.maxStack = 24;
        return method;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        boolean manager = transformedName.equals(MANAGER.replace('/', '.'));
        boolean target = transformedName.equals(TARGET.replace('/', '.'));
        boolean renderer = transformedName.equals("xy177.innervoiceantique.client.render.HudVoiceRenderer")
                || transformedName.equals("xy177.innervoiceantique.client.render.WorldVoiceRenderer");
        if (!manager && !target && !renderer) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (renderer && method.name.equals("updateTarget")) {
                int targetSlot = -1;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode
                            && instruction.getOpcode() == Opcodes.CHECKCAST
                            && ((org.objectweb.asm.tree.TypeInsnNode) instruction).desc.equals(TARGET)) {
                        AbstractInsnNode next = instruction.getNext();
                        while (next != null && next.getOpcode() < 0) next = next.getNext();
                        if (next instanceof VarInsnNode && next.getOpcode() == Opcodes.ASTORE)
                            targetSlot = ((VarInsnNode) next).var;
                    }
                }
                if (targetSlot >= 0) for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (!call.owner.equals(node.name) || !(call.name.equals("initialDelay")
                            && call.desc.equals("(L" + TARGET + ";)J")
                            || call.name.equals("delayFor") && call.desc.equals("(F)J"))) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    code.add(new VarInsnNode(Opcodes.ALOAD, targetSlot));
                    code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "getEntityId", "()I", false));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xy177/snowsbosses/compat/MechasentInnerVoice", "delay",
                            "(JLnet/minecraft/client/Minecraft;I)J", false));
                    method.instructions.insert(instruction, code);
                    changes++;
                }
            }
            if (manager && method.name.equals("findCurrentTarget")
                    && method.desc.equals("(Lnet/minecraft/client/Minecraft;)Ljava/util/Optional;")) {
                LabelNode normal = new LabelNode();
                InsnList code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MANAGER, "snowsBossesEventTarget", method.desc, false));
                code.add(new InsnNode(Opcodes.DUP));
                code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false));
                code.add(new JumpInsnNode(Opcodes.IFEQ, normal));
                code.add(new InsnNode(Opcodes.ARETURN));
                code.add(normal);
                code.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/util/Optional"}));
                code.add(new InsnNode(Opcodes.POP));
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("reloadIfNeeded")) {
                        method.instructions.insert(instruction, code);
                        changes++;
                        break;
                    }
                }
            }
            if (manager && method.name.equals("loadProfiles") && method.desc.equals("()V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() == Opcodes.RETURN) {
                        method.instructions.insertBefore(instruction,
                                new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "reload", "()V", false));
                        changes++;
                    }
                }
            }
            if (manager && (method.name.equals("scanEntityTarget") || method.name.equals("isEntityTargetStillValid"))) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.getOpcode() == Opcodes.INVOKESTATIC && call.owner.equals("net/minecraft/entity/EntityList")
                            && (call.name.equals("getKey") || call.name.equals("func_191301_a"))
                            && call.desc.equals("(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/ResourceLocation;")) {
                        call.owner = HOOK;
                        call.name = "entityKey";
                        changes++;
                    }
                }
            }
            if (manager && method.name.equals("stabilizeEntityTarget")
                    && method.desc.equals("(Lnet/minecraft/world/World;Lnet/minecraft/client/entity/EntityPlayerSP;Ljava/util/Optional;J)Ljava/util/Optional;")) {
                LabelNode normal = new LabelNode();
                InsnList code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 2));
                code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false));
                code.add(new JumpInsnNode(Opcodes.IFEQ, normal));
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new VarInsnNode(Opcodes.ALOAD, 2));
                code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false));
                code.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, TARGET));
                code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "getEntityId", "()I", false));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "immediate", "(Lnet/minecraft/world/World;I)Z", false));
                code.add(new JumpInsnNode(Opcodes.IFEQ, normal));
                code.add(new VarInsnNode(Opcodes.ALOAD, 2));
                code.add(new FieldInsnNode(Opcodes.PUTSTATIC, MANAGER, "stableEntityTarget", "Ljava/util/Optional;"));
                code.add(new LdcInsnNode(""));
                code.add(new FieldInsnNode(Opcodes.PUTSTATIC, MANAGER, "pendingEntityKey", "Ljava/lang/String;"));
                code.add(new InsnNode(Opcodes.LCONST_0));
                code.add(new FieldInsnNode(Opcodes.PUTSTATIC, MANAGER, "pendingEntitySince", "J"));
                code.add(new VarInsnNode(Opcodes.ALOAD, 2));
                code.add(new InsnNode(Opcodes.ARETURN));
                code.add(normal);
                code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(code);
                changes++;
            }
            if (target && method.name.equals("getTextLines") && method.desc.equals("()Ljava/util/List;")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "ownerPlayer", "Lnet/minecraft/client/entity/EntityPlayerSP;"));
                    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    code.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "entityId", "I"));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "lines",
                            "(Ljava/util/List;Lnet/minecraft/client/entity/EntityPlayerSP;I)Ljava/util/List;", false));
                    method.instructions.insertBefore(instruction, code);
                    changes++;
                }
            }
            if (target && method.name.equals("hasTextLines") && method.desc.equals("()Z")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction instanceof FieldInsnNode && instruction.getOpcode() == Opcodes.GETFIELD
                            && ((FieldInsnNode) instruction).name.equals("textLines")) {
                        method.instructions.set(instruction,
                                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "getTextLines", "()Ljava/util/List;", false));
                        changes++;
                    }
                }
            }
        }
        if (manager && changes > 0) node.methods.add(eventTarget());
        if (changes == 0) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
