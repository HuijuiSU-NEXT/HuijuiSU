# HuiJuiSU NEXT

> [BakaSU](https://github.com/Baka-SU/BakaSU)를 기반으로 깊이 커스터마이징된 Android Root 관리자
> BakaSU의 안정적인 코어를 유지하면서 국내 모딩 환경에 맞춰 UI를 대폭 재구성하고 기능을 강화했습니다.

[English](./README_EN.md) | [简体中文](./README.md) | [日本語](./README_JA.md) | [한국어](./README_KO.md) | [Русский](./README_RU.md)

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part License：GPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

---

## ✨ HuiJuiSU NEXT 핵심 기능

### 독립형 도구 허브

하단 내비게이션이 **5개의 탭**으로 재설계되었습니다: 홈 / **도구** / 슈퍼유저 / 모듈 / 설정.

도구가 홈 페이지에서 분리되어 정보 밀도가 너무 높아지는 것을 방지하고 발견하기 쉬워졌습니다.

### 7가지 강력한 도구

| 기능 | 설명 |
|---|---|
| **원터치 상태 검사** | 배터리 / 온도 / 부트로더 / 커널 / Root / SELinux / 저장소 |
| **성능 모드** | 절전 / 균형 / 성능 / 게임, 4단계 시스템 스케줄링 |
| **원터치 백업 패키지** | boot + init_boot + vbmeta + 모듈 스냅샷, restore.bat 포함 |
| **원터치 언브릭** | 백업 디렉토리에서 원클릭 dd 플래시백, 모듈 상태 복원 옵션 |
| **GKI 커널 다운로드** | built-in 모드 전용 커뮤니티 사전 컴파일 커널 리디렉션 |
| **커스텀 커널 플래시** | `.img` (built-in) 및 `.ko` (LKM) 두 가지 형식 지원 |
| **앱 관리자** | 동결 / 해제 / 강제 종료 / 데이터 삭제 / 제거 (현재 사용자) |

### 내장 숨김 (기본 최강)

HuiJuiSU NEXT는 **즉시 사용 가능한** 숨김 전략을 사용합니다:

- **경로 숨김**: 8개의 중요한 `sus_path` (ksu / ksud / modules / magisk / tmp 등)
- **마운트 숨김**: `hide_sus_mnts_for_non_su_procs`가 sus 마운트 지점을 숨김
- **로그 위장**: `avc_log_spoofing`이 SELinux 거부 로그를 숨김
- **커널 통계 숨김**: `sus_kstat`가 `/system` 및 `/vendor`를 덮어씀
- **자동 적용**: 앱 실행 시 백그라운드에서 자동으로 실행되며 규칙 업데이트 후 자동으로 다시 적용됨

### 모듈 스냅샷

백업 시 `/data/adb/modules/` 아래의 모든 모듈 활성화/제거 상태를 `modules.json`에 자동으로 기록합니다.

언브릭 시 "모듈 상태도 복원"을 선택하면 백업 시점의 구성으로 모듈을 자동 복원합니다. **수동으로 하나씩 다시 활성화할 필요가 없습니다**.

### 커스텀 커널 플래시

GKI 기기의 built-in 모드 문제점 해결:

- **`.img` 형식**: Root 권한으로 dd 플래시 + vbmeta 검증 처리(롤백 방지) + LKM 잔여물 제거 옵션
- **`.ko` 형식**: `ksud boot-patch -f -m`을 호출하여 자동 주입
- **스마트 인식**: 파일 형식과 크기를 자동 감지하여 잘못된 파일 플래시 방지

---

## 🧩 BakaSU에서 상속된 핵심 기능

1. 커널 기반 `su` 및 Root 권한 관리
2. [metamodules](https://kernelsu.org/guide/metamodule.html) 기반 모듈 시스템
3. [App Profile](https://kernelsu.org/guide/app-profile.html): Root 권한을 우리 안에 가둠
4. 비 GKI 및 GKI 1.0 지원
5. 다중 관리자 지원 (공식 KernelSU / KOWSU / BakaSU)

---

## ⚙️ 호환성 상태

- Android **GKI 2.0** 기기(커널 5.10+) 공식 지원
- 구형 커널(3.4+)도 호환되지만 커널을 직접 컴파일해야 합니다
- 지원 아키텍처: `arm64-v8a`, `armeabi-v7a`, `x86_64`
- 내장 [SUSFS](https://gitlab.com/simonpunk/susfs4ksu)는 커널 4.3+로의 백포트**만 지원**
- `Tracepoint Syscall Redirect hook`은 GKI2(5.10+) 커널만 지원

---

## 🪝 후크 모드

- **`Tracepoint Syscall Redirect hook`**: 기본 후크 모드, 업스트림에서 제공, `arm64-v8a` 또는 `x86_64` ABI의 GKI2 커널만 지원
- **`Manual Hook`**: 가장 호환성이 높은 후크, Linux 커널 3.4 ~ 6.18 지원
- **`SUSFS Inline Hook`**: [SuSFS](https://github.com/simonpunk/susfs4ksu)의 후크, `Manual Hook`과 유사

---

## 🔧 성능 최적화

### 커널 핫 패스 로그

v3.0.0에서 커널 고빈도 호출 경로의 `pr_info`를 모두 `pr_debug`로 전환했습니다:

- `syscall_hook.c` (arm64/x86_64/riscv64): 37곳
- `ksud_integration.c`: 18곳
- `setuid_hook.c`, `sucompat.c`, `dispatch.c`, `supercall.c` 등: 80곳 이상

### sulog 고속 반환

`ksu_sulog_emit_grant_root`에 고속 반환을 추가하여 sulog 비활성화 시 매번 `setresuid`의 후속 로직을 생략합니다.

---

## ⚠️ 업그레이드 안내

### 패키지 이름 변경

- **이전 패키지 이름**: `com.huijui.manager` / `com.huijuisu.huijuisu`
- **새 패키지 이름**: `com.huijui.sunext`

이전 버전에서 업그레이드하려면 이전 버전을 제거한 후 새 버전을 설치해야 합니다.

### 업그레이드 후 상태

| 항목 | 상태 |
|---|---|
| Root 권한 부여 목록 | ⚠️ 지워지며 재부여 필요 |
| 모듈 활성화 상태 | ✅ 유지됨 |
| SUSFS 구성 | ✅ 유지됨 |
| 백업 기록 | ✅ 유지됨 |

---

## 📜 라이선스

- `kernel/` 디렉토리의 파일은 [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) 라이선스를 따릅니다
- 아트 및 시각적 파일은 [OukaroMF](https://github.com/OukaroMF)가 제작했으며 계층적 라이선스를 따릅니다. 자세한 내용은 [`ASSETS_LICENSE.md`](../ASSETS_LICENSE.md)를 참조하세요.
- 위의 파일 또는 디렉토리를 제외한 모든 부분은 [GPL-3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) 라이선스를 따릅니다.

---

## 🙏 크레딧

- [KernelSU](https://github.com/tiann/KernelSU): 업스트림 기반
- [BakaSU](https://github.com/Baka-SU/BakaSU): 이 프로젝트의 기반
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra): Fork 소스
- [Magisk](https://github.com/topjohnwu/Magisk): 강력한 Root 도구
- [liquid-glass](https://github.com/nadeemiqbal/liquid-glass): 리퀴드 글래스 UI 라이브러리
- [genuine](https://github.com/brevent/genuine/): APK v2 서명 검증

---

## 💬 피드백

- GitHub Issues: https://github.com/hhcdxii-ship-it/HuijuiSU/issues
- QQ 그룹: 앱 내 About 페이지에서 "Join QQ Group" 클릭
