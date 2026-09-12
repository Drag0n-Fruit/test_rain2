package com.raincraft.client;

import com.raincraft.cycle.CycleState;
import com.raincraft.network.CycleNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Random;

public class RaincraftClient implements ClientModInitializer {

    private final Random random = new Random();
    private int soundCooldown = 0;

    @Override
    public void onInitializeClient() {
        // 서버 -> 클라 상태 동기화 패킷 수신
        ClientPlayNetworking.registerGlobalReceiver(CycleNetworking.CYCLE_SYNC_CHANNEL, (client, handler, buf, responseSender) -> {
            CycleState state = buf.readEnumConstant(CycleState.class);
            int ticksUntilRain = buf.readInt();
            client.execute(() -> ClientCycleData.update(state, ticksUntilRain));
        });

        // 매 클라이언트 틱마다 현재 상태에 맞는 연출 처리
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // 시계 아이템에 마우스를 올리면 다음 비까지 남은 시간을 툴팁으로 표시
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.isOf(Items.CLOCK)) {
                int ticks = ClientCycleData.getTicksUntilRain();
                int seconds = ticks / 20;
                Text line = switch (ClientCycleData.getState()) {
                    case DAY -> Text.literal("다음 비까지: " + (seconds / 60) + "분 " + (seconds % 60) + "초");
                    case WARNING -> Text.literal("하늘이 심상치 않다... 곧 비가 온다").formatted(net.minecraft.util.Formatting.GOLD);
                    case LIGHT_RAIN -> Text.literal("빗방울이 떨어지기 시작했다").formatted(net.minecraft.util.Formatting.BLUE);
                    case HEAVY_RAIN -> Text.literal("폭우! 당장 쉘터로!").formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD);
                    case HIBERNATION, RECOVERY -> Text.literal("동면 중...").formatted(net.minecraft.util.Formatting.GRAY);
                };
                lines.add(line);
            }
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        CycleState state = ClientCycleData.getState();

        switch (state) {
            case WARNING -> {
                // 하늘이 어두워지는 연출은 실제로는 셰이더/포그 조작이 필요.
                // 여기서는 가벼운 힌트로 가끔 천둥 소리만 살짝 재생 (거리감 있게)
                if (random.nextInt(400) == 0) {
                    client.player.playSound(SoundEvents.WEATHER_RAIN_ABOVE, 0.3f, 0.5f);
                }
            }
            case LIGHT_RAIN -> {
                spawnRainParticles(client, 2);
                if (soundCooldown-- <= 0) {
                    client.player.playSound(SoundEvents.WEATHER_RAIN, 0.4f, 1.0f);
                    soundCooldown = 60;
                }
            }
            case HEAVY_RAIN -> {
                spawnRainParticles(client, 8);
                if (soundCooldown-- <= 0) {
                    client.player.playSound(SoundEvents.WEATHER_RAIN, 1.0f, 0.8f);
                    soundCooldown = 20;
                }
                // 실제 "화면이 흔들릴 정도"의 카메라 쉐이크는
                // mixin.CameraShakeMixin 에서 Camera#update()를 후킹해서 처리한다.
                // (ClientCycleData.isHeavyRain() 을 그 mixin에서 읽어감)
            }
            default -> { /* DAY, HIBERNATION, RECOVERY는 별도 연출 없음 */ }
        }
    }

    private void spawnRainParticles(MinecraftClient client, int count) {
        if (client.world == null || client.player == null) return;
        double px = client.player.getX();
        double py = client.player.getY() + 15; // 머리 위 높은 곳에서 떨어지는 느낌
        double pz = client.player.getZ();

        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5) * 20;
            double oz = (random.nextDouble() - 0.5) * 20;
            client.world.addParticle(ParticleTypes.SPLASH,
                    px + ox, py, pz + oz,
                    0, -1.2, 0);
        }
    }
}
