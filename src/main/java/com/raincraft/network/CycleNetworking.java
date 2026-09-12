package com.raincraft.network;

import com.raincraft.cycle.CycleState;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import io.netty.buffer.Unpooled;

/**
 * 서버의 CycleState 변화를 모든 클라이언트에 방송한다.
 * 클라이언트 쪽 수신 처리는 client.RaincraftClient 에서 등록.
 */
public class CycleNetworking {

    public static final Identifier CYCLE_SYNC_CHANNEL =
            new Identifier("raincraft", "cycle_sync");

    public static void broadcastState(MinecraftServer server, CycleState state, int ticksUntilRain) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(state);
        buf.writeInt(ticksUntilRain);

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, CYCLE_SYNC_CHANNEL, buf);
        }
    }

    /** 플레이어가 새로 접속했을 때 현재 상태를 즉시 알려주기 위한 개별 전송 */
    public static void sendStateTo(ServerPlayerEntity player, CycleState state, int ticksUntilRain) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(state);
        buf.writeInt(ticksUntilRain);
        ServerPlayNetworking.send(player, CYCLE_SYNC_CHANNEL, buf);
    }
}
