# Codemagic Quickstart - Firma de Android

Guía rápida para configurar firma de Android en Codemagic y generar builds firmados.

---

## 📋 Pre-requisitos

- Cuenta de Codemagic con proyecto Watomagic conectado
- Acceso a Team Settings

---

## 🔧 Paso 1: Configurar Variables de Environment

**Ir a:** Team Settings → Environment variables → Create group

**Crear grupo:** `watomagic_keystore`

**Agregar 3 variables:**

| Variable | Tipo | Valor |
|----------|------|-------|
| `KEYSTORE_PASSWORD` | Secure text | Tu contraseña (sin caracteres especiales) |
| `KEY_PASSWORD` | Secure text | Puede ser otra o la misma |
| `KEY_ALIAS` | Text | `watomagic` |

⚠️ **IMPORTANTE:** `KEYSTORE_PASSWORD` y `KEY_PASSWORD` **deben ser idénticas** (formato JKS lo permite, PKCS12 no).

---

## 🔑 Paso 2: Generar Keystore

**Ejecutar workflow en Codemagic:**

1. Applications → Watomagic → Start new build
2. Workflow: **"🔐 Generate Keystore (ONE-TIME SETUP)"**
3. Start build
4. Esperar ~1 minuto
5. Descargar artifact: `watomagic-release.keystore`

**Guardar el keystore de forma segura:**
- Password manager (1Password, Bitwarden)
- Backup encriptado
- ⚠️ Si lo perdés, NO podés actualizar la app en producción

---

## 📤 Paso 3: Subir Keystore a Codemagic

**Ir a:** Team Settings → Code signing identities → Android

**Click:** Add key

**Configurar:**
- **Keystore file:** Subir `watomagic-release.keystore`
- **Keystore password:** La misma que pusiste en las variables
- **Key alias:** `watomagic`
- **Key password:** **LA MISMA contraseña**
- **Reference name:** `watomagic_keystore`

**Guardar**

---

## 🏗️ Paso 4: Build Firmado

**Ejecutar workflow:**

1. Applications → Watomagic → Start new build
2. Workflow: **"Watomagic Android Release Build (Signed APK)"**
3. Start build
4. Esperar ~3-5 minutos

**Resultado esperado:**
```
✅ BUILD SUCCESSFUL in 3m 45s
✅ APK firmado: app-Default-release.apk
```

**Descargar APK de artifacts**

---

## ✅ Verificación

**Build exitoso si ves:**
```bash
🔐 Verifying signing configuration...
✅ Keystore file exists: /Users/builder/.keystores/...
✅ All signing variables configured correctly

🏗️ Building SIGNED Android Release APK...
✅ Gradle build command completed

🔍 Validating APK artifacts...
✅ SIGNED APK generated successfully!
```

---

## 🚨 Troubleshooting

### Error: "Keystore was tampered with"

**Causa:** Password incorrecta

**Solución:**
1. Verificar contraseña en Code signing identities
2. Debe coincidir con la variable de environment

---

## 📝 Resumen Visual

```
┌─────────────────────────────────────┐
│ 1. Variables de Environment         │
│    watomagic_keystore group         │
│    - KEYSTORE_PASSWORD              │
│    - KEY_PASSWORD                   │
│    - KEY_ALIAS = watomagic          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ 2. Generar Keystore                 │
│    Workflow: generate-keystore      │
│    Descargar: watomagic-release.jks │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ 3. Subir Keystore                   │
│    Code signing identities          │
│    Reference: watomagic_keystore    │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ 4. Build Firmado                    │
│    Workflow: android-release        │
│    Output: app-Default-release.apk  │
└─────────────────────────────────────┘
```

---

## 🔄 Actualizar Keystore

Si necesitás cambiar contraseñas o regenerar:

1. Eliminar el signing identity actual en Codemagic
2. Actualizar variables en `watomagic_keystore` group
3. Ejecutar workflow generate-keystore
4. Subir nuevo keystore con mismo Reference name

---

**Última actualización:** 2025-11-20
**Tiempo total de configuración:** ~15 minutos
