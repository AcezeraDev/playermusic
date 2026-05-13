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
- Aba de extensoes para abrir o Snaptube ou outro app externo cadastrado.
- Atualizacao da biblioteca local apos novos downloads aparecerem no aparelho.
- Icone original renovado com visual de player moderno.

## APK

Depois do build, o APK fica em:

`release/PlayerMusic.apk`

Versao atual: `1.2.0`

SHA-256 do APK enviado neste repositorio:

`6B1C286330DC8C8AB50E9405D0AFAAA227C1A2F9FFDC518A57D500F58B6850EC`

## Build local

Abra no Android Studio ou rode:

```powershell
$env:JAVA_HOME='<caminho do JBR ou JDK 17+>'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleRelease
```
