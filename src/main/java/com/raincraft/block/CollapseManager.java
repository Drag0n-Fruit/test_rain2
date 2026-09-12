package com.raincraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.*;

/**
 * 폭우 중 "지상 + 플레이어가 설치한 블록"을 붕괴시키고, 다음날 복구하는 역할.
 *
 * 흐름:
 *  1. 평상시: markPlaced() / unmark() 로 트래킹 세트를 계속 최신 상태로 유지 (BlockItemMixin, break 이벤트에서 호출)
 *  2. HEAVY_RAIN 진입 시: onHeavyRainStart() 호출 -> 지상에 노출된 블록 중 일부를 확률적으로 붕괴 큐에 등록,
 *     붕괴 전 BlockState를 backup 맵에 저장
 *  3. HEAVY_RAIN 동안: tickCollapse() 를 매틱 호출 -> 큐에서 조금씩(BATCH_SIZE) 꺼내 실제로 파괴
 *  4. HIBERNATION -> RECOVERY 진입 시: beginRestore() 호출 -> backup 맵의 키들을 복구 큐에 적재
 *  5. RECOVERY 동안: tickRestore() 매틱 호출 -> 큐에서 조금씩 꺼내 원래 블록으로 되돌림
 */
public class CollapseManager extends PersistentState {

    private static final int BATCH_SIZE = 30;      // 틱당 처리량 (랙 방지)
    private static final float COLLAPSE_CHANCE = 0.5f; // 대상 블록 중 붕괴할 확률

    // 현재 존재하는, 플레이어가 설치한 블록 좌표 전체
    private final Set<BlockPos> tracked = new HashSet<>();

    // 이번 사이클에 파괴된 블록의 "복구용" 원본 상태 백업
    private final Map<BlockPos, BlockState> backup = new HashMap<>();

    private final Deque<BlockPos> collapseQueue = new ArrayDeque<>();
    private final Deque<BlockPos> restoreQueue = new ArrayDeque<>();

    // ===== 트래킹 (설치/파괴) =====

    public void markPlaced(BlockPos pos) {
        tracked.add(pos.toImmutable());
        markDirty();
    }

    public void unmark(BlockPos pos) {
        tracked.remove(pos);
        backup.remove(pos);
        markDirty();
    }

    // ===== 붕괴 =====

    public void onHeavyRainStart(ServerWorld world) {
        collapseQueue.clear();
        net.minecraft.util.math.random.Random random = world.getRandom();

        for (BlockPos pos : tracked) {
            if (backup.containsKey(pos)) continue; // 이미 이전 사이클에 파괴된 채 복구 대기중이면 스킵
            if (!world.isSkyVisible(pos)) continue; // 지하는 붕괴 대상 아님
            if (random.nextFloat() > COLLAPSE_CHANCE) continue;

            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;

            backup.put(pos.toImmutable(), state);
            collapseQueue.add(pos.toImmutable());
        }
        markDirty();
    }

    /** HEAVY_RAIN 동안 매틱 호출. */
    public void tickCollapse(ServerWorld world) {
        int count = 0;
        while (!collapseQueue.isEmpty() && count < BATCH_SIZE) {
            BlockPos pos = collapseQueue.poll();
            count++;

            if (world.getBlockState(pos).isAir()) continue;

            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            world.playSound(null, pos, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.BLOCKS, 0.6f, 0.7f);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    4, 0.3, 0.2, 0.3, 0.02);
        }
        if (count > 0) markDirty();
    }

    // ===== 복구 =====

    public void beginRestore(ServerWorld world) {
        restoreQueue.clear();
        restoreQueue.addAll(backup.keySet());
        markDirty();
    }

    /** RECOVERY 동안 매틱 호출. 큐가 비어서 완료되면 true 반환. */
    public boolean tickRestore(ServerWorld world) {
        int count = 0;
        while (!restoreQueue.isEmpty() && count < BATCH_SIZE) {
            BlockPos pos = restoreQueue.poll();
            count++;

            BlockState original = backup.remove(pos);
            if (original != null) {
                world.setBlockState(pos, original);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3, 0.3, 0.2, 0.3, 0.0);
            }
        }
        if (count > 0) markDirty();
        return restoreQueue.isEmpty();
    }

    // ===== PersistentState 저장/로드 =====

    private static final String KEY = "raincraft_collapse";

    public static CollapseManager get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(CollapseManager::fromNbt, CollapseManager::new, KEY);
    }

    public static CollapseManager fromNbt(NbtCompound tag) {
        CollapseManager m = new CollapseManager();

        NbtList trackedList = tag.getList("tracked", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < trackedList.size(); i++) {
            m.tracked.add(NbtHelper_readPos(trackedList.getCompound(i)));
        }

        NbtList backupList = tag.getList("backupPos", NbtElement.COMPOUND_TYPE);
        NbtList backupStateList = tag.getList("backupState", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < backupList.size(); i++) {
            BlockPos pos = NbtHelper_readPos(backupList.getCompound(i));
            BlockState state = net.minecraft.nbt.NbtHelper.toBlockState(
                    net.minecraft.registry.Registries.BLOCK.getReadOnlyWrapper(),
                    backupStateList.getCompound(i));
            m.backup.put(pos, state);
        }
        return m;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtList trackedList = new NbtList();
        for (BlockPos pos : tracked) {
            trackedList.add(writePos(pos));
        }
        tag.put("tracked", trackedList);

        NbtList backupPosList = new NbtList();
        NbtList backupStateList = new NbtList();
        for (Map.Entry<BlockPos, BlockState> e : backup.entrySet()) {
            backupPosList.add(writePos(e.getKey()));
            backupStateList.add(net.minecraft.nbt.NbtHelper.fromBlockState(e.getValue()));
        }
        tag.put("backupPos", backupPosList);
        tag.put("backupState", backupStateList);
        return tag;
    }

    private static NbtCompound writePos(BlockPos pos) {
        NbtCompound c = new NbtCompound();
        c.putInt("x", pos.getX());
        c.putInt("y", pos.getY());
        c.putInt("z", pos.getZ());
        return c;
    }

    private static BlockPos NbtHelper_readPos(NbtCompound c) {
        return new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
    }
}
