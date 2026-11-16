# Akkhara Keyboard

An Android keyboard for typing English and Myanmar (Burmese). This glossary fixes the words used for what the user sees on the keyboard and for how Myanmar text is put together.

## Language

### The keyboard

**Input language**:
The language the keyboard is currently typing: English or Myanmar. Exactly one is active at a time.
_Avoid_: Locale, mode, subtype

**Layout**:
The full set of keys belonging to one input language, across all of its layers.
_Avoid_: Keymap, keyboard (for this meaning)

**Layer**:
One screenful of keys within a layout. Each input language has a base layer and a shift layer; the two symbols layers are shared by both languages.
_Avoid_: Page, mode, state

**Symbols layer**:
Either of the two shared layers of digits and punctuation, reached from any layout.
_Avoid_: Numeric keyboard, number pad

**Key**:
One touchable cell on a layer. A key either types a character or performs an action (shift, backspace, enter, space, globe, layer switch).
_Avoid_: Button

**Alternate**:
An extra character offered when a key is held down, such as the Latin digit on a Myanmar digit key.
_Avoid_: Popup character, long-press character, accent

**Hint**:
The small character in the corner of a key that shows the alternate the key offers, such as the Latin digit on each key of the Myanmar top row.
_Avoid_: Secondary label, superscript

**Key preview**:
The enlarged copy of a character shown above a key while it is pressed.
_Avoid_: Popup, bubble, tooltip

**Globe key**:
The key that switches the input language. Held down, it opens Android's keyboard picker.
_Avoid_: Language key, switch key

**One-shot shift**:
The shift layer shown for one key press that types, after which the base layer returns.
_Avoid_: Temporary shift

**Shift lock**:
The shift layer held until the user turns it off.
_Avoid_: Caps lock (it also applies to Myanmar, which has no capitals)

**English conveniences**:
The two automatic edits made only in English: capitalising the start of a sentence, and turning a double space into a full stop.
_Avoid_: Autocorrect, smart typing

**Setup**:
The two steps a user must complete in Android before the keyboard can be used: enabling it, then selecting it.
_Avoid_: Onboarding, installation

### Myanmar text

**Syllable**:
One consonant (or independent vowel) together with every mark attached to it. Reordering never reaches outside the syllable being typed.
_Avoid_: Cluster, character, word

**Mark**:
Any Myanmar sign that attaches to a consonant: a medial, a vowel sign, a tone mark or asat.
_Avoid_: Diacritic, modifier

**Medial**:
One of the four consonant signs ျ ြ ွ ှ that attach to a consonant.

**Visual order**:
The order in which the parts of a syllable appear left to right on screen. `ေ` comes first in visual order.
_Avoid_: Typing order, display order

**Canonical order**:
The single order in which Unicode stores the parts of a syllable, whichever order they were typed in. `ေ` comes after the consonant and its medials in canonical order.
_Avoid_: Logical order, storage order, normalised order

**Reordering**:
Rewriting the syllable being typed so that it is in canonical order.
_Avoid_: Normalisation, autocorrect, composing

**Stacked consonant**:
A consonant written beneath the one before it, typed with the virama `္` between the two.
_Avoid_: Subscript, conjunct, pat sint

**Kinzi**:
The sequence `င်္`, drawn as a small mark above the following consonant.

### Translation

**Translation**:
Turning text in one input language into the other, done entirely on the phone. Myanmar text becomes English and English text becomes Myanmar.
_Avoid_: Transliteration, conversion

**Translate strip**:
The bar that opens above the keys when the user taps the translate key. The user types into it, sees the translation in it, and taps the translation to put it into the app they are typing in.
_Avoid_: Toolbar, suggestion bar, translate panel

**Translation direction**:
Which language is translated into which. It follows the input language when the translate strip opens, and the user can swap it.
_Avoid_: Language pair, mode

**Translation model**:
The file that does the translating. The user downloads it in the Akkhara app or imports a copy they already have; until then, translation is unavailable.
_Avoid_: Language pack, dictionary, AI

**Model download**:
Fetching the translation model from the internet inside the Akkhara app. It runs in the background, shown in a notification, and is the app's only use of the network.
_Avoid_: Update, sync, install

**Import**:
Choosing a translation model file with Android's file picker so the Akkhara app keeps its own copy.
_Avoid_: Install, sideload
