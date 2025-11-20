#!/bin/bash
# Script para verificar TODOS los imports requeridos en TODO el proyecto

BASE_DIR="app/src/main/java/com/parishod"
TEMP_ERRORS=$(mktemp)
TEMP_WARNINGS=$(mktemp)
echo "0" > "$TEMP_ERRORS"
echo "0" > "$TEMP_WARNINGS"
FILES_CHECKED=0

# Función para detectar tipo de archivo
detect_file_type() {
    local file="$1"
    local content=$(cat "$file" 2>/dev/null || echo "")
    
    if echo "$content" | grep -q "implements.*ReplyProvider"; then
        echo "ReplyProvider"
    elif echo "$content" | grep -q "extends.*Worker"; then
        echo "Worker"
    elif echo "$content" | grep -q "extends.*Service\|extends.*NotificationListenerService"; then
        echo "Service"
    elif echo "$content" | grep -q "extends.*Fragment\|:.*Fragment"; then
        echo "Fragment"
    elif echo "$content" | grep -q "extends.*Activity\|:.*Activity"; then
        echo "Activity"
    elif echo "$content" | grep -q "extends.*Adapter\|:.*Adapter"; then
        echo "Adapter"
    elif echo "$file" | grep -q "utils\|Utils"; then
        echo "Utils"
    elif echo "$file" | grep -q "model"; then
        echo "Model"
    else
        echo "Other"
    fi
}

# Verificar imports según tipo
check_imports_for_type() {
    local file="$1"
    local file_type="$2"
    local file_errors=0
    local file_warnings=0
    
    # Saltar archivos abstractos o interfaces
    if grep -q "abstract class\|interface " "$file" 2>/dev/null; then
        return 0
    fi
    
    case "$file_type" in
        "Activity")
            # Bundle solo si usa onCreate con Bundle
            if grep -q "onCreate.*Bundle\|onSaveInstanceState\|onRestoreInstanceState" "$file"; then
                if ! grep -q "^import android.os.Bundle" "$file"; then
                    echo "  ❌ FALTA: android.os.Bundle"
                    file_errors=$((file_errors + 1))
                fi
            fi
            ;;
        "Fragment")
            if ! grep -q "^import android.os.Bundle" "$file"; then
                echo "  ❌ FALTA: android.os.Bundle"
                file_errors=$((file_errors + 1))
            fi
            if ! grep -q "^import androidx.annotation.NonNull" "$file"; then
                echo "  ⚠️  RECOMENDADO: androidx.annotation.NonNull"
                file_warnings=$((file_warnings + 1))
            fi
            ;;
        "Service")
            # Services heredan Context de Service/NotificationListenerService
            # Solo verificar si realmente usa Context directamente
            if grep -q "getApplicationContext\|getBaseContext\|getSystemService" "$file"; then
                if ! grep -q "^import android.content.Context" "$file"; then
                    echo "  ⚠️  RECOMENDADO: android.content.Context (ya heredado pero útil para tipos)"
                    file_warnings=$((file_warnings + 1))
                fi
            fi
            ;;
        "Worker")
            if ! grep -q "^import android.content.Context" "$file"; then
                echo "  ❌ FALTA: android.content.Context"
                file_errors=$((file_errors + 1))
            fi
            if ! grep -q "^import androidx.annotation.NonNull" "$file"; then
                echo "  ❌ FALTA: androidx.annotation.NonNull"
                file_errors=$((file_errors + 1))
            fi
            if ! grep -q "^import androidx.work.Worker\|^import androidx.work.WorkerParameters" "$file"; then
                echo "  ❌ FALTA: androidx.work.Worker o WorkerParameters"
                file_errors=$((file_errors + 1))
            fi
            ;;
        "ReplyProvider")
            if ! grep -q "^import android.content.Context" "$file"; then
                echo "  ❌ FALTA: android.content.Context"
                file_errors=$((file_errors + 1))
            fi
            if ! grep -q "^import androidx.annotation.NonNull" "$file"; then
                echo "  ❌ FALTA: androidx.annotation.NonNull"
                file_errors=$((file_errors + 1))
            fi
            if ! grep -q "^import com.parishod.watomagic.replyproviders.model.NotificationData" "$file"; then
                echo "  ❌ FALTA: com.parishod.watomagic.replyproviders.model.NotificationData"
                file_errors=$((file_errors + 1))
            fi
            ;;
        "Adapter")
            # Adapters pueden usar parent.context sin import explícito
            # Solo requerir si se usa el tipo Context explícitamente
            if grep -q ": Context\|Context\s*[,\|\)]" "$file"; then
                if ! grep -q "^import android.content.Context" "$file"; then
                    echo "  ❌ FALTA: android.content.Context (usado como tipo)"
                    file_errors=$((file_errors + 1))
                fi
            elif grep -q "getApplicationContext\|getBaseContext\|getSystemService\|PreferencesManager.getPreferencesInstance" "$file"; then
                if ! grep -q "^import android.content.Context" "$file"; then
                    echo "  ⚠️  RECOMENDADO: android.content.Context"
                    file_warnings=$((file_warnings + 1))
                fi
            fi
            ;;
    esac
    
    # Actualizar contadores globales
    if [ $file_errors -gt 0 ]; then
        local current=$(cat "$TEMP_ERRORS")
        echo $((current + file_errors)) > "$TEMP_ERRORS"
    fi
    if [ $file_warnings -gt 0 ]; then
        local current=$(cat "$TEMP_WARNINGS")
        echo $((current + file_warnings)) > "$TEMP_WARNINGS"
    fi
    
    return $file_errors
}

echo "🔍 Verificando imports requeridos en TODO el proyecto..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Buscar todos los archivos Java y Kotlin
while IFS= read -r file; do
    FILES_CHECKED=$((FILES_CHECKED + 1))
done < <(find "$BASE_DIR" -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null)

find "$BASE_DIR" -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null | while read -r file; do
    file_type=$(detect_file_type "$file")
    filename=$(basename "$file")
    relative_path=${file#$BASE_DIR/}
    
    # Saltar archivos de test y algunos tipos específicos
    if echo "$file" | grep -q -E "(test|Test|R\.java|BuildConfig)"; then
        continue
    fi
    
    # Solo verificar tipos conocidos
    if [ "$file_type" = "Other" ] && ! echo "$file" | grep -q -E "(Activity|Fragment|Service|Worker|Adapter|Provider|Utils)"; then
        continue
    fi
    
    FILES_CHECKED=$((FILES_CHECKED + 1))
    echo "📄 [$file_type] $relative_path"
    
    check_imports_for_type "$file" "$file_type"
    result=$?
    
    if [ $result -eq 0 ]; then
        echo "  ✅ OK"
    fi
    
    ERRORS=$((ERRORS + result))
    echo ""
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Resumen:"
echo "  Archivos verificados: $FILES_CHECKED"
ERRORS=$(cat "$TEMP_ERRORS")
WARNINGS=$(cat "$TEMP_WARNINGS")
echo "  Errores encontrados: $ERRORS"
echo "  Advertencias: $WARNINGS"

# Limpiar archivos temporales
rm -f "$TEMP_ERRORS" "$TEMP_WARNINGS"

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "❌ HAY ERRORES QUE DEBEN CORREGIRSE ANTES DE COMPILAR"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo ""
    echo "⚠️  Hay imports recomendados faltantes (no críticos)"
    exit 0
else
    echo ""
    echo "✅ Todos los imports requeridos están presentes"
    exit 0
fi
