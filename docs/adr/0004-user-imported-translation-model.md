# The translation model is a file the user imports, not shipped or downloaded by the app

Translation runs a language model on the phone, and the model is far too large to ship inside the APK. ADR 0002 says any data a feature needs must ship inside the APK; this decision makes one exception to that sentence and keeps the rest of ADR 0002 intact. The user downloads the model file themselves in a browser, then picks it in the Akkhara app with Android's system file picker, and the app copies it into its private storage. The app still declares no network permission: the download happens in the user's browser, under the user's control, and the keyboard never reaches the network to fetch, update or check the model.

We considered asking for the network permission only to download the model, and rejected it because ADR 0002's guarantee is worth something only if it is absolute. We also considered a separate downloader app, and rejected it because it adds a second install for one file. The cost of this choice is a clumsier first run: the user must find, download and import a large file before translation works, and the app must explain how.

Translation stays on the phone: the text being translated and its translation are never stored or sent anywhere, as ADR 0002 requires of all typed text.
