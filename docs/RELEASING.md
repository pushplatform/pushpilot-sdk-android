# Выпуск самостоятельного Android SDK

## Статус — 2026-10-12

- Работа ведётся в `release/android-1.0.0`; версия `com.pushplatform:sdk-android:1.0.0`.
- `pushplatform/pushpilot-sdk-android` уже публичный: anonymous GitHub API вернул HTTP 200. Видимость не менялась.
- На Maven Central этой версии нет (HTTP 404). Настройки Central namespace, token и signing в окружении и Gradle отсутствуют.
- Выбран публичный Maven-репозиторий в отдельной ветке `maven` существующего GitHub-репозитория. Это обычная Maven-установка без GitHub credentials, соседних checkout и Maven Local.
- Исходный код SDK не меняется. Существующий release AAR от 2026-10-03 переиспользуется инкрементальной сборкой.
- Исправлены publication metadata (project URL, developer, SCM), добавлен documentation JAR и staging Maven repository вместо Maven Local.
- MIT notice скопирован из ранее одобренного владельцем Android notice (`release/licenses/android.LICENSE` RN-репозитория; решение от 2026-10-11). Apache badge в README приведён к этому notice.
- Предыдущий native audit подтвердил consumer build с Maven Local. Эти проверки и unit tests повторно не запускаются. Новая проверка будет одна: чистый consumer с публичным Maven URL.
- Staging publication успешно завершена: `BUILD SUCCESSFUL in 12s`, compile и bundle AAR — `UP-TO-DATE`. Проверки SDK не повторялись.
- SHA-256 AAR: `eb5912dced22c09887961e043f6f10c6a8601467876349af2cd49d93145b6a9d` (совпадает с исходной готовой сборкой).
- Публикация и новая проверка установки пока не завершены; результаты записываются ниже по мере выполнения.

## Установка

В `settings.gradle.kts` приложения:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/pushplatform/pushpilot-sdk-android/maven/")
            content { includeModule("com.pushplatform", "sdk-android") }
        }
    }
}
```

В `build.gradle.kts` модуля приложения:

```kotlin
dependencies {
    implementation("com.pushplatform:sdk-android:1.0.0")
}
```

AndroidX, OkHttp, Firebase BOM/messaging и Kotlin stdlib разрешаются транзитивно из POM/Gradle metadata. Для установки не нужны Firebase credentials. Для запуска приложения разработчик настраивает свой Firebase отдельно.

## Подготовка и публикация

Нужны JDK 17+, Android SDK platform 34 и Git push access к существующему публичному репозиторию. `ANDROID_HOME` должен указывать на SDK. Registry token, PGP signing и новый namespace для этого Maven-репозитория не требуются.

1. Закоммитить подготовку в release-ветку.
2. Выполнить `bash scripts/publish-maven.sh`. Скрипт запускает только incremental publication в `build/maven-repository`, копирует AAR, sources, documentation JAR, POM, Gradle metadata и checksums в отдельный временный Git checkout и пушит ветку `maven`. Если staging publication уже подготовлена в этом checkout, использовать `bash scripts/publish-maven.sh --prepared`, чтобы не повторять её. Исходники из соседних репозиториев не читаются.
3. Один раз выполнить `bash scripts/verify-installation.sh`. Новый минимальный consumer использует только публичный Maven URL для SDK через `exclusiveContent`. Проверяется `assembleDebug`, включая транзитивные зависимости и импорт публичного API. Приложение не запускается; реальные push, API calls и deploy не выполняются.
4. Записать результат, SHA AAR и source/distribution commits здесь. Закоммитить отчёт, запушить release-ветку, поставить и запушить `v1.0.0` на подтверждённую release-ветку.

Опубликованный version directory не перезаписывается: скрипт завершится ошибкой, если `1.0.0` уже есть. Исправления SDK требуют новой версии. Ветка `maven` обновляется обычным push без force.

## Maven Central

Текущий канал — публичный Maven GitHub repository, а не Maven Central. Для последующего переноса нужен verified namespace `com.pushplatform` в [Central Portal](https://central.sonatype.com/publishing/namespaces), publisher token и PGP signing key с опубликованным public key. Секреты передаются через защищённое окружение/registry secrets, не в чат. [Требования Central](https://central.sonatype.org/publish/requirements/) включают подписи, checksums, sources/docs и POM metadata. Этот перенос не нужен для текущей самостоятельной установки.

## PR

https://github.com/pushplatform/pushpilot-sdk-android/compare/main...release/android-1.0.0?expand=1

iOS, RN, Web, Flutter, server, console и база в рамках этой работы не изменяются.
