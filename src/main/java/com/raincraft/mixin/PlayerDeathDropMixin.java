package com.raincraft.mixin;

import com.raincraft.cycle.CycleManager;
import com.raincraft.cycle.CycleState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerEntity.dropInventory() 호출 직전에, 현재 사이클이 HEAVY_RAIN이면
 * 인벤토리를 통째로 비워버린다. dropInventory()는 그 다음에 실행되지만
 * 이미 비어있으므로 아무것도 드롭되지 않는다 -> "죽으면 아이템 삭제" 요구사항 충족.
 *
 * 평상시 사망(비가 안 올 때)은 정상적으로 드롭된다.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDeathDropMixin {

    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void raincraft$clearOnRainDeath(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld serverWorld) {
            CycleState state = CycleManager.get(serverWorld.getServer().getOverworld()).getState();
            if (state == CycleState.HEAVY_RAIN) {
                self.getInventory().clear();
            }
        }
    }
}
