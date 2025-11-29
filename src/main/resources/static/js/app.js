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

// Quick Start Function - Guest Login
async function quickStart() {
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
window.quickStart = quickStart;
window.toggleMobileMenu = toggleMobileMenu;
