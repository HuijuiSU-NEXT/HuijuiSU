// 数字滚动
document.querySelectorAll('[data-count]').forEach(el => {
  const target = parseInt(el.dataset.count, 10);
  const dur = 700;
  const t0 = performance.now();
  function tick(t) {
    const p = Math.min(1, (t - t0) / dur);
    const eased = 1 - Math.pow(1 - p, 3);
    el.textContent = Math.round(target * eased).toString();
    if (p < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
});

// 滚动出现
const io = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      e.target.classList.add('in');
      io.unobserve(e.target);
    }
  });
}, { threshold: 0.12 });
document.querySelectorAll('.reveal').forEach((el, i) => {
  el.style.transitionDelay = (i % 6) * 40 + 'ms';
  io.observe(el);
});

// 兜底：某些环境（无滚动、截图、JS 被限）不会触发 IntersectionObserver，
// 300ms 后统一显示，避免内容空白。
setTimeout(() => {
  document.querySelectorAll('.reveal:not(.in)').forEach(el => el.classList.add('in'));
}, 300);

// tabs
document.querySelectorAll('.tab').forEach(btn => {
  btn.addEventListener('click', () => {
    const id = btn.dataset.tab;
    document.querySelectorAll('.tab').forEach(b => b.classList.toggle('active', b === btn));
    document.querySelectorAll('.tabpane').forEach(p => {
      p.classList.toggle('active', p.id === 'tab-' + id);
    });
  });
});

// 复制
document.querySelectorAll('.copy').forEach(btn => {
  btn.addEventListener('click', async () => {
    const pre = document.getElementById(btn.dataset.copy);
    if (!pre) return;
    try {
      await navigator.clipboard.writeText(pre.textContent);
      const old = btn.textContent;
      btn.textContent = '已复制';
      setTimeout(() => (btn.textContent = old), 1400);
    } catch {
      btn.textContent = '复制失败';
      setTimeout(() => (btn.textContent = '复制'), 1400);
    }
  });
});

// 导航高亮 + 滚动阴影
const nav = document.getElementById('nav');
window.addEventListener('scroll', () => {
  nav.style.boxShadow = window.scrollY > 12 ? '0 10px 30px rgba(0,0,0,.35)' : 'none';
}, { passive: true });

// 科幻光标光斑
const glow = document.createElement('div');
glow.style.cssText = 'position:fixed;width:340px;height:340px;border-radius:50%;pointer-events:none;' +
  'background:radial-gradient(circle,rgba(62,230,196,.10),transparent 65%);' +
  'transform:translate(-50%,-50%);z-index:0;transition:opacity .3s;opacity:0';
document.body.appendChild(glow);
window.addEventListener('pointermove', e => {
  glow.style.left = e.clientX + 'px';
  glow.style.top = e.clientY + 'px';
  glow.style.opacity = '1';
}, { passive: true });
window.addEventListener('pointerleave', () => (glow.style.opacity = '0'));

// 顶部滚动进度条
const bar = document.getElementById('progress');
if (bar) {
  const onScroll = () => {
    const h = document.documentElement;
    const max = h.scrollHeight - h.clientHeight;
    bar.style.width = (max > 0 ? (h.scrollTop / max) * 100 : 0) + '%';
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}

// 页面标题轻微视差
const pageTitle = document.querySelector('.page-title');
if (pageTitle) {
  window.addEventListener('scroll', () => {
    const y = Math.min(window.scrollY, 400);
    pageTitle.style.transform = `translateY(${y * 0.08}px)`;
    pageTitle.style.opacity = String(1 - y / 900);
  }, { passive: true });
}

// 平滑锚点
document.querySelectorAll('a[href^="#"]').forEach(a => {
  a.addEventListener('click', e => {
    const id = a.getAttribute('href').slice(1);
    const el = document.getElementById(id);
    if (el) { e.preventDefault(); el.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
  });
});
