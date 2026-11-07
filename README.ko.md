<div align="center">

# HuiJuiSU · Nova

**KernelSU 포크용 Android 매니저 · 자체 Compose UI · 탐지 / 숨김 / 자가 복구 내장**

[![Release](https://img.shields.io/github/v/release/HuijuiSU-NEXT/HuijuiSU?label=release&color=2E9E6B)](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.13-4285F4)](https://developer.android.com/jetpack/compose)

[简体中文](README.md) · [繁體中文](README.zh-TW.md) · [English](README.en.md) · [日本語](README.ja.md) · **한국어** · [Русский](README.ru.md)

[다운로드](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) ·
[Telegram](https://t.me/Huijui123)

</div>

![홈](docs/images/v6.1/home.png)

---

## 무엇인가

**KernelSU 포크용 매니저**입니다. 단순히 "작동만 하는" 수준에서 멈추지 않고, root 사용자가 실제로 겪는 문제를 내장 기능으로 만들었습니다.

- **경고만 하지 않고 고칩니다**: 검진이 모듈 충돌을 찾고, 자가 복구가 한 번에 비활성화
- **명령어를 복사하게 만들지 않습니다**: `resetprop` 패치 12종 내장, 탭 한 번으로 적용
- **거짓 스위치는 없습니다**: 지원되지 않는 커널 기능은 "지원 안 함"으로 표시

UI는 완전 자체 제작이며 Compose `foundation`에만 의존합니다.

---

## 다른 곳에 없는 4가지

| 기능 | 한 줄 요약 | 다른 제품과의 차이 |
|---|---|---|
| **원탭 검진** | 5개 항목 검사 + 0~100 점수 + 수정 링크 | 다른 곳은 목록만 주고 결론을 주지 않음 |
| **자가 복구 센터** | 충돌 모듈 일괄 비활성화(되돌릴 수 있음) + 캐시 정리 | 다른 곳은 보고만 함 |
| **패치 센터** | `resetprop` 패치 12종, 재부팅 시 자동 복원 | 다른 곳은 명령어를 직접 입력하게 함 |
| **4.x 호환 모드** | 비 GKI 커널용 안내 + boot 패치 절차 | 다른 곳은 4.x를 지원하지 않음 |

---

## 화면

4탭 구조, 다크 글래스 소재, 움직이는 배경.

| 홈 | Dock |
|---|---|
| ![홈](docs/images/v6.1/home.png) | ![Dock](docs/images/v6.1/dock.png) |

- 다크 글래스 카드(상단 26% 하이라이트, 중앙은 배경이 비침, 하단은 어둡게)
- 카드·Dock·버튼이 같은 소재 언어를 공유
- 배경은 **비율 유지 cover**(늘어남 없음). 동영상과 이미지 모두 동일
- 중국어 / 영어 전환(196개 사전, 영구 저장)

---

## 기능

- **모듈**: 목록 / 활성화 / 비활성화 / 제거 / 온라인 설치 / 로컬 zip
- **슈퍼유저**: 허용 목록, 취소(커널 ioctl), 패키지로 추가, 실제 아이콘 표시
- **도구**: 플래시 / 탐지 / 원탭 숨김 / SUSFS / 커널 설정 / 터미널
- **설정**: UI 모드 / 글래스 / 밝기 / 강조색 / 배경 / 언어 / 정보

---

## 커널 지원

| 커널 | 방식 |
|---|---|
| 6.x (6.6 / 6.12 / 6.18) | LKM |
| 5.10 / 5.15 | LKM |
| 4.x (4.9 / 4.14 / 4.19) | 커널 패치 (**LKM 불가**) |
| 비 GKI 5.x | 커널 패치 |

---

## 설치

1. [Releases](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)에서 ABI에 맞는 APK 다운로드(모르면 `universal`)
2. 덮어쓰기 설치 가능(`applicationId` 동일, 데이터 유지)
3. 첫 실행 후 모듈 화면에서 root 허용

---

## 비교

| | KernelSU | SukiSU | 이 프로젝트 |
|---|---|---|---|
| SUSFS | ❌ | ✅ | ✅ |
| KPM | ❌ | ✅ | ❌ (커널 미구현) |
| 원탭 검진 | ❌ | ❌ | ✅ |
| 자가 복구 센터 | ❌ | ❌ | ✅ |
| 패치 센터 | ❌ | ❌ | ✅ |
| 4.x 호환 모드 | ❌ | ❌ | ✅ |
| 자체 UI | ❌ | ❌ | ✅ |

---

## 알려진 제한

- **세이프 모드**: 부팅 시 볼륨 키로 진입하는 커널 동작, 매니저에 쓰기 API 없음
- **KPM**: 이 포크의 커널에 로더가 없음
- **다국어**: UI는 중국어 / 영어만 지원(README는 6개 언어)
- **4.x**: 안내와 도구만 제공. 커널 빌드는 기기 커널 소스가 필요

---

## 링크

- [변경 이력](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
- [서드파티 고지](docs/THIRD_PARTY_NOTICES.md)
- [Telegram](https://t.me/Huijui123)

<div align="center">

**GPL-3.0** · 커널 모듈과 매니저는 동일 라이선스

</div>
