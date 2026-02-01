/* ===========================
   BEAM - Main Application JS
   =========================== */

const API_URL = window.location.origin;

// Navbar Scroll Effect
window.addEventListener('scroll', () => {
    const navbar = document.getElementById('navbar');
    if (navbar) {
        navbar.classList.toggle('scrolled', window.scrollY > 50);
    }
});

// Smooth Scroll for Navigation Links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Mobile Menu Toggle
function toggleMobileMenu() {
    const mobileMenu = document.getElementById('mobileMenu');
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');

    if (mobileMenu && mobileMenuBtn) {
        mobileMenu.classList.toggle('active');
        mobileMenuBtn.classList.toggle('active');
    }
}

// ===========================
// Auth Modal Functions
// ===========================

function openLoginModal() {
    const modal = document.getElementById('authModal');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
    if (loginForm) loginForm.style.display = 'block';
    if (registerForm) registerForm.style.display = 'none';

    // Clear previous errors
    clearAuthErrors();
}

function openRegisterModal() {
    const modal = document.getElementById('authModal');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
    if (loginForm) loginForm.style.display = 'none';
    if (registerForm) registerForm.style.display = 'block';

    // Clear previous errors
    clearAuthErrors();
}

function closeAuthModal() {
    const modal = document.getElementById('authModal');
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
    clearAuthErrors();
}

function showLoginForm(e) {
    if (e) e.preventDefault();
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (loginForm) loginForm.style.display = 'block';
    if (registerForm) registerForm.style.display = 'none';
    clearAuthErrors();
}

function showRegisterForm(e) {
    if (e) e.preventDefault();
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (loginForm) loginForm.style.display = 'none';
    if (registerForm) registerForm.style.display = 'block';
    clearAuthErrors();
}

function clearAuthErrors() {
    const loginError = document.getElementById('loginError');
    const registerError = document.getElementById('registerError');
    if (loginError) loginError.textContent = '';
    if (registerError) registerError.textContent = '';
}

// Handle Login
async function handleLogin(e) {
    e.preventDefault();

    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errorDiv = document.getElementById('loginError');
    const btn = document.getElementById('loginBtn');

    if (!username || !password) {
        errorDiv.textContent = i18n.t('error.required');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = `<span>${i18n.t('common.loading')}</span>`;

    try {
        const response = await fetch(`${API_URL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || data.message || i18n.t('error.loginFailed'));
        }

        // Store auth data
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            id: data.userId,
            username: data.username,
            displayName: data.displayName
        }));
        localStorage.setItem('username', data.username);
        localStorage.setItem('displayName', data.displayName);
        if (data.defaultRoomId) {
            localStorage.setItem('defaultRoomId', data.defaultRoomId);
        }

        // Redirect to chat
        window.location.href = '/chat.html';

    } catch (err) {
        console.error('Login error:', err);
        errorDiv.textContent = err.message || i18n.t('error.loginFailed');
        btn.disabled = false;
        btn.innerHTML = `<span>${i18n.t('auth.signIn')}</span>`;
    }
}

// Pending verification email
let pendingVerificationEmail = '';

// Handle Register
async function handleRegister(e) {
    e.preventDefault();

    const email = document.getElementById('registerEmail').value.trim();
    const username = document.getElementById('registerUsername').value.trim();
    const displayName = document.getElementById('registerDisplayName').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('registerConfirmPassword').value;
    const errorDiv = document.getElementById('registerError');
    const btn = document.getElementById('registerBtn');

    // Validation
    if (!email || !username || !displayName || !password || !confirmPassword) {
        errorDiv.textContent = i18n.t('error.required');
        return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errorDiv.textContent = i18n.t('error.invalidEmail');
        return;
    }

    if (username.length < 3 || username.length > 20) {
        errorDiv.textContent = i18n.t('error.usernameTooShort');
        return;
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        errorDiv.textContent = i18n.t('error.usernameInvalid');
        return;
    }

    if (password.length < 6) {
        errorDiv.textContent = i18n.t('error.passwordTooShort');
        return;
    }

    if (password !== confirmPassword) {
        errorDiv.textContent = i18n.t('error.passwordMismatch');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = `<span>${i18n.t('common.loading')}</span>`;

    try {
        const response = await fetch(`${API_URL}/api/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, username, displayName, password })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || data.message || i18n.t('error.registerFailed'));
        }

        // Store email for verification
        pendingVerificationEmail = email;

        // Show verification form
        showVerifyEmailForm(email);

    } catch (err) {
        console.error('Register error:', err);
        errorDiv.textContent = err.message || i18n.t('error.registerFailed');
        btn.disabled = false;
        btn.innerHTML = `<span>${i18n.t('auth.createAccount')}</span>`;
    }
}

// Show email verification form
function showVerifyEmailForm(email) {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const verifyForm = document.getElementById('verifyEmailForm');
    const verifyMessage = document.getElementById('verifyEmailMessage');

    if (loginForm) loginForm.style.display = 'none';
    if (registerForm) registerForm.style.display = 'none';
    if (verifyForm) verifyForm.style.display = 'block';
    if (verifyMessage) verifyMessage.textContent = i18n.t('auth.enterVerificationCode');

    clearAuthErrors();
}

// Handle email verification
async function handleVerifyEmail(e) {
    e.preventDefault();

    const code = document.getElementById('verificationCode').value.trim();
    const errorDiv = document.getElementById('verifyError');
    const btn = document.getElementById('verifyBtn');

    if (!code || code.length !== 6) {
        errorDiv.textContent = i18n.t('error.required');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = `<span>${i18n.t('common.loading')}</span>`;

    try {
        const response = await fetch(`${API_URL}/api/auth/email/verify`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: pendingVerificationEmail, code })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || data.message || i18n.t('error.verificationFailed'));
        }

        // Store auth data
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            id: data.userId,
            username: data.username,
            displayName: data.displayName
        }));
        localStorage.setItem('username', data.username);
        localStorage.setItem('displayName', data.displayName);

        // Redirect to chat
        window.location.href = '/chat.html';

    } catch (err) {
        console.error('Verification error:', err);
        errorDiv.textContent = err.message || i18n.t('error.verificationFailed');
        btn.disabled = false;
        btn.innerHTML = `<span>${i18n.t('auth.verify')}</span>`;
    }
}

// Resend verification code
async function resendVerificationCode() {
    const errorDiv = document.getElementById('verifyError');

    try {
        const response = await fetch(`${API_URL}/api/auth/email/resend`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: pendingVerificationEmail })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || data.message || i18n.t('error.verificationFailed'));
        }

        errorDiv.style.color = 'var(--success)';
        errorDiv.textContent = i18n.t('auth.verificationSent');
        setTimeout(() => {
            errorDiv.style.color = '';
            errorDiv.textContent = '';
        }, 3000);

    } catch (err) {
        console.error('Resend error:', err);
        errorDiv.textContent = err.message || i18n.t('error.verificationFailed');
    }
}

// Guest Login
async function guestLogin() {
    const button = document.getElementById('quickStartBtn');
    const loading = document.getElementById('loading');
    const error = document.getElementById('error');

    // UI State
    if (button) {
        button.disabled = true;
        button.style.opacity = '0.7';
    }
    if (loading) loading.classList.add('active');
    if (error) error.classList.remove('active');

    try {
        const response = await fetch(`${API_URL}/api/auth/guest`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(i18n.t('error.loginFailed'));
        }

        const data = await response.json();

        // Store auth data
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('username', data.user.username);
        localStorage.setItem('displayName', data.user.displayName);
        localStorage.setItem('defaultRoomId', data.defaultRoomId);

        // Redirect to chat
        window.location.href = '/chat.html';

    } catch (err) {
        console.error('Quick start error:', err);

        if (error) {
            error.querySelector('span').textContent = i18n.t('error.connectionFailed');
            error.classList.add('active');

            // Auto-hide error after 5 seconds
            setTimeout(() => {
                error.classList.remove('active');
            }, 5000);
        }

        if (button) {
            button.disabled = false;
            button.style.opacity = '1';
        }
        if (loading) loading.classList.remove('active');
    }
}

// Intersection Observer for Animations
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -100px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Initialize animations on DOM Load
document.addEventListener('DOMContentLoaded', () => {


    // Observe animated elements
    const animatedElements = document.querySelectorAll('.feature-card, .tech-item, .arch-layer, .security-card');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(20px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });

    // Add active state to current nav link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-links a').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });
});

// Expose functions to global scope for onclick handlers
window.toggleMobileMenu = toggleMobileMenu;
window.openLoginModal = openLoginModal;
window.openRegisterModal = openRegisterModal;
window.closeAuthModal = closeAuthModal;
window.showLoginForm = showLoginForm;
window.showRegisterForm = showRegisterForm;
window.handleLogin = handleLogin;
window.handleRegister = handleRegister;
window.handleVerifyEmail = handleVerifyEmail;
window.resendVerificationCode = resendVerificationCode;
window.guestLogin = guestLogin;

// Close modal on outside click
document.addEventListener('click', (e) => {
    const modal = document.getElementById('authModal');
    if (e.target === modal) {
        closeAuthModal();
    }
});

// Close modal on Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeAuthModal();
        closeTourWelcome();
    }
});

// ===========================
// Tour / Tutorial Functions
// ===========================

// Start tour - shows welcome modal first
function startTour() {
    // Create and show tour welcome modal
    showTourWelcome();
}

// Show tour welcome modal
function showTourWelcome() {
    // Remove existing tour welcome if any
    const existing = document.getElementById('tourWelcome');
    if (existing) existing.remove();

    const welcomeHTML = `
        <div class="tour-welcome active" id="tourWelcome">
            <div class="tour-overlay active"></div>
            <div class="tour-welcome-content">
                <div class="tour-welcome-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <polygon points="10,8 16,12 10,16 10,8"/>
                    </svg>
                </div>
                <h2>${i18n.t('tour.welcomeTitle')}</h2>
                <p>${i18n.t('tour.welcomeDescription')}</p>
                <div class="tour-welcome-features">
                    <span class="tour-feature-tag">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
                        </svg>
                        ${i18n.t('tour.realtimeChat')}
                    </span>
                    <span class="tour-feature-tag">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0110 0v4"/>
                        </svg>
                        ${i18n.t('tour.secureEncrypted')}
                    </span>
                    <span class="tour-feature-tag">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                            <circle cx="9" cy="7" r="4"/>
                            <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
                        </svg>
                        ${i18n.t('tour.teamCollaboration')}
                    </span>
                    <span class="tour-feature-tag">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
                        </svg>
                        ${i18n.t('tour.lightningFast')}
                    </span>
                </div>
                <div class="tour-welcome-actions">
                    <button class="btn btn-primary btn-lg" onclick="beginTour()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"/>
                            <polygon points="10,8 16,12 10,16 10,8"/>
                        </svg>
                        ${i18n.t('tour.startTour')}
                    </button>
                    <button class="btn btn-ghost" onclick="closeTourWelcome()">
                        ${i18n.t('tour.maybeLater')}
                    </button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', welcomeHTML);
    document.body.style.overflow = 'hidden';
}

// Close tour welcome modal
function closeTourWelcome() {
    const welcome = document.getElementById('tourWelcome');
    if (welcome) {
        welcome.classList.remove('active');
        setTimeout(() => welcome.remove(), 300);
        document.body.style.overflow = '';
    }
}

// Begin the actual tour - enters as guest and starts tutorial
async function beginTour() {
    closeTourWelcome();

    const loading = document.getElementById('loading');
    if (loading) loading.classList.add('active');

    try {
        // Login as guest
        const response = await fetch(`${API_URL}/api/auth/guest`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(i18n.t('tour.failedToStart'));
        }

        const data = await response.json();

        // Store auth data
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('username', data.user.username);
        localStorage.setItem('displayName', data.user.displayName);
        localStorage.setItem('defaultRoomId', data.defaultRoomId);

        // Set tour mode flag
        localStorage.setItem('tourMode', 'true');
        localStorage.setItem('tourStep', '0');

        // Redirect to chat with tour mode
        window.location.href = '/chat.html?tour=true';

    } catch (err) {
        console.error('Tour start error:', err);
        if (loading) loading.classList.remove('active');

        const error = document.getElementById('error');
        if (error) {
            error.querySelector('span').textContent = i18n.t('tour.failedToStart');
            error.classList.add('active');
            setTimeout(() => error.classList.remove('active'), 5000);
        }
    }
}

// Expose tour functions
window.startTour = startTour;
window.showTourWelcome = showTourWelcome;
window.closeTourWelcome = closeTourWelcome;
window.beginTour = beginTour;
