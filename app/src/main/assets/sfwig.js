// Injected into every Instagram page. Hides (instead of removing) the doom-scrolling entry points
// whenever the page changes, and adds the app's chat gestures. Removing nodes that React owns
// breaks Instagram's navigation, and re-evaluating on every change means something hidden too
// early (like a chat's Back button) is shown again once the page has loaded.
(function () {
    if (window.__sfwig) return;
    window.__sfwig = true;

    // Rules that don't depend on how deep things are nested are plain CSS
    var style = document.createElement('style');
    style.textContent = [
        // Explore keeps only the search bar (results drop down inside it), not the reels grid
        'html[data-sfwig-explore] main[role="main"] > :not(nav) { display: none !important; }',
        // The feed title ("< Following") links back to the "For you" feed
        'header a[href="/"] svg[aria-label="Back"] { display: none !important; }',
        'header a[href="/"] { pointer-events: none !important; }',
        // Long press on a message reacts, so don't start text selection or the system menu
        'html[data-sfwig-chat] [aria-label="Double tap to like"] {' +
        ' -webkit-user-select: none !important; user-select: none !important; -webkit-touch-callout: none !important; }'
    ].join('\n');
    (document.head || document.documentElement).appendChild(style);

    var APP_PROMO = /^(use the app|open app|open the app|get the app|open instagram|switch to the app)$/i;
    var DISMISS = /^(not now|close|cancel|continue on web|stay on web|use the website|not interested)$/i;

    var hidden = [];

    function ancestor(el, levels) {
        while (el && levels-- > 0) el = el.parentElement;
        return el;
    }

    // Only ever hide small items, never something that holds the page itself (login form,
    // cookie dialog, feed) even while it is still loading
    function isSafe(target) {
        return target && target !== document.body && target !== document.documentElement &&
            !target.querySelector('main, [role="main"], [role="dialog"], article, form, input, textarea') &&
            target.getElementsByTagName('*').length * 2 < document.body.getElementsByTagName('*').length;
    }

    function collect(label, levels, out) {
        document.querySelectorAll('[aria-label="' + label + '"]').forEach(function (el) {
            // Skip icons inside something already hidden, like the old remove() did
            if (out.some(function (t) { return t.contains(el); })) return;
            var target = ancestor(el, levels);
            if (isSafe(target)) out.push(target);
        });
    }

    function press(el) {
        el.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    }

    // "Use the app" prompts: dismiss them if they are a dialog, otherwise hide the banner
    function collectAppPromos(out) {
        document.querySelectorAll('a, button, [role="button"]').forEach(function (el) {
            if (!APP_PROMO.test(el.textContent.trim())) return;
            var dialog = el.closest('[role="dialog"]');
            if (dialog) {
                if (dialog.__sfwigDismissed) return;
                dialog.__sfwigDismissed = true;
                var dismiss = Array.prototype.find.call(dialog.querySelectorAll('a, button, [role="button"]'), function (b) {
                    return DISMISS.test(b.textContent.trim()) || b.querySelector('[aria-label="Close"]');
                });
                if (dismiss) press(dismiss);
                else document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
                return;
            }
            var banner = el;
            for (var e = el; e && e !== document.body; e = e.parentElement) {
                var position = getComputedStyle(e).position;
                if (position === 'fixed' || position === 'sticky') { banner = e; break; }
            }
            if (isSafe(banner)) out.push(banner);
        });
    }

    function update() {
        var path = location.pathname;
        document.documentElement.toggleAttribute('data-sfwig-explore', path.indexOf('/explore') === 0);
        document.documentElement.toggleAttribute('data-sfwig-chat', path.indexOf('/direct/') === 0);

        var targets = [];
        collect('Reels', 8, targets);
        collect('Home', 9, targets);

        // The DM inbox header has a Back arrow to the home feed. Chats keep theirs, and the feed
        // title's arrow is handled by the CSS above.
        var inConversation = path.indexOf('/direct/t/') === 0 ||
            document.querySelector('[aria-label="Conversation information"]');
        if (!inConversation && !document.querySelector('[aria-label="Notifications"]')) collect('Back', 5, targets);

        collectAppPromos(targets);

        hidden.forEach(function (el) {
            if (targets.indexOf(el) < 0) el.style.removeProperty('display');
        });
        targets.forEach(function (el) {
            el.style.setProperty('display', 'none', 'important');
        });
        hidden = targets;
    }

    var pending = false;
    new MutationObserver(function () {
        if (pending) return;
        pending = true;
        setTimeout(function () {
            pending = false;
            update();
        }, 100);
    }).observe(document, { childList: true, subtree: true });

    // Chat gestures. Each message row holds the bubble and a hover-only toolbar with React,
    // Reply and More buttons, which a touchscreen can never reveal, so press them directly.
    function bubbleAt(target) {
        return location.pathname.indexOf('/direct/') === 0 && target.closest &&
            target.closest('[aria-label="Double tap to like"]');
    }

    function messageAction(bubble, prefix) {
        var row = bubble.closest('[role="group"]');
        return row && row.querySelector('[role="button"][aria-label^="' + prefix + '"]');
    }

    function react(bubble, emoji) {
        var button = messageAction(bubble, 'React to message');
        if (!button) return;
        press(button);
        if (!emoji) return;
        var tries = 0;
        (function pick() {
            var dialogs = document.querySelectorAll('[role="dialog"]');
            var picker = dialogs[dialogs.length - 1];
            var choice = picker && Array.prototype.find.call(picker.querySelectorAll('[role="button"]'), function (b) {
                return b.textContent.trim() === emoji;
            });
            if (choice) press(choice);
            else if (++tries < 20) setTimeout(pick, 50);
        })();
    }

    // Double tap: like with a heart (Instagram's own double tap doesn't respond to touch)
    window.addEventListener('dblclick', function (e) {
        var bubble = bubbleAt(e.target);
        if (!bubble) return;
        e.stopImmediatePropagation();
        e.preventDefault();
        react(bubble, '\u2764\uFE0F');
    }, true);

    window.addEventListener('contextmenu', function (e) {
        if (bubbleAt(e.target)) e.preventDefault();
    }, true);

    // Long press: open the reaction picker. Swipe right: reply.
    var touch = null;
    var swallowTap = false;

    window.addEventListener('touchstart', function (e) {
        swallowTap = false;
        var bubble = e.touches.length === 1 && bubbleAt(e.target);
        if (!bubble) { touch = null; return; }
        touch = { bubble: bubble, x: e.touches[0].clientX, y: e.touches[0].clientY, dx: 0 };
        touch.timer = setTimeout(function () {
            if (touch && touch.dx < 10) { swallowTap = true; react(touch.bubble); touch = null; }
        }, 500);
    }, { capture: true, passive: true });

    // With the system menu suppressed, lifting the finger after a long press counts as a tap,
    // which would also open the message (e.g. a shared reel). Drop that tap, but not our own clicks.
    window.addEventListener('click', function (e) {
        if (e.isTrusted && swallowTap) {
            swallowTap = false;
            e.stopImmediatePropagation();
            e.preventDefault();
        }
    }, true);

    window.addEventListener('touchmove', function (e) {
        if (!touch) return;
        var dx = e.touches[0].clientX - touch.x, dy = e.touches[0].clientY - touch.y;
        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) clearTimeout(touch.timer);
        if (Math.abs(dy) > 30 && touch.dx < 20) { endSwipe(); return; }
        touch.dx = Math.max(0, dx);
        touch.bubble.style.transform = 'translateX(' + Math.min(touch.dx, 80) + 'px)';
    }, { capture: true, passive: true });

    function endSwipe() {
        if (!touch) return;
        clearTimeout(touch.timer);
        var bubble = touch.bubble;
        if (touch.dx > 60) {
            var reply = messageAction(bubble, 'Reply to message');
            if (reply) press(reply);
        }
        bubble.style.transition = 'transform 150ms';
        bubble.style.transform = '';
        setTimeout(function () { bubble.style.transition = ''; }, 150);
        touch = null;
    }

    window.addEventListener('touchend', endSwipe, true);
    window.addEventListener('touchcancel', endSwipe, true);
})();
