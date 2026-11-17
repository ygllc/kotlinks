/*
 * Copyright (C) 2026 YG LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

const searchInput = document.getElementById('shortcut-search');
const autocomplete = document.querySelector('m3e-autocomplete[for="shortcut-search"]');

// M3E options display "go/<shortcut>" and their value defaults to that
// text, so filtering works against what the user sees (typing "mm" or
// "go/mm" both match). Map the text back to a go-link URL.
function toGoUrl(text) {
  let t = (text || '').trim();
  if (t.toLowerCase().startsWith('go/')) t = t.slice(3);
  t = t.replace(/^\/+/, '');
  if (!t) return '/';
  return '/' + t.split('/').map(encodeURIComponent).join('/');
}

function navigateTo(text) {
  const url = toGoUrl(text);
  if (url !== '/') window.location.href = url;
}

// M3E emits `change` when the user picks an option (click or keyboard).
// Navigating on menu close instead would also fire when the user simply
// clicks away with an auto-activated option selected.
autocomplete?.addEventListener('change', () => {
  const value = autocomplete.value || searchInput?.value || '';
  navigateTo(value);
});

// Enter: prefer the selected/first visible option, otherwise treat the
// typed text as a shortcut so "mm/gh" + Enter works without a menu pick.
searchInput?.addEventListener('keydown', (ev) => {
  if (ev.key !== 'Enter') return;
  const selected = autocomplete?.querySelector('m3e-option[selected]');
  const firstVisible = !selected
    ? Array.from(autocomplete?.querySelectorAll('m3e-option') || [])
        .find(opt => opt.style.display !== 'none' && !opt.disabled)
    : null;
  const target = selected?.value || firstVisible?.value || searchInput.value;
  if (target) {
    ev.preventDefault();
    navigateTo(target);
  }
});

document.getElementById('reload-btn')?.addEventListener('click', () => {
  // go to /reload which triggers server-side reload and renders the reload page
  window.location.href = '/reload';
});

function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3000);
}
