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
- Player redesenhado com capa/placeholder, resumo da biblioteca e destaque da musica atual.
- Listas com cartoes mais espacados e estado visual para a faixa em reproducao.

## APK

Depois do build, o APK fica em:

`release/PlayerMusic.apk`

Versao atual: `1.1.0`

SHA-256 do APK enviado neste repositorio:

`2F5CA96DD590B7B5CF75E5AE4CAD31C46E0E4BFA237647B476760143EE773D4D`

## Build local

Abra no Android Studio ou rode:

```powershell
$env:JAVA_HOME='<caminho do JBR ou JDK 17+>'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleRelease
```
