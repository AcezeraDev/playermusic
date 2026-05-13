# PlayerMusic

PlayerMusic e um player Android nativo para tocar musicas ja baixadas no aparelho.

## Recursos

- Leitura da biblioteca local pelo MediaStore.
- Busca por musica, artista ou album.
- Playlists persistidas no app, incluindo favoritas.
- Fila de reproducao, tocar a seguir, aleatorio e repeticao.
- Playback em segundo plano com foreground service.
- Notificacao com controles de anterior, tocar/pausar e proxima.
- Interface escura, responsiva e feita sem dependencias externas.

## APK

Depois do build, o APK fica em:

`release/PlayerMusic.apk`

Download direto pela GitHub Release:

https://github.com/AcezeraDev/playermusic/releases/download/v1.0.0/PlayerMusic.apk

SHA-256 do APK enviado neste repositorio:

`11EC77C5755F9813A3CD13EE94E522EAF5C1F3F8B29CF0D22D88E5A181D4C7B5`

## Build local

Abra no Android Studio ou rode:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```
