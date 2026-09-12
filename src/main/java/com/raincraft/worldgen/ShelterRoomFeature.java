package com.raincraft.worldgen;

import com.raincraft.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * 자연 생성 쉘터 구조물.
 *
 * 구조: 중심(center)을 기준으로
 *  - dy = -1 : 바닥 (베드락)
 *  - dy =  0,1,2 : 내부 공간 (3x3, 공기)
 *  - dy =  3 : 천장 (베드락)
 *  - dx, dz = -2..2 중 가장자리(-2, 2)는 벽 (베드락)
 *  - 벽 하나의 중앙에 폭1 높이2 출입구를 뚫어서 드나들 수 있게 함
 *  - 내부 바닥 중앙에 ModBlocks.SHELTER_BEACON 배치 (쉘터 판정용, 채굴 불가)
 *
 * 실제 배치 위치(Y, 희소성)는 placed_feature json의 placement modifier들이 결정하고,
 * 여기서는 그 origin을 기준으로 방 모양만 깎아낸다.
 */
public class ShelterRoomFeature extends Feature<DefaultFeatureConfig> {

    private static final int RADIUS = 2; // 벽 포함 5x5 (내부는 3x3)

    public ShelterRoomFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos center = context.getOrigin();

        // 월드 바닥 근처면 취소 (베드락 레이어와 겹치면 안 예쁨)
        if (center.getY() < world.getBottomY() + 10) return false;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    boolean isWall = dx == -RADIUS || dx == RADIUS
                            || dz == -RADIUS || dz == RADIUS
                            || dy == -1 || dy == 3;
                    BlockPos pos = center.add(dx, dy, dz);
                    world.setBlockState(pos,
                            isWall ? Blocks.BEDROCK.getDefaultState() : Blocks.AIR.getDefaultState(),
                            3);
                }
            }
        }

        // 출입구: 네 벽 중 하나를 골라 폭1 높이2로 뚫는다 (안쪽 벽 + 바깥쪽 한 칸 더)
        Direction entranceDir = Direction.Type.HORIZONTAL.random(random);
        BlockPos wallCenter = center.offset(entranceDir, RADIUS);
        for (int dy = 0; dy <= 1; dy++) {
            world.setBlockState(wallCenter.up(dy), Blocks.AIR.getDefaultState(), 3);
            world.setBlockState(wallCenter.offset(entranceDir).up(dy), Blocks.AIR.getDefaultState(), 3);
        }

        // 쉘터 판정용 비콘 (내부 바닥 정중앙)
        world.setBlockState(center, ModBlocks.SHELTER_BEACON.getDefaultState(), 3);

        return true;
    }
}
