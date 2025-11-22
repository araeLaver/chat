const CACHE_NAME = 'securechat-v1.0.0';
const CACHE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  // 오프라인에서도 기본 기능 동작하도록 캐시
];

// Service Worker 설치 이벤트
self.addEventListener('install', event => {
  console.log('Service Worker: 설치됨');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Service Worker: 캐시 로딩됨');
        return cache.addAll(CACHE_ASSETS);
      })
      .catch(err => console.log('Service Worker: 캐시 로딩 실패', err))
  );
});

// Service Worker 활성화 이벤트
self.addEventListener('activate', event => {
  console.log('Service Worker: 활성화됨');
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cache => {
          if (cache !== CACHE_NAME) {
            console.log('Service Worker: 오래된 캐시 삭제됨');
            return caches.delete(cache);
          }
        })
      );
    })
  );
});

// 네트워크 요청 가로채기
self.addEventListener('fetch', event => {
  if (event.request.url.includes('/api/') || event.request.url.includes('/chat')) {
    // API 요청과 WebSocket은 항상 네트워크 우선
    return;
  }
  
  event.respondWith(
    fetch(event.request)
      .then(res => {
        // 응답 복사본 생성
        const resClone = res.clone();
        // 캐시에 저장
        caches.open(CACHE_NAME).then(cache => {
          cache.put(event.request, resClone);
        });
        return res;
      })
      .catch(() => {
        // 네트워크 실패시 캐시에서 가져오기
        return caches.match(event.request);
      })
  );
});

// 푸시 알림 수신
self.addEventListener('push', event => {
  if (event.data) {
    const data = event.data.json();
    const options = {
      body: data.body,
      icon: '/icon-192x192.png',
      badge: '/icon-96x96.png',
      vibrate: [100, 50, 100],
      data: {
        url: data.url || '/'
      },
      actions: [
        {
          action: 'open',
          title: '열기',
          icon: '/icon-96x96.png'
        },
        {
          action: 'close',
          title: '닫기'
        }
      ]
    };

    event.waitUntil(
      self.registration.showNotification(data.title, options)
    );
  }
});

// 알림 클릭 처리
self.addEventListener('notificationclick', event => {
  event.notification.close();

  if (event.action === 'open' || event.action === '') {
    const url = event.notification.data?.url || '/';
    event.waitUntil(
      clients.matchAll().then(clientList => {
        // 이미 열린 탭이 있으면 포커스
        for (const client of clientList) {
          if (client.url === url && 'focus' in client) {
            return client.focus();
          }
        }
        // 새 탭 열기
        if (clients.openWindow) {
          return clients.openWindow(url);
        }
      })
    );
  }
});

// 백그라운드 동기화
self.addEventListener('sync', event => {
  if (event.tag === 'background-sync') {
    event.waitUntil(
      // 오프라인에서 작성한 메시지들을 서버로 전송
      syncOfflineMessages()
    );
  }
});

async function syncOfflineMessages() {
  try {
    // IndexedDB에서 오프라인 메시지 가져오기
    const offlineMessages = await getOfflineMessages();
    
    for (const message of offlineMessages) {
      try {
        await fetch('/api/messages', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(message)
        });
        
        // 성공적으로 전송된 메시지는 로컬에서 삭제
        await deleteOfflineMessage(message.id);
      } catch (error) {
        console.log('메시지 동기화 실패:', error);
      }
    }
  } catch (error) {
    console.log('오프라인 메시지 동기화 실패:', error);
  }
}

// IndexedDB 헬퍼 함수들 (실제 구현 시 추가)
async function getOfflineMessages() {
  // IndexedDB에서 오프라인 메시지 조회
  return [];
}

async function deleteOfflineMessage(id) {
  // IndexedDB에서 메시지 삭제
}