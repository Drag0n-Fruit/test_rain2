package com.raincraft.client;

import com.raincraft.cycle.CycleState;

/**
 * 서버에서 받은 사이클 상태를 클라이언트에서 들고 있는 static 홀더.
 * 렌더링/사운드/HUD 코드는 전부 여기서 현재 상태를 읽어간다.
 */
public class ClientCycleData {
    private static volatile CycleState state = CycleState.DAY;
    private static volatile int ticksUntilRain = 0;

    public static CycleState getState() {
        return state;
    }

    public static int getTicksUntilRain() {
        return ticksUntilRain;
    }

    public static void update(CycleState newState, int newTicksUntilRain) {
        state = newState;
        ticksUntilRain = newTicksUntilRain;
    }

    public static boolean isHeavyRain() {
        return state == CycleState.HEAVY_RAIN;
    }
}
