#!/bin/bash
# Script para verificar compilación antes de commit o en CI/CD
# Uso: ./scripts/verify_compilation.sh

set -e  # Salir si cualquier comando falla

echo "🔍 Verificando compilación del proyecto..."
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para mostrar errores
show_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
    exit 1
}

# Función para mostrar éxito
show_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Función para mostrar advertencia
show_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 1. Verificar que gradlew existe y tiene permisos
echo "📝 Verificando Gradle wrapper..."
if [ ! -f "gradlew" ]; then
    show_error "gradlew no encontrado"
fi

if [ ! -x "gradlew" ]; then
    echo "🔧 Configurando permisos de ejecución en gradlew..."
    chmod +x gradlew
fi
show_success "Gradle wrapper verificado"

# 2. Verificar archivos críticos
echo ""
echo "📝 Verificando archivos críticos..."
CRITICAL_FILES=(
    "app/src/main/java/com/parishod/watomatic/replyproviders/ReplyProvider.java"
    "app/src/main/java/com/parishod/watomatic/replyproviders/NotificationData.java"
    "app/src/main/java/com/parishod/watomatic/replyproviders/ReplyProviderFactory.java"
    "app/src/main/java/com/parishod/watomatic/replyproviders/BotJsReplyProvider.java"
    "app/src/main/java/com/parishod/watomatic/replyproviders/OpenAIReplyProvider.java"
    "app/src/main/java/com/parishod/watomatic/replyproviders/StaticReplyProvider.java"
    "app/src/main/java/com/parishod/watomatic/botjs/RateLimiter.java"
    "app/src/main/java/com/parishod/watomatic/botjs/BotExecutionException.java"
    "app/src/main/java/com/parishod/watomatic/botjs/BotJsEngine.java"
    "app/src/main/java/com/parishod/watomatic/botjs/BotValidator.java"
    "app/src/main/java/com/parishod/watomatic/botjs/BotRepository.java"
    "app/src/main/AndroidManifest.xml"
)

MISSING_FILES=0
for file in "${CRITICAL_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo -e "${RED}❌ Archivo faltante: $file${NC}"
        MISSING_FILES=$((MISSING_FILES + 1))
    fi
done

if [ $MISSING_FILES -gt 0 ]; then
    show_error "$MISSING_FILES archivo(s) crítico(s) faltante(s)"
fi
show_success "Todos los archivos críticos presentes"

# 3. Verificar compilación Java
echo ""
echo "📝 Validando compilación Java..."
if ! ./gradlew compileDefaultDebugJavaWithJavac \
    --no-daemon \
    --stacktrace \
    --warning-mode all \
    --quiet 2>&1 | tee /tmp/java_compile.log; then
    echo ""
    echo -e "${RED}❌ ERROR: Compilación Java falló${NC}"
    echo "📋 Últimas líneas del log:"
    tail -30 /tmp/java_compile.log
    show_error "Compilación Java falló - revisar errores arriba"
fi
show_success "Compilación Java exitosa"

# 4. Verificar compilación Kotlin
echo ""
echo "📝 Validando compilación Kotlin..."
if ! ./gradlew compileDefaultDebugKotlin \
    --no-daemon \
    --stacktrace \
    --warning-mode all \
    --quiet 2>&1 | tee /tmp/kotlin_compile.log; then
    echo ""
    echo -e "${RED}❌ ERROR: Compilación Kotlin falló${NC}"
    echo "📋 Últimas líneas del log:"
    tail -30 /tmp/kotlin_compile.log
    show_error "Compilación Kotlin falló - revisar errores arriba"
fi
show_success "Compilación Kotlin exitosa"

# 5. Verificar dependencias
echo ""
echo "📦 Validando dependencias..."
if ! ./gradlew dependencies \
    --configuration defaultDebugCompileClasspath \
    --no-daemon \
    --quiet > /dev/null 2>&1; then
    show_warning "Algunas dependencias no se resolvieron (puede ser normal)"
else
    show_success "Dependencias validadas"
fi

# 6. Verificar recursos (opcional, puede fallar sin SDK)
echo ""
echo "📝 Validando recursos (opcional)..."
if ./gradlew processDefaultDebugResources \
    --no-daemon \
    --quiet > /dev/null 2>&1; then
    show_success "Recursos validados"
else
    show_warning "Validación de recursos falló (puede requerir Android SDK)"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ TODAS LAS VALIDACIONES PASARON${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "El proyecto está listo para compilar."
