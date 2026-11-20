# Key sounds are built into the app and never downloaded

Key sounds are a handful of short files shipped inside the APK. They are never fetched, updated or added from the network: ADR 0002 allows the network only for the translation model, and a keyboard has no business going online because a key was pressed. We also don't let users add their own sound files, which would need a file picker, storage, and checks on what a file is.

The sounds come from Kenney's *Interface Sounds* and *Impact Sounds* packs, which are released into the public domain under CC0 1.0. That means no licence to track and no credit to show, though `docs/third-party-sounds.md` credits them anyway and names each source file. Any sound added later must be CC0 or made by us.

They ship as 16-bit mono WAV at 22.05 kHz, cut to at most a quarter of a second. Compressed formats were rejected: Ogg Opus only plays from Android 10 and the app supports Android 7.0, and MP3 adds a short silence before each sound, which makes a key click late. Sixteen WAVs come to about 108 KB.
