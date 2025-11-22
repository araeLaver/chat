# BEAM API 문서

## 기본 정보

- **Base URL**: `https://your-domain.com/api`
- **인증 방식**: JWT Bearer Token
- **Content-Type**: `application/json`

---

## 인증 API

### 이메일 인증 (권장)

#### 1. 인증코드 발송
```http
POST /api/auth/email/send-code
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "인증번호가 이메일로 발송되었습니다"
}
```

---

#### 2. 인증코드 확인
```http
POST /api/auth/email/verify
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "verificationCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "이메일 인증이 완료되었습니다",
  "email": "user@example.com",
  "userId": 1
}
```

---

#### 3. 회원가입 완료
```http
POST /api/auth/email/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "displayName": "홍길동",
  "username": "gildong"
}
```

**Response:**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "gildong",
    "displayName": "홍길동",
    "email": "user@example.com"
  }
}
```

---

#### 4. 로그인 (OTP 발송)
```http
POST /api/auth/email/login
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "로그인 인증번호가 이메일로 발송되었습니다"
}
```

---

#### 5. 로그인 인증 확인
```http
POST /api/auth/email/login/verify
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "verificationCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "gildong",
    "displayName": "홍길동",
    "email": "user@example.com"
  }
}
```

---

## 채팅방 API

> **인증 필요**: `Authorization: Bearer {token}`

### 채팅방 생성
```http
POST /api/rooms
```

**Request Body:**
```json
{
  "roomName": "개발자 모임",
  "description": "개발 관련 이야기",
  "roomType": "PUBLIC",
  "maxMembers": 100
}
```

**roomType 옵션:**
- `PUBLIC` - 공개 채팅방
- `PRIVATE` - 비공개 채팅방 (초대만)
- `SECRET` - 비밀 채팅방

---

### 내 채팅방 목록
```http
GET /api/rooms/my-rooms
```

**Response:**
```json
[
  {
    "roomId": 1,
    "roomName": "개발자 모임",
    "description": "개발 관련 이야기",
    "roomType": "PUBLIC",
    "currentMembers": 5,
    "maxMembers": 100,
    "lastMessage": "안녕하세요!",
    "lastMessageTime": "2024-01-15T10:30:00",
    "unreadCount": 3,
    "myRole": "OWNER"
  }
]
```

---

### 채팅방 메시지 조회
```http
GET /api/rooms/{roomId}/messages
```

**Response:**
```json
[
  {
    "id": 1,
    "senderId": 1,
    "senderName": "홍길동",
    "content": "안녕하세요!",
    "messageType": "TEXT",
    "timestamp": "2024-01-15T10:30:00",
    "readCount": 5,
    "isMine": true
  }
]
```

---

### 메시지 전송
```http
POST /api/rooms/{roomId}/messages
```

**Request Body:**
```json
{
  "content": "안녕하세요!",
  "messageType": "TEXT"
}
```

**messageType 옵션:**
- `TEXT` - 텍스트 메시지
- `IMAGE` - 이미지
- `FILE` - 파일
- `VOICE` - 음성
- `VIDEO` - 동영상

---

### 멤버 추가
```http
POST /api/rooms/{roomId}/members
```

**Request Body:**
```json
{
  "userId": 2
}
```

---

### 채팅방 나가기
```http
POST /api/rooms/{roomId}/leave
```

---

### 읽음 처리
```http
POST /api/rooms/{roomId}/read
```

---

## 1:1 채팅 (DM) API

### 대화 목록 조회
```http
GET /api/dm/conversations
```

**Response:**
```json
[
  {
    "conversationId": "dm_1_2",
    "otherUser": {
      "id": 2,
      "username": "user2",
      "displayName": "김철수",
      "isOnline": true
    },
    "lastMessage": "오늘 회의 몇시야?",
    "lastMessageTime": "2024-01-15T10:30:00",
    "unreadCount": 1
  }
]
```

---

### DM 메시지 조회
```http
GET /api/dm/{conversationId}/messages
```

---

### DM 전송
```http
POST /api/dm/send
```

**Request Body:**
```json
{
  "receiverId": 2,
  "content": "안녕하세요!",
  "messageType": "TEXT"
}
```

---

## 친구 API

### 친구 요청
```http
POST /api/friends/request
```

**Request Body:**
```json
{
  "friendId": 2
}
```

---

### 친구 요청 수락
```http
POST /api/friends/accept
```

**Request Body:**
```json
{
  "friendId": 2
}
```

---

### 친구 목록
```http
GET /api/friends
```

---

### 받은 친구 요청
```http
GET /api/friends/requests/received
```

---

### 사용자 차단
```http
POST /api/friends/block
```

**Request Body:**
```json
{
  "userId": 2
}
```

---

## 파일 API

### 파일 업로드
```http
POST /api/files/upload
Content-Type: multipart/form-data
```

**Form Data:**
- `file` - 업로드할 파일 (최대 10MB)
- `roomId` (선택) - 그룹 채팅방 ID
- `conversationId` (선택) - DM 대화 ID

**Response:**
```json
{
  "success": true,
  "fileId": 1,
  "fileName": "image.png",
  "fileUrl": "/api/files/download/1",
  "thumbnailUrl": "/api/files/thumbnail/1",
  "fileSize": 102400,
  "category": "IMAGE"
}
```

---

### 파일 다운로드
```http
GET /api/files/download/{fileId}
```

---

## 검색 API

### 메시지 검색
```http
GET /api/search/messages?keyword=안녕&roomId=1
```

---

### 사용자 검색
```http
GET /api/search/users?keyword=홍길동
```

---

## WebSocket

### 연결
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({Authorization: 'Bearer ' + token}, function(frame) {
    // 연결 성공
});
```

### 구독 (Subscribe)

**그룹 채팅방:**
```javascript
stompClient.subscribe('/topic/room/{roomId}', function(message) {
    // 메시지 수신
});
```

**1:1 채팅:**
```javascript
stompClient.subscribe('/queue/dm/{conversationId}', function(message) {
    // DM 수신
});
```

**타이핑 인디케이터:**
```javascript
stompClient.subscribe('/topic/typing/{roomId}', function(typing) {
    // 타이핑 상태 수신
});
```

### 발송 (Send)

**메시지 전송:**
```javascript
stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
    roomId: 1,
    content: '안녕하세요!',
    messageType: 'TEXT'
}));
```

**타이핑 상태:**
```javascript
stompClient.send('/app/chat.typing', {}, JSON.stringify({
    roomId: 1,
    typing: true
}));
```

---

## 에러 응답

모든 API는 에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "success": false,
  "message": "에러 메시지",
  "error": "상세 에러 정보"
}
```

### HTTP 상태 코드
- `200` - 성공
- `400` - 잘못된 요청
- `401` - 인증 필요
- `403` - 권한 없음
- `404` - 리소스 없음
- `500` - 서버 에러

---

## Swagger UI

개발 환경에서 Swagger UI를 통해 API를 테스트할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```
