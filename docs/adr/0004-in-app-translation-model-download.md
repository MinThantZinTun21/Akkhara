# The app downloads the translation model itself, and that is its only network use

Translation runs a language model on the phone, and the model (about 2.6 GB) is far too large to ship inside the APK. The owner chose a download inside the Akkhara app over sending the user to a browser, so the app now declares the `INTERNET` permission. This reverses ADR 0002's central rule, and the owner accepted the cost: Android grants the permission to the whole app, so the keyboard process could in principle reach the network too.

To keep that cost as small as we can:

- **The download is the only network use.** It goes through Android's system `DownloadManager`, so the app contains no networking code of its own. No other feature may use the network: no analytics, crash reporting, cloud suggestions, update checks or "small" exceptions. ADR 0002's ban on those still stands.
- **One fixed file.** The app downloads a single URL pinned to one revision of the model on Hugging Face and checks the file's SHA-256 before using it. It never fetches a URL from anywhere else.
- **Wi-Fi only.** The download runs only on unmetered networks, because the file is large.
- **Same path as import.** The downloaded file goes through the same steps as a file the user imports with the system file picker: it is copied into the app's private storage, checked to open as a model, and only then counts as installed. Importing a file stays available for phones that cannot download it.
- **Nothing typed leaves the phone.** Translation runs on the phone. The text being translated and its translation are never stored or sent anywhere, as ADR 0002 requires of all typed text.

We considered leaving the download to the user's browser, which keeps the app free of the permission, and a separate downloader app. The owner chose the in-app download for a simpler first run.
