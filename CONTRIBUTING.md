# Contributing to Akkhara Keyboard

Thank you for helping. Issues and pull requests are both welcome.

## Reporting a bug or idea

Open an issue and include:

- what you did, what you expected, and what happened instead;
- your phone model and Android version;
- for Myanmar text, the exact characters involved. Paste them as text, not only as a screenshot.

Please never paste anything private you typed.

## Making a change

1. Fork the repo and branch from `main`.
2. Read [`GLOSSARY.md`](GLOSSARY.md) and use its words in code, tests and commit messages.
3. Write the test first where you can. Keyboard behaviour is tested in `engine/` with `FakeEditor` and `FakeTranslator`, without Android.
4. Run the same checks as CI:

   ```bash
   ./gradlew test lintDebug
   ```

5. Open a pull request against `main`. Explain what changed and why, and add a screenshot for anything visual.

## Rules the project keeps

These come from the decisions in [`docs/adr/`](docs/adr/). A pull request that breaks one of them needs a new ADR first.

- **Myanmar output is Unicode only.** Never Zawgyi ([ADR 0001](docs/adr/0001-unicode-only-myanmar-output.md)).
- **No network use.** No analytics, crash reporting, update checks or cloud features ([ADR 0002](docs/adr/0002-no-network-permission.md)). The one exception planned is downloading a translation model when the user asks.
- **Nothing typed or translated is stored or logged.**
- **The keyboard is a custom View drawn on a Canvas.** Jetpack Compose is used for the app's own screens only ([ADR 0003](docs/adr/0003-custom-view-keyboard-not-compose.md)).

## Licence

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
