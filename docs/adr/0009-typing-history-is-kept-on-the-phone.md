# Typed text may be kept on the phone, only when the user turns it on

**Status:** supersedes in part [ADR 0002](0002-no-network-permission.md), which said the app "stores no typed text". Its network rule is unchanged: typed text never leaves the phone.

The owner wants a later feature where the translation model can draw on what the user has typed before (retrieval). That needs the text kept somewhere, so ADR 0002's "stores no typed text" no longer holds absolutely. It is replaced by these rules:

- **Off until the user turns it on.** "Remember what I type" starts off; nothing is kept for anyone who did not ask.
- **Only on the phone.** It sits in the app's private SQLite database. App backup is already off, the app has no network permission beyond the model download, and nothing in typing history is ever sent anywhere.
- **Finished text, not keystrokes.** One entry per text field: its whole text when the user presses the action key, when the app empties the field, or when the user leaves it. Single keys, deletions and half-made words are never recorded.
- **Never secrets.** Password fields, fields marked as private (IME_FLAG_NO_PERSONALIZED_LEARNING, as in incognito tabs), number, phone, date and time fields, and the Akkhara app's own fields are never saved.
- **The user stays in charge.** They can read, search and delete entries, delete everything at once, and entries older than 90 days delete themselves.

We considered recording every key press (rejected: noisy, and it would keep deleted text) and an encrypted database with its own key (rejected for now: the app's private storage is already encrypted by the phone, and a key kept on the same phone adds little). Search is a plain "contains" match rather than SQLite full-text search, which splits words at spaces and so cannot find a Myanmar word inside a sentence. The retrieval feature that reads this history is a later decision.
