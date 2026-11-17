# GhostGram — предварительная версия

GhostGram — модификация открытого Android-клиента Telegram 13.2.2. Это **не** завершённый набор функций из первоначального задания и не официальный клиент Telegram.

## Реализовано

- Раздел «Настройки GhostGram» в настройках клиента с девятью категориями.
- Красная иконка и название GhostGram в отдельной standalone-сборке.
- Сохранение параметров и экспорт/импорт JSON через системный выбор файла.
- Защита снимков экрана и превью в списке недавних приложений через `FLAG_SECURE`.
- Локальная заметка для каждого аккаунта, чата и темы. Содержимое шифруется AES-256-GCM ключом Android Keystore.
- Управление автозапуском видео и GIF через существующие настройки Telegram.
- Переходы к штатным настройкам темы, папок чатов, PIN-кода, приватности, энергосбережения, загрузок и кэша.
- Локальные значки верификации и Ghost Premium в собственном профиле; они видны только в этом клиенте и не меняют права аккаунта на серверах Telegram.
- Отключение отправки индикатора набора текста. Настройки видимости статуса в сети остаются в штатном разделе приватности Telegram.

## Ограничения

- «Антиудаление», менеджер плагинов, общая AMOLED-тема, переводчик и другие функции первоначального задания ещё не реализованы. Пустые категории не следует считать готовыми функциями.
- Telegram API требует корректной работы статусов активности и прочтения; функции подавления этих сигналов не реализуются. См. [условия Telegram API](https://core.telegram.org/api/terms).
- Открытая версия исходников по умолчанию использует тестовые API ID/hash и package ID исходного standalone-клиента `org.telegram.messenger.web`. Для самостоятельной публикации нужны собственные API ID/hash и идентификатор приложения. Локальная тестовая сборка автора использует отдельный приватный API-конфиг и собственный ключ подписи.
- Приватный APK установлен и запущен на Nothing Phone (3a) Pro; вход в аккаунт и все функции пока не проверены.
- Для публичного APK требуется отдельная проверка функций на устройстве и решение о публикации индивидуальных API ID/hash, которые можно извлечь из бинарного файла.

## Сборка

Нужны JDK 17, Android SDK 36, NDK 27.2.12479018 и инструменты Android Build Tools. Инициализируйте Git-подмодули из `.gitmodules` и задайте `ANDROID_HOME`, `JAVA_HOME`. В корне репозитория выполните:

Для собственной сборки создайте файл `ghostgram.local.properties` в корне репозитория с двумя строками `TELEGRAM_API_ID=...` и `TELEGRAM_API_HASH=...`. Файл исключён из Git. Без него используется тестовый API исходного Telegram, который нельзя применять для публичного APK.

Для отдельного Android-приложения добавьте в тот же файл `GHOSTGRAM_PACKAGE=com.moskvol.ghostgram` (или свой уникальный идентификатор). Для standalone-варианта Gradle тогда убирает суффикс `.web`. Package ID должен точно совпадать с Android-приложением в Firebase `google-services.json`.

Личный Firebase-файл поместите в `TMessagesProj_AppStandalone/src/debug/google-services.json` для debug-варианта и в `TMessagesProj_AppStandalone/src/standalone/google-services.json` для standalone-выпуска. Эти два файла исключены из Git; файл в корне модуля — тестовая конфигурация исходного Telegram.

Для собственной подписи создайте `local-secrets/signing.properties` со значениями `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Укажите путь к своему keystore относительно корня проекта. Папка `local-secrets/` исключена из Git. Сохраните keystore и пароли в надёжном резервном месте: без них нельзя подписывать обновления уже установленного приложения. Без этого файла сборка использует тестовый ключ исходного проекта и не подходит для публикации.

```powershell
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :TMessagesProj_AppStandalone:assembleAfatDebug -Pandroid.injected.build.abi=arm64-v8a -Pandroid.injected.testOnly=false
```

APK появится в `TMessagesProj_AppStandalone/build/intermediates/apk/afat/debug/app.apk`. Для релизной сборки замените `assembleAfatDebug` на `assembleAfatStandalone`; результат будет в `TMessagesProj_AppStandalone/build/intermediates/apk/afat/standalone/app.apk`. Флаг `-Pandroid.injected.testOnly=false` обязателен для установки APK из файлового менеджера: без него Android возвращает `INSTALL_FAILED_TEST_ONLY`. В репозитории есть `gradlew` для Unix, но нет `gradlew.bat`, поэтому на Windows используется команда выше.

## Исходный код и лицензия

Основа: [DrKLO/Telegram](https://github.com/DrKLO/Telegram), версия 13.2.2. Исходный клиент распространяется на условиях GPL-2.0; текст лицензии находится в [LICENSE](LICENSE). Изменения GhostGram распространяются на тех же условиях. Исходный код должен оставаться доступным вместе с APK.
