# Android API — SDK 2.0.3

Основной объект — `PushPlatform.getInstance()`.

## Инициализация

```kotlin
PushPlatform.getInstance().initialize(context,
    PushPlatform.InitializeOptions(
        applicationId = "YOUR_APPLICATION_UUID",
        debugMode = false
    )) { result ->
    result.onFailure { error -> /* Handle registration error */ }
}
```

Completion возвращает Kotlin Result<String> с ID зарегистрированной installation.
Ключ серверной отправки не нужен. Повторная инициализация той же конфигурации
ожидает общий результат; изменение конфигурации в работающем процессе отклоняется.

## Регистрация и пользователь

| Метод | Назначение |
| --- | --- |
| `getInstallationId()` | ID после регистрации на сервере |
| `getSubscriptionId()` | ID FCM-подписки после принятия токена сервером |
| `getDeviceToken()` | Последний доступный FCM-токен |
| `getServerURL()` | API URL конфигурации |
| `isConfigured()` | Готовность нативной конфигурации |
| `refreshMetadata()` | Обновление метаданных устройства/разрешений |
| `login(userId, metadata, callback)` | Привязка внешнего пользователя |
| `logout(callback)` | Снятие привязки пользователя |

Для login/logout callback использует UserManager.Result<Unit>. При выходе
очередь старого login не должна повторно привязать предыдущего пользователя.

## Разрешения

`hasNotificationPermission()` возвращает состояние разрешения показа.
`requestNotificationPermission(activity)` запрашивает POST_NOTIFICATIONS на
поддерживаемых версиях Android. Передайте результат Activity в
`onRequestPermissionsResult(requestCode, grantResults)`.

## Существующий FirebaseMessagingService

`restore(context)` восстанавливает сохранённую нативную конфигурацию.
`registerFcmToken(token)` передаёт новый токен в очередь регистрации.
`handleRemoteMessage(context, message, displayNotification = true)` обрабатывает
сообщение и возвращает false для чужого payload. Если UI сообщения принадлежит
приложению, передайте displayNotification=false.

Нативный показ и receipt работают до запуска React Native или Flutter.

## Нажатия и foreground

В своей Activity передайте новый Intent в `handleNotificationIntent(intent)` и
вызовите setIntent(intent). RN/Flutter-мосты делают forwarding автоматически.
Каждое уведомление имеет отдельную PendingIntent identity.

Делегат получает didInitialize, didUpdateFcmToken, didUpdateDeviceToken,
didFailToRegisterFcmToken, didReceiveNotification, didOpenNotification и
onNotificationPermissionResult. Для управления foreground показом используется
notificationWillDisplay(NotificationDisplay): preventDefault(), а при отложенном
решении — deferDecision(), затем finish().

## Звонки

SDK доставляет opaque custom data. Системный calling UI, разрешения полноэкранного
вызова, сигналинг, ответ и аудиотранспорт принадлежат приложению.
[Миграция и интеграция](MIGRATION_2.md).
