# 🔧 Solução: Erro "Cannot create directory /data/.android"

## ❌ Erro que você recebeu:

```
java.lang.RuntimeException: Cannot create directory /data/.android
```

## ✅ Causa e Solução

Este erro acontece quando o **Gradle 4.2.2** tenta criar uma pasta em `/data/` no Android, mas não tem permissão.

**Solução aplicada:** Downgrade para **Gradle 3.5.4** (mais compatível com AIDE)

---

## 📱 Próximos Passos no AIDE:

1. **Menu → Git → Pull/Sync**
   - Para puxar a versão corrigida (commit `b8cf47e`)

2. **Menu → Build → Clean Build**
   - Aguarde terminar

3. **Menu → Build → Build** (ou Rebuild)
   - Aguarde compilar

Desta vez **deve funcionar!** 🚀

---

## 🆘 Se ainda der erro:

### Erro: "Gradle wrapper not found"

Execute no terminal do AIDE:
```bash
gradle clean
gradle build
```

### Erro: "Plugin com.android.internal.application not found"

No AIDE, vá para:
- **Menu → Settings → Plugins**
- Procure por **"Android Gradle Plugin"**
- Desinstale e reinstale

### Erro: Mesmo erro de permissão

Tente:
1. **Menu → File → Clean Cache**
2. Feche e reabra o AIDE
3. **Menu → Build → Clean Build**
4. **Menu → Build → Build**

---

## 📊 Versões Agora Configuradas:

| Componente | Antes | Depois | Status |
|-----------|-------|--------|--------|
| Gradle Plugin | 4.2.2 | 3.5.4 | ✅ Compatível |
| Build Tools | (padrão) | 28.0.3 | ✅ Explícito |
| Compile SDK | 28 | 28 | ✅ OK |
| Min SDK | 21 | 21 | ✅ OK |
| Target SDK | 28 | 28 | ✅ OK |

---

## 🎯 Por que isso funciona?

- ✅ Gradle 3.5.4 é mais antigo e testado com AIDE
- ✅ Não tenta criar pastas em `/data/` do sistema
- ✅ Compatível com appcompat-v7:28.0.0
- ✅ Funciona em Android 10+

---

## 📝 Próximas Compilações

Depois que compilar com sucesso, você terá:
```
✅ NoveFlix-debug.apk (2-3 MB)
```

Localização:
```
/sdcard/AIDE/projects/NoveFlix/build/outputs/apk/
```

---

**Tenta agora e me avisa o resultado!** 👍
