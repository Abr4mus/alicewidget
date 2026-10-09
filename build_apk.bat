@echo off
chcp 65001 > nul
echo ========================================================
echo   Сборка виджета «Умный Дом Яндекс: Алиса» для Android
echo ========================================================
echo.

where gradlew.bat >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [1/2] Запуск сборки APK через Gradle Wrapper...
    call gradlew.bat assembleDebug
    if %ERRORLEVEL% equ 0 (
        echo.
        echo [2/2] Сборка успешно завершена!
        echo APK файл находится в папке: app\build\outputs\apk\debug\
        pause
        exit /b 0
    ) else (
        echo.
        echo [!] Ошибка при выполнении gradlew.bat. Проверьте установку JDK и Android SDK.
    )
) else (
    echo [i] Для локальной сборки требуется установленный Android SDK.
    echo [i] Рекомендуется открыть проект в Android Studio или использовать GitHub Actions.
)

pause
