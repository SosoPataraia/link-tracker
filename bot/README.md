# LinkTracker — Telegram Bot

Telegram-бот, который отслеживает изменения на веб-страницах и уведомляет пользователя.

## Команды бота

| Команда | Описание |
|---------|----------|
| `/start` | Приветственное сообщение |
| `/help` | Список доступных команд |

На неизвестные команды бот отвечает сообщением об ошибке.

---

## Запуск локально (Windows)

### 1. Получите токен бота

Напишите [@BotFather](https://t.me/BotFather) в Telegram:
- Отправьте `/newbot`
- Следуйте инструкциям
- Скопируйте токен — он выглядит так: `123456789:AAGYDVqo...`

> Токен всегда содержит числа, двоеточие, и затем буквенно-цифровую строку.

### 2. Создайте файл `bot\.env`

В папке `C:\Users\User\link-tracker\bot\` создайте файл `.env`:

```
APP_TELEGRAM_TOKEN=123456789:AAGYDVqoQViRgDuLa-q1iR3dP3JzbaFiPfk
```

Замените значение на свой реальный токен от BotFather.

> `.env` добавлен в `.gitignore` — токен не попадёт в репозиторий.

### 3. Загрузите токен и запустите бота

Откройте PowerShell в `C:\Users\User\link-tracker` и выполните:

```powershell
Get-Content bot\.env | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
}
cd bot
..\mvnw.cmd spring-boot:run
```

Бот запущен — найдите его в Telegram и отправьте `/start`.

---

## Запуск тестов

```powershell
cd bot
..\mvnw.cmd test
```

Тесты не требуют реального токена — используется WireMock.

---

## Структура модуля `bot`

```
bot/
├── .env                                        ← токен (не коммитить!)
└── src/main/java/.../bot/
    ├── BotApplication.java                     ← точка входа
    ├── configuration/TelegramConfiguration.java ← бин TelegramBot
    ├── handler/CommandHandler.java             ← обработка /start, /help, неизвестных команд
    ├── properties/TelegramProperties.java      ← типобезопасная конфигурация
    └── service/
        ├── BotUpdatesListener.java             ← получение обновлений от Telegram
        └── BotCommandsRegistrar.java           ← авторегистрация команд в меню Telegram
```
