package com.raincraft;

import com.raincraft.block.ModBlocks;
import com.raincraft.block.PlacedBlockTracker;
import com.raincraft.cycle.CycleManager;
import com.raincraft.network.CycleNetworking;
import com.raincraft.worldgen.ModFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;

public class RaincraftMod implements ModInitializer {

    @Override
    public void onInitialize() {

        ModBlocks.register();
        ModFeatures.register();

        // 오버월드 전체 생물군계에 쉘터 방 자연 생성 추가
        // (실제 확률/높이는 data/raincraft/worldgen/placed_feature/shelter_room.json 에서 조절)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_STRUCTURES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("raincraft", "shelter_room"))
        );

        // 매 서버 틱마다 사이클 상태머신 진행
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CycleManager.get(server.getOverworld()).tick(server);
        });

        // 플레이어가 블록을 부수면 트래킹 목록에서 제거 (더 이상 붕괴/복구 대상이 아님)
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                PlacedBlockTracker.unmark(serverWorld, pos.toImmutable());
            }
        });

        // 새로 접속한 플레이어에게 현재 사이클 상태를 즉시 전송 (안 그러면 접속 전에 상태가 바뀐 경우 클라가 모름)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CycleManager manager = CycleManager.get(server.getOverworld());
            CycleNetworking.sendStateTo(handler.getPlayer(), manager.getState(), manager.getTicksUntilRain());
        });
    }
}
