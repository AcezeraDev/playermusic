# PlayerMusic

PlayerMusic e um player Android nativo para tocar musicas ja baixadas no aparelho.

## Recursos

- Leitura da biblioteca local pelo MediaStore.
- Busca por musica, artista ou album.
- Playlists persistidas no app, incluindo favoritas.
- Fila de reproducao, tocar a seguir, aleatorio e repeticao.
- Playback em segundo plano com foreground service.
- Notificacao com controles de anterior, tocar/pausar e proxima.
- Interface com modo claro e escuro, responsiva e feita sem dependencias externas.
- Tela principal reformulada em formato de dashboard com acoes rapidas e secao contextual.
- Interface do aplicativo recriada em uma Activity nova, mais simples, estavel e organizada.
- Player redesenhado com capa grande, mini player inferior, resumo da biblioteca e destaque da musica atual.
- Animacoes suaves nos botoes, na abertura do player e na troca de musica.
- Listas com cartoes mais espacados, capa real da musica e estado visual para a faixa em reproducao.
- Paineis personalizados com fundo, destaque colorido e animacao para historico, equalizador, letras, artista/album e downloads.
- Aba de extensoes redesenhada para downloads offline, equalizador, historico e apps externos.
- Atualizacao da biblioteca local apos novos downloads aparecerem no aparelho.
- Icone original renovado com visual de player moderno.
- Download por link direto de audio autorizado, salvando em Music/PlayerMusic.
- Links do YouTube abrem no app oficial, sem download automatico.
- Mini-player expansivel para tela cheia ao tocar ou puxar para cima.
- Equalizador por faixa quando o aparelho oferece suporte ao audio effect do Android.
- Letras salvas por musica, historico de faixas tocadas e tela de artista/album.

## APK

Depois do build, o APK fica em:

`release/PlayerMusic.apk`

Versao atual: `1.5.0`

SHA-256 do APK enviado neste repositorio:

`26C3E228243A4B233AAF93666C6054B01CEE312FD438899A7F837F5CDAFBEBE4`

## Build local

Abra no Android Studio ou rode:

```powershell
$env:JAVA_HOME='<caminho do JBR ou JDK 17+>'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleRelease
```
