/**
 * Duolingo Anti-Nag Script Injection
 * - Enforces removeNativeAppBanner
 * - Blocks app store redirects and window.open calls
 * - Actively removes mobile app install popups and restores scrolling
 */
(function() {
    'use strict';

    if (window.__duo_anti_nag_injected) return;
    window.__duo_anti_nag_injected = true;

    // 1. Enforce removeNativeAppBanner on window.duo
    try {
        let _duo = window.duo || {};
        _duo.removeNativeAppBanner = true;
        Object.defineProperty(window, 'duo', {
            get: function() { return _duo; },
            set: function(val) {
                if (val && typeof val === 'object') {
                    val.removeNativeAppBanner = true;
                }
                _duo = val;
            },
            configurable: true
        });
    } catch (e) {
        console.warn('[DuolingoApp] Error setting window.duo:', e);
    }

    // 2. Intercept window.open calls targeting app stores or intents
    try {
        const originalOpen = window.open;
        window.open = function(url, target, features) {
            if (typeof url === 'string') {
                const lower = url.toLowerCase();
                if (lower.includes('play.google.com') ||
                    lower.includes('apps.apple.com') ||
                    lower.includes('adjust.com') ||
                    lower.startsWith('market:') ||
                    lower.startsWith('intent:')) {
                    console.log('[DuolingoApp] Blocked window.open to store:', url);
                    return null;
                }
            }
            return originalOpen.apply(this, arguments);
        };
    } catch (e) {
        console.warn('[DuolingoApp] Error overriding window.open:', e);
    }

    // 3. Inject CSS Stylesheet if not already present
    function injectStyles() {
        if (document.getElementById('duo-anti-nag-styles')) return;
        const style = document.createElement('style');
        style.id = 'duo-anti-nag-styles';
        style.textContent = `
            ._3CwWq, ._3yEI4, .smartbanner, [class*="smartbanner"], [class*="smart-banner"],
            [name="apple-itunes-app"], div[data-test*="app-banner"],
            div[data-test*="open-in-app"], div[data-test*="download-the-app"],
            a[href*="play.google.com/store/apps/details?id=com.duolingo"],
            a[href*="apps.apple.com"], a[href*="adjust.com"],
            a[href^="market://"], a[href^="intent://"] {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                opacity: 0 !important;
                pointer-events: none !important;
            }
            div:has(> a[href*="play.google.com"]),
            div:has(> a[href*="apps.apple.com"]),
            div:has(> a[href*="app.adjust.com"]),
            div:has(> a[href^="market://"]),
            div:has(> a[href^="intent://"]) {
                display: none !important;
                height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
            }
            #overlays:has(a[href*="play.google.com"]),
            #overlays:has(a[href*="app.adjust.com"]),
            #overlays:has(a[href^="market://"]) {
                display: none !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }
            * { -webkit-tap-highlight-color: transparent !important; }
        `;
        (document.head || document.documentElement).appendChild(style);
    }

    if (document.head || document.documentElement) {
        injectStyles();
    } else {
        document.addEventListener('DOMContentLoaded', injectStyles);
    }

    // 4. Clean up install prompts from DOM and restore body scrolling
    const INSTALL_PROMPT_REGEX = /install (the )?app|download (the )?app|let[’']s install the app|continue in app|install duolingo|get the duolingo app|open in app/i;

    function cleanPrompts() {
        // Remove top banner element if found
        const topBanner = document.querySelector('._3CwWq, ._3yEI4');
        if (topBanner) {
            topBanner.remove();
        }

        // Check overlays container
        const overlays = document.getElementById('overlays');
        if (overlays && overlays.children.length > 0) {
            for (let i = 0; i < overlays.children.length; i++) {
                const child = overlays.children[i];
                const text = child.textContent || '';
                const hasStoreLink = child.querySelector('a[href*="play.google.com"], a[href*="adjust.com"], a[href^="market:"], a[href^="intent:"]');

                if (hasStoreLink || INSTALL_PROMPT_REGEX.test(text)) {
                    console.log('[DuolingoApp] Removed app install overlay prompt');
                    child.remove();
                    // Restore body scroll
                    if (document.body) {
                        document.body.style.overflow = '';
                        document.body.style.position = '';
                    }
                    if (document.documentElement) {
                        document.documentElement.style.overflow = '';
                    }
                }
            }
        }

        // Check for any standalone modals or dialogs in body
        const dialogs = document.querySelectorAll('[role="dialog"], [data-test="modal"]');
        dialogs.forEach(dialog => {
            const text = dialog.textContent || '';
            const hasStoreLink = dialog.querySelector('a[href*="play.google.com"], a[href*="adjust.com"], a[href^="market:"], a[href^="intent:"]');
            if (hasStoreLink || (INSTALL_PROMPT_REGEX.test(text) && text.length < 500)) {
                console.log('[DuolingoApp] Removed standalone dialog prompt');
                dialog.remove();
                if (document.body) {
                    document.body.style.overflow = '';
                    document.body.style.position = '';
                }
            }
        });

        // If trapped on web_to_app onboarding URL, redirect directly to /learn
        if (window.location.search.includes('context=webToApp') || window.location.pathname === '/nojs/splash') {
            window.location.href = 'https://www.duolingo.com/learn';
        }
    }

    // Run cleanPrompts on DOM changes using MutationObserver
    const observer = new MutationObserver(function(mutations) {
        cleanPrompts();
    });

    function startObserver() {
        cleanPrompts();
        if (document.body) {
            observer.observe(document.body, { childList: true, subtree: true });
        } else {
            observer.observe(document.documentElement, { childList: true, subtree: true });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startObserver);
    } else {
        startObserver();
    }

    // Periodic sweep for safety
    setInterval(cleanPrompts, 1500);

})();
