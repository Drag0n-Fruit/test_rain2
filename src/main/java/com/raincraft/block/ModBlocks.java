package com.raincraft.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 쉘터 자연 생성 구조물 내부에만 등장하는 판정용 블록.
 * 일부러 BlockItem을 등록하지 않는다 -> 플레이어가 캐거나 인벤토리에 넣거나
 * 손으로 설치할 방법이 없다. 오직 월드젠으로만 배치됨.
 * 베드락처럼 파괴 불가능하게 만들어서 쉘터를 부수고 도망 못 가게 함.
 */
public class ModBlocks {

    public static final Block SHELTER_BEACON = new Block(
            AbstractBlock.Settings.create()
                    .strength(-1.0f, 3600000.0f) // 베드락과 동일한 강도 (파괴 불가)
                    .luminance(state -> 6)        // 은은한 빛 - 쉘터 안이 안전하다는 시각적 신호
                    .nonOpaque()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier("raincraft", "shelter_beacon"), SHELTER_BEACON);
        // 의도적으로 BlockItem 등록 없음 - 자연 생성 전용 블록
    }
}
