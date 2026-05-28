(function () {
  const mapCanvas = document.getElementById('mapLayer');
  const particleCanvas = document.getElementById('particleLayer');
  const mapCtx = mapCanvas.getContext('2d');
  const pCtx = particleCanvas.getContext('2d', { alpha: true });
  const mapTitleEl = document.getElementById('mapTitle');

  let particles = [];
  let mapMeta = { title: '', equator: false, ignoreLand: false };
  let apiBase = '';
  let worldMapImg = null;
  let mapPixelData = null;
  let mapReady = false;
  let useProceduralMap = false;
  let landMask = null;

  /** 简化版世界陆地轮廓（归一化 0~1，近似真实地理） */
  const LAND_POLYGONS = [
    [[0.06,0.14],[0.12,0.10],[0.20,0.11],[0.27,0.18],[0.28,0.32],[0.24,0.40],[0.18,0.42],[0.12,0.36],[0.08,0.28]],
    [[0.17,0.46],[0.24,0.48],[0.29,0.58],[0.28,0.76],[0.22,0.82],[0.18,0.70],[0.16,0.56]],
    [[0.40,0.12],[0.52,0.10],[0.68,0.12],[0.82,0.18],[0.88,0.28],[0.85,0.38],[0.72,0.42],[0.58,0.40],[0.46,0.32],[0.38,0.22]],
    [[0.44,0.40],[0.54,0.38],[0.58,0.52],[0.56,0.68],[0.50,0.74],[0.44,0.62],[0.42,0.48]],
    [[0.70,0.56],[0.80,0.54],[0.86,0.62],[0.84,0.72],[0.74,0.74],[0.68,0.66]],
    [[0.30,0.52],[0.36,0.50],[0.40,0.56],[0.38,0.62],[0.32,0.60]],
    [[0.48,0.18],[0.56,0.16],[0.62,0.22],[0.58,0.30],[0.50,0.28]],
    [[0.10,0.78],[0.22,0.76],[0.26,0.88],[0.14,0.92],[0.06,0.86]],
  ];
  let simRunning = true;
  let mapDirty = true;
  let viewCrop = { x: 0, y: 0.06, w: 1, h: 0.88 };

  const PARTICLE_COUNT = 100;
  const TRAIL_LEN = 8;
  const MASK_W = 160;
  const MAP_DECODE_MAX = 480;
  const params = { wind: 5, windAngle: 180, temp: 0, salinity: 35, layout: 1, coriolis: true };
  const buoys = [];

  const GEO_LAYOUTS = {
    0: { title: '开阔大洋（全球）', equator: true, defaultWind: 90, ignoreLand: true, crop: { x: 0, y: 0.06, w: 1, h: 0.88 } },
    1: { title: '赤道太平洋', equator: true, defaultWind: 180, ignoreLand: false, crop: { x: 0.02, y: 0.12, w: 0.58, h: 0.76 } },
    2: { title: '北大西洋', equator: false, defaultWind: 225, ignoreLand: false, crop: { x: 0.16, y: 0.08, w: 0.40, h: 0.74 } },
    3: { title: '西太平洋', equator: true, defaultWind: 200, ignoreLand: false, crop: { x: 0.48, y: 0.10, w: 0.50, h: 0.76 } },
  };

  const MISSIONS = {
    warm: { wind: 6, windAngle: 315, temp: 2, layout: 2, coriolis: true, hint: '暖流任务：已切换为北大西洋。观察粒子沿欧洲西岸向北偏转。' },
    elnino: { wind: 3, windAngle: 160, temp: 3.5, layout: 1, coriolis: true, hint: '厄尔尼诺：信风减弱，暖水向东回流。' },
  };

  function stageSize() {
    const rect = mapCanvas.parentElement.getBoundingClientRect();
    return { cw: rect.width, ch: rect.height };
  }

  function isWatermarkZone(px, py, w, h) {
    return py > h * 0.86 && px < w * 0.26;
  }

  function isOceanPixel(r, g, b, a) {
    if (a < 100) return true;
    const brightness = (r + g + b) / 3;
    if (useProceduralMap) return brightness < 70;
    if (g > r + 15 && g > b + 10 && g > 85) return false;
    const maxC = Math.max(r, g, b);
    const minC = Math.min(r, g, b);
    const sat = maxC < 8 ? 0 : (maxC - minC) / maxC;
    if (brightness < 82 && sat < 0.4) return true;
    if (brightness < 110 && b >= r * 0.85 && b >= g * 0.75 && sat < 0.35) return true;
    return false;
  }

  function rebuildLandMask() {
    if (!mapPixelData || mapMeta.ignoreLand) {
      landMask = null;
      return;
    }
    const { cw, ch } = stageSize();
    const srcW = mapPixelData.width;
    const srcH = mapPixelData.height;
    const mh = Math.max(48, Math.round(MASK_W * (ch / cw)));
    const data = new Uint8Array(MASK_W * mh);
    for (let j = 0; j < mh; j++) {
      for (let i = 0; i < MASK_W; i++) {
        const nx = i / MASK_W;
        const ny = j / mh;
        const px = Math.min(srcW - 1, Math.floor((viewCrop.x + nx * viewCrop.w) * srcW));
        const py = Math.min(srcH - 1, Math.floor((viewCrop.y + ny * viewCrop.h) * srcH));
        data[j * MASK_W + i] = isOceanAt(px, py, srcW, srcH) ? 1 : 0;
      }
    }
    landMask = { w: MASK_W, h: mh, data };
  }

  function isOceanAt(px, py, w, h) {
    if (isWatermarkZone(px, py, w, h)) return true;
    const i = (py * w + px) * 4;
    const d = mapPixelData.data;
    return isOceanPixel(d[i], d[i + 1], d[i + 2], d[i + 3]);
  }

  function onLand(x, y) {
    if (mapMeta.ignoreLand || !landMask) return false;
    const { cw, ch } = stageSize();
    if (!cw || !ch) return false;
    const i = Math.min(landMask.w - 1, Math.max(0, Math.floor((x / cw) * landMask.w)));
    const j = Math.min(landMask.h - 1, Math.max(0, Math.floor((y / ch) * landMask.h)));
    return landMask.data[j * landMask.w + i] === 0;
  }

  function onMapLoaded(img) {
    worldMapImg = img;
    let w = img.naturalWidth;
    let h = img.naturalHeight;
    if (w > MAP_DECODE_MAX) {
      h = Math.round((h * MAP_DECODE_MAX) / w);
      w = MAP_DECODE_MAX;
    }
    const off = document.createElement('canvas');
    off.width = w;
    off.height = h;
    const octx = off.getContext('2d', { willReadFrequently: true });
    octx.drawImage(img, 0, 0, w, h);
    mapPixelData = octx.getImageData(0, 0, w, h);
    mapReady = true;
    applyLayoutView();
    rebuildLandMask();
    mapDirty = true;
    resetParticles();
    drawMapLayer();
    resize();
  }

  function buildProceduralWorldPixels(w, h) {
    const off = document.createElement('canvas');
    off.width = w;
    off.height = h;
    const ctx = off.getContext('2d');
    ctx.fillStyle = '#061018';
    ctx.fillRect(0, 0, w, h);
    ctx.fillStyle = '#c8d0d8';
    ctx.strokeStyle = '#9aa8b4';
    ctx.lineWidth = 1;
    LAND_POLYGONS.forEach((poly) => {
      ctx.beginPath();
      poly.forEach((p, i) => {
        const x = p[0] * w;
        const y = p[1] * h;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
    });
    return ctx.getImageData(0, 0, w, h);
  }

  function enableProceduralMap() {
    useProceduralMap = true;
    worldMapImg = null;
    mapPixelData = buildProceduralWorldPixels(480, 240);
    mapReady = true;
    applyLayoutView();
    rebuildLandMask();
    mapDirty = true;
    resetParticles();
    drawMapLayer();
    resize();
  }

  function drawProceduralViewport(cw, ch) {
    const crop = viewCrop;
    mapCtx.fillStyle = '#061018';
    mapCtx.fillRect(0, 0, cw, ch);
    LAND_POLYGONS.forEach((poly) => {
      mapCtx.beginPath();
      poly.forEach((p, i) => {
        const x = ((p[0] - crop.x) / crop.w) * cw;
        const y = ((p[1] - crop.y) / crop.h) * ch;
        if (i === 0) mapCtx.moveTo(x, y);
        else mapCtx.lineTo(x, y);
      });
      mapCtx.closePath();
      mapCtx.fillStyle = '#c8d0d8';
      mapCtx.strokeStyle = '#8a98a4';
      mapCtx.lineWidth = 1;
      mapCtx.fill();
      mapCtx.stroke();
    });
  }

  function loadWorldMap() {
    const tryImg = (src, next) => {
      const img = new Image();
      img.onload = () => { useProceduralMap = false; onMapLoaded(img); };
      img.onerror = () => { if (next) next(); else enableProceduralMap(); };
      img.src = src;
    };
    if (window.AndroidBridge && typeof AndroidBridge.getWorldMapBase64 === 'function') {
      try {
        const b64 = AndroidBridge.getWorldMapBase64();
        if (b64 && b64.length > 100) {
          tryImg('data:image/png;base64,' + b64, () => enableProceduralMap());
          return;
        }
      } catch (e) { /* fallback */ }
    }
    const base = 'file:///android_asset/ocean_sim/';
    tryImg(base + 'world_map.png', () => tryImg(base + 'world_map.svg', () => enableProceduralMap()));
  }

  window.loadMapFromAndroid = loadWorldMap;

  function applyLayoutView() {
    const geo = GEO_LAYOUTS[params.layout] || GEO_LAYOUTS[1];
    mapMeta.title = geo.title;
    mapMeta.equator = geo.equator;
    mapMeta.ignoreLand = !!geo.ignoreLand;
    viewCrop = geo.crop || { x: 0, y: 0.06, w: 1, h: 0.88 };
    if (mapTitleEl) mapTitleEl.textContent = mapMeta.title;
    if (mapReady) rebuildLandMask();
    mapDirty = true;
  }

  function applyLayoutDefaults() {
    const geo = GEO_LAYOUTS[params.layout] || GEO_LAYOUTS[1];
    if (geo.defaultWind != null) params.windAngle = geo.defaultWind;
  }

  function resize() {
    const { cw, ch } = stageSize();
    if (!cw || !ch) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    [mapCanvas, particleCanvas].forEach((c) => {
      c.width = Math.floor(cw * dpr);
      c.height = Math.floor(ch * dpr);
      c.style.width = cw + 'px';
      c.style.height = ch + 'px';
    });
    mapCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
    pCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
    if (mapReady) rebuildLandMask();
    mapDirty = true;
    if (!particles.length && mapReady) resetParticles();
  }

  function initBuoys(cw, ch) {
    buoys.length = 0;
    for (let i = 0; i < 3; i++) {
      let x, y, guard = 0;
      do {
        x = cw * (0.25 + i * 0.25);
        y = ch * (0.35 + Math.random() * 0.3);
        guard++;
      } while (onLand(x, y) && guard < 20);
      buoys.push({ x, y, trail: [] });
    }
  }

  function resetParticles() {
    const { cw, ch } = stageSize();
    if (!cw || !ch) return;
    initBuoys(cw, ch);
    particles = [];
    for (let n = 0; n < PARTICLE_COUNT; n++) {
      let x, y, guard = 0;
      do {
        x = Math.random() * cw;
        y = Math.random() * ch;
        guard++;
      } while (onLand(x, y) && guard < 30);
      particles.push({ x, y, vx: 0, vy: 0, trail: [] });
    }
  }

  function windForce() {
    const rad = (params.windAngle * Math.PI) / 180;
    const strength = 0.06 + params.wind * 0.032;
    let fx = Math.cos(rad) * strength;
    let fy = Math.sin(rad) * strength;
    if (params.coriolis) {
      const deflect = 0.28 * strength;
      fx += -fy * deflect * 1.2;
      fy += fx * deflect * 0.5;
    }
    fx += params.temp * 0.01;
    fy += Math.sin((params.temp || 0) * 0.3) * 0.008;
    const salt = (params.salinity - 35) * 0.004;
    fx += salt;
    fy -= salt * 0.6;
    return { fx, fy };
  }

  function step() {
    const { cw, ch } = stageSize();
    const { fx, fy } = windForce();
    particles.forEach((p) => {
      p.vx = p.vx * 0.91 + fx + (Math.random() - 0.5) * 0.012;
      p.vy = p.vy * 0.91 + fy + (Math.random() - 0.5) * 0.012;
      p.x += p.vx;
      p.y += p.vy;
      if (onLand(p.x, p.y)) {
        p.vx *= -0.55;
        p.vy *= -0.55;
        p.x += p.vx * 2.5;
        p.y += p.vy * 2.5;
      }
      if (p.x < 0) p.x = cw;
      if (p.x > cw) p.x = 0;
      if (p.y < 0) p.y = ch;
      if (p.y > ch) p.y = 0;
      p.trail.push({ x: p.x, y: p.y });
      if (p.trail.length > TRAIL_LEN) p.trail.shift();
    });
    let sx = 0, sy = 0, n = 0;
    particles.forEach((p) => {
      if (Math.hypot(p.vx, p.vy) > 0.02) { sx += p.vx; sy += p.vy; n++; }
    });
    if (n > 0) {
      const ax = sx / n, ay = sy / n;
      buoys.forEach((b) => {
        b.x += ax * 3.5;
        b.y += ay * 3.5;
        if (onLand(b.x, b.y)) { b.x -= ax * 4; b.y -= ay * 4; }
        if (b.x < 0) b.x = cw;
        if (b.x > cw) b.x = 0;
        if (b.y < 0) b.y = ch;
        if (b.y > ch) b.y = 0;
        b.trail.push({ x: b.x, y: b.y });
        if (b.trail.length > 20) b.trail.shift();
      });
    }
  }

  function drawMapLayer() {
    const { cw, ch } = stageSize();
    if (!cw || !ch) return;
    mapCtx.clearRect(0, 0, cw, ch);
    if (mapReady && worldMapImg && worldMapImg.naturalWidth > 0) {
      const iw = worldMapImg.naturalWidth;
      const ih = worldMapImg.naturalHeight;
      mapCtx.drawImage(
        worldMapImg,
        viewCrop.x * iw, viewCrop.y * ih, viewCrop.w * iw, viewCrop.h * ih,
        0, 0, cw, ch
      );
    } else if (mapReady && useProceduralMap) {
      drawProceduralViewport(cw, ch);
    } else {
      mapCtx.fillStyle = '#061018';
      mapCtx.fillRect(0, 0, cw, ch);
      mapCtx.fillStyle = '#ffffff';
      mapCtx.font = '12px sans-serif';
      mapCtx.fillText('地图加载中…', 12, Math.max(20, ch / 2));
    }
    if (mapMeta.equator && mapReady) {
      const eqY = ch * 0.48;
      mapCtx.strokeStyle = 'rgba(255,255,255,0.35)';
      mapCtx.setLineDash([5, 5]);
      mapCtx.beginPath();
      mapCtx.moveTo(0, eqY);
      mapCtx.lineTo(cw, eqY);
      mapCtx.stroke();
      mapCtx.setLineDash([]);
    }
  }

  function drawParticleLayer() {
    const { cw, ch } = stageSize();
    pCtx.clearRect(0, 0, cw, ch);
    const cx = cw * 0.5, cy = ch * 0.5;
    const len = 36 + params.wind * 2;
    const rad = (params.windAngle * Math.PI) / 180;
    const ex = cx + Math.cos(rad) * len;
    const ey = cy + Math.sin(rad) * len;
    pCtx.strokeStyle = 'rgba(255,255,255,0.85)';
    pCtx.lineWidth = 2;
    pCtx.beginPath();
    pCtx.moveTo(cx, cy);
    pCtx.lineTo(ex, ey);
    pCtx.stroke();
    buoys.forEach((b) => {
      if (b.trail.length > 1) {
        pCtx.strokeStyle = 'rgba(255, 255, 255, 0.75)';
        pCtx.lineWidth = 2;
        pCtx.beginPath();
        pCtx.moveTo(b.trail[0].x, b.trail[0].y);
        for (let i = 1; i < b.trail.length; i++) pCtx.lineTo(b.trail[i].x, b.trail[i].y);
        pCtx.stroke();
      }
      pCtx.fillStyle = '#ffffff';
      pCtx.beginPath();
      pCtx.arc(b.x, b.y, 5, 0, Math.PI * 2);
      pCtx.fill();
    });
    particles.forEach((p) => {
      if (p.trail.length > 1) {
        pCtx.strokeStyle = 'rgba(255, 255, 255, 0.55)';
        pCtx.lineWidth = 1.2;
        pCtx.beginPath();
        pCtx.moveTo(p.trail[0].x, p.trail[0].y);
        for (let i = 1; i < p.trail.length; i++) pCtx.lineTo(p.trail[i].x, p.trail[i].y);
        pCtx.stroke();
      }
      pCtx.fillStyle = '#ffffff';
      pCtx.beginPath();
      pCtx.arc(p.x, p.y, 1.8, 0, Math.PI * 2);
      pCtx.fill();
    });
  }

  function loop() {
    requestAnimationFrame(loop);
    if (mapDirty) {
      drawMapLayer();
      mapDirty = false;
    }
    if (!simRunning) return;
    step();
    drawParticleLayer();
  }

  window.setSimRunning = function (on) { simRunning = !!on; };
  document.addEventListener('visibilitychange', () => {
    simRunning = document.visibilityState === 'visible';
  });

  function avgDriftAngle() {
    let sx = 0, sy = 0, n = 0;
    particles.forEach((p) => {
      if (Math.hypot(p.vx, p.vy) > 0.04) { sx += p.vx; sy += p.vy; n++; }
    });
    if (!n) return params.windAngle;
    return ((Math.atan2(sy, sx) * 180) / Math.PI + 360) % 360;
  }

  function bindControls() {
    const wind = document.getElementById('wind');
    const windAngle = document.getElementById('windAngle');
    const temp = document.getElementById('temp');
    const salinity = document.getElementById('salinity');
    const layout = document.getElementById('layout');
    const coriolis = document.getElementById('coriolis');
    const sync = () => {
      const layoutChanged = params.layout !== +layout.value;
      params.wind = +wind.value;
      params.windAngle = +windAngle.value;
      params.temp = +temp.value;
      params.salinity = +salinity.value;
      params.layout = +layout.value;
      params.coriolis = coriolis.checked;
      if (layoutChanged) applyLayoutDefaults();
      document.getElementById('windVal').textContent = params.wind;
      document.getElementById('angleVal').textContent = Math.round(params.windAngle) + '°';
      document.getElementById('tempVal').textContent = params.temp + '℃';
      document.getElementById('salinityVal').textContent = params.salinity + '‰';
      windAngle.value = params.windAngle;
      applyLayoutView();
      if (layoutChanged && mapReady) resetParticles();
    };
    [wind, windAngle, temp, salinity, layout, coriolis].forEach((el) => el.addEventListener('input', sync));
    applyLayoutDefaults();
    windAngle.value = params.windAngle;
    applyLayoutView();
    sync();
    document.getElementById('btnReset').onclick = resetParticles;
    document.getElementById('btnPredict').onclick = runPredict;
    document.querySelectorAll('.mission').forEach((btn) => {
      btn.addEventListener('click', () => {
        const m = MISSIONS[btn.dataset.mission];
        if (!m) return;
        wind.value = m.wind;
        windAngle.value = m.windAngle;
        temp.value = m.temp;
        layout.value = m.layout;
        coriolis.checked = m.coriolis;
        sync();
        showMissionHint(m.hint);
      });
    });
  }

  function showMissionHint(text) {
    const el = document.getElementById('missionHint');
    if (!el) return;
    el.textContent = text;
    el.classList.remove('hidden');
  }

  async function runPredict() {
    const panel = document.getElementById('predictPanel');
    panel.classList.remove('hidden');
    document.getElementById('manualTrend').textContent = '正在请求 AI 预测…';
    const body = {
      windPower: params.wind,
      windAngle: Math.round(params.windAngle),
      coriolisEnabled: params.coriolis,
      continentLayout: params.layout,
      tempDiff: params.temp,
      salinity: params.salinity,
      manualDriftAngle: avgDriftAngle(),
    };
    try {
      const base = apiBase || (window.AndroidBridge && AndroidBridge.getApiBase()) || '';
      const res = await fetch(base + 'current/predict', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const json = await res.json();
      if (json.code === 200 && json.data) {
        const d = json.data;
        document.getElementById('manualTrend').textContent = '【人工推演】' + d.manualTrend;
        document.getElementById('aiTrend').textContent = '【AI 预测】' + d.aiTrend;
        document.getElementById('comparison').textContent = (d.comparison || '') + ' ' + (d.explanation || '');
      } else {
        document.getElementById('comparison').textContent = json.msg || '预测失败';
      }
    } catch (e) {
      document.getElementById('comparison').textContent = '无法连接后端：' + e.message;
    }
  }

  window.setApiBase = function (base) { apiBase = base; };
  window.resize = resize;
  window.addEventListener('resize', resize);
  loadWorldMap();
  bindControls();
  resize();
  loop();
})();
