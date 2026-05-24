(function () {
  const canvas = document.getElementById('sim');
  const ctx = canvas.getContext('2d');
  let w, h, particles = [];
  let continents = [];
  let apiBase = '';

  const params = {
    wind: 5,
    windAngle: 90,
    temp: 0,
    layout: 1,
    coriolis: true,
  };

  const MISSIONS = {
    warm: { wind: 6, windAngle: 75, temp: 2, layout: 1, coriolis: true,
      hint: '暖流将热量向高纬度输送，影响沿岸气候。观察粒子是否沿赤道向西北漂移。' },
    elnino: { wind: 4, windAngle: 120, temp: 3.5, layout: 1, coriolis: true,
      hint: '厄尔尼诺：赤道中东太平洋海温异常升高，信风减弱，洋流路径改变。' },
  };

  function resize() {
    const rect = canvas.getBoundingClientRect();
    w = canvas.width = Math.floor(rect.width * (window.devicePixelRatio || 1));
    h = canvas.height = Math.floor(rect.height * (window.devicePixelRatio || 1));
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
    buildContinents();
    if (!particles.length) resetParticles();
  }

  function buildContinents() {
    const cw = canvas.clientWidth;
    const ch = canvas.clientHeight;
    continents = [];
    const add = (x, y, ww, hh) => continents.push({ x, y, w: ww, h: hh });
    if (params.layout === 0) return;
    if (params.layout === 1) {
      add(cw * 0.05, ch * 0.35, cw * 0.22, ch * 0.2);
      add(cw * 0.72, ch * 0.4, cw * 0.2, ch * 0.18);
    } else if (params.layout === 2) {
      add(cw * 0.15, ch * 0.2, cw * 0.25, ch * 0.35);
      add(cw * 0.55, ch * 0.55, cw * 0.3, ch * 0.2);
    } else {
      add(cw * 0.2, ch * 0.3, cw * 0.12, ch * 0.12);
      add(cw * 0.5, ch * 0.5, cw * 0.15, ch * 0.1);
      add(cw * 0.7, ch * 0.25, cw * 0.1, ch * 0.15);
    }
  }

  function resetParticles() {
    const n = 280;
    const cw = canvas.clientWidth;
    const ch = canvas.clientHeight;
    particles = [];
    for (let i = 0; i < n; i++) {
      particles.push({
        x: Math.random() * cw,
        y: Math.random() * ch,
        vx: 0, vy: 0,
        trail: [],
      });
    }
  }

  function windForce() {
    const rad = (params.windAngle * Math.PI) / 180;
    const strength = 0.08 + params.wind * 0.035;
    let fx = Math.cos(rad) * strength;
    let fy = Math.sin(rad) * strength;
    if (params.coriolis) {
      fx += -fy * 0.35;
      fy += fx * 0.15;
    }
    fx += params.temp * 0.012;
    return { fx, fy };
  }

  function hitsContinent(x, y) {
    return continents.some((c) => x >= c.x && x <= c.x + c.w && y >= c.y && y <= c.y + c.h);
  }

  function step() {
    const cw = canvas.clientWidth;
    const ch = canvas.clientHeight;
    const { fx, fy } = windForce();

    particles.forEach((p) => {
      p.vx = p.vx * 0.92 + fx + (Math.random() - 0.5) * 0.02;
      p.vy = p.vy * 0.92 + fy + (Math.random() - 0.5) * 0.02;
      p.x += p.vx;
      p.y += p.vy;

      if (hitsContinent(p.x, p.y)) {
        p.vx *= -0.6;
        p.vy *= -0.6;
        p.x += p.vx * 2;
        p.y += p.vy * 2;
      }
      if (p.x < 0) p.x = cw;
      if (p.x > cw) p.x = 0;
      if (p.y < 0) p.y = ch;
      if (p.y > ch) p.y = 0;

      p.trail.push({ x: p.x, y: p.y });
      if (p.trail.length > 12) p.trail.shift();
    });
  }

  function draw() {
    const cw = canvas.clientWidth;
    const ch = canvas.clientHeight;
    ctx.fillStyle = '#041828';
    ctx.fillRect(0, 0, cw, ch);

    ctx.fillStyle = 'rgba(30,80,60,0.85)';
    continents.forEach((c) => ctx.fillRect(c.x, c.y, c.w, c.h));

    const gx = cw / 2 + Math.cos((params.windAngle * Math.PI) / 180) * 40;
    const gy = ch / 2 + Math.sin((params.windAngle * Math.PI) / 180) * 40;
    ctx.strokeStyle = 'rgba(46,196,240,0.4)';
    ctx.beginPath();
    ctx.moveTo(cw / 2, ch / 2);
    ctx.lineTo(gx, gy);
    ctx.stroke();

    particles.forEach((p) => {
      if (p.trail.length > 1) {
        ctx.strokeStyle = 'rgba(46,196,240,0.35)';
        ctx.beginPath();
        ctx.moveTo(p.trail[0].x, p.trail[0].y);
        for (let i = 1; i < p.trail.length; i++) ctx.lineTo(p.trail[i].x, p.trail[i].y);
        ctx.stroke();
      }
      ctx.fillStyle = '#5ec8ff';
      ctx.beginPath();
      ctx.arc(p.x, p.y, 1.8, 0, Math.PI * 2);
      ctx.fill();
    });
  }

  function loop() {
    step();
    draw();
    requestAnimationFrame(loop);
  }

  function avgDriftAngle() {
    let sx = 0, sy = 0, n = 0;
    particles.forEach((p) => {
      if (Math.hypot(p.vx, p.vy) > 0.05) {
        sx += p.vx; sy += p.vy; n++;
      }
    });
    if (!n) return params.windAngle;
    return ((Math.atan2(sy, sx) * 180) / Math.PI + 360) % 360;
  }

  function bindControls() {
    const wind = document.getElementById('wind');
    const windAngle = document.getElementById('windAngle');
    const temp = document.getElementById('temp');
    const layout = document.getElementById('layout');
    const coriolis = document.getElementById('coriolis');

    const sync = () => {
      params.wind = +wind.value;
      params.windAngle = +windAngle.value;
      params.temp = +temp.value;
      params.layout = +layout.value;
      params.coriolis = coriolis.checked;
      document.getElementById('windVal').textContent = params.wind;
      document.getElementById('angleVal').textContent = params.windAngle + '°';
      document.getElementById('tempVal').textContent = params.temp + '℃';
      buildContinents();
    };
    [wind, windAngle, temp, layout, coriolis].forEach((el) => el.addEventListener('input', sync));
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
        resetParticles();
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
    const body = {
      windPower: params.wind,
      windAngle: params.windAngle,
      coriolisEnabled: params.coriolis,
      continentLayout: params.layout,
      tempDiff: params.temp,
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
        document.getElementById('manualTrend').textContent = '人工：' + d.manualTrend;
        document.getElementById('aiTrend').textContent = 'AI：' + d.aiTrend;
        document.getElementById('comparison').textContent = d.comparison + ' ' + (d.explanation || '');
      } else {
        document.getElementById('comparison').textContent = json.msg || '预测失败，请确认后端已启动';
      }
    } catch (e) {
      document.getElementById('comparison').textContent = '无法连接服务器：' + e.message;
    }
  }

  window.setApiBase = function (base) {
    apiBase = base;
  };

  window.addEventListener('resize', resize);
  bindControls();
  resize();
  loop();
})();
