# Akkhara Keyboard

An open-source Android keyboard for **Myanmar (Burmese)** and **English**.

မြန်မာ + English ကီးဘုတ် · မြန်မာ Unicode သီးသန့် · ဖုန်းထဲမှာပဲ ဘာသာပြန်

## Features

- **Myanmar in Unicode only.** You can type `ေ` before the consonant, as it looks on screen, and the keyboard stores the syllable in the correct Unicode order. It never writes Zawgyi ([ADR 0001](docs/adr/0001-unicode-only-myanmar-output.md)).
- **English and Myanmar layouts.** The globe key switches between them. The keyboard also has symbols layers, key previews, hints and alternates on long press, plus automatic capitals and a full stop on double space for English.
- **Private by design.** No analytics, no crash reporting, no cloud features. Nothing you type is stored or sent ([ADR 0002](docs/adr/0002-no-network-permission.md)).

### Coming next

These are built and are being merged into `main`:

- **On-device translation**, Myanmar ↔ English. You download a model once in the app, over Wi-Fi, and translation then works offline. There are eight models to choose from, from Qwen3.5 0.8B (under 1 GB) to Gemma 4 E4B, and the app can also import a `.litertlm` file you already have.
- **English grammar check.** Hold the translate key on English text.
- **Emoji panel**, with recent emoji and skin tones.
- **30 themes**: ten classic and twenty trending.

## Install

There is no release yet. To try it now, build it yourself (see below), then turn on **Akkhara** under *Settings → System → Keyboard → On-screen keyboard*.

## Build

You need Android Studio, or the Android SDK with JDK 17. The app supports Android 7.0 (API 24) and up.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. To run the tests and lint as CI does:

```bash
./gradlew test lintDebug
```

## Project layout

- `engine/`: the keyboard's logic in plain Kotlin, with no Android code. It holds the layouts, Myanmar reordering and shift. Most tests live here.
- `app/`: the Android input method service, the keyboard view (drawn on a Canvas, [ADR 0003](docs/adr/0003-custom-view-keyboard-not-compose.md)) and the settings app.
- `GLOSSARY.md`: the words the project uses for what users see.
- `docs/adr/`: decisions, with the reasons for them.

## Contributing

Contributions are welcome: bug reports, layout ideas, better translation prompts, and checks from people who know the Myanmar language well. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

[Apache License 2.0](LICENSE)
