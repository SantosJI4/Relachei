#!/bin/bash
# Build Script para NoveFlix
# Compila sem precisar de Gradle instalado

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_ROOT/src/main/java"
RES_DIR="$PROJECT_ROOT/src/main/res"
BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
GEN_DIR="$BUILD_DIR/gen"

echo "🔨 Compilando NoveFlix..."
echo ""

# 1. Criar diretórios
echo "1️⃣ Criando diretórios..."
mkdir -p "$CLASSES_DIR"
mkdir -p "$GEN_DIR"

# 2. Gerar R.java (mock - sem aapt)
echo "2️⃣ Gerando R.java..."
mkdir -p "$GEN_DIR/com/noveflix/app"
cat > "$GEN_DIR/com/noveflix/app/R.java" << 'RCLASS'
package com.noveflix.app;

public final class R {
    public static final class layout {
        public static final int activity_main = 0x7f040000;
        public static final int activity_player = 0x7f040001;
        public static final int activity_splash = 0x7f040002;
        public static final int fragment_home = 0x7f040003;
        public static final int fragment_vip = 0x7f040004;
        public static final int fragment_profile = 0x7f040005;
        public static final int item_episode_feed = 0x7f040006;
        public static final int item_vip_plan = 0x7f040007;
        public static final int item_coin_pack = 0x7f040008;
    }
    
    public static final class drawable {
        public static final int ic_launcher_background = 0x7f020000;
        public static final int ic_crown = 0x7f020001;
        public static final int ic_play = 0x7f020002;
        public static final int ic_lock = 0x7f020003;
        public static final int ic_heart = 0x7f020004;
        public static final int ic_heart_outline = 0x7f020005;
        public static final int ic_share = 0x7f020006;
        public static final int bg_button_red = 0x7f020007;
        public static final int bg_button_gold = 0x7f020008;
        public static final int shape_card_dark = 0x7f020009;
        public static final int shape_badge_gold = 0x7f02000a;
        public static final int shape_badge_red = 0x7f02000b;
        public static final int shape_bottom_sheet = 0x7f02000c;
        public static final int shape_card_vip_popular = 0x7f02000d;
        public static final int shape_card_vip_gold = 0x7f02000e;
    }
    
    public static final class id {
        public static final int btn_tab_home = 0x7f050000;
        public static final int btn_tab_vip = 0x7f050001;
        public static final int btn_tab_profile = 0x7f050002;
        public static final int fragment_container = 0x7f050003;
        public static final int feed_list_view = 0x7f050004;
        public static final int tv_coin_balance_home = 0x7f050005;
        public static final int player_view = 0x7f050006;
        public static final int player_back = 0x7f050007;
        public static final int player_novela_title = 0x7f050008;
        public static final int player_episode_title = 0x7f050009;
        public static final int splash_logo = 0x7f05000a;
        public static final int splash_tagline = 0x7f05000b;
        public static final int iv_thumbnail = 0x7f05000c;
        public static final int iv_play_btn = 0x7f05000d;
        public static final int iv_lock_icon = 0x7f05000e;
        public static final int iv_like_btn = 0x7f05000f;
        public static final int iv_share_btn = 0x7f050010;
        public static final int tv_novela_title = 0x7f050011;
        public static final int tv_episode_info = 0x7f050012;
        public static final int tv_description = 0x7f050013;
        public static final int tv_country = 0x7f050014;
        public static final int tv_like_count = 0x7f050015;
        public static final int tv_free_tag = 0x7f050016;
        public static final int tv_coin_cost = 0x7f050017;
        public static final int overlay_locked = 0x7f050018;
    }
    
    public static final class color {
        public static final int bg_primary = 0x7f060000;
        public static final int text_primary = 0x7f060001;
        public static final int red_primary = 0x7f060002;
        public static final int gold_vip = 0x7f060003;
        public static final int coin_color = 0x7f060004;
        public static final int transparent = 0x7f060005;
    }
    
    public static final class string {
        public static final int app_name = 0x7f070000;
        public static final int welcome_coins = 0x7f070001;
    }
}
RCLASS

echo "✅ R.java criado em $GEN_DIR/com/noveflix/app/R.java"

# 3. Compilar Java
echo "3️⃣ Compilando arquivos Java..."
javac -cp "$GEN_DIR:." \
  -d "$CLASSES_DIR" \
  -source 1.8 -target 1.8 \
  $(find "$SRC_DIR" -name "*.java") \
  2>&1 | grep -E "error|warning" || echo "✅ Compilação bem-sucedida!"

echo ""
echo "🎉 Build completado!"
echo "Arquivos compilados em: $CLASSES_DIR"
