@echo off


REM APK Installation
echo -- Installing APKs
for %%f in (BuddyGPT\apk\*.apk) do (
    echo -- INSTALLING %%f
    adb install %%f
    if %errorlevel% equ 0 (
        echo -- Installation of %%f successful
    ) else (
        echo -- Installation of %%f failed
    )
)

echo -- Setting permissions
adb shell pm grant com.google.android.googlequicksearchbox android.permission.RECORD_AUDIO
REM Vérifier si les autorisations ont été ajoutées avec succès
if %errorlevel% equ 0 (
    echo -- Permission Record Audio added to google successfully
) else (
    echo -- Failed to add permission Record Audio to google
)
adb shell settings put secure tts_default_synth com.google.android.tts
REM Vérifier si les autorisations ont été ajoutées avec succès
if %errorlevel% equ 0 (
    echo -- Activating google text to speech success 
) else (
    echo -- Activating google text to speech failed
)
@pause