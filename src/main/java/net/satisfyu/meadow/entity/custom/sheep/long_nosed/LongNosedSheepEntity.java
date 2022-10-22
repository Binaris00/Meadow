package net.satisfyu.meadow.entity.custom.sheep.long_nosed;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.satisfyu.meadow.Meadow;
import net.satisfyu.meadow.block.ModBlocks;
import net.satisfyu.meadow.entity.custom.sheep.MeadowSheepEntity;

public class LongNosedSheepEntity extends MeadowSheepEntity {
    public LongNosedSheepEntity(EntityType<? extends SheepEntity> entityType, World world) {
        super(entityType, world, ModBlocks.HIGHLAND_WOOL, new Identifier(Meadow.MOD_ID, "entities/sheep/long_nosed"));
    }
}
