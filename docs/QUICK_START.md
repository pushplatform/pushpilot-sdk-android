# Быстрый старт — Android SDK 2.0.4

## Зависимости

Добавьте google(), mavenCentral() и публичный Maven-репозиторий SDK:

```kotlin
maven {
    url = uri("https://raw.githubusercontent.com/pushplatform/pushpilot-sdk-android/maven/")
    content { includeModule("ru.pushplatform", "sdk-android") }
}
```

В dependencies приложения:

```kotlin
implementation("ru.pushplatform:sdk-android:[2.0.4,3.0)")
```

Подключите Firebase google-services.json своего приложения и Google Services
Gradle plugin. Минимальная версия Android — API 21.

## Инициализация

В Application.onCreate():

```kotlin
PushPlatform.getInstance().initialize(
    this,
    PushPlatform.InitializeOptions(
        applicationId = "YOUR_APPLICATION_UUID",
        debugMode = false
    )
) { result ->
    result.onFailure { error -> /* Handle registration error */ }
}
```

Секретный ключ отправки не нужен для регистрации. В Android 13+ запросите
POST_NOTIFICATIONS. Для привязки пользователя вызовите login(userId), для выхода — logout.

## Собственный FirebaseMessagingService

SDK предоставляет свой service. Если приложение уже владеет MESSAGING_EVENT,
передавайте сообщения в handleRemoteMessage(applicationContext, message).
Для сообщений с собственным UI укажите displayNotification=false. Новые токены
передавайте через restore(applicationContext), затем registerFcmToken(token).
Обработка push не должна зависеть от старта JS/Dart.

Для собственной Activity передавайте onNewIntent в handleNotificationIntent
и сохраняйте Intent через setIntent. RN/Flutter-мосты выполняют forwarding сами.

## Звонки

SDK доставляет custom data; полноэкранный интерфейс, calling permissions,
сигналинг и аудио принадлежат приложению. Перед ответом проверяйте состояние
вызова и deadline. Отменённые и повторные сообщения должны быть идемпотентными.

[Полная интеграция](../README.md) · [Миграция](MIGRATION_2.md)
