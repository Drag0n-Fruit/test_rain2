package com.raincraft.mixin;

import com.raincraft.client.ClientCycleData;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * 폭우(HEAVY_RAIN) 상태일 때 카메라 위치에 미세한 랜덤 오프셋을 줘서
 * "화면이 흔들리는" 효과를 낸다.
 *
 * 주의: Camera#update 의 정확한 시그니처는 마인크래프트/맵핑 버전에 따라
 * 파라미터가 다를 수 있으니, 실제 빌드 시 Yarn 맵핑을 확인해서 맞춰야 한다.
 * 여기서는 update 종료 시점(TAIL)에 setPos로 흔들림을 덮어쓰는 방식.
 */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    public abstract Vec3d getPos();

    @Shadow
    protected abstract void setPos(Vec3d pos);

    private final Random raincraft$random = new Random();

    @Inject(method = "update*", at = @At("TAIL"))
    private void raincraft$shake(CallbackInfo ci) {
        if (!ClientCycleData.isHeavyRain()) return;

        double strength = 0.05; // 흔들림 세기 - 취향껏 조절
        Vec3d pos = getPos();
        Vec3d shaken = pos.add(
                (raincraft$random.nextDouble() - 0.5) * strength,
                (raincraft$random.nextDouble() - 0.5) * strength,
                (raincraft$random.nextDouble() - 0.5) * strength
        );
        setPos(shaken);
    }
}
