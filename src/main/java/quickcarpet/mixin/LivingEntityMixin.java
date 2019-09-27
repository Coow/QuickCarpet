package quickcarpet.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quickcarpet.annotation.Feature;
import quickcarpet.settings.Settings;

@Feature("mobInFireConvertsSandToSoulsand")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;", shift = At.Shift.BEFORE))
    private void convertSandToSoulsand(DamageSource source, CallbackInfo ci) {
        if (!Settings.mobInFireConvertsSandToSoulsand) return;

        BlockPos pos = new BlockPos(this.x, this.y, this.z);
        BlockState statePos = this.world.getBlockState(pos);

        BlockPos below = pos.method_10074(); // method_10074 = down
        BlockState stateBelow = this.world.getBlockState(below);

        if (statePos.getBlock() == Blocks.FIRE && stateBelow.getBlock().matches(BlockTags.SAND)) {
            this.world.setBlockState(below, Blocks.SOUL_SAND.getDefaultState());
        }
    }
}
