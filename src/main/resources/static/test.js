
    const userRole = 'ROLE_ADMIN';
    let partsData = [], bookingsData = [], workOrdersData = [], invoicesData = [], suppliersData = [], inquiriesData = [];
    let chartBook = null, chartInv = null;
    let currentSection = 'dashboard';
    let adminName = 'Admin', adminEmail = '';

    /* â”€â”€ Boot â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    document.addEventListener('DOMContentLoaded', async () => {
      await fetchProfile();
      await loadData();
      connectWS();
    });

    /* â”€â”€ Profile â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    async function fetchProfile() {
      try {
        const r = await fetch('/api/auth/me', { credentials: 'include' });
        if (r.status === 401 || r.status === 403) { window.location.href = '/admin-login.html'; return; }
        if (r.ok && (r.headers.get('content-type') || '').includes('json')) {
          const u = await r.json();
          adminName = u.name || 'Admin';
          adminEmail = u.email || '';
          document.getElementById('sb-name').textContent = adminName;
          document.getElementById('sb-av').textContent = adminName.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
          const sel = document.getElementById('session-email');
          if (sel) sel.textContent = adminEmail;
        }
      } catch (e) { console.warn('Profile:', e) }
    }

    /* â”€â”€ Load All Data â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    async function loadData() {
      try {
        const [pr, br, wr, ir, sr] = await Promise.all([
          safeGet('/api/parts'), safeGet('/api/bookings'),
          safeGet('/api/work-orders'), safeGet('/api/invoices'), safeGet('/api/suppliers')
        ]);
        partsData = pr || []; bookingsData = br || []; workOrdersData = wr || [];
        invoicesData = ir || []; suppliersData = sr || [];
        renderAll();
        loadInquiries(); // Also refresh inquiries
      } catch (e) {
        console.error('Load error:', e);
        toast('Load Error', 'Could not reach server. Check Spring Boot is running.', 'err');
      }
    }

    async function safeGet(url) {
      const r = await fetch(url, { credentials: 'include' });
      if (r.status === 401 || r.status === 403) { window.location.href = '/admin-login.html'; return null; }
      if (!r.ok) return null;
      const ct = r.headers.get('content-type') || '';
      return ct.includes('json') ? r.json() : null;
    }

    function renderAll() {
      renderDashboard();
      if (currentSection === 'workorders') renderWOTable();
      if (currentSection === 'parts') renderPartsTable();
      if (currentSection === 'invoices') renderInvTable();
      if (currentSection === 'suppliers') renderSupTable();
      updateNotifications();
    }

    /* â”€â”€ Dashboard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function renderDashboard() {
      const pending = bookingsData.filter(b => b.status === 'PENDING').length;
      const activeWO = workOrdersData.filter(w => w.status !== 'COMPLETED').length;
      const unpaid = invoicesData.filter(i => i.paymentStatus === 'UNPAID').length;
      animN('d-parts', partsData.length);
      animN('d-bookings', bookingsData.length);
      animN('d-pending', pending);
      animN('d-wo', activeWO);
      animN('d-unpaid', unpaid);
      renderBookChart(); renderInvChart(); renderActivity(); renderLowStock();
    }

    function animN(id, end, dur = 900) {
      const el = document.getElementById(id);
      if (!el) return;
      let start = null;
      const step = ts => {
        if (!start) start = ts;
        const p = Math.min((ts - start) / dur, 1);
        el.textContent = Math.floor(p * end).toLocaleString();
        if (p < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    }

    function renderBookChart() {
      const ctx = document.getElementById('chartBook');
      if (!ctx) return;
      if (chartBook) { chartBook.destroy(); chartBook = null; }
      const n = bookingsData.length;
      const pts = [12, 8, 5, 2, 0, -3, -5].map(o => Math.max(0, n - o));
      chartBook = new Chart(ctx, {
        type: 'line',
        data: {
          labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
          datasets: [{
            label: 'Bookings', data: pts,
            borderColor: '#f59e0b', backgroundColor: 'rgba(245,158,11,.06)',
            borderWidth: 2.5, pointBackgroundColor: '#fff', pointBorderColor: '#f59e0b',
            pointBorderWidth: 2, pointRadius: 5, fill: true, tension: .42
          }]
        },
        options: {
          responsive: true, maintainAspectRatio: false,
          plugins: {
            legend: { display: false }, tooltip: {
              backgroundColor: '#0f172a', titleFont: { family: 'Inter', size: 12, weight: '700' },
              bodyFont: { family: 'Inter', size: 12 }, padding: 10, cornerRadius: 8
            }
          },
          scales: {
            y: { beginAtZero: true, ticks: { precision: 0, font: { family: 'Inter', size: 11 } }, grid: { color: 'rgba(0,0,0,.04)' } },
            x: { grid: { display: false }, ticks: { font: { family: 'Inter', size: 11 } } }
          }
        }
      });
    }

    function renderInvChart() {
      const wrap = document.getElementById('chartInvWrap');
      if (!wrap) return;
      if (chartInv) { chartInv.destroy(); chartInv = null; }
      const cats = {};
      partsData.forEach(p => { const c = p.category || 'Other'; cats[c] = (cats[c] || 0) + (p.stockQuantity || 0); });
      const lbls = Object.keys(cats), vals = Object.values(cats);
      if (!lbls.length) { wrap.innerHTML = '<div class="empty"><div class="empty-ico">ðŸ“¦</div>No data yet.</div>'; return; }
      if (!document.getElementById('chartInv')) wrap.innerHTML = '<div class="chart-wrap"><canvas id="chartInv"></canvas></div>';
      chartInv = new Chart(document.getElementById('chartInv'), {
        type: 'doughnut',
        data: {
          labels: lbls, datasets: [{
            data: vals,
            backgroundColor: ['#f59e0b', '#0ea5e9', '#8b5cf6', '#10b981', '#ef4444', '#f97316', '#64748b'],
            borderWidth: 0, hoverOffset: 8
          }]
        },
        options: {
          responsive: true, maintainAspectRatio: false, cutout: '68%',
          plugins: {
            legend: { position: 'right', labels: { boxWidth: 10, padding: 14, font: { family: 'Inter', size: 11 } } },
            tooltip: { backgroundColor: '#0f172a', titleFont: { family: 'Inter', size: 12 }, bodyFont: { family: 'Inter', size: 12 }, padding: 10, cornerRadius: 8 }
          }
        }
      });
    }

    function renderActivity() {
      const host = document.getElementById('act-feed');
      if (!host) return;
      const items = [...bookingsData].slice(0, 10);
      if (!items.length) { host.innerHTML = '<div class="empty"><div class="empty-ico">ðŸ“‹</div>No bookings yet.</div>'; return; }
      host.innerHTML = items.map((b, i) => `
    <div class="act-item" style="animation:slideR .3s ${i * .06}s both">
      <div class="act-ico">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
      </div>
      <div>
        <div class="act-title">Booking from <strong>${h(b.customerName)}</strong></div>
        <div class="act-sub">${h(b.serviceType)} &bull; ${h(b.preferredDate || 'TBD')} &bull; <span class="badge b-${b.status}">${b.status}</span></div>
      </div>
    </div>`).join('');
    }

    function renderLowStock() {
      const host = document.getElementById('low-feed');
      const badge = document.getElementById('low-badge');
      if (!host) return;
      const low = partsData.filter(p => (p.stockQuantity || 0) < 10);
      if (badge) badge.textContent = low.length ? `${low.length} items` : '';
      if (!partsData.length) { host.innerHTML = '<div class="empty"><div class="empty-ico">ðŸ“¦</div>No parts loaded.</div>'; return; }
      if (!low.length) { host.innerHTML = '<div class="empty" style="color:var(--green)"><div class="empty-ico">âœ…</div>All stock levels healthy!</div>'; return; }
      host.innerHTML = low.map(p => `
    <div class="alert-row">
      <div><div class="alert-name">${h(p.name)}</div><div class="alert-sub">${p.stockQuantity} units &bull; ${h(p.category || 'â€”')}</div></div>
      <button class="btn btn-danger btn-sm" onclick="openEditPart(${p.id})">Restock</button>
    </div>`).join('');
    }

    /* â”€â”€ Work Orders â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function renderWOTable() {
      const tb = document.getElementById('wo-tbody');
      if (!tb) return;
      if (!workOrdersData.length) { tb.innerHTML = '<tr><td colspan="9"><div class="empty"><div class="empty-ico">ðŸ› ï¸</div>No work orders yet.</div></td></tr>'; return; }
      tb.innerHTML = workOrdersData.map(wo => `
    <tr>
      <td style="color:var(--t3);font-size:11px;font-weight:600">#${wo.id}</td>
      <td><strong>#${wo.booking?.id}</strong> â€” ${h(wo.booking?.customerName || '?')}</td>
      <td style="max-width:130px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${h(wo.booking?.vehicleDetails || 'â€”')}</td>
      <td>${h(wo.mechanicName)}</td>
      <td>${h(wo.bayNumber || 'â€”')}</td>
      <td><strong>${wo.laborHours || 0}h</strong></td>
      <td><span class="badge b-${wo.status}">${wo.status}</span></td>
      <td><button class="btn btn-outline btn-sm" onclick="openWOPart(${wo.id})">+ Parts</button></td>
      <td>
        <div style="display:flex;gap:5px">
          <button class="btn btn-outline btn-sm" onclick="openEditWO(${wo.id})">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="generateInvoiceFor(${wo.id})">Invoice</button>
        </div>
      </td>
    </tr>`).join('');
    }

    async function submitWO() {
      const bookingId = document.getElementById('wo-booking').value;
      if (!bookingId) { toast('Error', 'Select a booking.', 'err'); return; }
      const body = {
        bookingId, mechanicName: document.getElementById('wo-mech').value.trim() || 'Unassigned',
        bayNumber: document.getElementById('wo-bay').value.trim(),
        laborHours: document.getElementById('wo-hours').value,
        notes: document.getElementById('wo-notes').value
      };
      const r = await apiPost('/api/work-orders', body);
      if (r) { workOrdersData.unshift(r); renderWOTable(); renderDashboard(); closeModal('mo-wo'); toast('Work Order Created', 'WO #' + r.id + ' created.', 'ok'); }
    }

    function openEditWO(id) {
      const wo = workOrdersData.find(w => w.id === id);
      if (!wo) return;
      document.getElementById('wo-edit-id').value = id;
      document.getElementById('wo-edit-status').value = wo.status;
      document.getElementById('wo-edit-hours').value = wo.laborHours || 0;
      document.getElementById('wo-edit-mech').value = wo.mechanicName || '';
      document.getElementById('wo-edit-bay').value = wo.bayNumber || '';
      document.getElementById('wo-edit-notes').value = wo.notes || '';
      openModal('mo-wo-edit');
    }

    async function submitWOEdit() {
      const id = document.getElementById('wo-edit-id').value;
      const body = {
        status: document.getElementById('wo-edit-status').value,
        laborHours: document.getElementById('wo-edit-hours').value,
        mechanicName: document.getElementById('wo-edit-mech').value,
        bayNumber: document.getElementById('wo-edit-bay').value,
        notes: document.getElementById('wo-edit-notes').value
      };
      const r = await apiPut('/api/work-orders/' + id + '/status', body);
      if (r) { workOrdersData = workOrdersData.map(w => w.id === r.id ? r : w); renderWOTable(); renderDashboard(); closeModal('mo-wo-edit'); toast('Updated', 'Work Order #' + id + ' updated.', 'ok'); }
    }

    function openWOPart(woId) {
      document.getElementById('wop-wo-id').value = woId;
      document.getElementById('mo-wop-id').textContent = woId;
      const sel = document.getElementById('wop-part-sel');
      sel.innerHTML = partsData.map(p => `<option value="${p.id}">${h(p.name)} (Stock: ${p.stockQuantity}) â€” $${parseFloat(p.price).toFixed(2)}</option>`).join('');
      openModal('mo-wo-part');
    }

    async function submitWOPart() {
      const woId = document.getElementById('wop-wo-id').value;
      const partId = document.getElementById('wop-part-sel').value;
      const qty = document.getElementById('wop-qty').value;
      const r = await fetch('/api/work-orders/' + woId + '/parts', { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ partId, quantity: qty }) });
      if (r.ok) { await loadData(); closeModal('mo-wo-part'); toast('Part Added', 'Part added to Work Order #' + woId + ' and stock deducted.', 'ok'); }
      else { const msg = await r.text(); toast('Error', msg || 'Failed to add part.', 'err'); }
    }

    async function generateInvoiceFor(woId) {
      if (!confirm('Generate invoice for Work Order #' + woId + '?')) return;
      const r = await fetch('/api/invoices/work-order/' + woId, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: '{}' });
      const data = r.ok ? await r.json() : null;
      if (data) { invoicesData.unshift(data); if (currentSection === 'invoices') renderInvTable(); renderDashboard(); toast('Invoice Generated', '#' + data.id + ' â€” $' + parseFloat(data.grandTotal).toFixed(2), 'ok'); }
      else { const t = await r.text().catch(() => ''); toast('Error', t || 'Failed.', 'err'); }
    }

    /* â”€â”€ Parts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function renderPartsTable() {
      const tb = document.getElementById('parts-tbody');
      if (!tb) return;
      if (!partsData.length) { tb.innerHTML = '<tr><td colspan="7"><div class="empty"><div class="empty-ico">ðŸ“¦</div>No parts yet.</div></td></tr>'; return; }
      tb.innerHTML = partsData.map(p => `
    <tr>
      <td style="color:var(--t3);font-size:11px;font-weight:600">#${p.id}</td>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          ${p.imageUrl ? `<img src="${p.imageUrl}" style="width:32px;height:32px;object-fit:cover;border-radius:4px;background:#1e293b">` : '<div style="width:32px;height:32px;border-radius:4px;background:#1e293b;display:flex;align-items:center;justify-content:center;color:var(--t3)">ðŸ“·</div>'}
          <strong>${h(p.name)}</strong>
        </div>
      </td>
      <td><span class="badge b-gray">${h(p.category || 'â€”')}</span></td>
      <td>$${parseFloat(p.price || 0).toFixed(2)}</td>
      <td><span class="badge ${p.stockQuantity < 10 ? 'b-red' : 'b-green'}">${p.stockQuantity}</span></td>
      <td>${p.stockQuantity < 5 ? '<span class="badge b-red">âš  Reorder</span>' : p.stockQuantity < 10 ? '<span class="badge b-orange">Low</span>' : '<span class="badge b-green">OK</span>'}</td>
      <td><div style="display:flex;gap:5px">
        <button class="btn btn-outline btn-sm" onclick="openEditPart(${p.id})">Edit</button>
        <button class="btn btn-danger btn-sm" onclick="deletePart(${p.id})">Delete</button>
      </div></td>
    </tr>`).join('');
    }

    function openAddPart() {
      document.getElementById('mo-part-title').textContent = 'Add New Part';
      document.getElementById('p-id').value = ''; document.getElementById('p-name').value = '';
      document.getElementById('p-cat').value = ''; document.getElementById('p-price').value = '';
      document.getElementById('p-stock').value = ''; document.getElementById('p-desc').value = '';
      document.getElementById('p-image-data').value = ''; document.getElementById('p-image-file').value = '';
      openModal('mo-part');
    }

    function openEditPart(id) {
      const p = partsData.find(x => x.id === id);
      if (!p) return;
      document.getElementById('mo-part-title').textContent = 'Edit Part';
      document.getElementById('p-id').value = p.id; document.getElementById('p-name').value = p.name;
      document.getElementById('p-cat').value = p.category; document.getElementById('p-price').value = p.price;
      document.getElementById('p-stock').value = p.stockQuantity; document.getElementById('p-desc').value = p.description || '';
      document.getElementById('p-image-data').value = p.imageUrl || ''; document.getElementById('p-image-file').value = '';
      openModal('mo-part');
    }

    function removePartImage() {
      document.getElementById('p-image-data').value = '';
      document.getElementById('p-image-file').value = '';
      toast('Image Removed', 'Image cleared. Click Save to apply.', 'ok');
    }

    async function submitPart() {
      const fileInput = document.getElementById('p-image-file');
      let imageUrl = document.getElementById('p-image-data').value || null;
      if (fileInput.files && fileInput.files[0]) {
        const file = fileInput.files[0];
        imageUrl = await new Promise((resolve, reject) => {
          const img = new Image();
          img.onload = () => {
            const canvas = document.createElement('canvas');
            const MAX = 800;
            let w = img.width, h = img.height;
            if (w > MAX || h > MAX) {
              if (w > h) { h *= MAX / w; w = MAX; }
              else { w *= MAX / h; h = MAX; }
            }
            canvas.width = w; canvas.height = h;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, w, h);
            resolve(canvas.toDataURL('image/jpeg', 0.8));
          };
          img.onerror = reject;
          img.src = URL.createObjectURL(file);
        });
      }

      const id = document.getElementById('p-id').value;
      const body = {
        name: document.getElementById('p-name').value.trim(), category: document.getElementById('p-cat').value,
        price: parseFloat(document.getElementById('p-price').value),
        stockQuantity: parseInt(document.getElementById('p-stock').value),
        description: document.getElementById('p-desc').value.trim(),
        imageUrl: imageUrl
      };
      const r = id ? await apiPut('/api/parts/' + id, body) : await apiPost('/api/parts', body);
      if (r) { partsData = id ? partsData.map(p => p.id === r.id ? r : p) : [r, ...partsData]; renderPartsTable(); renderDashboard(); closeModal('mo-part'); toast(id ? 'Part Updated' : 'Part Added', '"' + r.name + '" saved.', 'ok'); }
    }

    async function deletePart(id) {
      const p = partsData.find(x => x.id === id);
      if (!p || !confirm('Delete "' + p.name + '"?')) return;
      const r = await fetch('/api/parts/' + id, { method: 'DELETE', credentials: 'include' });
      if (r.ok) { partsData = partsData.filter(x => x.id !== id); renderPartsTable(); renderDashboard(); toast('Deleted', '"' + p.name + '" removed.', 'ok'); }
      else toast('Error', 'Could not delete part.', 'err');
    }

    /* â”€â”€ Invoices â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function renderInvTable() {
      const tb = document.getElementById('inv-tbody');
      if (!tb) return;
      if (!invoicesData.length) { tb.innerHTML = '<tr><td colspan="10"><div class="empty"><div class="empty-ico">ðŸ§¾</div>No invoices yet.</div></td></tr>'; return; }
      tb.innerHTML = invoicesData.map(inv => `
    <tr>
      <td style="color:var(--t3);font-size:11px;font-weight:600">#${inv.id}</td>
      <td>WO #${inv.workOrder?.id || 'â€”'}</td>
      <td><strong>${h(inv.workOrder?.booking?.customerName || 'â€”')}</strong></td>
      <td>$${parseFloat(inv.partsTotal || 0).toFixed(2)}</td>
      <td>$${parseFloat(inv.laborTotal || 0).toFixed(2)}</td>
      <td>$${parseFloat(inv.taxAmount || 0).toFixed(2)}</td>
      <td><strong>$${parseFloat(inv.grandTotal || 0).toFixed(2)}</strong></td>
      <td>
        <select class="btn btn-outline btn-sm" onchange="updateInvPayStatus(${inv.id},this.value)" style="cursor:pointer">
          <option value="UNPAID"  ${inv.paymentStatus === 'UNPAID' ? 'selected' : ''}>Unpaid</option>
          <option value="PARTIAL" ${inv.paymentStatus === 'PARTIAL' ? 'selected' : ''}>Partial</option>
          <option value="PAID"    ${inv.paymentStatus === 'PAID' ? 'selected' : ''}>Paid</option>
        </select>
      </td>
      <td>
        <select class="btn btn-outline btn-sm" onchange="updateInvPayMethod(${inv.id},this.value)" style="cursor:pointer">
          <option value="NONE"   ${inv.paymentMethod === 'NONE' ? 'selected' : ''}>â€”</option>
          <option value="CASH"   ${inv.paymentMethod === 'CASH' ? 'selected' : ''}>Cash</option>
          <option value="CARD"   ${inv.paymentMethod === 'CARD' ? 'selected' : ''}>Card</option>
          <option value="ONLINE" ${inv.paymentMethod === 'ONLINE' ? 'selected' : ''}>Online</option>
        </select>
      </td>
      <td><button class="btn btn-outline btn-sm" onclick="printInv(${inv.id})">Print</button></td>
    </tr>`).join('');
    }

    async function updateInvPayStatus(id, status) {
      const inv = invoicesData.find(i => i.id === id);
      const method = inv?.paymentMethod || 'NONE';
      const r = await apiPut('/api/invoices/' + id + '/payment', { paymentStatus: status, paymentMethod: method });
      if (r) { invoicesData = invoicesData.map(i => i.id === r.id ? r : i); renderDashboard(); toast('Invoice Updated', 'Payment status â†’ ' + status, 'ok'); }
    }
    async function updateInvPayMethod(id, method) {
      const inv = invoicesData.find(i => i.id === id);
      const status = inv?.paymentStatus || 'UNPAID';
      const r = await apiPut('/api/invoices/' + id + '/payment', { paymentStatus: status, paymentMethod: method });
      if (r) { invoicesData = invoicesData.map(i => i.id === r.id ? r : i); toast('Updated', 'Payment method â†’ ' + method, 'ok'); }
    }
    async function generateInvoice() {
      const woId = document.getElementById('inv-wo-sel').value;
      const notes = document.getElementById('inv-notes').value;
      if (!woId) { toast('Error', 'Select a work order.', 'err'); return; }
      const r = await fetch('/api/invoices/work-order/' + woId, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ notes }) });
      const data = r.ok ? await r.json() : null;
      if (data) { invoicesData.unshift(data); renderInvTable(); renderDashboard(); closeModal('mo-inv-gen'); toast('Invoice Generated', '#' + data.id + ' â€” $' + parseFloat(data.grandTotal).toFixed(2), 'ok'); }
      else { const t = await r.text().catch(() => ''); toast('Error', t || 'Failed.', 'err'); }
    }
    function printInv(id) { window.open('/api/invoices/' + id + '/print', '_blank'); }

    /* â”€â”€ Suppliers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function renderSupTable() {
      const tb = document.getElementById('sup-tbody');
      if (!tb) return;
      if (!suppliersData.length) { tb.innerHTML = '<tr><td colspan="7"><div class="empty"><div class="empty-ico">ðŸ¤</div>No suppliers yet.</div></td></tr>'; return; }
      tb.innerHTML = suppliersData.map(s => `
    <tr>
      <td style="color:var(--t3);font-size:11px;font-weight:600">#${s.id}</td>
      <td><strong>${h(s.name)}</strong></td>
      <td>${h(s.contactPerson || 'â€”')}</td>
      <td>${h(s.phone || 'â€”')}</td>
      <td>${h(s.email || 'â€”')}</td>
      <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${h(s.partsSupplied || 'â€”')}</td>
      <td><div style="display:flex;gap:5px">
        <button class="btn btn-outline btn-sm" onclick="openEditSup(${s.id})">Edit</button>
        <button class="btn btn-danger btn-sm" onclick="draftPO(${s.id})">PO</button>
        <button class="btn btn-danger btn-sm" onclick="deleteSup(${s.id})">Del</button>
      </div></td>
    </tr>`).join('');
    }
    function openAddSup() { document.getElementById('mo-sup-title').textContent = 'Add Supplier'; document.getElementById('sup-id').value = '';['sup-name', 'sup-cp', 'sup-ph', 'sup-em', 'sup-addr', 'sup-parts'].forEach(id => document.getElementById(id).value = ''); openModal('mo-sup'); }
    function openEditSup(id) {
      const s = suppliersData.find(x => x.id === id); if (!s) return;
      document.getElementById('mo-sup-title').textContent = 'Edit Supplier';
      document.getElementById('sup-id').value = s.id; document.getElementById('sup-name').value = s.name || '';
      document.getElementById('sup-cp').value = s.contactPerson || ''; document.getElementById('sup-ph').value = s.phone || '';
      document.getElementById('sup-em').value = s.email || ''; document.getElementById('sup-addr').value = s.address || '';
      document.getElementById('sup-parts').value = s.partsSupplied || ''; openModal('mo-sup');
    }
    async function submitSupplier() {
      const id = document.getElementById('sup-id').value;
      const body = {
        name: document.getElementById('sup-name').value.trim(), contactPerson: document.getElementById('sup-cp').value.trim(),
        phone: document.getElementById('sup-ph').value.trim(), email: document.getElementById('sup-em').value.trim(),
        address: document.getElementById('sup-addr').value.trim(), partsSupplied: document.getElementById('sup-parts').value.trim()
      };
      const r = id ? await apiPut('/api/suppliers/' + id, body) : await apiPost('/api/suppliers', body);
      if (r) { suppliersData = id ? suppliersData.map(s => s.id === r.id ? r : s) : [...suppliersData, r]; renderSupTable(); closeModal('mo-sup'); toast(id ? 'Supplier Updated' : 'Supplier Added', '"' + r.name + '" saved.', 'ok'); }
    }
    async function deleteSup(id) {
      const s = suppliersData.find(x => x.id === id); if (!s || !confirm('Delete supplier "' + s.name + '"?')) return;
      const r = await fetch('/api/suppliers/' + id, { method: 'DELETE', credentials: 'include' });
      if (r.ok) { suppliersData = suppliersData.filter(x => x.id !== id); renderSupTable(); toast('Deleted', '"' + s.name + '" removed.', 'ok'); }
      else toast('Error', 'Could not delete.', 'err');
    }
    function draftPO(id) {
      const s = suppliersData.find(x => x.id === id);
      const low = partsData.filter(p => (p.stockQuantity || 0) < 5);
      const poHTML = `<!DOCTYPE html><html><head><title>Purchase Order</title>
    <style>body{font-family:Arial;margin:40px;color:#1e293b}h1{color:#0f172a}
    table{width:100%;border-collapse:collapse;margin:20px 0}
    th{background:#0f172a;color:#fff;padding:10px}td{padding:8px;border-bottom:1px solid #eee}
    .btn{padding:10px 24px;background:#f59e0b;border:none;border-radius:6px;font-weight:700;cursor:pointer;display:inline-block;margin-bottom:20px}
    </style></head><body>
    <div onclick="window.print()" class="btn">ðŸ–¨ Print / Save PDF</div>
    <h1>PURCHASE ORDER â€” SED Motors</h1>
    <p><strong>To:</strong> ${h(s.name)}<br><strong>Contact:</strong> ${h(s.contactPerson || 'â€”')}<br><strong>Phone:</strong> ${h(s.phone || 'â€”')}</p>
    <table><tr><th>Part Description</th><th>Qty Requested</th></tr>
    ${low.length ? low.map(p => `<tr><td>${h(p.name)}</td><td>10</td></tr>`).join('') : '<tr><td colspan="2">No low stock items.</td></tr>'}
    </table><p>Authorized by: ${h(adminName)}<br>Date: ${new Date().toLocaleDateString()}</p></body></html>`;
      const w = window.open('', '_blank'); w.document.write(poHTML); w.document.close();
    }

    /* â”€â”€ Inquiries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    async function loadInquiries() {
      const data = await safeGet('/api/inquiries');
      inquiriesData = data || [];
      renderInqTable();
      updateInqBadge();
    }

    function renderInqTable() {
      const tb = document.getElementById('inq-tbody');
      if (!tb) return;
      if (!inquiriesData.length) {
        tb.innerHTML = '<tr><td colspan="8"><div class="empty"><div class="empty-ico">ðŸ’¬</div>No inquiries yet.</div></td></tr>';
        return;
      }
      tb.innerHTML = inquiriesData.map(inq => `
        <tr>
          <td style="color:var(--t3);font-size:11px;font-weight:600">#${inq.id}</td>
          <td><strong>${h(inq.customerName)}</strong></td>
          <td>${h(inq.email)}</td>
          <td>${h(inq.phone || 'â€”')}</td>
          <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${h(inq.subject)}">${h(inq.subject)}</td>
          <td style="font-size:11.5px;color:var(--t2)">${inq.createdAt ? inq.createdAt.replace('T',' ').substring(0,16) : 'â€”'}</td>
          <td><span class="badge ${inq.status === 'RESPONDED' ? 'b-green' : 'b-orange'}">${inq.status}</span></td>
          <td>
            <div style="display:flex;gap:5px">
              <button class="btn btn-primary btn-sm" onclick="openInquiryReply(${inq.id})">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:12px;height:12px"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                Reply
              </button>
              <button class="btn btn-danger btn-sm" onclick="deleteInquiry(${inq.id})">Del</button>
            </div>
          </td>
        </tr>`).join('');
    }

    function updateInqBadge() {
      const pending = inquiriesData.filter(i => i.status === 'PENDING').length;
      const badge = document.getElementById('inq-badge');
      if (badge) {
        badge.style.display = pending > 0 ? 'inline-block' : 'none';
        badge.textContent = pending;
      }
    }

    function openInquiryReply(id) {
      const inq = inquiriesData.find(x => x.id === id);
      if (!inq) return;
      document.getElementById('inq-reply-id').value = id;
      document.getElementById('inq-reply-context').innerHTML = `
        <strong>${h(inq.customerName)}</strong> &bull; ${h(inq.email)}<br>
        <strong>Subject:</strong> ${h(inq.subject)}<br>
        <em style="color:var(--t3);">${h(inq.message)}</em>`;
      document.getElementById('inq-reply-body').value = inq.adminReply || '';
      openModal('mo-inq-reply');
    }

    async function submitInquiryReply() {
      const id = document.getElementById('inq-reply-id').value;
      const reply = document.getElementById('inq-reply-body').value.trim();
      if (!reply) { toast('Error', 'Reply cannot be empty.', 'err'); return; }
      const r = await apiPost('/api/inquiries/' + id + '/reply', { reply });
      if (r) {
        inquiriesData = inquiriesData.map(i => i.id === r.id ? r : i);
        renderInqTable(); updateInqBadge();
        closeModal('mo-inq-reply');
        toast('Reply Sent!', 'Customer has been emailed your response.', 'ok');
      }
    }

    async function deleteInquiry(id) {
      if (!confirm('Delete this inquiry?')) return;
      const r = await fetch('/api/inquiries/' + id, { method: 'DELETE', credentials: 'include' });
      if (r.ok) {
        inquiriesData = inquiriesData.filter(i => i.id !== id);
        renderInqTable(); updateInqBadge();
        toast('Deleted', 'Inquiry removed.', 'ok');
      } else toast('Error', 'Could not delete.', 'err');
    }

    /* â”€â”€ Email Composer (for booking customers) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function openEmailComposer(bookingId) {
      const b = bookingsData.find(x => x.id === bookingId);
      if (!b) return;
      document.getElementById('email-compose-bid').value = bookingId;
      document.getElementById('email-compose-to').value = b.email ? `${b.customerName} <${b.email}>` : 'No email on file';
      document.getElementById('email-compose-subject').value = 'Regarding your service booking #' + bookingId;
      document.getElementById('email-compose-body').value = '';
      openModal('mo-email-compose');
    }

    async function sendComposedEmail() {
      const id = document.getElementById('email-compose-bid').value;
      const subject = document.getElementById('email-compose-subject').value.trim();
      const message = document.getElementById('email-compose-body').value.trim();
      if (!subject || !message) { toast('Error', 'Subject and message are required.', 'err'); return; }
      const r = await apiPost('/api/bookings/' + id + '/send-email', { subject, message });
      if (r) { closeModal('mo-email-compose'); toast('Email Sent!', 'Message delivered to customer.', 'ok'); }
    }

    /* â”€â”€ Audit Log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    async function loadAudit() {
      const data = await safeGet('/api/audit-logs');
      const tb = document.getElementById('audit-tbody');
      if (!tb) return;
      if (!data || !data.length) { tb.innerHTML = '<tr><td colspan="4"><div class="empty">No audit entries yet.</div></td></tr>'; return; }
      tb.innerHTML = data.map(l => `
    <tr>
      <td style="font-size:12px;color:var(--t2);white-space:nowrap">${l.timestamp ? l.timestamp.replace('T', ' ').substring(0, 19) : 'â€”'}</td>
      <td><strong>${h(l.actorName)}</strong></td>
      <td>${h(l.action)}</td>
      <td><span class="badge b-gray">${h(l.entityType || 'â€”')}</span></td>
    </tr>`).join('');
    }

    /* â”€â”€ Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    const tabMeta = {
      dashboard: 'Dashboard Overview', workorders: 'Work Orders & Service Bays',
      parts: 'Parts & Inventory', invoices: 'Invoices & Billing',
      suppliers: 'Suppliers & Purchase Orders', crm: 'Customer & Vehicle CRM',
      audit: 'Audit Logs & Settings', inquiries: 'Customer Inquiries & Communication'
    };

    function go(tab) {
      currentSection = tab;
      document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
      document.getElementById('sec-' + tab).classList.add('active');
      document.querySelectorAll('.ni').forEach(n => n.classList.remove('active'));
      const ni = document.getElementById('nav-' + tab);
      if (ni) ni.classList.add('active');
      document.getElementById('pg-title').textContent = tabMeta[tab] || tab;
      if (tab === 'workorders') { populateBookingSelect(); renderWOTable(); }
      if (tab === 'parts') renderPartsTable();
      if (tab === 'invoices') { populateWOSelect(); renderInvTable(); }
      if (tab === 'suppliers') renderSupTable();
      if (tab === 'audit') loadAudit();
      if (tab === 'inquiries') loadInquiries();
    }

    function populateBookingSelect() {
      const sel = document.getElementById('wo-booking');
      sel.innerHTML = '<option value="">â€” Select Booking â€”</option>' +
        bookingsData.map(b => `<option value="${b.id}">#${b.id} â€” ${h(b.customerName)} (${h(b.serviceType)})</option>`).join('');
    }
    function populateWOSelect() {
      const sel = document.getElementById('inv-wo-sel');
      const existingWoIds = invoicesData.map(i => i.workOrder?.id);
      const eligible = workOrdersData.filter(w => !existingWoIds.includes(w.id));
      sel.innerHTML = '<option value="">â€” Select Work Order â€”</option>' +
        eligible.map(w => `<option value="${w.id}">#${w.id} â€” ${h(w.booking?.customerName || '?')} (${w.status})</option>`).join('');
    }

    /* â”€â”€ WebSocket â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function connectWS() {
      try {
        const sock = new SockJS('/ws');
        const client = Stomp.over(sock);
        client.debug = null;
        client.connect({}, () => {
          setLive(true);
          client.subscribe('/topic/admin-notifications', msg => {
            try { const b = JSON.parse(msg.body); bookingsData.unshift(b); renderDashboard(); updateNotifications(); toast('New Booking!', b.customerName + ' â€” ' + b.serviceType, ''); } catch (e) { }
          });
        }, () => { setLive(false); setTimeout(connectWS, 5000); });
      } catch (e) { setLive(false); }
    }

    function setLive(on) {
      const pill = document.getElementById('live-pill');
      const txt = document.getElementById('live-txt');
      const ws = document.getElementById('ws-status-badge');
      const wst = document.getElementById('ws-status-txt');
      if (pill) pill.classList.toggle('off', !on);
      if (txt) txt.textContent = on ? 'Live' : 'Reconnectingâ€¦';
      if (ws) { ws.textContent = on ? 'Connected' : 'Disconnected'; ws.className = 'badge ' + (on ? 'b-green' : 'b-red'); }
      if (wst) wst.textContent = on ? 'SockJS/STOMP active' : 'Attempting reconnectâ€¦';
    }

    /* â”€â”€ Notifications â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function toggleNotifications() {
      const drop = document.getElementById('notif-dropdown');
      drop.style.display = drop.style.display === 'none' ? 'block' : 'none';
    }
    document.addEventListener('click', e => {
      if (!e.target.closest('#btn-notifications') && !e.target.closest('#notif-dropdown')) {
        const drop = document.getElementById('notif-dropdown');
        if (drop) drop.style.display = 'none';
      }
    });

    function updateNotifications() {
      const pending = bookingsData.filter(b => b.status === 'PENDING').sort((a, b) => b.id - a.id);
      const badge = document.getElementById('notif-badge');
      if (pending.length > 0) {
        badge.style.display = 'block'; badge.textContent = pending.length;
      } else {
        badge.style.display = 'none';
      }
      
      const list = document.getElementById('notif-list');
      if (pending.length === 0) {
        list.innerHTML = '<div class="empty" style="padding:20px;font-size:12px;">No pending bookings.</div>';
      } else {
        list.innerHTML = pending.map(b => `
          <div style="padding:12px;border-radius:8px;cursor:pointer;transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.04)'" onmouseout="this.style.background='none'" onclick="openReviewBooking(${b.id})">
            <div style="font-weight:700;font-size:13px;color:var(--t1);">${h(b.customerName)}</div>
            <div style="font-size:11.5px;color:var(--t2);margin-top:2px;">${h(b.serviceType)} â€¢ ${h(b.preferredDate || 'No date')}</div>
          </div>
        `).join('');
      }
    }

    function openReviewBooking(id) {
      document.getElementById('notif-dropdown').style.display = 'none';
      const b = bookingsData.find(x => x.id === id);
      if (!b) return;
      const waNum = b.phone ? b.phone.replace(/\D/g, '') : '';
      document.getElementById('mo-review-body').innerHTML = `
        <div style="font-size:13px;color:var(--t1);margin-bottom:16px;line-height:1.6;background:rgba(255,255,255,0.03);padding:14px;border-radius:12px;border:1px solid var(--bdr);">
          <p><span style="color:var(--t3);width:80px;display:inline-block">Customer:</span> <strong>${h(b.customerName)}</strong></p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Phone:</span> ${h(b.phone)}</p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Email:</span> ${h(b.email || 'â€”')}</p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Vehicle:</span> ${h(b.vehicleDetails)}</p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Reg No:</span> ${h(b.vehicleRegistration || 'â€”')}</p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Service:</span> ${h(b.serviceType)}</p>
          <p><span style="color:var(--t3);width:80px;display:inline-block">Date/Time:</span> ${h(b.preferredDate)} ${h(b.preferredTime || '')}</p>
          <p style="margin-top:8px;padding-top:8px;border-top:1px solid var(--bdr);"><strong style="color:var(--t3)">Message:</strong><br>${h(b.message || 'No additional message')}</p>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;">
          <a href="tel:${h(b.phone || '')}" class="btn btn-outline btn-sm" style="flex:1;justify-content:center;text-decoration:none;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.11 13.73 19.79 19.79 0 0 1 1.04 5.1 2 2 0 0 1 3 3h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 10a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
            Call Customer
          </a>
          ${waNum ? `<a href="https://wa.me/${waNum}" target="_blank" class="btn btn-sm" style="flex:1;justify-content:center;background:#25D366;color:#fff;text-decoration:none;">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width:13px;height:13px"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 0 1-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 0 1-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 0 1 2.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0 0 12.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 0 0 5.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 0 0-3.48-8.413z"/></svg>
            WhatsApp
          </a>` : ''}
          <button class="btn btn-sm" style="flex:1;justify-content:center;background:var(--blue);color:#fff;" onclick="closeModal('mo-review-booking');openEmailComposer(${b.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            Send Email
          </button>
        </div>
        <div style="display:flex;gap:12px;">
          <button class="btn btn-primary" style="flex:1;justify-content:center;padding:12px;" onclick="confirmBookingStatus(${b.id}, 'CONFIRMED')">Accept Booking</button>
          <button class="btn btn-danger" style="flex:1;justify-content:center;padding:12px;" onclick="confirmBookingStatus(${b.id}, 'REJECTED')">Reject</button>
        </div>
      `;
      openModal('mo-review-booking');
    }

    async function confirmBookingStatus(id, newStatus) {
      const r = await apiPut('/api/bookings/' + id + '/status', { status: newStatus });
      if (r) {
        bookingsData = bookingsData.map(b => b.id === id ? r : b);
        updateNotifications(); renderDashboard(); closeModal('mo-review-booking');
        toast('Booking ' + (newStatus === 'CONFIRMED' ? 'Accepted' : 'Rejected'), 'Customer will be notified.', newStatus === 'CONFIRMED' ? 'ok' : 'err');
      }
    }

    /* â”€â”€ Modal Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function openModal(id) { document.getElementById(id).classList.add('open') }
    function closeModal(id) { document.getElementById(id).classList.remove('open') }
    document.querySelectorAll('.mo').forEach(m => m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open') }));

    /* â”€â”€ API Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    async function apiPost(url, body) {
      const r = await fetch(url, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      if (!r.ok) { const t = await r.text().catch(() => 'Error'); toast('Error', t, 'err'); return null; }
      return r.json();
    }
    async function apiPut(url, body) {
      const r = await fetch(url, { method: 'PUT', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      if (!r.ok) { const t = await r.text().catch(() => 'Error'); toast('Error', t, 'err'); return null; }
      return r.json();
    }

    /* â”€â”€ Toast â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function toast(title, msg, type = '') {
      const svgOk  = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6 9 17l-5-5"/></svg>`;
      const svgErr = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
      const svgBell= `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>`;
      const icons = { 'ok': svgOk, 'err': svgErr, '': svgBell };
      const host = document.getElementById('toast-host');
      const el = document.createElement('div');
      el.className = 'toast ' + (type === 'ok' ? 'ok' : type === 'err' ? 'err' : '');
      el.innerHTML = `<div class="toast-bar"></div><div class="toast-ico">${icons[type] || svgBell}</div><div><div class="toast-title">${h(title)}</div><div class="toast-msg">${h(msg)}</div></div>`;
      host.appendChild(el);
      setTimeout(() => { el.style.transition = 'opacity .4s,transform .4s'; el.style.opacity = '0'; el.style.transform = 'translateX(50px)'; setTimeout(() => el.remove(), 400); }, 5000);
    }

    /* â”€â”€ Security â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    function h(s) { if (s == null) return ''; return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') }
    async function logout() { try { await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }) } catch { }; window.location.href = '/index.html'; }
  
