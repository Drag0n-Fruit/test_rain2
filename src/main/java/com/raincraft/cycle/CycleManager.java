package com.raincraft.cycle;

import com.raincraft.block.CollapseManager;
import com.raincraft.network.CycleNetworking;
import com.raincraft.shelter.ShelterManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/**
 * 월드 전체의 사이클(낮 -> 경고 -> 약한비 -> 폭우 -> 동면 -> 복구)을 관리한다.
 * 반드시 오버월드 하나에만 붙여서 서버 전역 상태로 취급한다 (멀티 디멘션에서도 비는 하나의 사이클로 통일).
 */
public class CycleManager extends PersistentState {

    // ==== 타이밍 설정 (틱 단위, 20틱 = 1초) ====
    // 필요에 맞게 조절하세요. 지금은 대략 25분짜리 사이클로 잡았습니다.
    public static final int WARNING_START   = 18000; // 15분 - 하늘이 어두워지기 시작
    public static final int LIGHT_RAIN_START = 21000; // 17.5분 - 빗방울 시작
    public static final int HEAVY_RAIN_START = 22500; // 18.75분 - 본격 폭우
    public static final int CYCLE_LENGTH    = 24000; // 20분 - 이 시점까지 아무도 대피 못하면 강제 폭우 지속

    private CycleState state = CycleState.DAY;
    private int timer = 0;          // DAY~HEAVY_RAIN 동안 흐르는 타이머
    private int recoveryTicks = 0;  // RECOVERY 상태에서 복구 진행 중 흐르는 타이머

    public CycleState getState() {
        return state;
    }

    public int getTicksUntilRain() {
        return Math.max(0, HEAVY_RAIN_START - timer);
    }

    /** 서버 매 틱 호출. main mod class의 ServerTickEvents.END_SERVER_TICK 에서 연결. */
    public void tick(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        CycleState prev = state;

        switch (state) {
            case DAY -> {
                timer++;
                if (timer >= WARNING_START) {
                    state = CycleState.WARNING;
                }
            }
            case WARNING -> {
                timer++;
                if (timer >= LIGHT_RAIN_START) {
                    state = CycleState.LIGHT_RAIN;
                }
            }
            case LIGHT_RAIN -> {
                timer++;
                if (timer >= HEAVY_RAIN_START) {
                    state = CycleState.HEAVY_RAIN;
                    // 폭우 시작 시점 스냅샷: 이 시점 이후 붕괴 대상이 되는 "플레이어 설치 블록"들을 큐에 등록
                    CollapseManager.get(overworld).onHeavyRainStart(overworld);
                }
            }
            case HEAVY_RAIN -> {
                timer++;
                // 붕괴는 매틱 한번에 몰아서 하지 않고 큐에서 조금씩 처리 (랙 방지)
                CollapseManager.get(overworld).tickCollapse(overworld);
                // 지하(하늘이 안 보이는 곳)에 있으면 익사 판정
                DrowningHazard.tick(server);

                // 전원이 쉘터에 들어갔는지 체크 (20틱 = 1초마다만 검사해서 부하 감소)
                if (server.getTicks() % 20 == 0 && ShelterManager.everyoneSheltered(server)) {
                    state = CycleState.HIBERNATION;
                }

                // 아무도 대피 못한 채 사이클이 너무 길어지면 강제로 다음날로 (무한 폭우 방지, 선택사항)
                if (timer >= CYCLE_LENGTH) {
                    state = CycleState.HIBERNATION;
                }
            }
            case HIBERNATION -> {
                // 시간을 낮으로 스킵시키고 바로 복구 단계로
                overworld.setTimeOfDay(0);
                state = CycleState.RECOVERY;
                recoveryTicks = 0;
                CollapseManager.get(overworld).beginRestore(overworld);
            }
            case RECOVERY -> {
                boolean done = CollapseManager.get(overworld).tickRestore(overworld);
                recoveryTicks++;
                if (done || recoveryTicks > 600) { // 최대 30초 안에 강제 종료
                    state = CycleState.DAY;
                    timer = 0;
                }
            }
        }

        if (prev != state) {
            markDirty();
            CycleNetworking.broadcastState(server, state, getTicksUntilRain());
        }
    }

    // ==== PersistentState 저장/로드 ====

    private static final String KEY = "raincraft_cycle";

    public static CycleManager get(ServerWorld overworld) {
        PersistentStateManager manager = overworld.getPersistentStateManager();
        return manager.getOrCreate(CycleManager::fromNbt, CycleManager::new, KEY);
    }

    public static CycleManager fromNbt(NbtCompound tag) {
        CycleManager m = new CycleManager();
        m.state = CycleState.valueOf(tag.getString("state"));
        m.timer = tag.getInt("timer");
        m.recoveryTicks = tag.getInt("recoveryTicks");
        return m;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag.putString("state", state.name());
        tag.putInt("timer", timer);
        tag.putInt("recoveryTicks", recoveryTicks);
        return tag;
    }
}
