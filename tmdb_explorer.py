"""
NoveFlix — Explorador TMDB
Execute: pip install requests  →  python tmdb_explorer.py

Documenta a estrutura dos dados que o app Android vai consumir.
Chave: 684348ba260de2dbe6c437ac08ea68c4
"""

import requests
import json

API_KEY  = "684348ba260de2dbe6c437ac08ea68c4"
BASE_URL = "https://api.themoviedb.org/3"
IMG_BASE = "https://image.tmdb.org/t/p/w780"

COUNTRIES = {
    "JP": "🇯🇵 Japão",
    "KR": "🇰🇷 Coreia",
    "TR": "🇹🇷 Turquia",
    "MX": "🇲🇽 México",
    "CN": "🇨🇳 China",
}

def get(endpoint, params=None):
    p = {"api_key": API_KEY, "language": "pt-BR"}
    if params:
        p.update(params)
    r = requests.get(f"{BASE_URL}{endpoint}", params=p, timeout=10)
    r.raise_for_status()
    return r.json()


def discover_shows(country_code, page=1):
    """Retorna séries populares de um país."""
    return get("/discover/tv", {
        "with_origin_country": country_code,
        "sort_by": "popularity.desc",
        "page": page,
    })


def get_show_detail(series_id):
    """Retorna detalhes de uma série."""
    return get(f"/tv/{series_id}")


def get_season(series_id, season=1):
    """Retorna episódios de uma temporada."""
    return get(f"/tv/{series_id}/season/{season}")


def get_episode_detail(series_id, season, episode_number):
    """Retorna detalhes de um episódio específico."""
    return get(f"/tv/{series_id}/season/{season}/episode/{episode_number}")


# ─────────────────────────────────────────────────────────
# EXECUÇÃO PRINCIPAL
# ─────────────────────────────────────────────────────────
if __name__ == "__main__":

    print("=" * 60)
    print("  NoveFlix — Explorador TMDB")
    print("=" * 60)

    all_shows = []   # acumula para exportar JSON ao final

    for code, label in COUNTRIES.items():
        print(f"\n{'─'*50}")
        print(f"  {label}")
        print(f"{'─'*50}")

        data = discover_shows(code)
        shows = data.get("results", [])[:5]   # top 5 por país

        for show in shows:
            sid    = show["id"]
            title  = show.get("name", "?")
            poster = IMG_BASE + show["poster_path"] if show.get("poster_path") else None
            score  = show.get("vote_average", 0)

            print(f"\n  🎬 [{sid}] {title}  ⭐ {score:.1f}")
            if poster:
                print(f"     Poster: {poster}")

            # Busca 1º episódio da 1ª temporada
            try:
                season_data = get_season(sid, 1)
                episodes    = season_data.get("episodes", [])[:3]   # primeiros 3

                for ep in episodes:
                    ep_num   = ep.get("episode_number", 0)
                    ep_title = ep.get("name", "?")
                    ep_time  = ep.get("runtime", 0)
                    ep_still = IMG_BASE + ep["still_path"] if ep.get("still_path") else None

                    print(f"     Ep.{ep_num} — {ep_title} ({ep_time} min)")
                    if ep_still:
                        print(f"            Still: {ep_still}")

                    # Monta objeto no formato do app Android
                    all_shows.append({
                        "id":            f"{code.lower()}_{sid}_ep{ep_num}",
                        "novelaId":      str(sid),
                        "novelaTitle":   title,
                        "title":         ep_title,
                        "description":   ep.get("overview", ""),
                        "thumbnailUrl":  ep_still or poster or "",
                        "videoUrl":      "",     # preencher com URL de streaming real
                        "episodeNumber": ep_num,
                        "durationSeconds": (ep_time or 0) * 60,
                        "coinCost":      0 if ep_num == 1 else 2,
                        "likeCount":     int(score * 1000),
                        "viewCount":     int(score * 5000),
                        "countryFlag":   label.split()[0],
                        "countryCode":   code,
                        "posterUrl":     poster or "",
                    })

            except Exception as e:
                print(f"     ⚠  Temporada não encontrada: {e}")

    # ─── Exporta JSON compatível com o app Android ───
    output_file = "tmdb_feed_data.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(all_shows, f, ensure_ascii=False, indent=2)

    print(f"\n✅  {len(all_shows)} episódios exportados → {output_file}")
    print("\nEstrutura de um episódio:")
    if all_shows:
        print(json.dumps(all_shows[0], ensure_ascii=False, indent=2))

    print("\n📌 URLs de imagem TMDB:")
    print("   Poster   : https://image.tmdb.org/t/p/w780{poster_path}")
    print("   Still    : https://image.tmdb.org/t/p/w780{still_path}")
    print("   Backdrop : https://image.tmdb.org/t/p/original{backdrop_path}")
    print("\n📌 Tamanhos disponíveis: w92 w154 w185 w342 w500 w780 original")
