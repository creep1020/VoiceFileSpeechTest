# 무료 음성 파일 인식 테스트 앱

이 프로젝트는 Android 휴대폰에서 한 번 녹음한 영어 음성 파일을 저장하고,
그 **동일한 파일**을 Google ML Kit GenAI Speech Recognition API로 인식하는 독립 테스트 앱입니다.

## 중요한 점
- 유료 API 키나 서버를 사용하지 않습니다.
- 녹음 파일은 앱 내부 저장소에 보관되며 외부 서버로 업로드하지 않습니다.
- 최초 모델 준비 시 인터넷 연결이 필요할 수 있습니다. 모델이 준비된 뒤 오프라인 인식 여부를 실제 기기에서 확인해야 합니다.
- Google API가 alpha 단계이므로 기기/모드별 지원 여부를 런타임에서 검사합니다.
- 기본 모드는 Android 12(API 31)+ 지원을 대상으로 합니다. 사전 녹음 파일 인식은 Android 11(API 30)+에서 문서화되어 있습니다.
- 고급 모드는 Gemini Nano가 지원되는 특정 기기에서만 동작합니다.
- 이 프로젝트는 소스 프로젝트이며, 이 실행 환경에는 Android SDK/Gradle 빌드 도구가 없어 APK를 컴파일한 파일은 포함하지 않았습니다.

## 빌드 방법
1. PC에 Android Studio 최신 안정 버전을 설치합니다.
2. 압축을 풀고 `VoiceFileSpeechTest` 폴더를 Android Studio에서 엽니다.
3. Gradle Sync가 끝날 때까지 기다립니다. 필요한 Android SDK와 Gradle 구성요소를 설치합니다.
4. 휴대폰에서 개발자 옵션과 USB 디버깅을 켜고 PC에 연결합니다.
5. Android Studio의 Run 버튼으로 설치하거나, `Build > Build Bundle(s) / APK(s) > Build APK(s)`를 선택합니다.

## 휴대폰 테스트
1. 앱 실행 후 기본 모드부터 선택합니다.
2. `녹음 시작`을 누르고 다음 문장을 한 번 말합니다.
   `I want to learn English every day.`
3. `녹음 종료`를 누릅니다.
4. `녹음 재생`으로 목소리를 확인합니다.
5. `저장된 녹음 파일 인식`을 누릅니다.
6. 모델이 `DOWNLOADABLE`이면 최초 한 번 다운로드가 진행될 수 있습니다.
7. 인식 결과와 상태 문구를 확인합니다.
8. 이후 Wi-Fi/모바일 데이터를 끄고 다시 인식해 오프라인 작동 여부를 확인합니다.

## 다음 단계
테스트가 성공하면 이 인식 기능을 기존 일빵빵 앱과 연결하는 Android WebView 호스트 구조를 설계합니다.
기존 PWA의 학습 기록, 보상, Firebase 동기화 코드는 이 테스트 프로젝트에서 변경하지 않습니다.


## PC 없이 GitHub에서 APK 빌드하기
1. 휴대폰 브라우저에서 GitHub에 로그인합니다.
2. `New repository`로 새 저장소를 만듭니다. 이름은 `VoiceFileSpeechTest`로 합니다.
3. 저장소에서 `Add file` → `Upload files`를 선택합니다.
4. ZIP을 휴대폰에 내려받아 압축을 푼 뒤, 안의 프로젝트 파일과 `.github/workflows/build-apk.yml`까지 업로드합니다. `.github` 폴더가 보이지 않으면 GitHub 웹에서 `.github/workflows` 폴더를 만들고 `build-apk.yml`을 별도로 추가해야 합니다.
5. `Commit changes`를 누릅니다.
6. 저장소의 `Actions` 탭에서 `Build Android APK` 작업을 확인합니다.
7. 빌드가 성공하면 실행 기록을 열고 `Artifacts`에서 `VoiceFileSpeechTest-debug`를 내려받습니다.
8. ZIP을 풀고 `app-debug.apk`를 설치합니다.

주의: 이 프로젝트는 Google ML Kit GenAI Speech Recognition 알파 SDK를 사용하는 테스트 소스입니다. 이 실행 환경에서 Android SDK로 빌드하거나 실제 갤럭시에서 실행 검증한 것은 아닙니다. GitHub 빌드가 실패하면 Actions 로그를 바탕으로 오류를 수정해야 합니다.
