package com.raincraft.cycle;

import com.raincraft.shelter.ShelterManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 폭우 중 "하늘이 안 보이는 곳(지하)"에 있는 플레이어는 물에 잠긴 것으로 간주해
 * 익사 데미지를 계속 받는다. 쉘터 안에 있으면 예외.
 *
 * 실제로 물 블록을 채우지 않고 판정만 하는 이유는 성능 때문 (청크 단위로 물을 채우고
 * 되돌리는 건 멀티플레이 서버에서 랙의 주범이 되기 쉽다). 대신 클라이언트에서
 * 화면에 파란 오버레이/기포 파티클을 씌워서 "물에 잠긴 느낌"만 연출하면 충분하다.
 */
public class DrowningHazard {

    private static final int DAMAGE_INTERVAL_TICKS = 20; // 1초마다 데미지
    private static final float DAMAGE_AMOUNT = 2.0f;      // 하트 1개

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % DAMAGE_INTERVAL_TICKS != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (ShelterManager.isSheltered(player)) continue;

            BlockPos pos = player.getBlockPos();
            boolean underground = !player.getWorld().isSkyVisible(pos);

            if (underground) {
                player.setAir(Math.max(0, player.getAir() - DAMAGE_INTERVAL_TICKS));
                player.damage(
                        player.getDamageSources().drown(),
                        DAMAGE_AMOUNT
                );
            }
        }
    }
}
