# The app never requests the INTERNET permission

**Status:** superseded in part by [ADR 0004](0004-in-app-translation-model-download.md), which allows one network use: downloading the translation model, and by [ADR 0009](0009-typing-history-is-kept-on-the-phone.md), which lets the user keep a typing history on the phone. Everything else below still holds.

A keyboard sees everything the user types, so the app declares no network permission and stores no typed text. This rules out crash reporting SDKs, analytics, cloud suggestions and downloadable dictionaries; any future feature that needs data must ship it inside the APK. The guarantee is only worth something if it is absolute, so do not add the permission for a "small" feature.
