/* Presentation-only interactions: responsive navigation and accessible form controls. */
(() => {
  'use strict';
  const initialize = () => {
    const sidebar = document.getElementById('app-sidebar');
    const toggle = document.querySelector('.menu-toggle');
    const overlay = document.querySelector('.sidebar-overlay');
    const mobile = window.matchMedia('(max-width: 991px)');
    const setMenu = (open, restoreFocus = false) => {
      if (!sidebar || !toggle || !overlay) return;
      document.body.classList.toggle('sidebar-open', open);
      toggle.setAttribute('aria-expanded', String(open));
      toggle.setAttribute('aria-label', open ? 'Đóng menu điều hướng' : 'Mở menu điều hướng');
      overlay.hidden = !open;
      sidebar.inert = mobile.matches && !open;
      document.querySelector('.app-main')?.setAttribute('data-menu-open', String(open));
      if (open) sidebar.querySelector('a')?.focus();
      else if (restoreFocus) toggle.focus();
    };
    if (sidebar && toggle && overlay) {
      setMenu(false);
      toggle.addEventListener('click', () => setMenu(!document.body.classList.contains('sidebar-open')));
      overlay.addEventListener('click', () => setMenu(false, true));
      sidebar.addEventListener('click', event => {
        if (event.target.closest('a') && mobile.matches) setMenu(false);
      });
      mobile.addEventListener('change', () => setMenu(false));
      document.addEventListener('keydown', event => {
        if (!document.body.classList.contains('sidebar-open')) return;
        if (event.key === 'Escape') { setMenu(false, true); return; }
        if (event.key !== 'Tab') return;
        const links = [...sidebar.querySelectorAll('a[href], button:not([disabled])')];
        if (!links.length) return;
        const first = links[0], last = links[links.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      });
    }
    document.querySelectorAll('input[type="password"]').forEach(input => {
      const wrapper = document.createElement('div');
      wrapper.className = 'password-field';
      input.before(wrapper);
      wrapper.append(input);
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'password-toggle';
      button.setAttribute('aria-label', 'Hiện mật khẩu');
      button.setAttribute('aria-pressed', 'false');
      const icon = document.createElement('i');
      icon.className = 'far fa-eye';
      icon.setAttribute('aria-hidden', 'true');
      button.append(icon);
      button.addEventListener('click', () => {
        const visible = input.type === 'password';
        input.type = visible ? 'text' : 'password';
        button.setAttribute('aria-label', visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
        button.setAttribute('aria-pressed', String(visible));
        icon.className = visible ? 'far fa-eye-slash' : 'far fa-eye';
      });
      wrapper.append(button);
      if (!input.hasAttribute('autocomplete')) input.autocomplete = input.id === 'oldPassword' || document.getElementById('loginForm') ? 'current-password' : 'new-password';
    });
    const names = { 'fa-eye': 'Xem chi tiết', 'fa-edit': 'Chỉnh sửa', 'fa-trash': 'Xóa', 'fa-ban': 'Hủy', 'fa-check': 'Xác nhận', 'fa-times': 'Đóng' };
    document.querySelectorAll('button, a.btn').forEach(control => {
      if (control.textContent.trim() || control.hasAttribute('aria-label')) return;
      const name = control.title || Object.entries(names).find(([icon]) => control.querySelector('.' + icon))?.[1];
      if (name) { control.setAttribute('aria-label', name); if (!control.title) control.title = name; }
    });
    document.querySelectorAll('.table-responsive').forEach(region => {
      region.tabIndex = 0;
      region.setAttribute('role', 'region');
      region.setAttribute('aria-label', 'Bảng dữ liệu, cuộn ngang để xem thêm cột');
    });
    document.querySelectorAll('#loginBtn, #registerBtn').forEach(button => {
      const original = button.innerHTML;
      window.addEventListener('pageshow', () => { button.disabled = false; button.innerHTML = original; });
    });
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize);
  else initialize();
})();
