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

// ===== 卡片详情弹窗 =====
const FEATURE_DETAIL = {
  c1t: { zh:"内置一键隐藏", body:`
    <p><b>模块随 APK 分发</b>，不需要用户自己找包、导文件。</p>
    <ul>
      <li>首次启动自动部署到 <code>/data/adb/huijui/</code></li>
      <li>内置：Zygisk Next、LSPosed、HMA-OSS、OhMyKeymint、PIF-Dates</li>
      <li>自动导入 HMA 配置与 keybox，自动生成黑白名单</li>
      <li>安装前清理旧版本，避免新旧并存</li>
    </ul>` },
  c2t: { zh:"真·热刷入", body:`
    <p>接入内置元模块 Hybrid Mount 的运行时接口：</p>
    <p><code>hybrid-mount runtime load &lt;id&gt;</code></p>
    <ul>
      <li>纯 VFS 文件计划的模块：装完立即挂载，<b>不用重启</b></li>
      <li>Magic / Overlay 后端：明确提示「需重启」</li>
      <li>元模块未就绪时，提示先安装并重启</li>
    </ul>` },
  c3t: { zh:"开机秒激活", body:`
    <p>内置 <code>huijui_core</code> 模块，开机过程自动完成：</p>
    <ul>
      <li><b>post-fs-data</b>：首次应用隐藏默认值（只跑一次）</li>
      <li><b>boot-completed</b>：自动安装元模块与隐藏模块</li>
    </ul>
    <p>安装动作放在开机完成后，避免拖慢启动。</p>` },
  c4t: { zh:"极客工具箱", body:`
    <p>一张卡片一件事，每张都有可见执行状态：</p>
    <ul>
      <li>备份 boot 分区 / 重启 SystemUI / 清理缓存</li>
      <li>清理临时文件 / 模块备份与还原</li>
      <li>一键隐藏 / Evil Service 自查 / 元模块状态</li>
      <li>安装元模块 / 春秋词条修复 / 重启设备 / Root 状态</li>
    </ul>` },
  c5t: { zh:"功耗优化", body:`
    <ul>
      <li>背景模糊限流 <b>30fps</b>，退到后台停止刷新</li>
      <li>隐藏规则合并为<b>一次 su</b>，启动更快</li>
      <li>release 关闭 Koin 日志，冷启动更快</li>
      <li>壁纸解码按屏幕封顶，内存占用大幅下降</li>
      <li>ksud 调用全部加超时，避免界面无限转圈</li>
    </ul>` },
  c6t: { zh:"核心防误删", body:`
    <ul>
      <li>内置隐藏模块<b>禁止卸载</b></li>
      <li>同时<b>禁止停用</b>，避免隐藏链路失效</li>
      <li>部署失败会回传真实错误，不再笼统提示</li>
      <li>崩溃日志写入 cache/crash，便于排查</li>
    </ul>` }
};

const dlg = document.getElementById('featDlg');
if (dlg) {
  const dlgTitle = document.getElementById('dlgTitle');
  const dlgBody = document.getElementById('dlgBody');
  const close = () => dlg.close();

  document.querySelectorAll('.card[data-feature]').forEach(card => {
    const open = () => {
      const key = card.dataset.feature;
      const data = FEATURE_DETAIL[key];
      if (!data) return;
      dlgTitle.textContent = data.zh;
      dlgBody.innerHTML = data.body;
      dlg.showModal();
    };
    card.addEventListener('click', open);
    card.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open(); }
    });
  });

  dlg.querySelector('.dlg-close').addEventListener('click', close);
  dlg.addEventListener('click', e => { if (e.target === dlg) close(); });
}

// 卡片内跟随光标的光斑
document.querySelectorAll('.card').forEach(card => {
  card.addEventListener('pointermove', e => {
    const r = card.getBoundingClientRect();
    card.style.setProperty('--mx', ((e.clientX - r.left) / r.width * 100) + '%');
    card.style.setProperty('--my', ((e.clientY - r.top) / r.height * 100) + '%');
  }, { passive: true });
});
