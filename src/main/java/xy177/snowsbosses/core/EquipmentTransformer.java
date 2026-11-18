package xy177.snowsbosses.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class EquipmentTransformer implements IClassTransformer {
    private static final String HOOK="xy177/snowsbosses/content/MechasentHooks";
    @Override public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null)return null;
        boolean entity=transformedName.equals("net.minecraft.entity.Entity");
        boolean enderman=transformedName.equals("net.minecraft.entity.monster.EntityEnderman");
        boolean repair=transformedName.equals("net.minecraft.inventory.ContainerRepair$2");
        boolean render=transformedName.equals("net.minecraft.client.renderer.entity.Render");
        boolean vision=transformedName.equals("net.minecraft.client.renderer.EntityRenderer");
        boolean waterVision=transformedName.equals("xy177.farmersfuturedelight.client.ModernWaterRendering");
        boolean focus=transformedName.equals("net.minecraft.client.Minecraft");
        boolean items=transformedName.equals("net.minecraft.client.renderer.RenderItem");
        boolean living=transformedName.equals("net.minecraft.entity.EntityLivingBase");
        boolean death=living
                ||transformedName.equals("net.minecraft.entity.player.EntityPlayer")||transformedName.equals("net.minecraft.entity.player.EntityPlayerMP");
        boolean jei=transformedName.equals("mezz.jei.gui.recipes.RecipeLayout");
        if(!entity&&!enderman&&!repair&&!render&&!death&&!vision&&!waterVision&&!focus&&!items&&!jei)return bytes;
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);
        String repairOwner=null;
        if(repair)for(org.objectweb.asm.tree.FieldNode field:node.fields)
            if(field.desc.equals("Lnet/minecraft/inventory/ContainerRepair;")){repairOwner=field.name;break;}
        int changes=0;
        for(MethodNode method:node.methods){
            boolean safeWalkPatched=false;
            if(jei && method.name.equals("drawRecipe") && method.desc.equals("(Lnet/minecraft/client/Minecraft;II)V")){
                InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));list.add(new VarInsnNode(Opcodes.ALOAD,0));
                list.add(new FieldInsnNode(Opcodes.GETFIELD,node.name,"recipeWrapper","Lmezz/jei/api/recipe/IRecipeWrapper;"));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"xy177/snowsbosses/compat/MechasentJeiPlugin","synchronize",
                        "(Lmezz/jei/api/gui/IRecipeLayout;Lmezz/jei/api/recipe/IRecipeWrapper;)V",false));
                method.instructions.insert(list);changes++;
            }
            if(vision && named(method,"getNightVisionBrightness","func_180438_a")
                    || waterVision && method.name.equals("conduitLightmapVision") && method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)F")){
                LabelNode normal=new LabelNode();InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,waterVision?0:1));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"illuminated","(Lnet/minecraft/entity/EntityLivingBase;)Z",false));
                list.add(new JumpInsnNode(Opcodes.IFEQ,normal));list.add(new InsnNode(Opcodes.FCONST_1));list.add(new InsnNode(Opcodes.FRETURN));
                list.add(normal);list.add(new org.objectweb.asm.tree.FrameNode(Opcodes.F_SAME,0,null,0,null));method.instructions.insert(list);changes++;
            }
            if(enderman && named(method,"shouldAttackPlayer","func_70821_d") && method.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)Z")){
                LabelNode normal=new LabelNode();InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,1));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"gazeShield","(Lnet/minecraft/entity/player/EntityPlayer;)Z",false));
                list.add(new JumpInsnNode(Opcodes.IFEQ,normal));list.add(new InsnNode(Opcodes.ICONST_0));list.add(new InsnNode(Opcodes.IRETURN));list.add(normal);list.add(new org.objectweb.asm.tree.FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(list);changes++;
            }
            if(render && named(method,"getTeamColor","func_188298_c") && method.desc.equals("(Lnet/minecraft/entity/Entity;)I")){
                LabelNode normal=new LabelNode();InsnList list=new InsnList();list.add(new InsnNode(Opcodes.ICONST_M1));
                list.add(new VarInsnNode(Opcodes.ALOAD,1));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"outlineColor","(ILnet/minecraft/entity/Entity;)I",false));
                list.add(new InsnNode(Opcodes.DUP));list.add(new JumpInsnNode(Opcodes.IFLT,normal));list.add(new InsnNode(Opcodes.IRETURN));
                list.add(normal);list.add(new org.objectweb.asm.tree.FrameNode(Opcodes.F_SAME1,0,null,1,new Object[]{Opcodes.INTEGER}));
                list.add(new InsnNode(Opcodes.POP));method.instructions.insert(list);changes++;
            }
            for(AbstractInsnNode instruction:method.instructions.toArray()){
                if(items && named(method,"renderItemOverlayIntoGUI","func_180453_a")
                        && method.desc.equals("(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V")
                        && instruction instanceof MethodInsnNode
                        && ((MethodInsnNode)instruction).name.equals("showDurabilityBar")
                        && ((MethodInsnNode)instruction).desc.equals("(Lnet/minecraft/item/ItemStack;)Z")){
                    InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,2));
                    list.add(new VarInsnNode(Opcodes.ILOAD,3));list.add(new VarInsnNode(Opcodes.ILOAD,4));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"xy177/snowsbosses/client/MechasentArmRenderer","overlay","(Lnet/minecraft/item/ItemStack;II)V",false));
                    method.instructions.insertBefore(instruction,list);changes++;
                }
                if(focus && named(method,"setIngameNotInFocus","func_71364_i") && instruction.getOpcode()==Opcodes.RETURN){
                    method.instructions.insertBefore(instruction,new MethodInsnNode(Opcodes.INVOKESTATIC,"xy177/snowsbosses/client/MechasentKeys","focusLost","()V",false));changes++;
                }
                if(transformedName.equals("net.minecraft.entity.EntityLivingBase") && named(method,"frostWalk","func_184594_b")
                        && method.desc.equals("(Lnet/minecraft/util/math/BlockPos;)V") && instruction.getOpcode()==Opcodes.RETURN){
                    InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));list.add(new VarInsnNode(Opcodes.ALOAD,1));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"worldWalk","(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/math/BlockPos;)V",false));
                    method.instructions.insertBefore(instruction,list);changes++;
                }
                if(vision && instruction instanceof MethodInsnNode){
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if(call.name.equals("hasLightmapVision") && call.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/potion/Potion;)Z")){
                        method.instructions.insertBefore(call,new InsnNode(Opcodes.DUP2));
                        method.instructions.insert(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"nightVisionResult","(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/potion/Potion;Z)Z",false));
                        changes++;
                    }
                    if((call.name.equals("isPotionActive")||call.name.equals("func_70644_a")) && call.desc.equals("(Lnet/minecraft/potion/Potion;)Z")){
                        AbstractInsnNode potion=call.getPrevious();while(potion!=null && potion.getOpcode()<0)potion=potion.getPrevious();
                        if(potion instanceof FieldInsnNode && potion.getOpcode()==Opcodes.GETSTATIC){
                            FieldInsnNode field=(FieldInsnNode)potion;InsnList list=new InsnList();list.add(new InsnNode(Opcodes.DUP));
                            list.add(new FieldInsnNode(field.getOpcode(),field.owner,field.name,field.desc));list.add(new InsnNode(Opcodes.SWAP));
                            method.instructions.insertBefore(potion,list);
                        }else method.instructions.insertBefore(call,new InsnNode(Opcodes.DUP2));
                        method.instructions.insert(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"nightVisionResult",
                                "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/potion/Potion;Z)Z",false));
                        changes++;
                    }
                }
                if(entity && named(method,"move","func_70091_d") && instruction instanceof MethodInsnNode){
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if(!safeWalkPatched && (call.name.equals("isSneaking")||call.name.equals("func_70093_af")) && call.desc.equals("()Z")){
                        safeWalkPatched=true;
                        InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"safeWalk","(ZLnet/minecraft/entity/Entity;)Z",false));
                        method.instructions.insert(call,list);changes++;
                    }
                }
                if(living && named(method,"onUpdate","func_70071_h_") && instruction instanceof MethodInsnNode){
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if((call.name.equals("isPotionActive")||call.name.equals("func_70644_a")) && call.desc.equals("(Lnet/minecraft/potion/Potion;)Z")){
                        call.setOpcode(Opcodes.INVOKESTATIC);call.owner=HOOK;call.name="glowingPotion";
                        call.desc="(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/potion/Potion;)Z";call.itf=false;changes++;
                    }
                }
                if(entity && named(method,"isGlowing","func_184202_aL") && instruction.getOpcode()==Opcodes.IRETURN){
                    InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"glowing","(ZLnet/minecraft/entity/Entity;)Z",false));
                    method.instructions.insertBefore(instruction,list);changes++;
                }
                if(repair && repairOwner!=null && named(method,"canTakeStack","func_82869_a") && instruction.getOpcode()==Opcodes.IRETURN){
                    InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));
                    list.add(new FieldInsnNode(Opcodes.GETFIELD,node.name,repairOwner,"Lnet/minecraft/inventory/ContainerRepair;"));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"anvilAllowed","(ZLnet/minecraft/inventory/ContainerRepair;)Z",false));
                    method.instructions.insertBefore(instruction,list);changes++;
                }
                if(death && instruction instanceof MethodInsnNode){
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if(call.owner.equals("net/minecraftforge/common/ForgeHooks") && call.name.equals("onLivingDeath")){
                        InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));list.add(new VarInsnNode(Opcodes.ALOAD,1));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"afterDeathEvent","(ZLnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z",false));
                        method.instructions.insert(call,list);changes++;
                    }
                }
            }
        }
        if(changes==0)return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);
        return writer.toByteArray();
    }
    private static boolean named(MethodNode method,String mcp,String srg){return method.name.equals(mcp)||method.name.equals(srg);}
}
