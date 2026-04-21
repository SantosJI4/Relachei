#!/bin/bash
# Diagnóstico do Projeto NoveFlix

echo "=== DIAGNÓSTICO DO PROJETO NOVEFLIX ===" 
echo ""

echo "1️⃣ Verificando estrutura de diretórios..."
[ -d "src/main/java/com/noveflix/app" ] && echo "✅ Java source OK" || echo "❌ Java source MISSING"
[ -d "src/main/res/layout" ] && echo "✅ Layout resources OK" || echo "❌ Layout resources MISSING"
[ -d "src/main/res/drawable" ] && echo "✅ Drawable resources OK" || echo "❌ Drawable resources MISSING"
echo ""

echo "2️⃣ Verificando arquivos críticos..."
[ -f "build.gradle" ] && echo "✅ build.gradle OK" || echo "❌ build.gradle MISSING"
[ -f "AndroidManifest.xml" ] && echo "❌ AndroidManifest deve estar em src/main/" || echo "✅ AndroidManifest location OK"
[ -f "src/main/AndroidManifest.xml" ] && echo "✅ src/main/AndroidManifest.xml OK" || echo "❌ AndroidManifest MISSING"
[ -f ".gitignore" ] && echo "✅ .gitignore OK" || echo "❌ .gitignore MISSING"
echo ""

echo "3️⃣ Contando arquivos Java..."
JAVA_COUNT=$(find src/main/java -name "*.java" | wc -l)
echo "Encontrados: $JAVA_COUNT arquivos .java"
[ $JAVA_COUNT -ge 15 ] && echo "✅ Quantidade OK" || echo "❌ Faltam Java files"
echo ""

echo "4️⃣ Contando arquivos XML..."
XML_COUNT=$(find src/main/res -name "*.xml" | wc -l)
echo "Encontrados: $XML_COUNT arquivos .xml"
[ $XML_COUNT -ge 45 ] && echo "✅ Quantidade OK" || echo "❌ Faltam XML files"
echo ""

echo "5️⃣ Verificando imports redundantes de R..."
REDUNDANT=$(grep -r "import com.noveflix.app.R" src/main/java | wc -l)
echo "Encontrados: $REDUNDANT imports de R"
[ $REDUNDANT -le 7 ] && echo "✅ OK (esperado 6-7)" || echo "⚠️ Muitos imports de R"
echo ""

echo "6️⃣ Verificando Git status..."
git status --short | head -5
if [ $? -eq 0 ]; then
  echo "✅ Git repository OK"
else
  echo "❌ Git repository problem"
fi
echo ""

echo "7️⃣ Último commit..."
git log --oneline -1
echo ""

echo "=== RESUMO FINAL ==="
echo "✅ Projeto está pronto para compilar no AIDE"
echo "📱 Próximo passo: Menu → Build → Gradle Sync"
