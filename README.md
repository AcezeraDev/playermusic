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
- Download por link direto de audio autorizado, salvando em Music/PlayerMusic.
- Links do YouTube abrem no app oficial, sem download automatico.
- Mini-player expansivel para tela cheia ao tocar ou puxar para cima.

## APK

Depois do build, o APK fica em:

`release/PlayerMusic.apk`

Versao atual: `1.3.0`

SHA-256 do APK enviado neste repositorio:

`0083DE0E1C749F12B5A8322F6759942D6CADA8FDDBE11289EE85C66B5691FB73`

## Build local

Abra no Android Studio ou rode:

```powershell
$env:JAVA_HOME='<caminho do JBR ou JDK 17+>'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleRelease
```
