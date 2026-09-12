# Raincraft (Fabric 1.20.1)

레인월드 스타일 사이클/비/쉘터/카르마 시스템의 개발 뼈대입니다.

## 빌드하는 법

1. JDK 17 설치 확인
2. 프로젝트 루트에서:
   ```
   ./gradlew build
   ```
   (최초 실행 시 Gradle이 Minecraft/Yarn 매핑을 다운로드하느라 시간이 좀 걸립니다)
3. 결과물: `build/libs/raincraft-0.1.0.jar` → Fabric 서버/클라이언트의 `mods` 폴더에 넣으면 됩니다.
4. 개발 중 테스트: `./gradlew runClient` / `./gradlew runServer`

**주의**: `gradle.properties`의 버전들(yarn_mappings, loader_version, fabric_version)은
지금 시점 기준 값이라 시간이 지나면 최신 버전과 달라질 수 있습니다.
https://fabricmc.net/develop/ 에서 최신 조합 확인 후 맞춰주세요.

## 현재 구현된 것 (요구사항 1~9 매핑)

| 요구사항 | 구현 위치 | 상태 |
|---|---|---|
| 1. 시간 흐름 + 어두워짐 징조 | `cycle/CycleManager.java` (DAY→WARNING) | 상태머신 완료, 실제 하늘 어둡게 하는 시각효과는 미완 |
| 2. 시계로 확인 | `client/RaincraftClient.java` (ItemTooltipCallback) | 완료 (바닐라 시계에 툴팁 추가) |
| 3. 빗방울→폭우 + 화면 흔들림 + 사운드 | `client/RaincraftClient.java`, `mixin/CameraShakeMixin.java` | 파티클/사운드 완료, 카메라 쉐이크는 스텁 (Camera#update 시그니처 확인 필요) |
| 4. 지하 익사 | `cycle/DrowningHazard.java` | 완료 (실제 물 채우기는 안 하고 판정만 - 성능상 이유, 주석 참고) |
| 5. 쉘터 + 전원 동면 | `shelter/ShelterManager.java`, `worldgen/ShelterRoomFeature.java` | 완료 - 쉘터는 자연 생성 구조물 전용 (5x5x5 베드락 박스, 내부 3x3x3, 출입구 1곳). `ModBlocks.SHELTER_BEACON`은 BlockItem이 없어 플레이어가 얻거나 직접 설치 불가 |
| 6. 비 올때 사망 시 아이템 삭제 | `mixin/PlayerDeathDropMixin.java` | 완료 |
| 7. 지상 건축물 붕괴 | `block/CollapseManager.java`, `mixin/BlockItemMixin.java` | 완료 (틱당 배치 처리로 랙 방지) |
| 8. 다음날 복구 | `block/CollapseManager.java` (beginRestore/tickRestore) | 완료 |
| 9. 카르마 시스템 | 미구현 | TODO - Fabric API의 `AttachmentType` 또는 Cardinal Components 라이브러리 추천 |

## 쉘터 자연 생성 (신규)

- `worldgen/ShelterRoomFeature.java`: Feature 구현체. 5x5x5 베드락 박스 내부에 3x3x3 공동을 파고,
  벽 한쪽에 출입구(폭1 높이2)를 뚫은 뒤, 내부 바닥 중앙에 `ModBlocks.SHELTER_BEACON` 배치.
- `worldgen/ModFeatures.java`: Feature 정적 레지스트리 등록.
- `data/raincraft/worldgen/configured_feature/shelter_room.json`, `placed_feature/shelter_room.json`:
  생성 확률(`rarity_filter chance`)과 Y좌표 범위(`height_range`)를 여기서 조절. 지금은 chance=250
  (청크당 1/250 확률), Y -40~60 사이. 너무 안 나오거나 너무 자주 나오면 이 숫자만 바꾸면 됨.
- `RaincraftMod.java`에서 `BiomeModifications.addFeature(...)`로 오버월드 전체 생물군계에 등록.
- 쉘터 판정(`ShelterManager`)은 태그(`shelter_marker`)를 그대로 쓰고, 태그 내용만
  `raincraft:shelter_beacon`으로 바꿨기 때문에 기존 판정 코드는 수정 없이 그대로 작동함.
- `ModBlocks.SHELTER_BEACON`은 의도적으로 BlockItem을 등록하지 않음 → 플레이어가 캐거나
  인벤토리에 넣거나 손으로 다시 설치할 수 없음. 오직 자연 생성된 쉘터만 유효한 쉘터가 됨
  (플레이어가 아무 데나 비콘 깔아서 어뷰징하는 걸 원천 차단).

## 다음에 할 일 (우선순위 순)

1. **실제 빌드해서 컴파일 에러 잡기** — 이 코드는 Fabric API 시그니처 기준으로 작성했지만
   실제 Yarn 매핑 버전에 따라 메서드명이 미묘하게 다를 수 있습니다. 특히:
   - `CameraShakeMixin`의 `update*` 메서드 시그니처 (와일드카드로 잡았는데 실패하면
     `/mnt`가 아니라 로컬에서 `genSources`로 실제 시그니처 확인)
   - `NbtHelper.toBlockState`/`fromBlockState` 관련 임포트 경로
2. **쉘터 밀도/위치 밸런스**: `placed_feature/shelter_room.json`의 `chance` 값 조절.
   너무 흔하면 긴장감이 없고, 너무 희귀하면 사이클마다 다 죽어나가니 실제 플레이하면서 튜닝 필요
3. **쉘터가 기존 지형/동굴/광산과 겹칠 때 처리**: 지금은 무조건 그 자리를 파고 베드락으로
   덮어써버림. 동굴 한가운데 뜬금없이 박스가 생기는 게 어색하면, generate() 안에서
   주변 블록이 이미 공기(동굴)인 비율을 체크해서 너무 뻥 뚫린 곳이면 스킵하는 로직 추가 고려
4. **쉘터 밀폐 판정 강화**: 지금은 단순 근접 거리(반경 5블록)만 보는데, 필요하면
   `ShelterManager.isSheltered()`에 "출입구가 닫혔는지"까지 검사하는 로직 추가 가능
5. **하늘 어두워지는 실제 시각효과**: `WorldRenderEvents` 또는 클라이언트 밝기 오버레이 셰이더
6. **카르마 시스템**: 플레이어별 영구 데이터가 필요하므로 Fabric의 Attachment API
   (0.90+) 또는 Cardinal Components API 라이브러리 도입 추천
7. **밸런스 조절**: `CycleManager`의 타이밍 상수들, `CollapseManager.COLLAPSE_CHANCE`
   등은 전부 임의값이니 실제 플레이해보면서 조절

## 패키지 구조

```
com.raincraft
├── RaincraftMod.java          # 메인 진입점, 이벤트/월드젠 등록
├── cycle/
│   ├── CycleState.java        # 상태 enum
│   ├── CycleManager.java      # 상태머신 (PersistentState)
│   └── DrowningHazard.java    # 지하 익사 판정
├── shelter/
│   └── ShelterManager.java    # 쉘터 판정 (태그 기반 근접 판정)
├── block/
│   ├── ModBlocks.java          # 쉘터 비콘 블록 (아이템 없음, 자연 생성 전용)
│   ├── PlacedBlockTracker.java
│   └── CollapseManager.java   # 붕괴/복구 (PersistentState)
├── worldgen/
│   ├── ShelterRoomFeature.java # 쉘터 방 자연 생성 Feature
│   └── ModFeatures.java        # Feature 레지스트리 등록
├── network/
│   └── CycleNetworking.java   # 서버->클라 상태 동기화
├── client/
│   ├── RaincraftClient.java   # 클라 진입점, 파티클/사운드/툴팁
│   └── ClientCycleData.java   # 클라에서 들고 있는 현재 상태
└── mixin/
    ├── BlockItemMixin.java        # 블록 설치 감지
    ├── PlayerDeathDropMixin.java  # 폭우 사망 시 아이템 삭제
    └── CameraShakeMixin.java      # 카메라 흔들림 (클라 전용)
```
