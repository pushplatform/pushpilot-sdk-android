# Выпуск SDK 2.0.2

Версии пакета, нативных зависимостей и runtime metadata должны совпадать.
Инструкции установки и настройки приложения находятся в README.md;
изменения интеграции звонков — в docs/MIGRATION_2.md.

Версия задаётся в корневом файле `VERSION`. Из него Gradle формирует Maven-координаты и BuildConfig.SDK_VERSION, который используется в регистрации и User-Agent.

```sh
./gradlew :sdk:testDebugUnitTest :sdk:assembleRelease :sdk:publishReleasePublicationToReleaseStagingRepository
```

После проверки и коммита выполните `scripts/publish-maven.sh --prepared`.
Скрипт берёт версию из `VERSION`, проверяет комплект артефактов и публикует новую
координату в публичную ветку `maven`. Существующие версии не перезаписываются.
