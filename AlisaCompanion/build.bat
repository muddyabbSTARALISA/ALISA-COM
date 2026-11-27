@echo off
chcp 65001 >nul
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle не найден в PATH.
  echo Для локальной сборки установи Gradle 8.8 и JDK 17.
  echo Либо загрузи этот проект на GitHub и запусти Actions - Build Forge mod.
  pause
  exit /b 1
)
gradle build --no-daemon
if errorlevel 1 (
  echo Сборка завершилась с ошибкой. Скопируй сюда весь текст ошибки.
) else (
  echo Готовый мод находится в build\libs\alisacompanion-0.4.0.jar
)
pause
