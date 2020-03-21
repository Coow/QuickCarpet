package quickcarpet.mixin.autoCraftingTable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import quickcarpet.annotation.Feature;
import quickcarpet.feature.CraftingTableBlockEntity;
import quickcarpet.utils.extensions.DynamicBlockEntityProvider;

import javax.annotation.Nullable;

import static quickcarpet.settings.Settings.autoCraftingTable;

@Feature("autoCraftingTable")
@Mixin(CraftingTableBlock.class)
public class CraftingTableBlockMixin extends Block implements DynamicBlockEntityProvider {
    protected CraftingTableBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public boolean providesBlockEntity() {
        return autoCraftingTable;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockView var1) {
        return new CraftingTableBlockEntity();
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onActivate(BlockState blockState, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!hasBlockEntity()) return;
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CraftingTableBlockEntity) {
                player.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
        cir.cancel();
    }

    public boolean hasComparatorOutput(BlockState blockState) {
        return hasBlockEntity();
    }

    @Override
    public int getComparatorOutput(BlockState blockState, World world, BlockPos pos) {
        if (!hasBlockEntity()) return 0;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CraftingTableBlockEntity) {
            CraftingTableBlockEntity craftingTableBlockEntity = (CraftingTableBlockEntity) blockEntity;
            int filled = 0;
            for (ItemStack stack : craftingTableBlockEntity.inventory) {
                if (!stack.isEmpty()) filled++;
            }
            return (filled * 15) / 9;
        }
        return 0;
    }

    @Override
    public void onBlockRemoved(BlockState state1, World world, BlockPos pos, BlockState state2, boolean boolean_1) {
        if (state1.getBlock() != state2.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CraftingTableBlockEntity) {
                CraftingTableBlockEntity craftingTableBlockEntity = ((CraftingTableBlockEntity)blockEntity);
                ItemScatterer.spawn(world, pos, craftingTableBlockEntity.inventory);
                if (!craftingTableBlockEntity.output.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), craftingTableBlockEntity.output);
                }
                world.updateComparators(pos, this);
            }

            super.onBlockRemoved(state1, world, pos, state2, boolean_1);
        }
    }
}
