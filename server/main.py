"""
NoveFlix — Servidor de Streaming v2.0
FastAPI + SQLite + HTTP Range + Batch Upload + FFprobe automático

Variáveis de ambiente:
  ADMIN_TOKEN  — token do painel admin (padrão: noveflix2024)
  MAX_FILE_MB  — tamanho máximo por upload em MB (padrão: 500)
  PORT         — porta do servidor (padrão: 8080)
"""

import os
import re
import uuid
import json
import sqlite3
import subprocess
import aiofiles
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Header, Request, Depends
from fastapi.responses import StreamingResponse, HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional

# ── Configuração ───────────────────────────────────────────────────────────────
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "noveflix2024")
UPLOAD_DIR  = Path("uploads")
THUMB_DIR   = Path("uploads/thumbs")
DB_PATH     = "noveflix.db"
MAX_FILE_MB = int(os.environ.get("MAX_FILE_MB", "500"))
PORT        = int(os.environ.get("PORT", 8080))

UPLOAD_DIR.mkdir(exist_ok=True)
THUMB_DIR.mkdir(exist_ok=True)

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
app = FastAPI(title="NoveFlix API", version="2.0.0", docs_url="/docs")

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
    thumb = ep.get("thumbnail_url", "")
    if thumb and not thumb.startswith("http"):
        ep["thumbnailUrl"] = f"{base_url}/api/thumbs/{thumb}"
    else:
        ep["thumbnailUrl"] = thumb
    ep.pop("thumbnail_url", None)
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


def probe_video(file_path: Path) -> dict:
    """Extrai duração e metadados via ffprobe. Retorna {} se não disponível."""
    try:
        result = subprocess.run(
            [
                "ffprobe", "-v", "quiet", "-print_format", "json",
                "-show_format", "-show_streams", str(file_path)
            ],
            capture_output=True, text=True, timeout=30
        )
        if result.returncode == 0:
            data = json.loads(result.stdout)
            duration = 0
            if "format" in data and "duration" in data["format"]:
                duration = int(float(data["format"]["duration"]))
            return {"duration_seconds": duration}
    except Exception:
        pass
    return {"duration_seconds": 0}


def extract_thumbnail(video_path: Path, thumb_name: str) -> bool:
    """Extrai frame do vídeo como thumbnail via ffmpeg. Retorna True se ok."""
    thumb_path = THUMB_DIR / thumb_name
    try:
        result = subprocess.run(
            [
                "ffmpeg", "-y", "-i", str(video_path),
                "-ss", "00:00:05", "-vframes", "1",
                "-vf", "scale=320:-1",
                "-q:v", "3",
                str(thumb_path)
            ],
            capture_output=True, timeout=60
        )
        return result.returncode == 0 and thumb_path.exists()
    except Exception:
        return False


def parse_filename(filename: str) -> dict:
    """
    Tenta extrair da filename: número de episódio e título.
    Exemplos reconhecidos:
      Goblin_E01_O_Encontro.mp4     → ep=1, title="O Encontro"
      goblin-ep02-amor.mp4          → ep=2, title="amor"
      S01E03 - Titulo Aqui.mp4      → ep=3, title="Titulo Aqui"
      03.mp4                        → ep=3, title=""
    """
    stem = Path(filename).stem
    # S01E03 ou E03
    m = re.search(r"[Ss]\d+[Ee](\d+)", stem)
    if m:
        ep_num = int(m.group(1))
        title  = re.sub(r"[Ss]\d+[Ee]\d+", "", stem).strip(" -_.")
        return {"episode_number": ep_num, "title": title or stem}
    # ep02 / EP_02
    m = re.search(r"[Ee][Pp][_\-]?(\d+)", stem)
    if m:
        ep_num = int(m.group(1))
        title  = re.sub(r"[Ee][Pp][_\-]?\d+", "", stem).strip(" -_.")
        return {"episode_number": ep_num, "title": title or stem}
    # _E01_ ou -E01-
    m = re.search(r"[_\-][Ee](\d{1,3})[_\-]", stem)
    if m:
        ep_num = int(m.group(1))
        before = stem[:m.start()].strip("_-")
        after  = stem[m.end():].strip("_-")
        title  = (after or before or stem).replace("_", " ").replace("-", " ")
        return {"episode_number": ep_num, "title": title}
    # Só número no início: "03 - Titulo"
    m = re.match(r"^(\d{1,3})[\s\-_.]+(.+)$", stem)
    if m:
        return {"episode_number": int(m.group(1)), "title": m.group(2).replace("_", " ")}
    # Só número: "03"
    m = re.match(r"^(\d{1,3})$", stem)
    if m:
        return {"episode_number": int(m.group(1)), "title": ""}
    return {"episode_number": 1, "title": stem.replace("_", " ").replace("-", " ")}


def upsert_novela(conn, title: str, country_code: str, description: str = "", poster_url: str = "") -> str:
    existing = conn.execute("SELECT id FROM novelas WHERE title=?", (title,)).fetchone()
    if existing:
        return existing["id"]
    novela_id = f"novela_{uuid.uuid4().hex[:10]}"
    conn.execute(
        "INSERT INTO novelas (id, title, description, country_code, poster_url) VALUES (?, ?, ?, ?, ?)",
        (novela_id, title, description, country_code.upper(), poster_url)
    )
    return novela_id


# ── API pública ────────────────────────────────────────────────────────────────
@app.get("/health")
async def health():
    return {"status": "ok", "service": "NoveFlix Streaming API", "version": "2.0.0"}


@app.get("/api/feed")
async def get_feed(request: Request, page: int = 1, per_page: int = 20):
    offset   = (page - 1) * per_page
    base_url = str(request.base_url).rstrip("/")
    conn     = get_db()
    rows     = conn.execute(
        "SELECT * FROM episodes ORDER BY novela_title ASC, episode_number ASC LIMIT ? OFFSET ?",
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
async def list_novelas(request: Request):
    base_url = str(request.base_url).rstrip("/")
    conn  = get_db()
    rows  = conn.execute("SELECT * FROM novelas ORDER BY title").fetchall()
    novelas = []
    for r in rows:
        n = dict(r)
        poster = n.get("poster_url", "")
        if poster and not poster.startswith("http"):
            n["poster_url"] = f"{base_url}/api/thumbs/{poster}"
        ep_count = conn.execute(
            "SELECT COUNT(*) FROM episodes WHERE novela_id=?", (n["id"],)
        ).fetchone()[0]
        n["episode_count"] = ep_count
        novelas.append(n)
    conn.close()
    return novelas


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
    conn = get_db()
    conn.execute("UPDATE episodes SET view_count = view_count + 1 WHERE id=?", (episode_id,))
    conn.commit()
    conn.close()
    return {"ok": True}


@app.post("/api/episodes/{episode_id}/like")
async def toggle_like(episode_id: str, liked: bool = True):
    conn = get_db()
    if liked:
        conn.execute("UPDATE episodes SET like_count = like_count + 1 WHERE id=?", (episode_id,))
    else:
        conn.execute("UPDATE episodes SET like_count = MAX(0, like_count - 1) WHERE id=?", (episode_id,))
    conn.commit()
    conn.close()
    return {"ok": True}


# ── Thumbnail estático ────────────────────────────────────────────────────────
@app.get("/api/thumbs/{filename}")
async def serve_thumb(filename: str):
    if not re.match(r"^[a-zA-Z0-9._-]+$", filename):
        raise HTTPException(status_code=400, detail="Nome inválido")
    path = THUMB_DIR / filename
    if not path.exists():
        raise HTTPException(status_code=404, detail="Thumbnail não encontrada")
    async def gen():
        async with aiofiles.open(path, "rb") as f:
            while True:
                data = await f.read(65536)
                if not data:
                    break
                yield data
    return StreamingResponse(gen(), media_type="image/jpeg")


# ── Streaming de vídeo com HTTP Range ──────────────────────────────────────────
@app.get("/api/stream/{filename}")
async def stream_video(filename: str, request: Request):
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


# ── API admin ─────────────────────────────────────────────────────────────────

@app.post("/api/admin/novelas", dependencies=[Depends(require_admin)])
async def create_novela(
    request:      Request,
    title:        str        = Form(...),
    description:  str        = Form(""),
    country_code: str        = Form("KR"),
    poster:       UploadFile = File(None),
):
    conn = get_db()
    existing = conn.execute("SELECT id FROM novelas WHERE title=?", (title,)).fetchone()
    if existing:
        conn.close()
        raise HTTPException(status_code=409, detail="Novela já existe com este título")

    poster_url = ""
    if poster and poster.filename:
        ext = Path(poster.filename).suffix.lower()
        if ext not in {".jpg", ".jpeg", ".png", ".webp"}:
            conn.close()
            raise HTTPException(status_code=400, detail="Poster deve ser jpg/png/webp")
        poster_name = f"poster_{uuid.uuid4().hex[:12]}{ext}"
        poster_path = THUMB_DIR / poster_name
        async with aiofiles.open(poster_path, "wb") as f:
            while True:
                chunk = await poster.read(65536)
                if not chunk:
                    break
                await f.write(chunk)
        poster_url = poster_name

    novela_id = f"novela_{uuid.uuid4().hex[:10]}"
    conn.execute(
        "INSERT INTO novelas (id, title, description, country_code, poster_url) VALUES (?, ?, ?, ?, ?)",
        (novela_id, title, description, country_code.upper(), poster_url)
    )
    conn.commit()
    conn.close()
    base_url = str(request.base_url).rstrip("/")
    return {
        "id":          novela_id,
        "title":       title,
        "description": description,
        "country_code": country_code.upper(),
        "poster_url":  f"{base_url}/api/thumbs/{poster_url}" if poster_url else "",
    }


@app.put("/api/admin/novelas/{novela_id}", dependencies=[Depends(require_admin)])
async def update_novela(
    novela_id:    str,
    request:      Request,
    title:        str        = Form(...),
    description:  str        = Form(""),
    country_code: str        = Form("KR"),
    poster:       UploadFile = File(None),
):
    conn = get_db()
    row = conn.execute("SELECT * FROM novelas WHERE id=?", (novela_id,)).fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Novela não encontrada")

    poster_url = dict(row)["poster_url"]
    if poster and poster.filename:
        ext = Path(poster.filename).suffix.lower()
        if ext not in {".jpg", ".jpeg", ".png", ".webp"}:
            conn.close()
            raise HTTPException(status_code=400, detail="Poster deve ser jpg/png/webp")
        poster_name = f"poster_{uuid.uuid4().hex[:12]}{ext}"
        poster_path = THUMB_DIR / poster_name
        async with aiofiles.open(poster_path, "wb") as f:
            while True:
                chunk = await poster.read(65536)
                if not chunk:
                    break
                await f.write(chunk)
        if poster_url and not poster_url.startswith("http"):
            old = THUMB_DIR / poster_url
            if old.exists():
                old.unlink(missing_ok=True)
        poster_url = poster_name

    conn.execute(
        "UPDATE novelas SET title=?, description=?, country_code=?, poster_url=? WHERE id=?",
        (title, description, country_code.upper(), poster_url, novela_id)
    )
    conn.execute(
        "UPDATE episodes SET novela_title=?, country_code=?, country_flag=? WHERE novela_id=?",
        (title, country_code.upper(), COUNTRY_FLAGS.get(country_code.upper(), "🌍"), novela_id)
    )
    conn.commit()
    conn.close()
    return {"ok": True}


@app.delete("/api/admin/novelas/{novela_id}", dependencies=[Depends(require_admin)])
async def delete_novela(novela_id: str):
    conn = get_db()
    eps = conn.execute(
        "SELECT video_filename, thumbnail_url FROM episodes WHERE novela_id=?", (novela_id,)
    ).fetchall()
    for ep in eps:
        vf = ep["video_filename"]
        if vf:
            p = UPLOAD_DIR / vf
            if p.exists():
                p.unlink(missing_ok=True)
        tf = ep["thumbnail_url"]
        if tf and not tf.startswith("http"):
            p = THUMB_DIR / tf
            if p.exists():
                p.unlink(missing_ok=True)
    conn.execute("DELETE FROM episodes WHERE novela_id=?", (novela_id,))
    row = conn.execute("SELECT poster_url FROM novelas WHERE id=?", (novela_id,)).fetchone()
    if row and row["poster_url"] and not row["poster_url"].startswith("http"):
        p = THUMB_DIR / row["poster_url"]
        if p.exists():
            p.unlink(missing_ok=True)
    conn.execute("DELETE FROM novelas WHERE id=?", (novela_id,))
    conn.commit()
    conn.close()
    return {"message": "Novela e todos os episódios removidos"}


@app.post("/api/admin/upload", dependencies=[Depends(require_admin)])
async def upload_episode(
    request:          Request,
    novela_id:        str        = Form(...),
    episode_title:    str        = Form(""),
    episode_number:   int        = Form(1),
    description:      str        = Form(""),
    coin_cost:        int        = Form(2),
    thumbnail_url:    str        = Form(""),
    duration_seconds: int        = Form(0),
    video:            UploadFile = File(...),
):
    conn = get_db()
    novela = conn.execute("SELECT * FROM novelas WHERE id=?", (novela_id,)).fetchone()
    if not novela:
        conn.close()
        raise HTTPException(status_code=404, detail="Novela não encontrada")
    novela = dict(novela)
    conn.close()

    ext = Path(video.filename).suffix.lower() if video.filename else ""
    if ext not in ALLOWED_EXT:
        raise HTTPException(status_code=400, detail=f"Extensão não permitida: {ext}")

    parsed = parse_filename(video.filename or "")
    if not episode_title:
        episode_title = parsed["title"] or f"Episódio {episode_number}"
    if episode_number == 1 and parsed["episode_number"] > 1:
        episode_number = parsed["episode_number"]

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
                    raise HTTPException(status_code=413, detail=f"Arquivo muito grande. Máximo: {MAX_FILE_MB} MB")
                await out.write(chunk)
    except HTTPException:
        file_path.unlink(missing_ok=True)
        raise

    if duration_seconds == 0:
        probe = probe_video(file_path)
        duration_seconds = probe["duration_seconds"]

    thumb_ref = thumbnail_url
    if not thumb_ref:
        thumb_name = f"thumb_{uuid.uuid4().hex[:12]}.jpg"
        if extract_thumbnail(file_path, thumb_name):
            thumb_ref = thumb_name
        else:
            thumb_ref = ""

    country_code = novela["country_code"]
    country_flag = COUNTRY_FLAGS.get(country_code, "🌍")
    coin         = 0 if episode_number == 1 else max(0, coin_cost)
    episode_id   = f"{country_code.lower()}_{novela_id}_ep{episode_number}"

    conn = get_db()
    conn.execute("""
        INSERT OR REPLACE INTO episodes
        (id, novela_id, novela_title, title, description, video_filename,
         thumbnail_url, episode_number, duration_seconds, coin_cost,
         country_code, country_flag)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        episode_id, novela_id, novela["title"], episode_title, description,
        filename, thumb_ref, episode_number, duration_seconds,
        coin, country_code, country_flag
    ))
    conn.commit()
    conn.close()

    base_url = str(request.base_url).rstrip("/")
    thumb_url = f"{base_url}/api/thumbs/{thumb_ref}" if thumb_ref and not thumb_ref.startswith("http") else thumb_ref
    return {
        "id":              episode_id,
        "videoUrl":        f"{base_url}/api/stream/{filename}",
        "thumbnailUrl":    thumb_url,
        "durationSeconds": duration_seconds,
        "message":         "Upload concluído com sucesso",
    }


@app.post("/api/admin/upload-batch", dependencies=[Depends(require_admin)])
async def upload_batch(
    request:   Request,
    novela_id: str              = Form(...),
    start_ep:  int              = Form(1),
    coin_cost: int              = Form(2),
    videos:    List[UploadFile] = File(...),
):
    conn = get_db()
    novela = conn.execute("SELECT * FROM novelas WHERE id=?", (novela_id,)).fetchone()
    if not novela:
        conn.close()
        raise HTTPException(status_code=404, detail="Novela não encontrada")
    novela = dict(novela)
    conn.close()

    results  = []
    base_url = str(request.base_url).rstrip("/")
    sorted_videos = sorted(videos, key=lambda v: v.filename or "")

    for idx, video in enumerate(sorted_videos):
        ext = Path(video.filename).suffix.lower() if video.filename else ""
        if ext not in ALLOWED_EXT:
            results.append({"file": video.filename, "error": f"Extensão não permitida: {ext}"})
            continue

        parsed  = parse_filename(video.filename or "")
        ep_num  = parsed["episode_number"]
        if ep_num <= 1 and idx > 0:
            ep_num = start_ep + idx
        elif ep_num == 1 and idx == 0:
            ep_num = start_ep
        ep_title = parsed["title"] or f"Episódio {ep_num}"

        filename  = f"{uuid.uuid4().hex}{ext}"
        file_path = UPLOAD_DIR / filename
        size      = 0
        ok        = True

        try:
            async with aiofiles.open(file_path, "wb") as out:
                while True:
                    chunk = await video.read(65536)
                    if not chunk:
                        break
                    size += len(chunk)
                    if size > MAX_FILE_MB * 1024 * 1024:
                        ok = False
                        break
                    await out.write(chunk)
        except Exception:
            ok = False

        if not ok:
            file_path.unlink(missing_ok=True)
            results.append({"file": video.filename, "error": "Falha ao salvar arquivo"})
            continue

        probe = probe_video(file_path)
        duration_seconds = probe["duration_seconds"]

        thumb_name = f"thumb_{uuid.uuid4().hex[:12]}.jpg"
        if not extract_thumbnail(file_path, thumb_name):
            thumb_name = ""

        country_code = novela["country_code"]
        country_flag = COUNTRY_FLAGS.get(country_code, "🌍")
        coin         = 0 if ep_num == 1 else max(0, coin_cost)
        episode_id   = f"{country_code.lower()}_{novela_id}_ep{ep_num}"

        conn = get_db()
        conn.execute("""
            INSERT OR REPLACE INTO episodes
            (id, novela_id, novela_title, title, description, video_filename,
             thumbnail_url, episode_number, duration_seconds, coin_cost,
             country_code, country_flag)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            episode_id, novela_id, novela["title"], ep_title, "",
            filename, thumb_name, ep_num, duration_seconds,
            coin, country_code, country_flag
        ))
        conn.commit()
        conn.close()

        thumb_url = f"{base_url}/api/thumbs/{thumb_name}" if thumb_name else ""
        results.append({
            "file":            video.filename,
            "id":              episode_id,
            "episodeNumber":   ep_num,
            "title":           ep_title,
            "durationSeconds": duration_seconds,
            "thumbnailUrl":    thumb_url,
            "videoUrl":        f"{base_url}/api/stream/{filename}",
        })

    return {"processed": len(results), "results": results}


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
        "SELECT video_filename, thumbnail_url, novela_id FROM episodes WHERE id=?", (episode_id,)
    ).fetchone()

    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Episódio não encontrado")

    filename  = row["video_filename"]
    thumb     = row["thumbnail_url"]

    conn.execute("DELETE FROM episodes WHERE id=?", (episode_id,))
    conn.commit()
    conn.close()

    if filename:
        p = UPLOAD_DIR / filename
        if p.exists():
            p.unlink(missing_ok=True)
    if thumb and not thumb.startswith("http"):
        p = THUMB_DIR / thumb
        if p.exists():
            p.unlink(missing_ok=True)

    return {"message": "Episódio removido"}

# ── Painel Admin (HTML) ────────────────────────────────────────────────────────
ADMIN_HTML = r"""<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>NoveFlix Admin</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--red:#E50914;--dark:#0A0A0A;--surface:#141414;--card:#1E1E1E;--border:#2A2A2A;--text:#FFF;--muted:#888;--green:#4caf50;--gold:#ffc107}
body{background:var(--dark);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;min-height:100vh}
#login{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;gap:16px}
.logo{font-size:2.5rem;font-weight:900;color:var(--red)}.logo span{color:var(--text)}
.inp{background:var(--card);border:1px solid var(--border);color:var(--text);padding:12px 16px;border-radius:8px;font-size:1rem;width:320px;outline:none}.inp:focus{border-color:var(--red)}
.btn{background:var(--red);color:#fff;border:none;padding:10px 24px;border-radius:8px;font-size:.9rem;font-weight:600;cursor:pointer;transition:.2s}.btn:hover{background:#b20710}.btn:disabled{opacity:.5;cursor:default}
.btn-ghost{background:transparent;border:1px solid var(--border);color:var(--text)}.btn-ghost:hover{border-color:var(--red);background:transparent}
.btn-sm{padding:4px 10px;font-size:.75rem}.btn-danger{background:#c0392b}.btn-green{background:#1e7e34}.btn-green:hover{background:#155724}
#main{display:none}
header{background:var(--surface);border-bottom:1px solid var(--border);padding:14px 24px;display:flex;align-items:center;justify-content:space-between;gap:16px;position:sticky;top:0;z-index:100}
.header-logo{font-size:1.4rem;font-weight:900;color:var(--red)}
.stats{display:flex;gap:20px}.stat{text-align:center}.stat-val{font-size:1.2rem;font-weight:700;color:var(--red)}.stat-lbl{font-size:.68rem;color:var(--muted)}
.tabs{display:flex;border-bottom:2px solid var(--border);background:var(--surface);padding:0 24px}
.tab{padding:14px 20px;cursor:pointer;font-size:.88rem;font-weight:600;color:var(--muted);border-bottom:2px solid transparent;margin-bottom:-2px;transition:.2s;white-space:nowrap}.tab.active{color:var(--red);border-bottom-color:var(--red)}
.tab-panel{display:none;padding:24px}.tab-panel.active{display:block}
.form-card{background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:24px;max-width:680px}
.form-title{font-size:.75rem;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;margin-bottom:20px;font-weight:700}
.field{margin-bottom:14px}.field label{display:block;font-size:.8rem;color:var(--muted);margin-bottom:5px}
.field input,.field select,.field textarea{width:100%;background:var(--card);border:1px solid var(--border);color:var(--text);padding:10px 12px;border-radius:6px;font-size:.9rem;outline:none}
.field input:focus,.field select:focus,.field textarea:focus{border-color:var(--red)}
.field textarea{resize:vertical;min-height:68px}.field select option{background:var(--card)}
.row2{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.drop{border:2px dashed var(--border);border-radius:8px;padding:24px;text-align:center;cursor:pointer;background:var(--card);transition:.2s}.drop:hover,.drop.drag{border-color:var(--red);background:#180808}
.drop-icon{font-size:2rem;margin-bottom:6px}.drop-hint{color:var(--muted);font-size:.82rem}.drop-file{color:var(--text);font-size:.82rem;margin-top:8px;font-weight:600}
.drop-multi{border:2px dashed var(--border);border-radius:8px;padding:32px;text-align:center;cursor:pointer;background:var(--card);transition:.2s}.drop-multi:hover,.drop-multi.drag{border-color:var(--green);background:#0a1f0a}
.prog-wrap{display:none;margin-top:10px}.prog-bar{background:var(--border);border-radius:4px;height:6px;overflow:hidden}.prog-fill{background:var(--red);height:100%;width:0%;transition:width .3s}.prog-txt{font-size:.75rem;color:var(--muted);margin-top:4px;text-align:right}
.batch-list{margin-top:16px;display:flex;flex-direction:column;gap:8px;max-height:320px;overflow-y:auto}
.batch-item{background:var(--card);border:1px solid var(--border);border-radius:8px;padding:10px 14px;display:grid;grid-template-columns:1fr auto auto auto;gap:10px;align-items:center;font-size:.85rem}
.batch-item.done{border-color:var(--green)}.batch-item.err{border-color:var(--red)}
.batch-fname{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--muted)}.batch-ep{color:var(--text);font-weight:600;min-width:60px;text-align:center}.batch-dur{color:var(--muted);font-size:.75rem;min-width:50px;text-align:right}.batch-st{font-size:.75rem;min-width:60px;text-align:right}
.list-hdr{font-size:.75rem;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;margin-bottom:16px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px}
.search{background:var(--card);border:1px solid var(--border);color:var(--text);padding:8px 14px;border-radius:6px;font-size:.88rem;outline:none;width:220px}.search:focus{border-color:var(--red)}
table{width:100%;border-collapse:collapse;font-size:.85rem}
th{color:var(--muted);text-align:left;padding:8px 10px;border-bottom:1px solid var(--border);font-size:.72rem;text-transform:uppercase;letter-spacing:.04em;white-space:nowrap}
td{padding:10px 10px;border-bottom:1px solid #1a1a1a;vertical-align:middle}
tr:hover td{background:#161616}
.badge{display:inline-block;padding:2px 7px;border-radius:4px;font-size:.7rem;font-weight:700}.free{background:#1a3a1a;color:#4caf50}.coin{background:#3a2a0a;color:#ffc107}
.thumb{width:56px;height:32px;object-fit:cover;border-radius:3px;background:var(--border);display:block}
.empty{text-align:center;padding:60px;color:var(--muted);font-size:.9rem}
.novela-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px;margin-top:4px}
.novela-card{background:var(--card);border:1px solid var(--border);border-radius:10px;overflow:hidden;transition:.2s}.novela-card:hover{border-color:var(--red);transform:translateY(-2px)}
.novela-poster{width:100%;aspect-ratio:2/3;object-fit:cover;background:var(--border);display:block}
.novela-info{padding:10px}.novela-name{font-size:.88rem;font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.novela-meta{font-size:.72rem;color:var(--muted);margin-top:2px}
.novela-actions{display:flex;gap:6px;padding:0 10px 10px}
.overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.75);z-index:500;align-items:center;justify-content:center}.overlay.show{display:flex}
.modal{background:var(--surface);border:1px solid var(--border);border-radius:14px;padding:28px;width:min(520px,95vw);max-height:90vh;overflow-y:auto}
.modal-title{font-size:1rem;font-weight:700;margin-bottom:20px;display:flex;justify-content:space-between;align-items:center}
.modal-close{background:none;border:none;color:var(--muted);font-size:1.2rem;cursor:pointer;padding:4px 8px}
.toast{position:fixed;bottom:24px;right:24px;background:var(--card);border:1px solid var(--border);color:var(--text);padding:12px 20px;border-radius:8px;font-size:.9rem;transform:translateY(100px);transition:.3s;z-index:9999;max-width:340px}.toast.show{transform:translateY(0)}.toast.ok{border-color:var(--green)}.toast.err{border-color:var(--red)}
.chip{display:inline-block;background:#1e2a3a;color:#7eb8f7;border-radius:4px;padding:1px 6px;font-size:.72rem;margin-left:4px}
@media(max-width:700px){.row2{grid-template-columns:1fr}.batch-item{grid-template-columns:1fr auto}}
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
  <div class="tabs">
    <div class="tab active" onclick="switchTab('novelas')">📺 Novelas</div>
    <div class="tab" onclick="switchTab('upload')">⬆️ Upload Único</div>
    <div class="tab" onclick="switchTab('batch')">📦 Upload em Lote</div>
    <div class="tab" onclick="switchTab('episodes')">🎬 Episódios</div>
  </div>
  <div class="tab-panel active" id="tab-novelas">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <div class="list-hdr" style="margin:0">Novelas <span id="novelaCnt" style="color:#fff;font-weight:700"></span></div>
      <button class="btn btn-green" onclick="openNovelModal()">+ Nova Novela</button>
    </div>
    <div class="novela-grid" id="novelaGrid"><div class="empty">Nenhuma novela. Crie a primeira!</div></div>
  </div>
  <div class="tab-panel" id="tab-upload">
    <div class="form-card">
      <div class="form-title">Upload de Episódio</div>
      <form id="uf" onsubmit="uploadSingle(event)">
        <div class="field"><label>Novela *</label><select id="fNovelaId" required><option value="">— Selecione —</option></select></div>
        <div class="row2">
          <div class="field"><label>Nº Episódio *</label><input id="fEpNum" type="number" min="1" value="1" required></div>
          <div class="field"><label>Custo (moedas)</label><input id="fCoins" type="number" min="0" value="2"></div>
        </div>
        <div class="field"><label>Título <span class="chip">auto</span></label><input id="fTitle" placeholder="Deixe vazio para extrair do nome do arquivo"></div>
        <div class="field"><label>Sinopse</label><textarea id="fDesc" placeholder="Descrição..."></textarea></div>
        <div class="field"><label>Duração (seg) <span class="chip">ffprobe auto</span></label><input id="fDur" type="number" min="0" value="0" placeholder="0 = detectar automaticamente"></div>
        <div class="field"><label>URL Thumbnail (opcional) <span class="chip">auto do vídeo</span></label><input id="fThumb" type="url" placeholder="https://..."></div>
        <div class="field">
          <label>Arquivo de Vídeo *</label>
          <div class="drop" id="dz" onclick="document.getElementById('fVideo').click()" ondragover="dOver(event)" ondragleave="dLeave(event)" ondrop="dDrop(event)">
            <div class="drop-icon">🎬</div>
            <div class="drop-hint">Clique ou arraste o vídeo aqui</div>
            <div class="drop-hint">.mp4 .mkv .webm .avi .mov</div>
            <div class="drop-file" id="fname"></div>
          </div>
          <input id="fVideo" type="file" accept="video/*" style="display:none" onchange="fSel(this)">
        </div>
        <div class="prog-wrap" id="pw"><div class="prog-bar"><div class="prog-fill" id="pf"></div></div><div class="prog-txt" id="pt">0%</div></div>
        <button class="btn" type="submit" id="subBtn" style="width:100%;margin-top:8px">Fazer Upload</button>
      </form>
    </div>
  </div>
  <div class="tab-panel" id="tab-batch">
    <div class="form-card" style="max-width:780px">
      <div class="form-title">Upload em Lote — Múltiplos Episódios</div>
      <div class="row2" style="margin-bottom:14px">
        <div class="field"><label>Novela *</label><select id="bNovelaId"><option value="">— Selecione —</option></select></div>
        <div class="row2">
          <div class="field"><label>Ep. inicial</label><input id="bStartEp" type="number" min="1" value="1"></div>
          <div class="field"><label>Custo (moedas)</label><input id="bCoins" type="number" min="0" value="2"></div>
        </div>
      </div>
      <div class="field" style="margin-bottom:20px">
        <label>Arquivos de Vídeo * <span class="chip">título e ep. extraídos do nome do arquivo</span></label>
        <div class="drop-multi" id="bdz" onclick="document.getElementById('bVideos').click()" ondragover="bdOver(event)" ondragleave="bdLeave(event)" ondrop="bdDrop(event)">
          <div class="drop-icon" style="font-size:2.5rem;color:var(--green)">📁</div>
          <div class="drop-hint" style="font-size:.9rem;margin-top:4px">Clique ou arraste <strong style="color:var(--green)">múltiplos vídeos</strong> aqui</div>
          <div class="drop-hint">.mp4 .mkv .webm .avi .mov</div>
          <div class="drop-file" id="bfnames" style="color:var(--green)"></div>
        </div>
        <input id="bVideos" type="file" accept="video/*" multiple style="display:none" onchange="bfSel(this)">
      </div>
      <div id="batchPreview" style="display:none">
        <div style="font-size:.8rem;color:var(--muted);margin-bottom:10px">Pré-visualização:</div>
        <div class="batch-list" id="batchItems"></div>
        <div class="prog-wrap" id="bpw" style="margin-top:16px"><div class="prog-bar"><div class="prog-fill" id="bpf" style="background:var(--green)"></div></div><div class="prog-txt" id="bpt">Enviando...</div></div>
        <button class="btn btn-green" id="batchBtn" onclick="uploadBatch()" style="width:100%;margin-top:16px">📦 Enviar Todos</button>
      </div>
    </div>
  </div>
  <div class="tab-panel" id="tab-episodes">
    <div class="list-hdr">
      <span>Episódios <span id="epCnt" style="color:#fff;font-weight:700"></span></span>
      <input class="search" id="sq" placeholder="Buscar..." oninput="filterEps()">
    </div>
    <div id="epList"><div class="empty">Carregando...</div></div>
  </div>
</div>
<div class="overlay" id="novelModal">
  <div class="modal">
    <div class="modal-title"><span id="novelModalTitle">Nova Novela</span><button class="modal-close" onclick="closeNovelModal()">✕</button></div>
    <form id="novelForm" onsubmit="saveNovela(event)">
      <input type="hidden" id="nId">
      <div class="field"><label>Título *</label><input id="nTitle" placeholder="Nome da novela" required></div>
      <div class="field"><label>País</label>
        <select id="nCountry">
          <option value="KR">🇰🇷 Coreia</option><option value="JP">🇯🇵 Japão</option>
          <option value="TR">🇹🇷 Turquia</option><option value="MX">🇲🇽 México</option>
          <option value="CN">🇨🇳 China</option><option value="BR">🇧🇷 Brasil</option>
          <option value="IN">🇮🇳 Índia</option><option value="PH">🇵🇭 Filipinas</option><option value="US">🇺🇸 EUA</option>
        </select>
      </div>
      <div class="field"><label>Sinopse</label><textarea id="nDesc" placeholder="Descrição da novela..."></textarea></div>
      <div class="field">
        <label>Poster (capa da novela)</label>
        <div class="drop" id="pdz" onclick="document.getElementById('posterFile').click()" ondragover="pdOver(event)" ondragleave="pdLeave(event)" ondrop="pdDrop(event)" style="padding:16px">
          <div class="drop-icon" style="font-size:1.5rem">🖼️</div>
          <div class="drop-hint">Clique ou arraste o poster (.jpg .png .webp)</div>
          <div class="drop-file" id="pname"></div>
        </div>
        <input id="posterFile" type="file" accept="image/jpeg,image/png,image/webp" style="display:none" onchange="posterSel(this)">
        <div id="posterPreviewWrap" style="margin-top:8px;display:none"><img id="posterPreview" style="height:120px;border-radius:6px;object-fit:cover"></div>
      </div>
      <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:8px">
        <button type="button" class="btn btn-ghost" onclick="closeNovelModal()">Cancelar</button>
        <button type="submit" class="btn">Salvar</button>
      </div>
    </form>
  </div>
</div>
<div class="toast" id="toast"></div>
<script>
const API=window.location.origin;
let TOKEN=localStorage.getItem('nf_tok')||'';
let ALL_EPS=[],ALL_NOVELAS=[],BATCH_FILES=[];
if(TOKEN)initMain();
function doLogin(){TOKEN=document.getElementById('ti').value.trim();if(!TOKEN)return;fetch(API+'/api/admin/episodes',{headers:{'X-Admin-Token':TOKEN}}).then(r=>r.ok?(localStorage.setItem('nf_tok',TOKEN),initMain()):toast('Token inválido','err')).catch(()=>toast('Erro de conexão','err'));}
function logout(){localStorage.removeItem('nf_tok');TOKEN='';document.getElementById('main').style.display='none';document.getElementById('login').style.display='flex';}
async function initMain(){document.getElementById('login').style.display='none';document.getElementById('main').style.display='block';await Promise.all([loadNovelas(),loadEps()]);loadStats();}
async function loadStats(){try{const[fd,nv]=await Promise.all([fetch(API+'/api/feed?per_page=1').then(r=>r.json()),fetch(API+'/api/novelas').then(r=>r.json())]);document.getElementById('stats').innerHTML=`<div class="stat"><div class="stat-val">${fd.total}</div><div class="stat-lbl">Episódios</div></div><div class="stat"><div class="stat-val">${nv.length}</div><div class="stat-lbl">Novelas</div></div>`;}catch(e){}}
function switchTab(name){document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));document.querySelectorAll('.tab-panel').forEach(p=>p.classList.remove('active'));const tabs=['novelas','upload','batch','episodes'];document.querySelectorAll('.tab')[tabs.indexOf(name)].classList.add('active');document.getElementById('tab-'+name).classList.add('active');}
async function loadNovelas(){try{ALL_NOVELAS=await fetch(API+'/api/novelas').then(r=>r.json());renderNovelas();populateNovelSelects();document.getElementById('novelaCnt').textContent='('+ALL_NOVELAS.length+')';}catch(e){toast('Erro ao carregar novelas','err');}}
function renderNovelas(){const g=document.getElementById('novelaGrid');if(!ALL_NOVELAS.length){g.innerHTML='<div class="empty">Nenhuma novela. Crie a primeira!</div>';return;}g.innerHTML=ALL_NOVELAS.map(n=>`<div class="novela-card"><img class="novela-poster" src="${n.poster_url||''}" onerror="this.src=''" alt="${esc(n.title)}"><div class="novela-info"><div class="novela-name" title="${esc(n.title)}">${esc(n.title)}</div><div class="novela-meta">${flagFor(n.country_code)} ${n.episode_count||0} ep.</div></div><div class="novela-actions"><button class="btn btn-ghost btn-sm" onclick="editNovela('${n.id}')">✏️</button><button class="btn btn-danger btn-sm" onclick="deleteNovela('${n.id}','${esc(n.title)}')">🗑️</button></div></div>`).join('');}
function populateNovelSelects(){const opts=ALL_NOVELAS.map(n=>`<option value="${n.id}">${esc(n.title)}</option>`).join('');document.getElementById('fNovelaId').innerHTML='<option value="">— Selecione —</option>'+opts;document.getElementById('bNovelaId').innerHTML='<option value="">— Selecione —</option>'+opts;}
function flagFor(c){return{KR:'🇰🇷',JP:'🇯🇵',TR:'🇹🇷',MX:'🇲🇽',CN:'🇨🇳',BR:'🇧🇷',IN:'🇮🇳',PH:'🇵🇭',US:'🇺🇸'}[c]||'🌍';}
function openNovelModal(d){document.getElementById('novelModalTitle').textContent=d?'Editar Novela':'Nova Novela';document.getElementById('nId').value=d?d.id:'';document.getElementById('nTitle').value=d?d.title:'';document.getElementById('nCountry').value=d?d.country_code:'KR';document.getElementById('nDesc').value=d?(d.description||''):'';document.getElementById('pname').textContent='';document.getElementById('posterPreviewWrap').style.display='none';document.getElementById('posterFile').value='';document.getElementById('novelModal').classList.add('show');}
function closeNovelModal(){document.getElementById('novelModal').classList.remove('show');}
function editNovela(id){const n=ALL_NOVELAS.find(x=>x.id===id);if(n)openNovelModal(n);}
async function saveNovela(e){e.preventDefault();const id=document.getElementById('nId').value;const fd=new FormData();fd.append('title',document.getElementById('nTitle').value);fd.append('country_code',document.getElementById('nCountry').value);fd.append('description',document.getElementById('nDesc').value);const pf=document.getElementById('posterFile').files[0];if(pf)fd.append('poster',pf);const url=id?`${API}/api/admin/novelas/${id}`:`${API}/api/admin/novelas`;const method=id?'PUT':'POST';try{const r=await fetch(url,{method,headers:{'X-Admin-Token':TOKEN},body:fd});const data=await r.json();if(r.ok){toast(id?'Novela atualizada':'Novela criada','ok');closeNovelModal();await loadNovelas();loadStats();}else{toast(data.detail||'Erro','err');}}catch(err){toast('Erro de rede','err');}}
async function deleteNovela(id,title){if(!confirm(`Remover "${title}" e TODOS os episódios?`))return;const r=await fetch(`${API}/api/admin/novelas/${id}`,{method:'DELETE',headers:{'X-Admin-Token':TOKEN}});r.ok?(toast('Removida','ok'),loadNovelas(),loadEps(),loadStats()):toast('Erro','err');}
function pdOver(e){e.preventDefault();document.getElementById('pdz').classList.add('drag');}function pdLeave(){document.getElementById('pdz').classList.remove('drag');}function pdDrop(e){e.preventDefault();pdLeave();const f=e.dataTransfer.files[0];if(f)setPoster(f);}function posterSel(i){if(i.files[0])setPoster(i.files[0]);}
function setPoster(f){const dt=new DataTransfer();dt.items.add(f);document.getElementById('posterFile').files=dt.files;document.getElementById('pname').textContent='🖼️ '+f.name;document.getElementById('posterPreview').src=URL.createObjectURL(f);document.getElementById('posterPreviewWrap').style.display='block';}
async function loadEps(){try{ALL_EPS=await fetch(API+'/api/admin/episodes',{headers:{'X-Admin-Token':TOKEN}}).then(r=>r.json());renderEps(ALL_EPS);document.getElementById('epCnt').textContent='('+ALL_EPS.length+')';}catch(e){toast('Erro ao carregar episódios','err');}}
function renderEps(eps){if(!eps.length){document.getElementById('epList').innerHTML='<div class="empty">Nenhum episódio.</div>';return;}document.getElementById('epList').innerHTML=`<table><thead><tr><th>Thumb</th><th>Novela</th><th>Ep</th><th>Título</th><th>País</th><th>Duração</th><th>Moedas</th><th>Vídeo</th><th></th></tr></thead><tbody>${eps.map(e=>`<tr><td>${e.thumbnailUrl?`<img class="thumb" src="${e.thumbnailUrl}" onerror="this.style.visibility='hidden'">`:'<div class="thumb"></div>'}</td><td style="font-weight:600;max-width:110px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(e.novelaTitle)}">${esc(e.novelaTitle)}</td><td style="color:var(--muted)">Ep.${e.episodeNumber}</td><td style="max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(e.title)}">${esc(e.title)}</td><td style="font-size:1.1rem">${e.countryFlag}</td><td style="color:var(--muted);font-size:.8rem">${fmtDur(e.durationSeconds)}</td><td>${e.coinCost===0?'<span class="badge free">GRÁTIS</span>':`<span class="badge coin">🪙${e.coinCost}</span>`}</td><td>${e.videoUrl?'<span style="color:var(--green)">✓</span>':'<span style="color:var(--muted)">—</span>'}</td><td><button class="btn btn-danger btn-sm" onclick="delEp(\'${e.id}\')">✕</button></td></tr>`).join('')}</tbody></table>`;}
function filterEps(){const q=document.getElementById('sq').value.toLowerCase();renderEps(ALL_EPS.filter(e=>e.novelaTitle.toLowerCase().includes(q)||e.title.toLowerCase().includes(q)||e.countryCode.toLowerCase().includes(q)));}
async function delEp(id){if(!confirm('Remover episódio e vídeo?'))return;const r=await fetch(API+'/api/admin/episodes/'+id,{method:'DELETE',headers:{'X-Admin-Token':TOKEN}});r.ok?(toast('Removido','ok'),loadEps(),loadStats()):toast('Erro','err');}
async function uploadSingle(e){e.preventDefault();const vid=document.getElementById('fVideo').files[0];if(!vid){toast('Selecione um vídeo','err');return;}const nid=document.getElementById('fNovelaId').value;if(!nid){toast('Selecione uma novela','err');return;}const fd=new FormData();fd.append('novela_id',nid);fd.append('episode_title',document.getElementById('fTitle').value);fd.append('episode_number',document.getElementById('fEpNum').value);fd.append('description',document.getElementById('fDesc').value);fd.append('coin_cost',document.getElementById('fCoins').value);fd.append('thumbnail_url',document.getElementById('fThumb').value);fd.append('duration_seconds',document.getElementById('fDur').value);fd.append('video',vid);const btn=document.getElementById('subBtn'),pw=document.getElementById('pw'),pf=document.getElementById('pf'),pt=document.getElementById('pt');btn.disabled=true;btn.textContent='Enviando...';pw.style.display='block';pf.style.width='0%';pt.textContent='0%';const xhr=new XMLHttpRequest();xhr.upload.onprogress=ev=>{if(ev.lengthComputable){const p=Math.round(ev.loaded/ev.total*100);pf.style.width=p+'%';pt.textContent=p+'%';}};xhr.onload=()=>{btn.disabled=false;btn.textContent='Fazer Upload';pw.style.display='none';if(xhr.status===200){let res={};try{res=JSON.parse(xhr.responseText);}catch(e){}toast('Upload concluído! Duração: '+fmtDur(res.durationSeconds||0),'ok');document.getElementById('uf').reset();document.getElementById('fname').textContent='';loadEps();loadStats();}else{let msg='Erro '+xhr.status;try{msg=JSON.parse(xhr.responseText).detail||msg;}catch(e){}toast(msg,'err');}};xhr.onerror=()=>{btn.disabled=false;btn.textContent='Fazer Upload';toast('Erro de rede','err');};xhr.open('POST',API+'/api/admin/upload');xhr.setRequestHeader('X-Admin-Token',TOKEN);xhr.send(fd);}
function bdOver(e){e.preventDefault();document.getElementById('bdz').classList.add('drag');}function bdLeave(e){document.getElementById('bdz').classList.remove('drag');}function bdDrop(e){e.preventDefault();bdLeave();const f=e.dataTransfer.files;if(f.length)setBatchFiles(f);}function bfSel(i){if(i.files.length)setBatchFiles(i.files);}
function setBatchFiles(files){BATCH_FILES=Array.from(files).filter(f=>['mp4','mkv','webm','avi','mov'].includes(f.name.split('.').pop().toLowerCase())).sort((a,b)=>a.name.localeCompare(b.name));document.getElementById('bfnames').textContent=BATCH_FILES.length?BATCH_FILES.length+' arquivo(s)':'Nenhum vídeo válido';if(!BATCH_FILES.length){document.getElementById('batchPreview').style.display='none';return;}const se=parseInt(document.getElementById('bStartEp').value)||1;const items=BATCH_FILES.map((f,idx)=>{const p=parseFilename(f.name);const ep=p.ep>0?p.ep:se+idx;return`<div class="batch-item" id="bi-${idx}"><div class="batch-fname" title="${esc(f.name)}">📎 ${esc(f.name)}</div><div class="batch-ep">Ep. ${ep}</div><div class="batch-dur">${(f.size/1048576).toFixed(1)} MB</div><div class="batch-st" id="bst-${idx}">⏳</div></div>`;}).join('');document.getElementById('batchItems').innerHTML=items;document.getElementById('batchPreview').style.display='block';document.getElementById('bpw').style.display='none';}
async function uploadBatch(){const nid=document.getElementById('bNovelaId').value;if(!nid){toast('Selecione uma novela','err');return;}if(!BATCH_FILES.length){toast('Selecione os vídeos','err');return;}const btn=document.getElementById('batchBtn');btn.disabled=true;btn.textContent='Enviando em lote...';document.getElementById('bpw').style.display='block';document.getElementById('bpf').style.width='0%';document.getElementById('bpt').textContent='0 / '+BATCH_FILES.length;const fd=new FormData();fd.append('novela_id',nid);fd.append('start_ep',document.getElementById('bStartEp').value);fd.append('coin_cost',document.getElementById('bCoins').value);BATCH_FILES.forEach(f=>fd.append('videos',f));const xhr=new XMLHttpRequest();xhr.upload.onprogress=ev=>{if(ev.lengthComputable){const p=Math.round(ev.loaded/ev.total*100);document.getElementById('bpf').style.width=p+'%';document.getElementById('bpt').textContent=p+'% — transferindo...';}};xhr.onload=()=>{btn.disabled=false;btn.textContent='📦 Enviar Todos';if(xhr.status===200){let res={};try{res=JSON.parse(xhr.responseText);}catch(e){}const results=res.results||[];let ok=0,err=0;results.forEach((r,idx)=>{const el=document.getElementById('bst-'+idx);const bi=document.getElementById('bi-'+idx);if(el){if(r.error){el.textContent='❌ erro';el.style.color='var(--red)';if(bi)bi.classList.add('err');err++;}else{el.textContent='✅ '+fmtDur(r.durationSeconds);el.style.color='var(--green)';if(bi)bi.classList.add('done');ok++;}}});document.getElementById('bpt').textContent=`Concluído: ${ok} ok, ${err} erros`;document.getElementById('bpf').style.width='100%';toast(`${ok} episódio(s) enviado(s)`+(err?' | '+err+' erro(s)':''),err?'err':'ok');loadEps();loadStats();}else{let msg='Erro '+xhr.status;try{msg=JSON.parse(xhr.responseText).detail||msg;}catch(e){}toast(msg,'err');}};xhr.onerror=()=>{btn.disabled=false;btn.textContent='📦 Enviar Todos';toast('Erro de rede','err');};xhr.open('POST',API+'/api/admin/upload-batch');xhr.setRequestHeader('X-Admin-Token',TOKEN);xhr.send(fd);}
function parseFilename(fn){const s=fn.replace(/\.[^.]+$/,'');let m;m=s.match(/[Ss]\d+[Ee](\d+)/);if(m)return{ep:parseInt(m[1]),title:s.replace(/[Ss]\d+[Ee]\d+/,'').trim().replace(/^[-_.]+|[-_.]+$/g,'')};m=s.match(/[Ee][Pp][_-]?(\d+)/i);if(m)return{ep:parseInt(m[1]),title:s.replace(/[Ee][Pp][_-]?\d+/i,'').trim().replace(/^[-_.]+|[-_.]+$/g,'')};m=s.match(/[_-][Ee](\d{1,3})[_-]/);if(m)return{ep:parseInt(m[1]),title:s.slice(m.index+m[0].length).replace(/[-_]/g,' ').trim()};m=s.match(/^(\d{1,3})[\s\-_.]+(.+)$/);if(m)return{ep:parseInt(m[1]),title:m[2].replace(/[-_]/g,' ')};m=s.match(/^(\d{1,3})$/);if(m)return{ep:parseInt(m[1]),title:''};return{ep:0,title:s.replace(/[-_]/g,' ')};}
function dOver(e){e.preventDefault();document.getElementById('dz').classList.add('drag');}function dLeave(e){document.getElementById('dz').classList.remove('drag');}function dDrop(e){e.preventDefault();dLeave();const f=e.dataTransfer.files[0];if(f)setFile(f);}function fSel(i){if(i.files[0])setFile(i.files[0]);}
function setFile(f){const dt=new DataTransfer();dt.items.add(f);document.getElementById('fVideo').files=dt.files;document.getElementById('fname').textContent='📎 '+f.name+' ('+(f.size/1048576).toFixed(1)+' MB)';const p=parseFilename(f.name);if(p.ep>0)document.getElementById('fEpNum').value=p.ep;if(p.title&&!document.getElementById('fTitle').value)document.getElementById('fTitle').value=p.title;}
function fmtDur(s){if(!s)return'—';const h=Math.floor(s/3600),m=Math.floor((s%3600)/60),sec=s%60;return h>0?`${h}h${String(m).padStart(2,'0')}m`:`${m}m${String(sec).padStart(2,'0')}s`;}
function esc(str){return String(str||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function toast(msg,type=''){const t=document.getElementById('toast');t.textContent=msg;t.className='toast show '+(type||'');setTimeout(()=>t.classList.remove('show'),3500);}
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
