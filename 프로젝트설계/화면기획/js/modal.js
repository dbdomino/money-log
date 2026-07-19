/**
 * money-log modal helper — copy to money-app/src/main/resources/static/js/modal.js
 */
(function (global) {
  function qs(sel, root) {
    return (root || document).querySelector(sel);
  }
  function qsa(sel, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(sel));
  }

  function openModal(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.classList.add("is-open");
    el.setAttribute("aria-hidden", "false");
    document.body.classList.add("is-modal-open");
    var focusable = qs("button, [href], input, select, textarea", el);
    if (focusable) focusable.focus();
  }

  function closeModal(el) {
    if (!el) return;
    el.classList.remove("is-open");
    el.setAttribute("aria-hidden", "true");
    if (!document.querySelector(".modal-root.is-open")) {
      document.body.classList.remove("is-modal-open");
    }
  }

  function closeAll() {
    qsa(".modal-root.is-open").forEach(closeModal);
    document.body.classList.remove("is-modal-open");
  }

  function initDeeplink() {
    var params = new URLSearchParams(global.location.search);
    var m = params.get("m");
    if (!m) return;
    var mapAttr = document.body.getAttribute("data-modal-map");
    if (!mapAttr) return;
    try {
      var map = JSON.parse(mapAttr);
      if (map[m]) openModal(map[m]);
    } catch (e) {
      /* ignore */
    }
  }

  document.addEventListener("click", function (e) {
    var openBtn = e.target.closest("[data-modal-open]");
    if (openBtn) {
      e.preventDefault();
      openModal(openBtn.getAttribute("data-modal-open"));
      return;
    }
    var closeBtn = e.target.closest("[data-modal-close]");
    if (closeBtn) {
      e.preventDefault();
      closeModal(closeBtn.closest(".modal-root"));
      return;
    }
    if (e.target.classList.contains("modal-backdrop")) {
      closeModal(e.target.closest(".modal-root"));
    }
  });

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") closeAll();
  });

  document.addEventListener("DOMContentLoaded", initDeeplink);

  global.MoneyLogModal = { open: openModal, close: closeModal, closeAll: closeAll };
})(window);
