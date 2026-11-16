# The app never requests the INTERNET permission

A keyboard sees everything the user types, so the app declares no network permission and stores no typed text. This rules out crash reporting SDKs, analytics, cloud suggestions and downloadable dictionaries; any future feature that needs data must ship it inside the APK. The guarantee is only worth something if it is absolute, so do not add the permission for a "small" feature.
