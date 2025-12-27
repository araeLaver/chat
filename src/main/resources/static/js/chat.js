/* ===========================
   BEAM - Chat Application JS
   =========================== */

const API_URL = window.location.origin;

class ChatApp {
    constructor() {
        this.ws = null;
        this.currentRoom = localStorage.getItem('defaultRoomId') || null;
        this.token = localStorage.getItem('token');
        this.username = localStorage.getItem('username') || 'Guest';
        this.displayName = localStorage.getItem('displayName') || 'Guest';
        this.isTyping = false;
        this.typingTimeout = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;

        this.init();
    }

    init() {
        // Check authentication
        if (!this.token) {
            window.location.href = '/';
            return;
        }

        // Set user info
        this.updateUserInfo();

        // Connect WebSocket
        this.connectWebSocket();

        // Setup event listeners
        this.setupEventListeners();

        // Load initial data
        this.loadConversations();
    }

    updateUserInfo() {
        const userAvatarEl = document.getElementById('userAvatar');
        const userNameEl = document.getElementById('userName');

        if (userAvatarEl) {
            userAvatarEl.textContent = this.getInitial(this.displayName);
        }
        if (userNameEl) {
            userNameEl.textContent = this.displayName;
        }
    }

    connectWebSocket() {
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${wsProtocol}//${window.location.host}/ws`;

        // 보안: URL 쿼리 파라미터 대신 Sec-WebSocket-Protocol 헤더로 토큰 전송
        // URL에 토큰을 포함하면 서버 로그에 노출될 수 있음
        this.ws = new WebSocket(wsUrl, [`access_token,${this.token}`]);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            this.showToast('Connected', 'success');
            if (this.currentRoom) {
                this.joinRoom(this.currentRoom);
            }
        };

        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleMessage(message);
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.showToast('Connection error', 'error');
        };

        this.ws.onclose = () => {
            console.log('WebSocket disconnected');
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                this.showToast(`Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`, 'warning');
                setTimeout(() => this.connectWebSocket(), 3000);
            } else {
                this.showToast('Connection lost. Please refresh the page.', 'error');
            }
        };
    }

    handleMessage(message) {
        switch (message.type) {
            case 'CHAT':
                this.displayMessage(message);
                break;
            case 'JOIN':
                this.addStatusMessage(`${message.sender} joined the room`);
                break;
            case 'LEAVE':
                this.addStatusMessage(`${message.sender} left the room`);
                break;
            case 'TYPING':
                this.showTypingIndicator(message.sender);
                break;
            default:
                console.log('Unknown message type:', message);
        }
    }

    setupEventListeners() {
        // Send message on Enter (Shift+Enter for new line)
        const messageInput = document.getElementById('messageInput');
        if (messageInput) {
            messageInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });

            // Typing indicator
            messageInput.addEventListener('input', () => {
                this.handleTyping();
                this.autoResizeTextarea(messageInput);
            });
        }

        // Send button
        const sendBtn = document.getElementById('sendBtn');
        if (sendBtn) {
            sendBtn.addEventListener('click', () => this.sendMessage());
        }

        // Mobile menu toggle
        const menuBtn = document.getElementById('menuBtn');
        if (menuBtn) {
            menuBtn.addEventListener('click', () => this.toggleSidebar());
        }

        // Sidebar close button
        const sidebarClose = document.getElementById('sidebarClose');
        if (sidebarClose) {
            sidebarClose.addEventListener('click', () => this.closeSidebar());
        }

        // Mobile overlay
        const overlay = document.getElementById('mobileOverlay');
        if (overlay) {
            overlay.addEventListener('click', () => this.closeSidebar());
        }

        // Tab switching
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchTab(e.currentTarget.dataset.tab);
            });
        });

        // Logout
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // File upload
        const fileInput = document.getElementById('fileInput');
        if (fileInput) {
            fileInput.addEventListener('change', (e) => this.handleFileUpload(e));
        }

        // Search functionality
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.handleSearch(e.target.value);
                }, 300);
            });
        }

        // Settings button
        const settingsBtn = document.getElementById('settingsBtn');
        if (settingsBtn) {
            settingsBtn.addEventListener('click', () => this.openSettings());
        }

        // Close modals on overlay click
        document.querySelectorAll('.modal-overlay').forEach(overlay => {
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    overlay.classList.remove('active');
                }
            });
        });

        // Close modals on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                document.querySelectorAll('.modal-overlay.active').forEach(modal => {
                    modal.classList.remove('active');
                });
            }
        });
    }

    autoResizeTextarea(textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 140) + 'px';
    }

    sendMessage() {
        const input = document.getElementById('messageInput');
        const text = input.value.trim();

        if (!text || !this.ws || this.ws.readyState !== WebSocket.OPEN) return;

        // Hide welcome state if visible
        const welcomeState = document.getElementById('welcomeState');
        if (welcomeState) {
            welcomeState.style.display = 'none';
        }

        const message = {
            type: 'CHAT',
            roomId: this.currentRoom,
            sender: this.username,
            content: text,
            timestamp: new Date().toISOString()
        };

        this.ws.send(JSON.stringify(message));
        input.value = '';
        input.style.height = 'auto';

        // Stop typing indicator
        this.isTyping = false;
    }

    displayMessage(message) {
        const container = document.getElementById('messagesContainer');
        if (!container) return;

        // Hide welcome state
        const welcomeState = document.getElementById('welcomeState');
        if (welcomeState) {
            welcomeState.style.display = 'none';
        }

        const isSent = message.sender === this.username;

        // Get previous message to determine grouping
        const allMessages = container.querySelectorAll('.message-group');
        const prevMessage = allMessages[allMessages.length - 1];
        const prevSender = prevMessage?.dataset.sender;
        const isNewSender = prevSender !== message.sender;

        // Update previous message bubble position if same sender
        if (!isNewSender && prevMessage) {
            const prevBubbleClass = this.getBubblePosition(prevMessage);
            if (prevBubbleClass === 'bubble-single') {
                prevMessage.classList.remove('bubble-single');
                prevMessage.classList.add('bubble-first');
            } else if (prevBubbleClass === 'bubble-last') {
                prevMessage.classList.remove('bubble-last');
                prevMessage.classList.add('bubble-middle');
            }
        }

        const messageGroup = document.createElement('div');
        messageGroup.className = `message-group ${isSent ? 'sent' : 'received'}`;
        messageGroup.dataset.sender = message.sender;

        // Determine bubble position
        const bubblePosition = isNewSender ? 'bubble-single' : 'bubble-last';
        messageGroup.classList.add(bubblePosition);

        // Show avatar only for new sender (received messages)
        if (!isSent && isNewSender) {
            messageGroup.classList.add('show-avatar');
        }

        const time = new Date(message.timestamp).toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        messageGroup.innerHTML = `
            <div class="message-avatar">${this.getInitial(message.sender)}</div>
            <div class="message-content">
                ${!isSent ? `<div class="message-sender">${this.escapeHtml(message.sender)}</div>` : ''}
                <div class="message-bubble">${this.escapeHtml(message.content)}</div>
                <div class="message-time">${time}</div>
            </div>
        `;

        // Remove typing indicator if exists
        const typingEl = container.querySelector('.typing-indicator');
        if (typingEl) {
            typingEl.remove();
        }

        container.appendChild(messageGroup);
        this.scrollToBottom();
    }

    getBubblePosition(element) {
        if (element.classList.contains('bubble-first')) return 'bubble-first';
        if (element.classList.contains('bubble-middle')) return 'bubble-middle';
        if (element.classList.contains('bubble-last')) return 'bubble-last';
        if (element.classList.contains('bubble-single')) return 'bubble-single';
        return 'bubble-single';
    }

    addStatusMessage(text) {
        const container = document.getElementById('messagesContainer');
        if (!container) return;

        const statusDiv = document.createElement('div');
        statusDiv.className = 'status-message';
        statusDiv.textContent = text;
        container.appendChild(statusDiv);
        this.scrollToBottom();
    }

    showTypingIndicator(username) {
        if (username === this.username) return;

        const container = document.getElementById('messagesContainer');
        if (!container) return;

        // Remove existing typing indicator
        let typingEl = container.querySelector('.typing-indicator');

        if (!typingEl) {
            typingEl = document.createElement('div');
            typingEl.className = 'typing-indicator';
            typingEl.innerHTML = `
                <div class="message-avatar">${this.getInitial(username)}</div>
                <div class="typing-dots">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            `;
            container.appendChild(typingEl);
            this.scrollToBottom();
        }

        // Auto-remove after 3 seconds
        clearTimeout(this.typingTimeout);
        this.typingTimeout = setTimeout(() => {
            if (typingEl && typingEl.parentNode) {
                typingEl.remove();
            }
        }, 3000);
    }

    handleTyping() {
        if (!this.isTyping) {
            this.isTyping = true;

            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                this.ws.send(JSON.stringify({
                    type: 'TYPING',
                    roomId: this.currentRoom,
                    sender: this.username
                }));
            }
        }

        clearTimeout(this.typingTimeout);
        this.typingTimeout = setTimeout(() => {
            this.isTyping = false;
        }, 1000);
    }

    joinRoom(roomId) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

        this.currentRoom = roomId;

        const message = {
            type: 'JOIN',
            roomId: roomId,
            sender: this.username
        };

        this.ws.send(JSON.stringify(message));

        // Clear messages
        const container = document.getElementById('messagesContainer');
        if (container) {
            container.innerHTML = '';
        }

        this.addStatusMessage(`Joined ${roomId}`);

        // Update header
        const chatName = document.getElementById('chatName');
        if (chatName) chatName.textContent = roomId;

        // Close sidebar on mobile
        this.closeSidebar();
    }

    switchTab(tabName) {
        // Update active tab button
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        // Load data based on tab
        switch (tabName) {
            case 'chats':
            case 'rooms':
                this.loadConversations();
                break;
            case 'friends':
                this.loadFriends();
                break;
        }
    }

    async loadConversations() {
        const conversationList = document.getElementById('conversationList');
        if (!conversationList) return;

        conversationList.innerHTML = '<div class="loading-spinner"></div>';

        try {
            const response = await fetch(`${API_URL}/api/rooms/my-rooms`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) throw new Error('Failed to load rooms');

            const rooms = await response.json();

            if (rooms.length === 0) {
                conversationList.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">💬</div>
                        <div class="empty-state-text">No conversations yet</div>
                    </div>
                `;
                return;
            }

            conversationList.innerHTML = rooms.map(room => {
                const timeAgo = room.lastMessageTime ? this.getTimeAgo(room.lastMessageTime) : '';

                return `
                    <div class="conversation-item ${room.roomId === this.currentRoom ? 'active' : ''}"
                         onclick="chatApp.joinRoom('${room.roomId}')">
                        <div class="conversation-avatar">
                            ${this.getRoomAvatar(room.roomType)}
                            ${room.currentMembers > 0 ? '<div class="online-indicator"></div>' : ''}
                        </div>
                        <div class="conversation-info">
                            <div class="conversation-name">
                                <span>${this.escapeHtml(room.roomName)}</span>
                                <span class="conversation-time">${timeAgo}</span>
                            </div>
                            <div class="conversation-preview">${this.escapeHtml(room.lastMessage || 'No messages')}</div>
                        </div>
                        ${room.unreadCount > 0 ? `<div class="unread-badge">${room.unreadCount}</div>` : ''}
                    </div>
                `;
            }).join('');

        } catch (error) {
            console.error('Load conversations error:', error);
            conversationList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <div class="empty-state-text">Failed to load conversations</div>
                </div>
            `;
        }
    }

    async sendFriendRequest(userId, buttonElement) {
        try {
            buttonElement.disabled = true;
            buttonElement.innerHTML = '<span class="loading-spinner-small"></span>';

            const response = await fetch(`${API_URL}/api/friends/request`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ friendId: userId })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send friend request');
            }

            buttonElement.innerHTML = `
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
                Sent
            `;
            buttonElement.classList.add('sent');

        } catch (error) {
            console.error('Send friend request error:', error);
            buttonElement.disabled = false;
            buttonElement.innerHTML = `
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="8.5" cy="7" r="4"/>
                    <line x1="20" y1="8" x2="20" y2="14"/>
                    <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
                Add
            `;
            alert(error.message || 'Failed to send friend request');
        }
    }

    async loadFriends() {
        const conversationList = document.getElementById('conversationList');
        if (!conversationList) return;

        conversationList.innerHTML = '<div class="loading-spinner"></div>';

        try {
            const response = await fetch(`${API_URL}/api/friends/list`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) throw new Error('Failed to load friends');

            const friends = await response.json();

            if (friends.length === 0) {
                conversationList.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">👥</div>
                        <div class="empty-state-text">No friends yet</div>
                    </div>
                `;
                return;
            }

            conversationList.innerHTML = friends.map(friend => `
                <div class="conversation-item" onclick="chatApp.startDirectMessage(${friend.friendId})">
                    <div class="conversation-avatar">
                        ${this.getInitial(friend.displayName)}
                        ${friend.isOnline ? '<div class="online-indicator"></div>' : ''}
                    </div>
                    <div class="conversation-info">
                        <div class="conversation-name">
                            <span>${this.escapeHtml(friend.displayName)}</span>
                            <span class="conversation-time">${friend.isOnline ? 'Online' : 'Offline'}</span>
                        </div>
                        <div class="conversation-preview">@${this.escapeHtml(friend.username)}</div>
                    </div>
                </div>
            `).join('');

        } catch (error) {
            console.error('Load friends error:', error);
            conversationList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <div class="empty-state-text">Failed to load friends</div>
                </div>
            `;
        }
    }

    toggleSidebar() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('mobileOverlay');

        if (sidebar) sidebar.classList.toggle('active');
        if (overlay) overlay.classList.toggle('active');
    }

    closeSidebar() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('mobileOverlay');

        if (sidebar) sidebar.classList.remove('active');
        if (overlay) overlay.classList.remove('active');
    }

    handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;

        this.showToast(`File upload: ${file.name}`, 'info');
        // TODO: Implement file upload to server
    }

    logout() {
        if (confirm('Are you sure you want to logout?')) {
            if (this.ws) this.ws.close();
            localStorage.clear();
            window.location.href = '/';
        }
    }

    scrollToBottom() {
        const container = document.getElementById('messagesContainer');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }

    getInitial(name) {
        return name ? name.charAt(0).toUpperCase() : '?';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showToast(message, type = 'info') {
        const container = document.getElementById('toastContainer');
        if (!container) return;

        const icons = {
            success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/></svg>',
            error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/></svg>',
            warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><path d="M12 9v4M12 17h.01"/></svg>',
            info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>'
        };

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-icon">${icons[type]}</div>
            <span class="toast-message">${message}</span>
        `;

        container.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('toast-out');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    getRoomAvatar(roomType) {
        const avatars = {
            'PUBLIC': '🌍',
            'PRIVATE': '🔒',
            'SECRET': '🔐'
        };
        return avatars[roomType] || '💬';
    }

    getTimeAgo(timestamp) {
        const now = new Date();
        const time = new Date(timestamp);
        const diff = Math.floor((now - time) / 1000);

        if (diff < 60) return 'Just now';
        if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
        if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
        if (diff < 604800) return `${Math.floor(diff / 86400)}d ago`;

        return time.toLocaleDateString('en-US');
    }

    startDirectMessage(friendId) {
        this.showToast('Direct messaging coming soon', 'info');
        // TODO: Implement direct messaging
    }

    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
        }
    }

    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
        }
    }

    openSettings() {
        // Populate settings with current user info
        const displayNameInput = document.getElementById('settingsDisplayName');
        const usernameSpan = document.getElementById('settingsUsername');
        const emailSpan = document.getElementById('settingsEmail');

        if (displayNameInput) displayNameInput.value = this.displayName;
        if (usernameSpan) usernameSpan.textContent = this.username;

        // Get user info from localStorage
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        if (emailSpan) emailSpan.textContent = user.email || '-';

        this.openModal('settingsModal');
    }

    async createRoom(event) {
        event.preventDefault();

        const roomName = document.getElementById('roomName').value.trim();
        const description = document.getElementById('roomDescription').value.trim();
        const roomType = document.getElementById('roomType').value;
        const maxMembers = parseInt(document.getElementById('maxMembers').value) || 100;

        if (!roomName) {
            this.showToast('Please enter a room name', 'error');
            return;
        }

        const createBtn = document.querySelector('#createRoomForm button[type="submit"]');
        if (createBtn) {
            createBtn.disabled = true;
            createBtn.textContent = 'Creating...';
        }

        try {
            const response = await fetch(`${API_URL}/api/rooms`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify({
                    roomName,
                    description,
                    roomType,
                    maxMembers
                })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to create room');
            }

            const room = await response.json();
            this.showToast('Room created successfully', 'success');
            this.closeModal('createRoomModal');

            // Reset form
            document.getElementById('createRoomForm').reset();

            // Reload conversations and join new room
            await this.loadConversations();
            this.joinRoom(room.roomId);

        } catch (error) {
            console.error('Create room error:', error);
            this.showToast(error.message || 'Failed to create room', 'error');
        } finally {
            if (createBtn) {
                createBtn.disabled = false;
                createBtn.textContent = 'Create Room';
            }
        }
    }

    async handleSearch(query) {
        if (!query || query.trim().length < 2) {
            const activeTab = document.querySelector('.tab-btn.active');
            if (activeTab) {
                this.switchTab(activeTab.dataset.tab);
            }
            return;
        }

        const conversationList = document.getElementById('conversationList');
        if (!conversationList) return;

        const activeTab = document.querySelector('.tab-btn.active');
        const tabName = activeTab ? activeTab.dataset.tab : 'chats';

        try {
            let endpoint = '';
            if (tabName === 'friends') {
                endpoint = `/api/friends/search?query=${encodeURIComponent(query)}`;
            } else {
                endpoint = `/api/rooms/search?keyword=${encodeURIComponent(query)}`;
            }

            const response = await fetch(`${API_URL}${endpoint}`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) throw new Error('Search failed');

            const results = await response.json();

            if (results.length === 0) {
                conversationList.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">🔍</div>
                        <div class="empty-state-text">No results found</div>
                    </div>
                `;
                return;
            }

            if (tabName === 'friends') {
                conversationList.innerHTML = results.map(user => `
                    <div class="conversation-item search-result-item" data-user-id="${user.userId}">
                        <div class="conversation-avatar">
                            ${this.getInitial(user.displayName)}
                            ${user.isOnline ? '<div class="online-indicator"></div>' : ''}
                        </div>
                        <div class="conversation-info">
                            <div class="conversation-name">
                                <span>${this.escapeHtml(user.displayName)}</span>
                                <span class="conversation-time">${user.isOnline ? 'Online' : 'Offline'}</span>
                            </div>
                            <div class="conversation-preview">@${this.escapeHtml(user.username)}</div>
                        </div>
                        <button class="add-friend-btn" onclick="event.stopPropagation(); chatApp.sendFriendRequest(${user.userId}, this)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                                <circle cx="8.5" cy="7" r="4"/>
                                <line x1="20" y1="8" x2="20" y2="14"/>
                                <line x1="23" y1="11" x2="17" y2="11"/>
                            </svg>
                            Add
                        </button>
                    </div>
                `).join('');
            } else {
                conversationList.innerHTML = results.map(room => `
                    <div class="conversation-item" onclick="chatApp.joinRoom('${room.roomId}')">
                        <div class="conversation-avatar">
                            ${this.getRoomAvatar(room.roomType)}
                        </div>
                        <div class="conversation-info">
                            <div class="conversation-name">
                                <span>${this.escapeHtml(room.roomName)}</span>
                                <span class="conversation-time">${room.currentMembers}/${room.maxMembers}</span>
                            </div>
                            <div class="conversation-preview">${this.escapeHtml(room.description || 'No description')}</div>
                        </div>
                    </div>
                `).join('');
            }

        } catch (error) {
            console.error('Search error:', error);
            conversationList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <div class="empty-state-text">Search failed</div>
                </div>
            `;
        }
    }
}

// Initialize chat app
let chatApp;
document.addEventListener('DOMContentLoaded', () => {
    chatApp = new ChatApp();

    // Check if tour mode is active
    if (localStorage.getItem('tourMode') === 'true' || window.location.search.includes('tour=true')) {
        setTimeout(() => {
            chatApp.startTour();
        }, 1000); // Wait for UI to load
    }
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (chatApp && chatApp.ws) {
        chatApp.ws.close();
    }
});

// ===========================
// Tour / Tutorial System
// ===========================
class TourGuide {
    constructor(chatApp) {
        this.chatApp = chatApp;
        this.currentStep = 0;
        this.overlay = null;
        this.spotlight = null;
        this.tooltip = null;

        this.steps = [
            {
                target: '.sidebar',
                title: 'Sidebar Navigation',
                content: 'This is your main navigation area. Here you can browse your chats, friends list, and chat rooms.',
                position: 'right'
            },
            {
                target: '.sidebar-search',
                title: 'Search',
                content: 'Quickly find conversations, friends, or rooms using the search bar.',
                position: 'right'
            },
            {
                target: '.sidebar-tabs',
                title: 'Tab Navigation',
                content: 'Switch between Chats, Friends, and Rooms tabs to organize your conversations.',
                position: 'right'
            },
            {
                target: '#createRoomBtn',
                title: 'Create New Room',
                content: 'Click here to create a new chat room. You can make it public, private, or secret!',
                position: 'right'
            },
            {
                target: '.user-profile',
                title: 'Your Profile',
                content: 'View your profile status and access settings from here.',
                position: 'top'
            },
            {
                target: '.chat-header',
                title: 'Chat Header',
                content: 'See who you\'re chatting with and access call features and room settings.',
                position: 'bottom'
            },
            {
                target: '.messages-container',
                title: 'Message Area',
                content: 'All your messages appear here. Scroll up to see older messages.',
                position: 'left'
            },
            {
                target: '.input-area',
                title: 'Send Messages',
                content: 'Type your message here and press Enter to send. Use Shift+Enter for new lines. You can also attach files!',
                position: 'top'
            },
            {
                target: '#settingsBtn',
                title: 'Settings',
                content: 'Access your account settings, change display name, and manage your profile.',
                position: 'top'
            },
            {
                target: '#logoutBtn',
                title: 'Logout',
                content: 'When you\'re done, click here to safely log out of your account.',
                position: 'bottom'
            }
        ];
    }

    start() {
        this.createTourElements();
        this.addTourBadge();
        this.showStep(0);
    }

    createTourElements() {
        // Remove existing elements
        this.cleanup();

        // Create overlay
        this.overlay = document.createElement('div');
        this.overlay.className = 'tour-overlay';
        this.overlay.id = 'tourOverlay';
        document.body.appendChild(this.overlay);

        // Create spotlight
        this.spotlight = document.createElement('div');
        this.spotlight.className = 'tour-spotlight';
        this.spotlight.id = 'tourSpotlight';
        document.body.appendChild(this.spotlight);

        // Create tooltip
        this.tooltip = document.createElement('div');
        this.tooltip.className = 'tour-tooltip';
        this.tooltip.id = 'tourTooltip';
        document.body.appendChild(this.tooltip);
    }

    addTourBadge() {
        const chatName = document.getElementById('chatName');
        if (chatName && !document.querySelector('.tour-mode-badge')) {
            const badge = document.createElement('span');
            badge.className = 'tour-mode-badge';
            badge.innerHTML = `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polygon points="10,8 16,12 10,16 10,8"/>
                </svg>
                Tour Mode
            `;
            chatName.parentElement.appendChild(badge);
        }
    }

    showStep(stepIndex) {
        if (stepIndex < 0 || stepIndex >= this.steps.length) {
            this.endTour();
            return;
        }

        this.currentStep = stepIndex;
        const step = this.steps[stepIndex];
        const target = document.querySelector(step.target);

        if (!target) {
            // Skip to next step if target not found
            this.showStep(stepIndex + 1);
            return;
        }

        // Show overlay
        this.overlay.classList.add('active');

        // Position spotlight
        const rect = target.getBoundingClientRect();
        const padding = 8;

        this.spotlight.style.top = (rect.top - padding) + 'px';
        this.spotlight.style.left = (rect.left - padding) + 'px';
        this.spotlight.style.width = (rect.width + padding * 2) + 'px';
        this.spotlight.style.height = (rect.height + padding * 2) + 'px';

        // Build tooltip content
        this.tooltip.innerHTML = `
            <div class="tour-tooltip-arrow ${this.getArrowPosition(step.position)}"></div>
            <div class="tour-header">
                <span class="tour-step-badge">${stepIndex + 1}</span>
                <span class="tour-title">${step.title}</span>
            </div>
            <div class="tour-content">${step.content}</div>
            <div class="tour-progress">
                ${this.steps.map((_, i) => `
                    <span class="tour-progress-dot ${i < stepIndex ? 'completed' : ''} ${i === stepIndex ? 'active' : ''}"></span>
                `).join('')}
            </div>
            <div class="tour-actions">
                ${stepIndex > 0 ? `
                    <button class="btn btn-secondary" onclick="chatApp.tour.showStep(${stepIndex - 1})">
                        Previous
                    </button>
                ` : `
                    <button class="btn btn-secondary tour-skip" onclick="chatApp.tour.endTour()">
                        Skip Tour
                    </button>
                `}
                <button class="btn btn-primary" onclick="chatApp.tour.${stepIndex === this.steps.length - 1 ? 'endTour' : `showStep(${stepIndex + 1})`}()">
                    ${stepIndex === this.steps.length - 1 ? 'Finish' : 'Next'}
                </button>
            </div>
        `;

        // Position tooltip
        this.positionTooltip(rect, step.position);

        // Show tooltip
        setTimeout(() => {
            this.tooltip.classList.add('active');
        }, 100);
    }

    getArrowPosition(position) {
        const opposite = {
            'top': 'bottom',
            'bottom': 'top',
            'left': 'right',
            'right': 'left'
        };
        return opposite[position] || 'top';
    }

    positionTooltip(targetRect, position) {
        const tooltipWidth = 360;
        const tooltipHeight = this.tooltip.offsetHeight || 250;
        const margin = 16;

        let top, left;

        switch (position) {
            case 'top':
                top = targetRect.top - tooltipHeight - margin;
                left = targetRect.left + (targetRect.width / 2) - (tooltipWidth / 2);
                break;
            case 'bottom':
                top = targetRect.bottom + margin;
                left = targetRect.left + (targetRect.width / 2) - (tooltipWidth / 2);
                break;
            case 'left':
                top = targetRect.top + (targetRect.height / 2) - (tooltipHeight / 2);
                left = targetRect.left - tooltipWidth - margin;
                break;
            case 'right':
                top = targetRect.top + (targetRect.height / 2) - (tooltipHeight / 2);
                left = targetRect.right + margin;
                break;
            default:
                top = targetRect.bottom + margin;
                left = targetRect.left;
        }

        // Keep tooltip within viewport
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        if (left < margin) left = margin;
        if (left + tooltipWidth > viewportWidth - margin) left = viewportWidth - tooltipWidth - margin;
        if (top < margin) top = margin;
        if (top + tooltipHeight > viewportHeight - margin) top = viewportHeight - tooltipHeight - margin;

        this.tooltip.style.top = top + 'px';
        this.tooltip.style.left = left + 'px';
    }

    endTour() {
        // Cleanup tour mode
        localStorage.removeItem('tourMode');
        localStorage.removeItem('tourStep');

        // Remove URL parameter
        if (window.location.search.includes('tour=true')) {
            window.history.replaceState({}, '', window.location.pathname);
        }

        // Hide elements with animation
        if (this.tooltip) this.tooltip.classList.remove('active');

        setTimeout(() => {
            if (this.overlay) this.overlay.classList.remove('active');
            this.cleanup();

            // Show completion message
            this.chatApp.showToast('Tour completed! Enjoy BEAM!', 'success');
        }, 300);
    }

    cleanup() {
        // Remove tour elements
        const overlay = document.getElementById('tourOverlay');
        const spotlight = document.getElementById('tourSpotlight');
        const tooltip = document.getElementById('tourTooltip');
        const badge = document.querySelector('.tour-mode-badge');

        if (overlay) overlay.remove();
        if (spotlight) spotlight.remove();
        if (tooltip) tooltip.remove();
        if (badge) badge.remove();

        this.overlay = null;
        this.spotlight = null;
        this.tooltip = null;
    }
}

// Add tour method to ChatApp
ChatApp.prototype.startTour = function() {
    this.tour = new TourGuide(this);
    this.tour.start();
};

ChatApp.prototype.endTour = function() {
    if (this.tour) {
        this.tour.endTour();
    }
};
