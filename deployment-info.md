# 🚀 자동 배포 테스트

현재 설정된 자동 배포 규칙:
- main 브랜치 push → Production 환경 배포
- develop 브랜치 push → Development 환경 배포

배포 진행 과정:
1. GitHub Actions 실행 (빌드 & 테스트)
2. Koyeb API 호출하여 서비스 생성/업데이트
3. Docker 이미지 빌드
4. 서비스 시작 (2-3분 소요)

배포 상태 확인:
- GitHub: https://github.com/araeLaver/simple-chat-server/actions
- Koyeb: https://app.koyeb.com/services
