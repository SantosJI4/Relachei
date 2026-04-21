package com.noveflix.app.data;

import com.noveflix.app.models.CoinPack;
import com.noveflix.app.models.Episode;
import com.noveflix.app.models.VipPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fornece dados mock para o feed. Substitua pelas chamadas de API real.
 * Sugestões de API ao final deste arquivo.
 */
public class MockDataProvider {

    // Thumbnails de placeholder (substitua por URLs reais da API)
    private static final String BASE_THUMB = "https://picsum.photos/seed/";

    public static List<Episode> getFeedEpisodes() {
        List<Episode> list = new ArrayList<>();

        // === JAPÃO 🇯🇵 ===
        list.add(new Episode("jp_001_ep1", "jp_001", "Amor em Tóquio",
                "Primeiro Encontro",
                "Yuki encontra Hiroshi em uma tarde chuvosa no metrô de Tóquio e suas vidas mudam para sempre.",
                BASE_THUMB + "tokyo1/720/1280", "", 1, 1380, 0, 24200, 89000,
                "🇯🇵", "JP"));

        list.add(new Episode("jp_001_ep2", "jp_001", "Amor em Tóquio",
                "Segredos Revelados",
                "Hiroshi descobre que Yuki esconde um segredo sombrio de seu passado.",
                BASE_THUMB + "tokyo2/720/1280", "", 2, 1440, 2, 18700, 67000,
                "🇯🇵", "JP"));

        list.add(new Episode("jp_002_ep1", "jp_002", "Sakura Proibida",
                "A Herança",
                "Uma jovem retorna à sua cidade natal e descobre que foi herdeira de um clã antigo.",
                BASE_THUMB + "sakura1/720/1280", "", 1, 1320, 0, 31000, 102000,
                "🇯🇵", "JP"));

        // === COREIA 🇰🇷 ===
        list.add(new Episode("kr_001_ep1", "kr_001", "Destino em Seul",
                "A Promessa",
                "Dois rivais de infância se reencontram anos depois em uma grande empresa de moda.",
                BASE_THUMB + "seoul1/720/1280", "", 1, 2700, 0, 52000, 215000,
                "🇰🇷", "KR"));

        list.add(new Episode("kr_001_ep2", "kr_001", "Destino em Seul",
                "Ciúmes",
                "A chegada de uma ex-namorada coloca em risco o relacionamento recém-nascido.",
                BASE_THUMB + "seoul2/720/1280", "", 2, 2580, 2, 41000, 198000,
                "🇰🇷", "KR"));

        list.add(new Episode("kr_002_ep1", "kr_002", "Flores de Inverno",
                "Reencontro Gelado",
                "Ela pensava ter esquecido. Ele nunca esqueceu. O inverno os aproxima novamente.",
                BASE_THUMB + "winter1/720/1280", "", 1, 3060, 0, 39500, 167000,
                "🇰🇷", "KR"));

        // === TURQUIA 🇹🇷 ===
        list.add(new Episode("tr_001_ep1", "tr_001", "Paixão em Istambul",
                "O Acordo",
                "Para salvar sua família, Leyla aceita um casamento arranjado com o temido empresário Kaan.",
                BASE_THUMB + "istanbul1/720/1280", "", 1, 2880, 0, 47300, 193000,
                "🇹🇷", "TR"));

        list.add(new Episode("tr_001_ep2", "tr_001", "Paixão em Istambul",
                "Orgulho Ferido",
                "Kaan descobre que Leyla não é quem ele pensava, mas os sentimentos já tomaram conta.",
                BASE_THUMB + "istanbul2/720/1280", "", 2, 2940, 2, 38200, 172000,
                "🇹🇷", "TR"));

        list.add(new Episode("tr_002_ep1", "tr_002", "Bósforo Vermelho",
                "Traição",
                "A família mais poderosa de Istambul esconde segredos que podem destruir tudo.",
                BASE_THUMB + "bosphorus1/720/1280", "", 1, 3120, 0, 29800, 145000,
                "🇹🇷", "TR"));

        // === MÉXICO 🇲🇽 ===
        list.add(new Episode("mx_001_ep1", "mx_001", "Coração Partido",
                "Nova Vida",
                "Após ser deixada no altar, Valentina decide recomeçar em uma cidade desconhecida.",
                BASE_THUMB + "mexico1/720/1280", "", 1, 2520, 0, 33400, 128000,
                "🇲🇽", "MX"));

        list.add(new Episode("mx_001_ep2", "mx_001", "Coração Partido",
                "O Milionário",
                "O charmoso e misterioso Rafael entra na vida de Valentina quando ela menos esperava.",
                BASE_THUMB + "mexico2/720/1280", "", 2, 2640, 2, 27600, 110000,
                "🇲🇽", "MX"));

        list.add(new Episode("mx_002_ep1", "mx_002", "Sangue e Rosa",
                "A Herdeira",
                "Sofia descobre ser filha adotiva de uma família de narcotraficantes.",
                BASE_THUMB + "mexico3/720/1280", "", 1, 2760, 0, 44100, 181000,
                "🇲🇽", "MX"));

        // === CHINA 🇨🇳 ===
        list.add(new Episode("cn_001_ep1", "cn_001", "Dinastia Proibida",
                "A Profecia",
                "Uma jovem camponesa é confundida com a princesa desaparecida e levada ao palácio imperial.",
                BASE_THUMB + "dynasty1/720/1280", "", 1, 2400, 0, 61200, 248000,
                "🇨🇳", "CN"));

        list.add(new Episode("cn_001_ep2", "cn_001", "Dinastia Proibida",
                "O Príncipe das Sombras",
                "O príncipe misterioso descobre a verdade, mas guarda segredo por razões próprias.",
                BASE_THUMB + "dynasty2/720/1280", "", 2, 2520, 2, 55800, 232000,
                "🇨🇳", "CN"));

        return list;
    }

    public static List<VipPlan> getVipPlans() {
        List<VipPlan> plans = new ArrayList<>();

        plans.add(new VipPlan(
                "pack",
                VipPlan.TYPE_PACK,
                "Pack Novelas",
                "3 novelas completas",
                "R$ 4,99",
                "Uso único • Sem expiração",
                Arrays.asList("3 novelas à sua escolha", "Acesso aos episódios completos",
                        "Sem anúncios durante episódios"),
                0, false, false, -1, 3
        ));

        plans.add(new VipPlan(
                "flash",
                VipPlan.TYPE_FLASH,
                "Flash 12h",
                "12 horas ilimitadas",
                "R$ 2,99",
                "Válido por 12 horas",
                Arrays.asList("Acesso ilimitado por 12h", "Sem anúncios", "+5 moedas bônus"),
                5, false, false, 12 * 60 * 60 * 1000L, 0
        ));

        plans.add(new VipPlan(
                "weekly",
                VipPlan.TYPE_WEEKLY,
                "Semanal",
                "7 dias de acesso total",
                "R$ 9,99",
                "por semana",
                Arrays.asList("Acesso ilimitado por 7 dias", "Sem anúncios",
                        "+30 moedas bônus", "Qualidade HD"),
                30, false, false, 7L * 24 * 60 * 60 * 1000L, 0
        ));

        plans.add(new VipPlan(
                "monthly",
                VipPlan.TYPE_MONTHLY,
                "Mensal",
                "30 dias premium",
                "R$ 24,99",
                "por mês",
                Arrays.asList("Acesso ilimitado por 30 dias", "Sem anúncios",
                        "+100 moedas bônus", "Qualidade HD",
                        "Download offline", "Suporte prioritário"),
                100, true, false, 30L * 24 * 60 * 60 * 1000L, 0
        ));

        plans.add(new VipPlan(
                "annual",
                VipPlan.TYPE_ANNUAL,
                "Anual",
                "365 dias — melhor preço",
                "R$ 149,99",
                "equivale a R$ 12,50/mês",
                Arrays.asList("Acesso ilimitado por 365 dias", "Sem anúncios",
                        "+500 moedas bônus", "Qualidade HD 4K",
                        "Download offline", "Acesso antecipado a novelas",
                        "Suporte VIP exclusivo"),
                500, false, true, 365L * 24 * 60 * 60 * 1000L, 0
        ));

        return plans;
    }

    public static List<CoinPack> getCoinPacks() {
        List<CoinPack> packs = new ArrayList<>();
        packs.add(new CoinPack("coins_15",  "Starter",  15,  0,  "R$ 2,99",  false, 0));
        packs.add(new CoinPack("coins_50",  "Popular",  50,  0,  "R$ 7,99",  false, 0));
        packs.add(new CoinPack("coins_120", "Mega",    100, 20,  "R$ 14,99", true,  17));
        packs.add(new CoinPack("coins_300", "Ultra",   250, 50,  "R$ 29,99", false, 33));
        return packs;
    }
}

/*
 * ============================================================
 * SUGESTÕES DE API PARA NOVELAS ESTRANGEIRAS
 * ============================================================
 *
 * 1. KDRAMA / JDRAMA / CDRAMA (Coreia, Japão, China):
 *    - KissAsian API (não oficial): scraping de episódios legendados
 *    - Viki (Rakuten Viki) API: api.viki.io — plataforma licenciada
 *      com dramas asiáticos em 200+ idiomas. Requer chave de API.
 *    - MyDramaList API: mydramalist.com/api — metadados de dramas
 *      (elenco, avaliações, sinopse). Gratuito com registro.
 *
 * 2. NOVELAS TURCAS (Dizi):
 *    - TurkFlix / DiziBox APIs (não oficiais)
 *    - TMDB (themoviedb.org/api) — suporta séries turcas com
 *      metadados, posters e trailers. Gratuito, muito completo.
 *
 * 3. NOVELAS MEXICANAS / LATINAS:
 *    - Televisa / Televisa Now (parceria comercial necessária)
 *    - TMDB novamente — ótima cobertura de telenovelas latinas
 *
 * 4. API RECOMENDADA PARA COMEÇAR:
 *    - TMDB (The Movie Database): https://api.themoviedb.org/3
 *      • Gratuita com chave de desenvolvedor
 *      • Endpoint: /tv/popular, /search/tv, /tv/{id}/season/{n}
 *      • Parâmetro language=pt-BR para títulos em português
 *      • Parâmetro with_origin_country=JP|KR|TR|MX para filtrar
 *      • Imagens em: https://image.tmdb.org/t/p/w500/{poster_path}
 *
 * 5. HOSPEDAGEM DE VÍDEO:
 *    - Para hospedar os episódios, considere: Bunny CDN, Cloudflare Stream,
 *      Mux.com ou AWS CloudFront com S3.
 * ============================================================
 */
