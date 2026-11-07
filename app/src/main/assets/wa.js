(function () {
  if (window.__waGuard) return;
  window.__waGuard = true;
  var NAMES = ['status', 'channels', 'updates'];
  var ICON = /^(status|newsletter|channels?)(-outline|-refreshed|-filled|-unread|-active)?$/;
  var BUTTONISH = 'button,[role="button"],[role="tab"],a';
  var timer = null;

  function sweep() {
    timer = null;
    var els = document.querySelectorAll('[aria-label],[data-icon]');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      var label = (el.getAttribute('aria-label') || '').trim().toLowerCase();
      var icon = (el.getAttribute('data-icon') || '').toLowerCase();
      var isNav = NAMES.indexOf(label) >= 0 && el.matches(BUTTONISH);
      var isIcon = ICON.test(icon);
      if (!isNav && !isIcon) continue;
      var target = isNav ? el : el.closest(BUTTONISH);
      if (target && target.style.display !== 'none') {
        target.style.setProperty('display', 'none', 'important');
        window.__waHidden = (window.__waHidden || 0) + 1;
      }
    }
  }
  function later() { if (!timer) timer = setTimeout(sweep, 300); }

  sweep();
  new MutationObserver(later).observe(document.documentElement, { childList: true, subtree: true });
})();
