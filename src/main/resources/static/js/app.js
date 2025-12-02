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
        errorDiv.textContent = 'Please fill in all fields';
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span>Signing in...</span>';

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
            throw new Error(data.error || data.message || 'Login failed');
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
        errorDiv.textContent = err.message || 'Login failed. Please check your credentials.';
        btn.disabled = false;
        btn.innerHTML = '<span>Sign In</span>';
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
        errorDiv.textContent = 'Please fill in all fields';
        return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errorDiv.textContent = 'Please enter a valid email address';
        return;
    }

    if (username.length < 3 || username.length > 20) {
        errorDiv.textContent = 'Username must be 3-20 characters';
        return;
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        errorDiv.textContent = 'Username can only contain letters, numbers, and underscores';
        return;
    }

    if (password.length < 6) {
        errorDiv.textContent = 'Password must be at least 6 characters';
        return;
    }

    if (password !== confirmPassword) {
        errorDiv.textContent = 'Passwords do not match';
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span>Creating account...</span>';

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
            throw new Error(data.error || data.message || 'Registration failed');
        }

        // Store email for verification
        pendingVerificationEmail = email;

        // Show verification form
        showVerifyEmailForm(email);

    } catch (err) {
        console.error('Register error:', err);
        errorDiv.textContent = err.message || 'Registration failed. Please try again.';
        btn.disabled = false;
        btn.innerHTML = '<span>Create Account</span>';
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
    if (verifyMessage) verifyMessage.textContent = `Enter the 6-digit code sent to ${email}`;

    clearAuthErrors();
}

// Handle email verification
async function handleVerifyEmail(e) {
    e.preventDefault();

    const code = document.getElementById('verificationCode').value.trim();
    const errorDiv = document.getElementById('verifyError');
    const btn = document.getElementById('verifyBtn');

    if (!code || code.length !== 6) {
        errorDiv.textContent = 'Please enter a 6-digit code';
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span>Verifying...</span>';

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
            throw new Error(data.error || data.message || 'Verification failed');
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
        errorDiv.textContent = err.message || 'Verification failed. Please try again.';
        btn.disabled = false;
        btn.innerHTML = '<span>Verify</span>';
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
            throw new Error(data.error || data.message || 'Failed to resend code');
        }

        errorDiv.style.color = 'var(--success)';
        errorDiv.textContent = 'Verification code sent!';
        setTimeout(() => {
            errorDiv.style.color = '';
            errorDiv.textContent = '';
        }, 3000);

    } catch (err) {
        console.error('Resend error:', err);
        errorDiv.textContent = err.message || 'Failed to resend code';
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
            throw new Error('Login failed');
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
            error.querySelector('span').textContent = 'Connection failed. Please try again.';
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
    console.log('BEAM Application Initialized');

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
    }
});
