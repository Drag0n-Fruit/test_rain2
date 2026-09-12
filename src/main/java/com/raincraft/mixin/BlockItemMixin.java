package com.raincraft.mixin;

import com.raincraft.block.PlacedBlockTracker;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BlockItem.place() 가 성공적으로 끝난 시점을 가로채서
 * "플레이어가 설치한 블록"으로 기록한다. 이렇게 기록된 블록만 폭우 붕괴 대상이 된다
 * (자연 생성 지형은 붕괴하지 않음).
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void raincraft$onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue().isAccepted() && context.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = context.getBlockPos();
            PlacedBlockTracker.mark(serverWorld, pos);
        }
    }
}
