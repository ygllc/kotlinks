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

autocomplete?.addEventListener('change', () => {
  const value = autocomplete.value || searchInput.value || '';
  let t = value.trim();
  if (t.toLowerCase().startsWith('go/')) t = t.slice(3);
  t = t.replace(/^\/+/, '');
  if (t) {
    window.location.href = '/' + t.split('/').map(encodeURIComponent).join('/');
  }
});

document.getElementById('go-home').addEventListener('click', () => {
  window.location.href = '/';
});
