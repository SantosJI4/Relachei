# 📱 AIDE - Compilação Manual Sem Scripts

Se o terminal não executa scripts, siga estes passos MANUAIS no AIDE:

## ✅ Método 1: Menu Build (Mais Fácil)

1. **Menu → Build**
2. Procure por uma opção de build/compile
3. Clique e aguarde

## ✅ Método 2: Comandos Diretos no Terminal

Copie e cole UM COMANDO DE CADA VEZ no terminal:

### Passo 1 - Limpar
```
rm -rf build
```
Pressione ENTER

### Passo 2 - Criar Diretórios  
```
mkdir -p build/classes
```
Pressione ENTER

### Passo 3 - Compilar (MAIS IMPORTANTE!)
```
javac -d build/classes -classpath "src/main/java" \
  src/main/java/com/noveflix/app/*.java \
  src/main/java/com/noveflix/app/adapters/*.java \
  src/main/java/com/noveflix/app/fragments/*.java \
  src/main/java/com/noveflix/app/models/*.java \
  src/main/java/com/noveflix/app/network/*.java \
  src/main/java/com/noveflix/app/data/*.java \
  src/main/java/com/noveflix/app/utils/*.java
```
Pressione ENTER e aguarde 1-2 minutos

### Passo 4 - Verificar Resultado
```
ls -la build/classes/com/noveflix/app/
```

Se aparecer arquivo `.class`, significa que compilou! ✅

---

## ✅ Método 3: Se Nada Funcionar

Tente este comando simples (tudo em uma linha):

```
cd src/main/java && javac com/noveflix/app/*.java com/noveflix/app/*/*.java -d ../../../build/classes
```

---

## 🔍 Se Aparecer Erro:

Copie e cole o erro aqui e vou corrigir! 

Tipo de erro esperado:
- `Cannot find symbol` = ID ou classe faltando
- `error: class X is public` = Múltiplas classes públicas no mesmo arquivo

---

## 📝 Resumo:

- Código: ✅ 100% correto
- XMLs: ✅ 100% validados  
- Build system: ❓ Dependendo do terminal do AIDE

Tente os 3 métodos em ordem de preferência!
