# Диагностика Android SDK 2.0.1

## Версия и зависимости

Maven-координата — com.pushplatform:sdk-android:2.0.1. Проверьте её в dependency
report Gradle. Публичный Maven-репозиторий SDK добавляется отдельно от google()
и mavenCentral(); инструкция приведена в README.md. Версия runtime metadata
и User-Agent берётся из BuildConfig.SDK_VERSION, формируемого из VERSION.

## Инициализация и токен

Вызывайте initialize в Application.onCreate с UUID приложения и API URL.
Секретный ключ отправки в клиенте не требуется. Проверьте completion регистрации:
выдача FCM-токена ещё не означает готовую backend-подписку. При недоступности сети
SDK повторяет регистрацию после восстановления соединения.

## Нет уведомления

Проверьте Firebase google-services.json и разрешение POST_NOTIFICATIONS.
Если приложение объявляет свой FirebaseMessagingService, передавайте сообщения
в handleRemoteMessage, токены — в registerFcmToken после restore(context).
Проверьте foreground preventDefault и системные настройки notification channel.

## Не открывается нужное уведомление

Передавайте Activity.onNewIntent в handleNotificationIntent и сохраняйте Intent.
Не используйте общий PendingIntent для разных уведомлений. RN/Flutter выполняют
forwarding через свои мосты.

## Входящий звонок из фона

Сообщение должно обрабатываться нативно до JS/Dart. Полноэкранный calling UI и его
permissions настраивает приложение. Кнопка ответа в уведомлении должна запускать
Activity напрямую; запуск Activity через notification BroadcastReceiver может быть
заблокирован Android. До запуска runtime сохраните pending answer и payload.
Проверяйте deadline и терминальное состояние звонка перед ответом.

## После системного force-stop

Android не возобновляет доставку FCM, пока пользователь снова не откроет приложение.
Не путайте force-stop с обычной выгрузкой фонового процесса.

[API](API_REFERENCE.md) · [Миграция](MIGRATION_2.md)
