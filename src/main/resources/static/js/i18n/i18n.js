/**
 * BEAM Internationalization (i18n) Module
 * Supports: Korean (ko), English (en), Japanese (ja)
 */
class I18n {
    constructor() {
        this.locale = localStorage.getItem('beam-locale') || this.detectBrowserLanguage();
        this.translations = {};
        this.fallbackLocale = 'en';
        this.supportedLocales = ['ko', 'en', 'ja'];
        this.loaded = false;
    }

    /**
     * Detect browser language and return supported locale
     */
    detectBrowserLanguage() {
        const browserLang = navigator.language || navigator.userLanguage;
        const shortLang = browserLang.split('-')[0].toLowerCase();

        if (this.supportedLocales.includes(shortLang)) {
            return shortLang;
        }
        return 'ko'; // Default to Korean
    }

    /**
     * Initialize i18n - load translations
     */
    async init() {
        try {
            await this.loadLocale(this.locale);
            this.loaded = true;
            this.translatePage();
            return true;
        } catch (error) {
            console.error('Failed to initialize i18n:', error);
            // Try fallback locale
            if (this.locale !== this.fallbackLocale) {
                try {
                    await this.loadLocale(this.fallbackLocale);
                    this.locale = this.fallbackLocale;
                    this.loaded = true;
                    this.translatePage();
                    return true;
                } catch (fallbackError) {
                    console.error('Failed to load fallback locale:', fallbackError);
                }
            }
            return false;
        }
    }

    /**
     * Load locale JSON file
     */
    async loadLocale(locale) {
        if (!this.supportedLocales.includes(locale)) {
            throw new Error(`Unsupported locale: ${locale}`);
        }

        const response = await fetch(`/js/i18n/locales/${locale}.json`);
        if (!response.ok) {
            throw new Error(`Failed to load locale: ${locale}`);
        }

        this.translations[locale] = await response.json();
    }

    /**
     * Get translation for key
     * @param {string} key - Dot-notation key (e.g., 'auth.login')
     * @param {object} params - Optional parameters for interpolation
     */
    t(key, params = {}) {
        const keys = key.split('.');
        let value = this.translations[this.locale];

        for (const k of keys) {
            if (value && typeof value === 'object' && k in value) {
                value = value[k];
            } else {
                // Try fallback locale
                value = this.getFallbackValue(keys);
                break;
            }
        }

        if (typeof value !== 'string') {
            console.warn(`Translation not found: ${key}`);
            return key; // Return key as fallback
        }

        // Interpolate parameters: {{name}} -> value
        return value.replace(/\{\{(\w+)\}\}/g, (match, paramKey) => {
            return params[paramKey] !== undefined ? params[paramKey] : match;
        });
    }

    /**
     * Get value from fallback locale
     */
    getFallbackValue(keys) {
        if (!this.translations[this.fallbackLocale]) {
            return null;
        }

        let value = this.translations[this.fallbackLocale];
        for (const k of keys) {
            if (value && typeof value === 'object' && k in value) {
                value = value[k];
            } else {
                return null;
            }
        }
        return value;
    }

    /**
     * Change current locale
     */
    async setLocale(locale) {
        if (!this.supportedLocales.includes(locale)) {
            console.error(`Unsupported locale: ${locale}`);
            return false;
        }

        if (!this.translations[locale]) {
            try {
                await this.loadLocale(locale);
            } catch (error) {
                console.error(`Failed to load locale: ${locale}`, error);
                return false;
            }
        }

        this.locale = locale;
        localStorage.setItem('beam-locale', locale);
        this.translatePage();

        // Dispatch event for components to update
        window.dispatchEvent(new CustomEvent('localeChanged', { detail: { locale } }));

        return true;
    }

    /**
     * Get current locale
     */
    getLocale() {
        return this.locale;
    }

    /**
     * Get all supported locales with names
     */
    getSupportedLocales() {
        return [
            { code: 'ko', name: '한국어', nativeName: '한국어' },
            { code: 'en', name: 'English', nativeName: 'English' },
            { code: 'ja', name: 'Japanese', nativeName: '日本語' }
        ];
    }

    /**
     * Translate all elements with data-i18n attribute
     */
    translatePage() {
        // Translate text content
        document.querySelectorAll('[data-i18n]').forEach(element => {
            const key = element.getAttribute('data-i18n');
            const translation = this.t(key);
            if (translation !== key) {
                element.textContent = translation;
            }
        });

        // Translate placeholders
        document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
            const key = element.getAttribute('data-i18n-placeholder');
            const translation = this.t(key);
            if (translation !== key) {
                element.placeholder = translation;
            }
        });

        // Translate titles (tooltips)
        document.querySelectorAll('[data-i18n-title]').forEach(element => {
            const key = element.getAttribute('data-i18n-title');
            const translation = this.t(key);
            if (translation !== key) {
                element.title = translation;
            }
        });

        // Translate aria-labels
        document.querySelectorAll('[data-i18n-aria]').forEach(element => {
            const key = element.getAttribute('data-i18n-aria');
            const translation = this.t(key);
            if (translation !== key) {
                element.setAttribute('aria-label', translation);
            }
        });

        // Update HTML lang attribute
        document.documentElement.lang = this.locale;
    }

    /**
     * Format date according to locale
     */
    formatDate(date, options = {}) {
        const defaultOptions = {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        };
        return new Date(date).toLocaleDateString(this.locale, { ...defaultOptions, ...options });
    }

    /**
     * Format time according to locale
     */
    formatTime(date, options = {}) {
        const defaultOptions = {
            hour: '2-digit',
            minute: '2-digit'
        };
        return new Date(date).toLocaleTimeString(this.locale, { ...defaultOptions, ...options });
    }

    /**
     * Format relative time (e.g., "5 minutes ago")
     */
    formatRelativeTime(date) {
        const now = new Date();
        const diff = now - new Date(date);
        const seconds = Math.floor(diff / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (seconds < 60) {
            return this.t('time.justNow');
        } else if (minutes < 60) {
            return this.t('time.minutesAgo', { count: minutes });
        } else if (hours < 24) {
            return this.t('time.hoursAgo', { count: hours });
        } else if (days < 7) {
            return this.t('time.daysAgo', { count: days });
        } else {
            return this.formatDate(date);
        }
    }

    /**
     * Format number according to locale
     */
    formatNumber(number, options = {}) {
        return new Intl.NumberFormat(this.locale, options).format(number);
    }
}

// Create global instance
const i18n = new I18n();

// Export for ES modules (if needed)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { I18n, i18n };
}
