"""
NoveFlix — Servidor de Streaming
FastAPI + SQLite + HTTP Range (necessário para mobile/ExoPlayer)

Variáveis de ambiente:
  ADMIN_TOKEN  — token do painel admin (padrão: noveflix2024)
  MAX_FILE_MB  — tamanho máximo por upload em MB (padrão: 500)
  PORT         — porta do servidor (padrão: 8080)
"""

import os
import re
import uuid
import sqlite3
import aiofiles
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Header, Request, Depends
from fastapi.responses import StreamingResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware

# ── Configuração ───────────────────────────────────────────────────────────────
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "noveflix2024")
UPLOAD_DIR  = Path("uploads")
DB_PATH     = "noveflix.db"
MAX_FILE_MB = int(os.environ.get("MAX_FILE_MB", "500"))
PORT        = int(os.environ.get("PORT", 8080))

UPLOAD_DIR.mkdir(exist_ok=True)

ALLOWED_EXT = {".mp4", ".mkv", ".webm", ".avi", ".mov"}
MEDIA_TYPES = {
    ".mp4":  "video/mp4",
    ".mkv":  "video/x-matroska",
    ".webm": "video/webm",
    ".avi":  "video/x-msvideo",
    ".mov":  "video/quicktime",
}
COUNTRY_FLAGS = {
    "JP": "🇯🇵", "KR": "🇰🇷", "TR": "🇹🇷",
    "MX": "🇲🇽", "CN": "🇨🇳", "BR": "🇧🇷",
    "IN": "🇮🇳", "PH": "🇵🇭", "US": "🇺🇸",
}

# ── Banco de dados ─────────────────────────────────────────────────────────────
def get_db():
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS novelas (
            id           TEXT PRIMARY KEY,
            title        TEXT NOT NULL,
            description  TEXT DEFAULT '',
            country_code TEXT DEFAULT '',
            poster_url   TEXT DEFAULT '',
            created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS episodes (
            id               TEXT PRIMARY KEY,
            novela_id        TEXT NOT NULL,
            novela_title     TEXT NOT NULL,
            title            TEXT NOT NULL,
            description      TEXT DEFAULT '',
            video_filename   TEXT DEFAULT '',
            thumbnail_url    TEXT DEFAULT '',
            episode_number   INTEGER DEFAULT 1,
            duration_seconds INTEGER DEFAULT 0,
            coin_cost        INTEGER DEFAULT 2,
            country_code     TEXT DEFAULT '',
            country_flag     TEXT DEFAULT '',
            like_count       INTEGER DEFAULT 0,
            view_count       INTEGER DEFAULT 0,
            created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (novela_id) REFERENCES novelas(id)
        );
    """)
    conn.commit()
    conn.close()


init_db()

# ── Auth ───────────────────────────────────────────────────────────────────────
async def require_admin(x_admin_token: str = Header(...)):
    if x_admin_token != ADMIN_TOKEN:
        raise HTTPException(status_code=401, detail="Token inválido")

# ── App ────────────────────────────────────────────────────────────────────────
app = FastAPI(title="NoveFlix API", version="1.0.0", docs_url="/docs")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Helpers ────────────────────────────────────────────────────────────────────
def row_to_episode(row, base_url: str) -> dict:
    ep = dict(row)
    filename = ep.get("video_filename", "")
    ep["videoUrl"]        = f"{base_url}/api/stream/{filename}" if filename else ""
    ep["thumbnailUrl"]    = ep.pop("thumbnail_url", "")
    ep["novelaId"]        = ep.pop("novela_id", "")
    ep["novelaTitle"]     = ep.pop("novela_title", "")
    ep["episodeNumber"]   = ep.pop("episode_number", 1)
    ep["durationSeconds"] = ep.pop("duration_seconds", 0)
    ep["coinCost"]        = ep.pop("coin_cost", 2)
    ep["likeCount"]       = ep.pop("like_count", 0)
    ep["viewCount"]       = ep.pop("view_count", 0)
    ep["countryFlag"]     = ep.pop("country_flag", "")
    ep["countryCode"]     = ep.pop("country_code", "")
    ep.pop("video_filename", None)
    ep.pop("created_at", None)
    return ep

# ── API pública ────────────────────────────────────────────────────────────────
@app.get("/health")
async def health():
    return {"status": "ok", "service": "NoveFlix Streaming API"}


@app.get("/api/feed")
async def get_feed(request: Request, page: int = 1, per_page: int = 20):
    """Feed principal para o app Android."""
    offset   = (page - 1) * per_page
    base_url = str(request.base_url).rstrip("/")
    conn     = get_db()
    rows     = conn.execute(
        "SELECT * FROM episodes ORDER BY created_at DESC LIMIT ? OFFSET ?",
        (per_page, offset)
    ).fetchall()
    total = conn.execute("SELECT COUNT(*) FROM episodes").fetchone()[0]
    conn.close()
    return {
        "page":     page,
        "per_page": per_page,
        "total":    total,
        "episodes": [row_to_episode(r, base_url) for r in rows],
    }


@app.get("/api/novelas")
async def list_novelas():
    conn  = get_db()
    rows  = conn.execute("SELECT * FROM novelas ORDER BY title").fetchall()
    conn.close()
    return [dict(r) for r in rows]


@app.get("/api/novelas/{novela_id}/episodes")
async def get_novela_episodes(novela_id: str, request: Request):
    base_url = str(request.base_url).rstrip("/")
    conn     = get_db()
    rows     = conn.execute(
        "SELECT * FROM episodes WHERE novela_id=? ORDER BY episode_number",
        (novela_id,)
    ).fetchall()
    conn.close()
    return [row_to_episode(r, base_url) for r in rows]


@app.get("/api/episodes/{episode_id}")
async def get_episode(episode_id: str, request: Request):
    base_url = str(request.base_url).rstrip("/")
    conn     = get_db()
    row      = conn.execute("SELECT * FROM episodes WHERE id=?", (episode_id,)).fetchone()
    conn.close()
    if not row:
        raise HTTPException(status_code=404, detail="Episódio não encontrado")
    return row_to_episode(row, base_url)


@app.post("/api/episodes/{episode_id}/view")
async def increment_view(episode_id: str):
    """Chamado pelo app quando o vídeo começa a tocar."""
    conn = get_db()
    conn.execute("UPDATE episodes SET view_count = view_count + 1 WHERE id=?", (episode_id,))
    conn.commit()
    conn.close()
    return {"ok": True}


@app.post("/api/episodes/{episode_id}/like")
async def toggle_like(episode_id: str, liked: bool = True):
    """Curtir / descurtir um episódio."""
    conn = get_db()
    if liked:
        conn.execute("UPDATE episodes SET like_count = like_count + 1 WHERE id=?", (episode_id,))
    else:
        conn.execute("UPDATE episodes SET like_count = MAX(0, like_count - 1) WHERE id=?", (episode_id,))
    conn.commit()
    conn.close()
    return {"ok": True}

# ── Streaming de vídeo com HTTP Range ──────────────────────────────────────────
@app.get("/api/stream/{filename}")
async def stream_video(filename: str, request: Request):
    """
    Streaming com suporte a Range Requests.
    Necessário para o ExoPlayer pausar/avançar sem rebuffering.
    """
    if not re.match(r"^[a-zA-Z0-9._-]+$", filename):
        raise HTTPException(status_code=400, detail="Nome de arquivo inválido")

    file_path = UPLOAD_DIR / filename
    if not file_path.exists() or not file_path.is_file():
        raise HTTPException(status_code=404, detail="Vídeo não encontrado")

    file_size    = file_path.stat().st_size
    range_header = request.headers.get("Range")
    ext          = file_path.suffix.lower()
    media_type   = MEDIA_TYPES.get(ext, "video/mp4")

    if range_header:
        match = re.match(r"bytes=(\d+)-(\d*)", range_header)
        if not match:
            raise HTTPException(status_code=416, detail="Range inválido")

        start = int(match.group(1))
        end   = int(match.group(2)) if match.group(2) else min(start + 10 * 1024 * 1024 - 1, file_size - 1)
        end   = min(end, file_size - 1)

        if start >= file_size:
            raise HTTPException(status_code=416, detail="Range fora dos limites")

        chunk_size = end - start + 1

        async def generate_range():
            async with aiofiles.open(file_path, "rb") as f:
                await f.seek(start)
                remaining = chunk_size
                while remaining > 0:
                    data = await f.read(min(65536, remaining))
                    if not data:
                        break
                    remaining -= len(data)
                    yield data

        return StreamingResponse(
            generate_range(),
            status_code=206,
            media_type=media_type,
            headers={
                "Content-Range":  f"bytes {start}-{end}/{file_size}",
                "Accept-Ranges":  "bytes",
                "Content-Length": str(chunk_size),
                "Cache-Control":  "public, max-age=3600",
            },
        )

    # Sem Range — stream completo
    async def generate_full():
        async with aiofiles.open(file_path, "rb") as f:
            while True:
                data = await f.read(65536)
                if not data:
                    break
                yield data

    return StreamingResponse(
        generate_full(),
        media_type=media_type,
        headers={
            "Accept-Ranges":  "bytes",
            "Content-Length": str(file_size),
            "Cache-Control":  "public, max-age=3600",
        },
    )

# ── API admin (token protegido) ────────────────────────────────────────────────
@app.post("/api/admin/upload", dependencies=[Depends(require_admin)])
async def upload_episode(
    request:        Request,
    novela_title:   str        = Form(...),
    episode_title:  str        = Form(...),
    episode_number: int        = Form(1),
    description:    str        = Form(""),
    country_code:   str        = Form("JP"),
    coin_cost:      int        = Form(2),
    thumbnail_url:  str        = Form(""),
    duration_seconds: int      = Form(0),
    video:          UploadFile = File(...),
):
    # Validar extensão
    ext = Path(video.filename).suffix.lower() if video.filename else ""
    if ext not in ALLOWED_EXT:
        raise HTTPException(
            status_code=400,
            detail=f"Extensão não permitida: {ext}. Use: {', '.join(ALLOWED_EXT)}"
        )

    # Salvar vídeo em chunks (não carrega o arquivo inteiro na memória)
    filename  = f"{uuid.uuid4().hex}{ext}"
    file_path = UPLOAD_DIR / filename
    size      = 0

    try:
        async with aiofiles.open(file_path, "wb") as out:
            while True:
                chunk = await video.read(65536)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_FILE_MB * 1024 * 1024:
                    raise HTTPException(
                        status_code=413,
                        detail=f"Arquivo muito grande. Máximo: {MAX_FILE_MB} MB"
                    )
                await out.write(chunk)
    except HTTPException:
        file_path.unlink(missing_ok=True)
        raise

    # Upsert novela
    conn      = get_db()
    novela_id = f"novela_{uuid.uuid4().hex[:8]}"
    existing  = conn.execute(
        "SELECT id FROM novelas WHERE title=?", (novela_title,)
    ).fetchone()

    if existing:
        novela_id = existing["id"]
    else:
        conn.execute(
            "INSERT INTO novelas (id, title, country_code) VALUES (?, ?, ?)",
            (novela_id, novela_title, country_code.upper())
        )

    # Inserir episódio
    episode_id   = f"{country_code.lower()}_{novela_id}_ep{episode_number}"
    country_flag = COUNTRY_FLAGS.get(country_code.upper(), "🌍")
    coin         = 0 if episode_number == 1 else max(0, coin_cost)

    conn.execute("""
        INSERT OR REPLACE INTO episodes
        (id, novela_id, novela_title, title, description, video_filename,
         thumbnail_url, episode_number, duration_seconds, coin_cost,
         country_code, country_flag)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        episode_id, novela_id, novela_title, episode_title, description,
        filename, thumbnail_url, episode_number, duration_seconds,
        coin, country_code.upper(), country_flag
    ))

    conn.commit()
    conn.close()

    base_url = str(request.base_url).rstrip("/")
    return {
        "id":       episode_id,
        "videoUrl": f"{base_url}/api/stream/{filename}",
        "message":  "Upload concluído com sucesso",
    }


@app.get("/api/admin/episodes", dependencies=[Depends(require_admin)])
async def admin_list_episodes(request: Request):
    base_url = str(request.base_url).rstrip("/")
    conn     = get_db()
    rows     = conn.execute(
        "SELECT * FROM episodes ORDER BY novela_title, episode_number"
    ).fetchall()
    conn.close()
    return [row_to_episode(r, base_url) for r in rows]


@app.delete("/api/admin/episodes/{episode_id}", dependencies=[Depends(require_admin)])
async def delete_episode(episode_id: str):
    conn = get_db()
    row  = conn.execute(
        "SELECT video_filename FROM episodes WHERE id=?", (episode_id,)
    ).fetchone()

    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Episódio não encontrado")

    filename = row["video_filename"]
    conn.execute("DELETE FROM episodes WHERE id=?", (episode_id,))

    # Remove novela se não tiver mais episódios
    novela_id = conn.execute(
        "SELECT novela_id FROM episodes WHERE id=?", (episode_id,)
    )
    conn.commit()
    conn.close()

    file_path = UPLOAD_DIR / filename
    if file_path.exists():
        file_path.unlink()

    return {"message": "Episódio removido"}

# ── Painel Admin (HTML) ────────────────────────────────────────────────────────
ADMIN_HTML = """<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>NoveFlix — Admin</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--red:#E50914;--dark:#0A0A0A;--surface:#141414;--card:#1E1E1E;--border:#2A2A2A;--text:#FFF;--muted:#888}
body{background:var(--dark);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;min-height:100vh}
#login{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;gap:16px}
.logo{font-size:2.5rem;font-weight:900;color:var(--red)}
.logo span{color:var(--text)}
.inp{background:var(--card);border:1px solid var(--border);color:var(--text);padding:12px 16px;border-radius:8px;font-size:1rem;width:320px;outline:none}
.inp:focus{border-color:var(--red)}
.btn{background:var(--red);color:#fff;border:none;padding:12px 32px;border-radius:8px;font-size:1rem;font-weight:600;cursor:pointer;transition:.2s}
.btn:hover{background:#b20710}
.btn:disabled{opacity:.5;cursor:default}
.btn-ghost{background:transparent;border:1px solid var(--border);color:var(--text)}
.btn-sm{padding:4px 10px;font-size:.75rem}
.btn-danger{background:#c0392b}
#main{display:none}
header{background:var(--surface);border-bottom:1px solid var(--border);padding:16px 24px;display:flex;align-items:center;justify-content:space-between;gap:16px}
.header-logo{font-size:1.4rem;font-weight:900;color:var(--red);white-space:nowrap}
.stats{display:flex;gap:24px}
.stat{text-align:center}
.stat-val{font-size:1.3rem;font-weight:700;color:var(--red)}
.stat-lbl{font-size:.7rem;color:var(--muted)}
.content{display:grid;grid-template-columns:380px 1fr;min-height:calc(100vh - 65px)}
.panel{background:var(--surface);border-right:1px solid var(--border);padding:24px;overflow-y:auto}
.panel-title{font-size:.75rem;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;margin-bottom:20px}
.field{margin-bottom:14px}
.field label{display:block;font-size:.8rem;color:var(--muted);margin-bottom:5px}
.field input,.field select,.field textarea{width:100%;background:var(--card);border:1px solid var(--border);color:var(--text);padding:10px 12px;border-radius:6px;font-size:.9rem;outline:none}
.field input:focus,.field select:focus,.field textarea:focus{border-color:var(--red)}
.field textarea{resize:vertical;min-height:68px}
.field select option{background:var(--card)}
.row2{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.drop{border:2px dashed var(--border);border-radius:8px;padding:24px;text-align:center;cursor:pointer;background:var(--card);transition:.2s}
.drop:hover,.drop.drag{border-color:var(--red);background:#180808}
.drop-icon{font-size:2rem;margin-bottom:6px}
.drop-hint{color:var(--muted);font-size:.82rem}
.drop-file{color:var(--text);font-size:.82rem;margin-top:8px;font-weight:600}
.prog-wrap{display:none;margin-top:10px}
.prog-bar{background:var(--border);border-radius:4px;height:6px;overflow:hidden}
.prog-fill{background:var(--red);height:100%;width:0%;transition:width .3s}
.prog-txt{font-size:.75rem;color:var(--muted);margin-top:4px;text-align:right}
.list-area{padding:24px;overflow-y:auto}
.list-hdr{font-size:.75rem;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;margin-bottom:16px;display:flex;justify-content:space-between;align-items:center}
.search{background:var(--card);border:1px solid var(--border);color:var(--text);padding:8px 14px;border-radius:6px;font-size:.88rem;outline:none;width:220px}
.search:focus{border-color:var(--red)}
table{width:100%;border-collapse:collapse;font-size:.85rem}
th{color:var(--muted);text-align:left;padding:8px 10px;border-bottom:1px solid var(--border);font-size:.72rem;text-transform:uppercase;letter-spacing:.04em;white-space:nowrap}
td{padding:10px 10px;border-bottom:1px solid #1a1a1a;vertical-align:middle}
tr:hover td{background:#161616}
.badge{display:inline-block;padding:2px 7px;border-radius:4px;font-size:.7rem;font-weight:700}
.free{background:#1a3a1a;color:#4caf50}
.coin{background:#3a2a0a;color:#ffc107}
.thumb{width:48px;height:28px;object-fit:cover;border-radius:3px;background:var(--border);display:block}
.empty{text-align:center;padding:60px;color:var(--muted);font-size:.9rem}
.toast{position:fixed;bottom:24px;right:24px;background:var(--card);border:1px solid var(--border);color:var(--text);padding:12px 20px;border-radius:8px;font-size:.9rem;transform:translateY(100px);transition:.3s;z-index:9999;max-width:320px}
.toast.show{transform:translateY(0)}
.toast.ok{border-color:#4caf50}
.toast.err{border-color:var(--red)}
@media(max-width:900px){.content{grid-template-columns:1fr}.panel{border-right:none;border-bottom:1px solid var(--border)}}
</style>
</head>
<body>

<div id="login">
  <div class="logo">Nove<span>Flix</span></div>
  <p style="color:var(--muted);font-size:.9rem">Painel de Administração</p>
  <input id="ti" class="inp" type="password" placeholder="Token de administrador" onkeydown="if(event.key==='Enter')doLogin()">
  <button class="btn" onclick="doLogin()">Entrar</button>
</div>

<div id="main">
  <header>
    <div class="header-logo">NoveFlix Admin</div>
    <div class="stats" id="stats"></div>
    <button class="btn btn-ghost" onclick="logout()" style="padding:8px 16px;font-size:.85rem">Sair</button>
  </header>
  <div class="content">

    <!-- PAINEL UPLOAD -->
    <div class="panel">
      <div class="panel-title">Novo Episódio</div>
      <form id="uf" onsubmit="upload(event)">
        <div class="field">
          <label>Nome da Novela *</label>
          <input id="fNovela" list="dl-novelas" placeholder="Ex: Goblin" required>
          <datalist id="dl-novelas"></datalist>
        </div>
        <div class="row2">
          <div class="field">
            <label>Episódio *</label>
            <input id="fEpNum" type="number" min="1" value="1" required>
          </div>
          <div class="field">
            <label>País</label>
            <select id="fCountry">
              <option value="KR">🇰🇷 Coreia</option>
              <option value="JP">🇯🇵 Japão</option>
              <option value="TR">🇹🇷 Turquia</option>
              <option value="MX">🇲🇽 México</option>
              <option value="CN">🇨🇳 China</option>
              <option value="BR">🇧🇷 Brasil</option>
              <option value="IN">🇮🇳 Índia</option>
              <option value="PH">🇵🇭 Filipinas</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label>Título do Episódio *</label>
          <input id="fTitle" placeholder="Ex: O Encontro" required>
        </div>
        <div class="field">
          <label>Sinopse</label>
          <textarea id="fDesc" placeholder="Descrição do episódio..."></textarea>
        </div>
        <div class="row2">
          <div class="field">
            <label>Custo (moedas)</label>
            <input id="fCoins" type="number" min="0" value="2">
          </div>
          <div class="field">
            <label>Duração (seg)</label>
            <input id="fDur" type="number" min="0" value="0" placeholder="0 = auto">
          </div>
        </div>
        <div class="field">
          <label>URL da Thumbnail (TMDB ou outra)</label>
          <input id="fThumb" type="url" placeholder="https://image.tmdb.org/...">
        </div>
        <div class="field">
          <label>Arquivo de Vídeo *</label>
          <div class="drop" id="dz" onclick="document.getElementById('fVideo').click()"
               ondragover="dOver(event)" ondragleave="dLeave()" ondrop="dDrop(event)">
            <div class="drop-icon">🎬</div>
            <div class="drop-hint">Clique ou arraste o vídeo aqui</div>
            <div class="drop-hint">.mp4 &nbsp;.mkv &nbsp;.webm &nbsp;.avi &nbsp;.mov</div>
            <div class="drop-file" id="fname"></div>
          </div>
          <input id="fVideo" type="file" accept="video/*" style="display:none" onchange="fSel(this)">
        </div>
        <div class="prog-wrap" id="pw">
          <div class="prog-bar"><div class="prog-fill" id="pf"></div></div>
          <div class="prog-txt" id="pt">0%</div>
        </div>
        <button class="btn" type="submit" id="subBtn" style="width:100%;margin-top:8px">
          Fazer Upload
        </button>
      </form>
    </div>

    <!-- LISTA DE EPISÓDIOS -->
    <div class="list-area">
      <div class="list-hdr">
        <span>Episódios &nbsp;<span id="epCnt" style="color:#fff;font-weight:700"></span></span>
        <input class="search" id="sq" placeholder="Buscar..." oninput="filter()">
      </div>
      <div id="epList"><div class="empty">Nenhum episódio ainda.<br>Faça o upload do primeiro.</div></div>
    </div>

  </div>
</div>

<div class="toast" id="toast"></div>

<script>
const API = window.location.origin;
let TOKEN = localStorage.getItem('nf_tok') || '';
let ALL   = [];

if (TOKEN) initMain();

function doLogin() {
  TOKEN = document.getElementById('ti').value.trim();
  if (!TOKEN) return;
  fetch(API+'/api/admin/episodes', {headers:{'X-Admin-Token':TOKEN}})
    .then(r => r.ok ? (localStorage.setItem('nf_tok',TOKEN), initMain())
                    : toast('Token inválido','err'))
    .catch(() => toast('Erro de conexão','err'));
}

function logout() {
  localStorage.removeItem('nf_tok'); TOKEN='';
  document.getElementById('main').style.display='none';
  document.getElementById('login').style.display='flex';
}

async function initMain() {
  document.getElementById('login').style.display='none';
  document.getElementById('main').style.display='block';
  await loadEps(); await loadStats(); loadNovelas();
}

async function loadStats() {
  try {
    const [fd, nv] = await Promise.all([
      fetch(API+'/api/feed?per_page=1').then(r=>r.json()),
      fetch(API+'/api/novelas').then(r=>r.json()),
    ]);
    document.getElementById('stats').innerHTML =
      `<div class="stat"><div class="stat-val">${fd.total}</div><div class="stat-lbl">Episódios</div></div>
       <div class="stat"><div class="stat-val">${nv.length}</div><div class="stat-lbl">Novelas</div></div>`;
  } catch(e){}
}

async function loadNovelas() {
  try {
    const list = await fetch(API+'/api/novelas').then(r=>r.json());
    const dl = document.getElementById('dl-novelas');
    dl.innerHTML = list.map(n=>`<option value="${n.title}">`).join('');
  } catch(e){}
}

async function loadEps() {
  try {
    ALL = await fetch(API+'/api/admin/episodes',{headers:{'X-Admin-Token':TOKEN}}).then(r=>r.json());
    renderEps(ALL);
    document.getElementById('epCnt').textContent = '('+ALL.length+')';
  } catch(e){ toast('Erro ao carregar episódios','err'); }
}

function renderEps(eps) {
  if (!eps.length) {
    document.getElementById('epList').innerHTML='<div class="empty">Nenhum episódio encontrado.</div>';
    return;
  }
  document.getElementById('epList').innerHTML = `
    <table>
      <thead><tr>
        <th>Thumb</th><th>Novela</th><th>Ep</th><th>Título</th>
        <th>País</th><th>Moedas</th><th>Vídeo</th><th></th>
      </tr></thead>
      <tbody>
        ${eps.map(e=>`<tr>
          <td>${e.thumbnailUrl?`<img class="thumb" src="${e.thumbnailUrl}" onerror="this.style.visibility='hidden'">`:'<div class="thumb"></div>'}</td>
          <td style="font-weight:600;max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${e.novelaTitle}">${e.novelaTitle}</td>
          <td style="color:var(--muted)">Ep.${e.episodeNumber}</td>
          <td style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${e.title}">${e.title}</td>
          <td style="font-size:1.1rem">${e.countryFlag}</td>
          <td>${e.coinCost===0?'<span class="badge free">GRÁTIS</span>':`<span class="badge coin">🪙${e.coinCost}</span>`}</td>
          <td>${e.videoUrl?'<span style="color:#4caf50;font-size:.8rem">✓</span>':'<span style="color:var(--muted);font-size:.8rem">—</span>'}</td>
          <td><button class="btn btn-danger btn-sm" onclick="delEp('${e.id}')">✕</button></td>
        </tr>`).join('')}
      </tbody>
    </table>`;
}

function filter() {
  const q = document.getElementById('sq').value.toLowerCase();
  renderEps(ALL.filter(e=>
    e.novelaTitle.toLowerCase().includes(q)||
    e.title.toLowerCase().includes(q)||
    e.countryCode.toLowerCase().includes(q)
  ));
}

async function delEp(id) {
  if (!confirm('Remover este episódio e o vídeo?')) return;
  const r = await fetch(API+'/api/admin/episodes/'+id,
    {method:'DELETE', headers:{'X-Admin-Token':TOKEN}});
  r.ok ? (toast('Removido','ok'), loadEps(), loadStats())
       : toast('Erro ao remover','err');
}

async function upload(e) {
  e.preventDefault();
  const vid = document.getElementById('fVideo').files[0];
  if (!vid) { toast('Selecione um arquivo de vídeo','err'); return; }

  const fd = new FormData();
  fd.append('novela_title',    document.getElementById('fNovela').value);
  fd.append('episode_title',   document.getElementById('fTitle').value);
  fd.append('episode_number',  document.getElementById('fEpNum').value);
  fd.append('description',     document.getElementById('fDesc').value);
  fd.append('country_code',    document.getElementById('fCountry').value);
  fd.append('coin_cost',       document.getElementById('fCoins').value);
  fd.append('thumbnail_url',   document.getElementById('fThumb').value);
  fd.append('duration_seconds',document.getElementById('fDur').value);
  fd.append('video',           vid);

  const btn=document.getElementById('subBtn'),
        pw=document.getElementById('pw'),
        pf=document.getElementById('pf'),
        pt=document.getElementById('pt');

  btn.disabled=true; btn.textContent='Enviando...';
  pw.style.display='block'; pf.style.width='0%'; pt.textContent='0%';

  const xhr = new XMLHttpRequest();
  xhr.upload.onprogress = ev => {
    if (ev.lengthComputable) {
      const p = Math.round(ev.loaded/ev.total*100);
      pf.style.width=p+'%'; pt.textContent=p+'%';
    }
  };
  xhr.onload = () => {
    btn.disabled=false; btn.textContent='Fazer Upload';
    if (xhr.status===200) {
      toast('Upload concluído!','ok');
      document.getElementById('uf').reset();
      document.getElementById('fname').textContent='';
      pw.style.display='none';
      loadEps(); loadStats(); loadNovelas();
    } else {
      let msg='Erro '+xhr.status;
      try { msg = JSON.parse(xhr.responseText).detail || msg; } catch(e){}
      toast(msg,'err');
    }
  };
  xhr.onerror = () => { btn.disabled=false; btn.textContent='Fazer Upload'; toast('Erro de rede','err'); };
  xhr.open('POST', API+'/api/admin/upload');
  xhr.setRequestHeader('X-Admin-Token', TOKEN);
  xhr.send(fd);
}

// Drop zone
function dOver(e){ e.preventDefault(); document.getElementById('dz').classList.add('drag'); }
function dLeave(){ document.getElementById('dz').classList.remove('drag'); }
function dDrop(e){ e.preventDefault(); dLeave(); const f=e.dataTransfer.files[0]; if(f) setFile(f); }
function fSel(i){ if(i.files[0]) setFile(i.files[0]); }
function setFile(f){
  const dt=new DataTransfer(); dt.items.add(f);
  document.getElementById('fVideo').files=dt.files;
  document.getElementById('fname').textContent='📎 '+f.name+' ('+(f.size/1048576).toFixed(1)+' MB)';
}

// Toast
function toast(msg,type=''){
  const t=document.getElementById('toast');
  t.textContent=msg; t.className='toast show '+(type||'');
  setTimeout(()=>t.classList.remove('show'),3000);
}
</script>
</body>
</html>"""


@app.get("/admin", response_class=HTMLResponse)
async def admin_panel():
    return HTMLResponse(ADMIN_HTML)


# ── Entry point ────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=PORT, reload=False)
