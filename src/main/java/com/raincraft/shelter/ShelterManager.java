package com.raincraft.shelter;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 쉘터 판정 로직.
 * "data/raincraft/tags/blocks/shelter_marker.json" 태그가 붙은 블록 주변 SHELTER_RADIUS
 * 범위 안에 플레이어가 있으면 대피한 것으로 인정한다.
 *
 * 지금은 단순 근접 판정이지만, 나중에 "머리 위가 막혀있는지"(밀폐 여부)까지
 * 검사하도록 강화할 수 있다 (예: 위쪽 몇 블록이 공기가 아니어야 함).
 */
public class ShelterManager {

    public static final TagKey<Block> SHELTER_MARKER =
            TagKey.of(RegistryKeys.BLOCK, new Identifier("raincraft", "shelter_marker"));

    private static final int SHELTER_RADIUS = 5;

    public static boolean everyoneSheltered(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) return false; // 접속자가 없으면 스킵 조건 자체가 성립 안 함

        for (ServerPlayerEntity player : players) {
            if (player.isSpectator()) continue; // 관전자는 판정에서 제외
            if (!isSheltered(player)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSheltered(ServerPlayerEntity player) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();

        for (BlockPos pos : BlockPos.iterate(
                center.add(-SHELTER_RADIUS, -SHELTER_RADIUS, -SHELTER_RADIUS),
                center.add(SHELTER_RADIUS, SHELTER_RADIUS, SHELTER_RADIUS))) {
            if (world.getBlockState(pos).isIn(SHELTER_MARKER)) {
                return true;
            }
        }
        return false;
    }
}
