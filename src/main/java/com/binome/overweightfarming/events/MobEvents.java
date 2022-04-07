package com.binome.overweightfarming.events;

import com.binome.overweightfarming.OverweightFarming;
import com.binome.overweightfarming.init.OFItems;
import com.binome.overweightfarming.util.OFItemsForEmeralds;
import com.binome.overweightfarming.util.OvergrowthHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = OverweightFarming.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobEvents {

    @SubscribeEvent
    public void onVillagerTradesInit(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();
        if (profession == VillagerProfession.FARMER) {
            VillagerTrades.ItemListing hatTrade = new OFItemsForEmeralds(OFItems.STRAW_HAT.get(), 20, 1, 4);
            Int2ObjectMap<List<VillagerTrades.ItemListing>> map = event.getTrades();
            List<VillagerTrades.ItemListing> list = event.getTrades().get(5);
            list.add(hatTrade);
            map.put(5, list);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        Level world = entity.level;
        if (!world.isClientSide()) {
            if (entity.getItemBySlot(EquipmentSlot.HEAD).is(OFItems.STRAW_HAT.get())) {
                int radius = 40;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -1; y <= 1; y++) {
                            BlockPos entityPosition = entity.blockPosition();
                            BlockPos cropPos = new BlockPos(entityPosition.getX() + x, entityPosition.getY() + y, entityPosition.getZ() + z);
                            BlockState state = world.getBlockState(cropPos);
                            if (state.is(BlockTags.CROPS)) {
                                Block block = state.getBlock();
                                float v = world.getRandom().nextFloat();
                                boolean flag = v < 1.6540289E-4 && world.getRandom().nextBoolean();
                                boolean validForOverweight = false;
                                if (world instanceof ServerLevel serverLevel) {
                                    if (flag) {
                                        if (state.hasProperty(CropBlock.AGE)) {
                                            if (block instanceof CropBlock crop) {
                                                int age = state.getValue(CropBlock.AGE);
                                                if (age < crop.getMaxAge()) {
                                                    crop.growCrops(serverLevel, cropPos, state);
                                                }
                                                if (age == crop.getMaxAge())
                                                    validForOverweight = true;
                                            } else if (block instanceof StemBlock) {
                                                int age = state.getValue(StemBlock.AGE);
                                                if (age < StemBlock.MAX_AGE) {
                                                    serverLevel.setBlock(cropPos, state.setValue(StemBlock.AGE, age + 1), 2);
                                                }
                                            }
                                        }
                                        if (state.hasProperty(BeetrootBlock.AGE) && block instanceof BeetrootBlock beetrootBlock) {
                                            int age = state.getValue(BeetrootBlock.AGE);
                                            if (age < beetrootBlock.getMaxAge()) {
                                                beetrootBlock.growCrops(serverLevel, cropPos, state);
                                            }
                                            if (age == beetrootBlock.getMaxAge())
                                                validForOverweight = true;
                                        }
                                        if (validForOverweight) {
                                            for (Block overgrowth : OvergrowthHandler.CROPS_TO_OVERGROWN.keySet()) {
                                                if (state.is(overgrowth)) {
                                                    OvergrowthHandler.overweightGrowth(serverLevel.getRandom(), state, serverLevel, cropPos, overgrowth);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        AgeableMob mob = event.getChild();
        Mob mobA = event.getParentA();
        Mob mobB = event.getParentB();
        Player player = event.getCausedByPlayer();
        if (player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(OFItems.STRAW_HAT.get()) && mob instanceof Animal) {
            Level world = mob.getLevel();
            if (world instanceof ServerLevel level && mobA instanceof Animal animalParentA && mobB instanceof Animal animalParentB){
                Random random = world.getRandom();
                int tries = 0;
                if (random.nextInt(3) == 0) {
                    tries++;
                    if (random.nextInt(12) == 0) {
                        tries++;
                    }
                }
                for (int i = 0; i < tries; i++) {
                    AgeableMob newChild = animalParentA.getBreedOffspring(level, animalParentB);
                    if (newChild != null) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.awardStat(Stats.ANIMALS_BRED);
                            CriteriaTriggers.BRED_ANIMALS.trigger(serverPlayer, animalParentA, animalParentB, newChild);
                        }

                        newChild.setBaby(true);
                        newChild.moveTo(animalParentA.getX(), animalParentA.getY(), animalParentA.getZ(), 0.0F, 0.0F);
                        level.addFreshEntityWithPassengers(newChild);
                    }
                }
            }
        }
    }

}
