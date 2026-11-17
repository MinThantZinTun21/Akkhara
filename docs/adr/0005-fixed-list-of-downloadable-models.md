# The app may download any model from a fixed list, not just one file

_ADR 0006 replaces the foreground model download service mentioned here: Android's `DownloadManager` now downloads the listed models. The fixed list, the pins and one download at a time still stand._

ADR 0004 let the app download exactly one pinned file. The owner now wants to choose between translation models in settings, keep several on the phone, and switch the one in use. This ADR replaces ADR 0004's "one fixed file" rule; the rest of ADR 0004 stands unchanged: Wi-Fi only, a foreground model download service that holds the only networking code, the SHA-256 and open checks, and no other network use.

- **A fixed list, built into the app.** Each entry pins one file on Hugging Face to a commit (never a branch name) with its size and SHA-256. The list starts with Gemma 4 E2B (2.59 GB) and Gemma 4 E4B (3.66 GB) from `litert-community`. The app never fetches the list, a URL or a checksum from anywhere; adding a model means a new release.
- **Only files anyone can download.** A model that needs a Hugging Face login or token (such as Gemma 3 1B) is not listed, because the app would have to hold a credential.
- **One download at a time**, so the service, its notification and its resume logic stay as simple as ADR 0004's.
- **Several models on the phone, one in use.** Each model has its own file in private storage. The keyboard reads which model is in use when a translation starts, so switching needs no restart. The translator service unloads the old model when the model in use changes.

We considered keeping one model and replacing it on each switch. That saves storage but means a multi-gigabyte download to switch back. Given the free space on the target phones, the owner chose to keep them side by side.
