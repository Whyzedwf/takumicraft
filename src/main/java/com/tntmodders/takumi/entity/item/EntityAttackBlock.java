package com.tntmodders.takumi.entity.item;

import com.tntmodders.takumi.core.TakumiEntityCore;
import com.tntmodders.takumi.entity.ITakumiEntity;
import com.tntmodders.takumi.entity.mobs.EntitySeaGuardianCreeper;
import com.tntmodders.takumi.entity.mobs.EntitySquidCreeper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class EntityAttackBlock extends EntityLiving {
    public static final Block BLOCK = Blocks.OBSIDIAN;

    private final BossInfoServer bossInfo =
            (BossInfoServer) new BossInfoServer(new TextComponentTranslation("entity.attackblock.name"),
                    BossInfo.Color.BLUE, BossInfo.Overlay.NOTCHED_20).setDarkenSky(true);

    private int spawnTick;
    private List<ITakumiEntity> entities = new ArrayList<>();

    private final Potion[] potions =
            {MobEffects.FIRE_RESISTANCE, MobEffects.JUMP_BOOST, MobEffects.SPEED, MobEffects.WATER_BREATHING,
                    MobEffects.RESISTANCE, MobEffects.REGENERATION};

    public EntityAttackBlock(World worldIn) {
        super(worldIn);
        this.setSize(1, 2);
        this.isImmuneToFire = true;

        TakumiEntityCore.getEntityList().forEach(iTakumiEntity -> {
            if (iTakumiEntity.takumiRank() == ITakumiEntity.EnumTakumiRank.LOW ||
                    iTakumiEntity.takumiRank() == ITakumiEntity.EnumTakumiRank.MID) {
                if (iTakumiEntity.getClass() != EntitySeaGuardianCreeper.class &&
                        iTakumiEntity.getClass() != EntitySquidCreeper.class) {
                    entities.add(iTakumiEntity);
                }
            }
        });
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("sp", spawnTick);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
        this.spawnTick = compound.getInteger("sp");
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(100000);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(25000);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.taskEntries.clear();
        this.targetTasks.taskEntries.clear();
    }

    @Override
    public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
    }

    @Override
    protected boolean isMovementBlocked() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 15728880;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        if (source.isExplosion() && amount > 2) {
            this.setDead();
            this.world.loadedEntityList.forEach(entity -> {
                if (entity instanceof EntityLiving) {
                    ((EntityLiving) entity).setAttackTarget(null);
                }
            });
            this.world.playerEntities.forEach(player -> {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.attackEntityFrom(
                            DamageSource.causeMobDamage(this).setDamageIsAbsolute().setDamageAllowedInCreativeMode(),
                            Integer.MAX_VALUE);
                    player.sendMessage(new TextComponentTranslation("entity.attackblock.lose"));
                }
            });
        }
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (this.world.getBlockState(this.getPosition().add(x, y, z)).getBlock() != BLOCK &&
                            !this.world.isAirBlock(this.getPosition().add(x, y, z))) {
                        this.world.setBlockToAir(this.getPosition().add(x, y, z));
                    }
                }
            }
        }
        this.spawnTick++;
        if (this.spawnTick < 1000 && this.spawnTick % 10 == 0 && this.spawnTick != 0 && !this.world.isRemote) {
            for (int i = -this.spawnTick / 10; i <= this.spawnTick / 10; i++) {
                if (this.getPosition().getY() != 1) {
                    this.world.setBlockState(this.getPosition().add(i, -1, -this.spawnTick / 10),
                            BLOCK.getDefaultState());
                    this.world.setBlockState(this.getPosition().add(i, -1, this.spawnTick / 10),
                            BLOCK.getDefaultState());
                    this.world.setBlockState(this.getPosition().add(-this.spawnTick / 10, -1, i),
                            BLOCK.getDefaultState());
                    this.world.setBlockState(this.getPosition().add(this.spawnTick / 10, -1, i),
                            BLOCK.getDefaultState());
                }
                for (int k = 0; k < 5; k++) {
                    this.world.setBlockToAir(this.getPosition().add(i, k, -this.spawnTick / 10));
                }
                for (int k = 0; k < 5; k++) {
                    this.world.setBlockToAir(this.getPosition().add(i, k, this.spawnTick / 10));
                }
                for (int k = 0; k < 5; k++) {
                    this.world.setBlockToAir(this.getPosition().add(-this.spawnTick / 10, k, i));
                }
                for (int k = 0; k < 5; k++) {
                    this.world.setBlockToAir(this.getPosition().add(this.spawnTick / 10, k, i));
                }

                if (this.spawnTick / 10 == 2) {
                    for (int x = -2; x <= 2; x++) {
                        for (int y = 0; y <= 3; y++) {
                            for (int z = -2; z <= 2; z++) {
                                if (y == 3) {
                                    this.world.setBlockState(this.getPosition().add(x, y, z), BLOCK.getDefaultState());
                                } else if (x == -2 || z == -2 || x == 2 || z == 2) {
                                    if (x != 0 && z != 0) {
                                        this.world.setBlockState(this.getPosition().add(x, y, z),
                                                BLOCK.getDefaultState());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.ticksExisted % 20 == 0) {
            if (!this.world.isRemote) {
                int r = this.spawnTick > 1000 ? 100 : this.spawnTick / 10;
                for (int i = 0; i < 5; i++) {
                    int rx = MathHelper.getInt(this.rand, -r, r);
                    int rz = MathHelper.getInt(this.rand, -r, r);
                    for (int y = 0; y <= 5; y++) {
                        if (this.world.getBlockState(this.getPosition().add(rx, y, rz)).getBlock() != BLOCK) {
                            this.world.setBlockToAir(this.getPosition().add(rx, y, rz));
                        }
                    }
                }
               if(this.world.getDifficulty() != EnumDifficulty.PEACEFUL){
                   if (this.rand.nextInt(10) == 0) {
                       try {
                           EntityCreeper igniteTarget = ((EntityCreeper) this.world.loadedEntityList.stream().filter(
                                   entity -> entity instanceof EntityCreeper).sorted((o1, o2) -> {
                               if (o1.getDistanceSqToEntity(EntityAttackBlock.this) ==
                                       o2.getDistanceSqToEntity(EntityAttackBlock.this)) {
                                   return 0;
                               }
                               return (o1.getDistanceSqToEntity(EntityAttackBlock.this) >
                                       o2.getDistanceSqToEntity(EntityAttackBlock.this)) ? 1 : -1;
                           }).toArray()[0]);
                           if (igniteTarget.getDistanceSqToEntity(this) < 100) {
                               igniteTarget.ignite();
                           }
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
                   if (r > 10) {
                       Entity entity = null;
                       if (!entities.isEmpty()) {
                           try {
                               entity =
                                       (Entity) entities.get(this.rand.nextInt(entities.size())).getClass().getConstructor(
                                               World.class).newInstance(this.world);
                           } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                               e.printStackTrace();
                           }
                       }
                       if (entity != null) {
                           int rx = MathHelper.getInt(this.rand, -r, r);
                           int rz = MathHelper.getInt(this.rand, -r, r);

                           if ((Math.abs(rx) > 5 && Math.abs(rz) > 5) || this.rand.nextInt(20) == 0) {
                               entity.setPosition(this.posX + rx, this.posY, this.posZ + rz);
                           }
                           if (entity instanceof EntityLiving) {
                               ((EntityLiving) entity).onInitialSpawn(
                                       this.world.getDifficultyForLocation(this.getPosition().add(rx, 0, rz)), null);
                               ((EntityLiving) entity).addPotionEffect(
                                       new PotionEffect(this.potions[this.rand.nextInt(this.potions.length)], 10 * 60 * 20,
                                               0));
                           }
                           this.world.spawnEntity(entity);
                       }
                   }
               }
            }
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        this.bossInfo.setName(this.getDisplayName());
    }
}