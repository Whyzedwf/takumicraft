package com.tntmodders.takumi.core;

import com.tntmodders.takumi.TakumiCraftCore;
import com.tntmodders.takumi.core.client.TakumiModelCore;
import com.tntmodders.takumi.entity.ITakumiEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TakumiEntityCore {
    private static List<Biome> biomes = new ArrayList<>();
    public static List<ITakumiEntity> entityList = new ArrayList<>();

    public static void register() {
        for (Field fileld : Biomes.class.getDeclaredFields()) {
            try {
                TakumiEntityCore.biomes.add(((Biome) fileld.get(null)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        TakumiEntityCore.biomes.remove(Biomes.HELL);
        TakumiEntityCore.biomes.remove(Biomes.VOID);
        File packFile = FMLCommonHandler.instance().findContainerFor(TakumiCraftCore.TakumiInstance).getSource();
        String s = packFile.toURI().getPath();
        if (s.endsWith("jar")) {
            s = s + "/";
        }
        String spack = s + ("com.tntmodders.takumi.entity.mobs".replace(".", "/"));
        TakumiCraftCore.LOGGER.info("takumicraft" + spack);
        List<File> files = Arrays.asList(new File(spack).listFiles());
        ArrayList<EntityHolder> entityHolders = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String className = "com.tntmodders.takumi.entity.mobs." + file.getName().substring(0, file.getName().indexOf(".class"));
            try {
                Class clazz = Class.forName(className);
                ITakumiEntity entity = ((ITakumiEntity) clazz.getConstructor(World.class).newInstance(Minecraft.getMinecraft().world));
                entityHolders.add(new EntityHolder(clazz, entity));
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        entityHolders.sort(new EntityComparator());
        for (EntityHolder holder : entityHolders) {
            Class clazz = holder.clazz;
            ITakumiEntity entity = holder.entity;
            ResourceLocation location = new ResourceLocation(TakumiCraftCore.MODID, entity.getRegisterName());
            EntityRegistry.registerModEntity(location, clazz, location.getResourcePath(), entity.getRegisterID(), TakumiCraftCore.TakumiInstance, 64, 2, true,
                    entity.getPrimaryColor(), entity.getSecondaryColor());
            if (!entity.isCustomSpawn() && entity.takumiRank().getSpawnWeight() != 0) {
                EntityRegistry.addSpawn(clazz, entity.takumiRank().getSpawnWeight(), 1, 20, EnumCreatureType.MONSTER, biomes.toArray(new Biome[0]));
            }
            if (FMLCommonHandler.instance().getSide().isClient()) {
                TakumiModelCore.registerEntityRender(clazz, entity);
            }
            TakumiEntityCore.entityList.add(entity);
            TakumiCraftCore.LOGGER.info("Registered entity on ID " + entity.getRegisterID() + " : " + location.getResourcePath() + " , " + entity.takumiRank().name() + " and " + entity.takumiType().name());

            String sResource = s + ("assets.takumicraft.advancements.".replace(".", "/"));
            File file = new File(sResource + "slay_" + entity.getRegisterName() + ".json");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File oldFile = new File(s + "assets/takumicraft/temp_adv/slay.json");
            FileReader h_fr = null;
            String buf = "";
            try {
                String h_s;
                h_fr = new FileReader(oldFile);
                BufferedReader h_br = new BufferedReader(h_fr);
                while (true) {
                    h_s = h_br.readLine();
                    if (h_s == null) {
                        break;
                    }

                    h_s = h_s.replaceAll("creeper_hoge", entity.getRegisterName());

                    buf = buf + h_s;
                }
                h_fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(buf);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    static class EntityComparator implements Comparator<EntityHolder> {
        @Override
        public int compare(EntityHolder o1, EntityHolder o2) {
            return o1.entity.getRegisterID() < o2.entity.getRegisterID() ? -1 : 1;
        }
    }

    static class EntityHolder {
        final Class clazz;
        final ITakumiEntity entity;

        EntityHolder(Class cls, ITakumiEntity ent) {
            this.clazz = cls;
            this.entity = ent;
        }
    }
}
