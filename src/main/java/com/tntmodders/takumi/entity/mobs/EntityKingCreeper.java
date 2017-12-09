package com.tntmodders.takumi.entity.mobs;

import com.tntmodders.takumi.TakumiCraftCore;
import com.tntmodders.takumi.core.TakumiBlockCore;
import com.tntmodders.takumi.entity.EntityTakumiAbstractCreeper;
import com.tntmodders.takumi.utils.TakumiUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo.Color;
import net.minecraft.world.BossInfo.Overlay;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

import java.lang.reflect.Field;
import java.util.Random;

public class EntityKingCreeper extends EntityTakumiAbstractCreeper {
    
    private static final DataParameter <Integer> ATTACK_ID = EntityDataManager.createKey(EntityKingCreeper.class, DataSerializers.VARINT);
    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(new TextComponentTranslation("entity.kingcreeper.name"), Color
            .GREEN, Overlay.PROGRESS).setDarkenSky(true).setCreateFog(true);
    private DamageSource lastSource;
    
    public EntityKingCreeper(World worldIn) {
        super(worldIn);
        this.isImmuneToFire = true;
        try {
            Field field = EntityCreeper.class.getDeclaredField("fuseTime");
            field.setAccessible(true);
            field.set(this, 90);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(200);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(100);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1000);
    }
    
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ATTACK_ID, 0);
    }
    
    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("attackid", this.getAttackID());
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setAttackID(compound.getInteger("attackid"));
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        if (this.getHealth() < this.getMaxHealth() / 2) {
            if (!this.getPowered()) {
                this.onStruckByLightning(null);
            }
            this.heal(0.01f);
        }
    }
    
    @Override
    public void takumiExplode() {
        if (this.lastSource != null && this.lastSource.isProjectile() && this.lastSource.getTrueSource() != null) {
            this.projectileCounter();
        }
        int maxID = 14;
        int always = 0;
        float power = this.getPowered() ? 10 : 6;
        if (!this.world.isRemote) {
            this.setAttackID(this.rand.nextInt(maxID + 1));
            TakumiCraftCore.LOGGER.info(this.getAttackID());
            //debug only
            if (always != 0) {
                this.setAttackID(always);
            }
        }
        switch (this.getAttackID()) {
            //ランダム爆発1
            case 1:
                if (!this.world.isRemote) {
                    for (int i = 0; i < (this.getPowered() ? 20 : 10); i++) {
                        BlockPos pos = this.createRandomPos(this.getPosition(), 2.5);
                        this.world.createExplosion(this, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, power / 2, true);
                    }
                }
                break;
            //ファイアボール
            case 2:
                this.motionY = 75d;
                if (this.isPotionActive(MobEffects.JUMP_BOOST)) {
                    this.motionY += (this.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
                }
                if (this.isSprinting()) {
                    float f = this.rotationYaw * 0.017453292F;
                    this.motionX -= MathHelper.sin(f) * 0.2F;
                    this.motionZ += MathHelper.cos(f) * 0.2F;
                }
                this.move(MoverType.SELF, motionX, motionY, motionZ);
                this.isAirBorne = true;
                ForgeHooks.onLivingJump(this);
                for (int t = 0; t < (this.getPowered() ? 20 : 10); t++) {
                    Random rand = new Random();
                    int i = this.getPowered() ? 10 : 5;
                    double x = this.posX + this.rand.nextInt(i * 2) - i;
                    double y = this.posY + this.rand.nextInt(i) - i / 2;
                    double z = this.posZ + this.rand.nextInt(i * 2) - i;
                    EntityLargeFireball fireball = new EntityLargeFireball(this.world, x, y, z, 0, -0.5, 0);
                    fireball.motionX = 0;
                    fireball.motionY = -1;
                    fireball.motionZ = 0;
                    fireball.explosionPower = this.getPowered() ? 5 : 3;
                    if (!this.world.isRemote) {
                        this.world.spawnEntity(fireball);
                    }
                }
                break;
            //ランダム落雷
            case 3:
                for (int i = 0; i < 10 * (this.getPowered() ? 3 : 1); i++) {
                    BlockPos pos = this.createRandomPos(this.getPosition(), 1.5);
                    EntityLightningBolt bolt = new EntityLightningBolt(this.world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false);
                    this.world.addWeatherEffect(bolt);
                    this.world.spawnEntity(bolt);
                    if (!this.world.isRemote) {
                        this.world.newExplosion(this, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, power / 2.5f, true, true);
                    }
                }
                break;
            //火薬岩
            case 4:
                for (int t = 0; t < power * 1.25; t++) {
                    EntityLivingBase entity = this.getAttackTarget();
                    if (entity != null) {
                        int MpX = (int) (entity.posX + MathHelper.getInt(this.rand, -2, -5));
                        int MpY = (int) entity.posY;
                        int MpZ = (int) (entity.posZ + MathHelper.getInt(this.rand, -2, -5));
                        int PpX = (int) (entity.posX + MathHelper.getInt(this.rand, 2, 5));
                        int PpY = (int) entity.posY;
                        int PpZ = (int) (entity.posZ + MathHelper.getInt(this.rand, 2, 5));
                        IBlockState state = this.rand.nextInt(10) == 0 ? TakumiBlockCore.DUMMY_GUNORE.getDefaultState() :
                                            TakumiBlockCore.GUNORE.getDefaultState();
                        this.world.setBlockState(new BlockPos(MpX, MpY, MpZ), state);
                        this.world.setBlockState(new BlockPos(PpX, PpY, PpZ), state);
                        this.world.createExplosion(this, MpX, MpY, MpZ, 0, false);
                        this.world.createExplosion(this, PpX, PpY, PpZ, 0, false);
                    } else if (!this.world.isRemote) {
                        this.world.createExplosion(this, this.posX + MathHelper.getInt(this.rand, -4, 4), this.posY + MathHelper.getInt(this.rand,
                                -4, 4), this.posZ + MathHelper.getInt(this.rand, -4, 4), power, true);
                    }
                }
                break;
            //爆破突進
            case 5:
                int l = 25;
                Vec3d vec3d = this.getLookVec().scale(l);
                BlockPos pos = this.getPosition().add(new Vec3i(vec3d.x, vec3d.y, vec3d.z));
                for (double d = 0; d < l; d += 0.5) {
                    if (!this.world.isRemote) {
                        this.world.createExplosion(this, this.posX + vec3d.x / (l - d), this.posY + vec3d.y / (l - d), this.posZ + vec3d.z / (l -
                                d), power / 1.5f, true);
                    }
                }
                this.setPosition(pos.getX(), pos.getY(), pos.getZ());
                break;
            //巨匠召喚
            case 6:
                if (!this.world.isRemote) {
                    for (int i = 0; i < power / 2 + 1; i++) {
                        EntityCreeper creeper = new EntityCreeper(this.world);
                        creeper.copyLocationAndAnglesFrom(this);
                        TakumiUtils.takumiSetPowered(creeper, true);
                        this.world.spawnEntity(creeper);
                    }
                }
                break;
            //十字爆発
            case 7:
                if (!this.world.isRemote) {
                    for (int x = -4; x <= 4; x++) {
                        this.world.createExplosion(this, this.posX + x, this.posY, this.posZ, power / 3 * 2, true);
                    }
                    for (int z = -4; z <= 4; z++) {
                        this.world.createExplosion(this, this.posX, this.posY, this.posZ + z, power / 3 * 2, true);
                    }
                }
                break;
            //ランダム爆発2
            case 8:
                for (int t = 0; t <= 2 + rand.nextInt(3) * power / 5; t++) {
                    int x = rand.nextInt(11) - 5;
                    int y = rand.nextInt(11) - 5;
                    int z = rand.nextInt(11) - 5;
                    if (!this.world.isRemote) { this.world.createExplosion(this, this.posX + x, this.posY + y, this.posZ + z, power / 3, true); }
                }
                break;
            //火薬岩の塔
            case 9:
                if (!this.world.isRemote) {
                    int x = (int) this.posX;
                    int y = (int) this.posY;
                    int z = (int) this.posZ;
                    for (int t = 0; t <= 10; t++) {
                        int v = MathHelper.getInt(this.rand, -4, 4);
                        int w = MathHelper.getInt(this.rand, -4, 4);
                        for (int ty = 0; ty < 6; ty++) {
                            if (this.world.getBlockState(new BlockPos((int) (this.posX + v), (int) (this.posY + ty), (int) (this.posZ + w)))
                                    .getBlock() == Blocks.AIR) {
                                this.world.setBlockState(new BlockPos((int) (this.posX + v), (int) (this.posY + ty), (int) (this.posZ + w)),
                                        this.rand.nextInt(20) == 0 ? TakumiBlockCore.DUMMY_GUNORE.getDefaultState() :
                                        TakumiBlockCore.GUNORE.getDefaultState());
                            }
                        }
                    }
                }
                break;
            //回復爆発
            case 10:
                if (!world.isRemote) { this.world.createExplosion(this, this.posX, this.posY, this.posZ, power, true); }
                this.heal(25);
                break;
            //多重爆発
            case 11:
                if (!this.world.isRemote) {
                    for (int t = 0; t < power; t++) {
                        this.world.createExplosion(this, this.posX, this.posY, this.posZ, power / 1.5f, true);
                    }
                }
                break;
            //全方位火球
            case 12:
                Entity var1 = this.getAttackTarget();
                if (var1 != null) {
                    for (int t = -18; t < 18; t++) {
    
                        float f1 = MathHelper.sqrt(this.getDistanceToEntity(var1)) * 0.5F;
                        double d0 = var1.posX - this.posX;
                        double d1 = var1.getEntityBoundingBox().minY + var1.height / 2.0F - (this.posY + this.height / 2.0F);
                        double d2 = var1.posZ - this.posZ;
                        EntityLargeFireball entityLargefireball = new EntityLargeFireball(this.world, this, d0 + this.rand.nextGaussian() * f1, d1,
                                d2 + this.rand.nextGaussian() * f1);
                        entityLargefireball.rotationYaw += t * 10;
                        entityLargefireball.posY = this.posY + this.height / 2.0F + 0.5D;
                        entityLargefireball.explosionPower = 2;
    
                        this.world.spawnEntity(entityLargefireball);
                    }
                } else if (!this.world.isRemote) {
                    this.world.createExplosion(this, this.posX, this.posY, this.posZ, power / 2.5f, true);
                }
                break;
            //匠化爆発
            case 13:
                //煙幕
            case 14:
            default:
                if (!this.world.isRemote) {
                    this.world.createExplosion(this, this.posX, this.posY, this.posZ, power, true);
                }
                break;
        }
        if (this.getHealth() > this.getMaxHealth() / 2 && !this.world.isThundering()) {
            TakumiUtils.takumiSetPowered(this, false);
        }
    }
    
    private void projectileCounter() {
        for (int i = 0; i < 10 * (this.getPowered() ? 3 : 1); i++) {
            BlockPos pos = this.createRandomPos(this.lastSource.getTrueSource().getPosition(), 2);
            EntityLightningBolt bolt = new EntityLightningBolt(this.world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false);
            this.world.addWeatherEffect(bolt);
            this.world.spawnEntity(bolt);
        }
        if (!this.world.isRemote) {
            this.world.createExplosion(this, this.lastSource.getTrueSource().posX, this.lastSource.getTrueSource().posY - 0.25, this.lastSource
                    .getTrueSource().posZ, 3f, true);
        }
        this.onStruckByLightning(null);
    }
    
    public int getAttackID() {
        return this.dataManager.get(ATTACK_ID);
    }
    
    private BlockPos createRandomPos(BlockPos point, double range) {
        return point.add(MathHelper.nextDouble(this.rand, -1 * range * (this.getPowered() ? 2 : 1), range * (this.getPowered() ? 2 :
                                                                                                             1)), MathHelper.nextDouble(this.rand,
                -1 * range * (
                this.getPowered() ? 2 : 1), range * (this.getPowered() ? 2 : 1)), MathHelper.nextDouble(this.rand, -1 * range * (this.getPowered() ?
                                                                                                                                 2 : 1), range * (
                this.getPowered() ? 2 : 1)));
    }
    
    public void setAttackID(int id) {
        this.dataManager.set(ATTACK_ID, id);
    }
    
    @Override
    public EnumTakumiRank takumiRank() {
        return EnumTakumiRank.BOSS;
    }
    
    @Override
    public EnumTakumiType takumiType() {
        return EnumTakumiType.NORMAL_MD;
    }
    
    @Override
    public int getExplosionPower() {
        return 0;
    }
    
    @Override
    public int getSecondaryColor() {
        return 0x88ff00;
    }
    
    @Override
    public boolean isCustomSpawn() {
        return true;
    }
    
    @Override
    public String getRegisterName() {
        return "kingcreeper";
    }
    
    @Override
    public int getRegisterID() {
        return 501;
    }
    
    @Override
    public int getPrimaryColor() {
        return 0x00ff00;
    }
    
    @Override
    public ResourceLocation getArmor() {
        return new ResourceLocation(TakumiCraftCore.MODID, "textures/entity/king_creeper_armor.png");
    }
    
    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        if (damageSrc == DamageSource.OUT_OF_WORLD || damageSrc.getTrueSource() instanceof EntityPlayer) {
            this.lastSource = damageSrc;
            if (damageSrc.isProjectile() && damageAmount > 2.5f) {
                damageAmount = 2.5f;
            } else if (damageAmount > 20) {
                damageAmount = 20 + (damageAmount - 20) / 10;
            }
            if (damageSrc.getTrueSource() instanceof EntityLivingBase) {
                this.setAttackTarget((EntityLivingBase) damageSrc.getTrueSource());
            }
            this.ignite();
            super.damageEntity(damageSrc, damageAmount);
        }
    }
    
    @Override
    protected void outOfWorld() {
        this.setHealth(0);
        super.outOfWorld();
    }
    
    
    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        this.bossInfo.setName(this.getDisplayName());
    }
    
    
    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.bossInfo.removePlayer(player);
    }
    
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossInfo.addPlayer(player);
    }
    
    @Override
    public void setDead() {
        if (!(this.getHealth() <= 0 || this.world.getDifficulty() == EnumDifficulty.PEACEFUL)) {
            if (!this.world.isRemote) {
                EntityKingCreeper kingCreeper = new EntityKingCreeper(this.world);
                NBTTagCompound tagCompound = new NBTTagCompound();
                this.writeEntityToNBT(tagCompound);
                tagCompound.setBoolean("ignited", false);
                kingCreeper.readEntityFromNBT(tagCompound);
                kingCreeper.setHealth(this.getHealth());
                kingCreeper.copyLocationAndAnglesFrom(this);
                if (this.getPowered()) {
                    TakumiUtils.takumiSetPowered(kingCreeper, true);
                }
                kingCreeper.setCreeperState(-1);
                kingCreeper.setAttackTarget(null);
                kingCreeper.setAttackID(this.getAttackID());
                this.world.spawnEntity(kingCreeper);
            }
        }
        super.setDead();
    }
    
    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }
    
    @Override
    protected boolean canDespawn() {
        return false;
    }
    
    @Override
    protected void despawnEntity() {
        Result result; if ((this.idleTime & 0x1F) == 0x1F && (result = ForgeEventFactory.canEntityDespawn(this)) != Result.DEFAULT) {
            if (result == Result.DENY) {
                this.idleTime = 0;
            } else {
                this.setHealth(0);
                this.setDead();
            }
        } else {
            Entity entity = this.world.getClosestPlayerToEntity(this, -1.0D);
            
            if (entity != null) {
                double d0 = entity.posX - this.posX;
                double d1 = entity.posY - this.posY;
                double d2 = entity.posZ - this.posZ;
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                
                if (this.canDespawn() && d3 > 16384.0D) {
                    this.setHealth(0);
                    this.setDead();
                }
                
                if (this.idleTime > 600 && this.rand.nextInt(800) == 0 && d3 > 1024.0D && this.canDespawn()) {
                    this.setHealth(0);
                    this.setDead();
                } else if (d3 < 1024.0D) {
                    this.idleTime = 0;
                }
            }
        }
    }
    
    @Override
    public boolean isNonBoss() {
        return false;
    }
    
    @Override
    public float getExplosionResistance(Explosion explosionIn, World worldIn, BlockPos pos, IBlockState blockStateIn) {
        return blockStateIn.getBlockHardness(worldIn, pos) == -1 ? 10000000f : 0.75f;
    }
}