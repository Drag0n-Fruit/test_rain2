package com.raincraft.cycle;

/**
 * 레인월드 사이클의 전체 상태.
 * 서버가 이 상태를 관리하고, 클라이언트는 패킷으로 전달받아 렌더링/사운드만 처리한다.
 */
public enum CycleState {
    DAY,          // 평상시 - 자유롭게 활동
    WARNING,      // 비 징조 - 하늘이 어두워짐, 시계로 확인 가능
    LIGHT_RAIN,   // 빗방울이 떨어지기 시작 (약한 파티클)
    HEAVY_RAIN,   // 본격적인 비 - 화면 흔들림, 지하 익사, 지상 붕괴, 사망 시 아이템 삭제
    HIBERNATION,  // 전원 쉘터 입장 완료 - 시간 스킵 중
    RECOVERY;     // 다음 날 - 파괴된 블록 복구 처리 중

    public boolean isDangerous() {
        return this == HEAVY_RAIN;
    }

    public boolean isRainAtAll() {
        return this == LIGHT_RAIN || this == HEAVY_RAIN;
    }
}
