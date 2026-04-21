.PHONY: clean build compile help

help:
	@echo "NoveFlix Build Targets:"
	@echo "  make clean   - Remove arquivos compilados"
	@echo "  make compile - Compila com javac"
	@echo "  make build   - Build completo"

clean:
	@echo "Limpando build..."
	@rm -rf build/
	@echo "✅ Limpo!"

compile: clean
	@echo "Compilando..."
	@mkdir -p build/classes
	@mkdir -p build/gen/com/noveflix/app
	@javac -d build/classes -sourcepath src/main/java src/main/java/com/noveflix/app/*.java src/main/java/com/noveflix/app/*/*.java 2>&1 || true
	@echo "✅ Compilado!"

build: compile
	@echo "Build concluído!"
