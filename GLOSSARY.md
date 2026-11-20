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
One screenful of keys within a layout. Each input language has a base layer and a shift layer; the two symbols layers are shared by both languages; and the number pad, phone pad and date/time pad are the layers for fields that take only those.
_Avoid_: Page, mode, state

**Symbols layer**:
Either of the two shared layers of digits and punctuation, reached from any layout.
_Avoid_: Numeric keyboard

**Pad**:
A layer shown in a field that takes only digits and a few characters: the number pad, phone pad or date/time pad. Its keys sit in a grid of four equal columns, and it offers no way to letters, symbols or emoji.
_Avoid_: Keypad, numeric keyboard

**Number pad**:
The layer shown in a field that takes only a number: the digits 0 to 9, a minus sign if the field takes negative numbers and a decimal point if it takes decimals. It offers no way to letters, symbols or emoji.
_Avoid_: Numeric keyboard, number layer, symbols layer (for this meaning)

**Phone pad**:
The layer shown in a field that takes a phone number: the digits with `+`, `*` and `#`. It offers no way to letters, symbols or emoji.
_Avoid_: Dialer, dial pad

**Date/time pad**:
The layer shown in a field that takes a date or a time: the number pad's digits with only the separators that kind of field needs: `/` and `-` for a date, `:` for a time, and all three with a space key for a field that takes both.
_Avoid_: Date picker, calendar

**Key**:
One touchable cell on a layer. A key either types a character or performs an action (shift, backspace, enter, space, globe, layer switch).
_Avoid_: Button

**Blank**:
A place on a pad where there is no key, because the field does not take the character that would go there. It keeps the other keys in their columns, and a touch on it does nothing.
_Avoid_: Empty key, gap, spacer

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
The row above the keys that shows the translation or the grammar check's correction, or that one is being made. The translate key, beside the globe key, translates the text in the text field, or only the selected part; tapping the result in the strip puts it in place of that text. Neither shows without a translation model, nor in a password field.
_Avoid_: Toolbar, suggestion bar, translate panel

**Translate as you type**:
The setting that makes the translate strip translate by itself when the user pauses typing, instead of waiting for a tap on Translate. The text changes only when the user taps the translation. Off unless the user turns it on.
_Avoid_: Auto translate, live translation, instant translate

**Translation direction**:
Which language is translated into which. Myanmar text becomes English, and anything else becomes Myanmar, whichever input language is showing.
_Avoid_: Language pair, mode

**Grammar check**:
Correcting the grammar of English text, done on the phone by the model in use, or, when that model translates only, by the smallest model on the phone that can check grammar. Holding the translate key on English text offers the grammar key; the correction shows in the translate strip and replaces the text only when tapped. When nothing needs correcting, the strip says so.
_Avoid_: Spell check, autocorrect, proofreading

**Grammar key**:
The key that appears above the translate key while it is held on English text, and starts a grammar check when the finger lifts on it. It is not offered while no model on the phone can check grammar.
_Avoid_: Popup, grammar button

**Prompt**:
The instruction the translation model is given for a task: one for each translation direction, and one for the grammar check. Each starts as Akkhara's default; the user may rewrite it in settings or reset it to the default. A model that translates only always uses its maker's own instruction instead.
_Avoid_: System prompt, template, instructions

**Translation model**:
The file that does the translating, and the grammar check. Akkhara offers a short, fixed list of them (eight, from Qwen3.5 0.8B to Gemma 4 E4B, smallest first), each downloaded in the Akkhara app, and the user may also import a model file they already have. Several may be on the phone at once; until one is, translation is unavailable.
_Avoid_: Language pack, dictionary, AI

**Translates only**:
Said of a translation model that cannot check grammar, such as Hy-MT2. While it is the model in use, another model on the phone checks grammar, and the app shows which.
_Avoid_: Translation-only model, limited model

**Model in use**:
The one translation model on the phone that the keyboard translates with, and checks grammar with unless it translates only. The user picks it in the Akkhara app; the first model on the phone is put in use by itself.
_Avoid_: Active model, default model, selected model

**Remove**:
Deleting a translation model from the phone to free its space. Removing the model in use puts another model on the phone in use, if there is one.
_Avoid_: Uninstall, delete (for this meaning)

**Model download**:
Fetching one of Akkhara's listed translation models from the internet inside the Akkhara app, one at a time. It runs in the background, shown in a notification, and is the app's only use of the network.
_Avoid_: Update, sync, install

**Import**:
Choosing a translation model file with Android's file picker so the Akkhara app keeps its own copy.
_Avoid_: Install, sideload

### Emoji

**Emoji panel**:
What takes the place of the keys to choose an emoji: one grid that scrolls through every emoji category, above a bar with a key back to the letters, a tab for each category, and backspace. It offers every emoji the phone can draw.
_Avoid_: Emoji keyboard, emoji picker, emoji layer

**Emoji key**:
The key that opens the emoji panel: on the symbols layers, and offered above the comma key while the comma key is held.
_Avoid_: Smiley key, emoji button

**Emoji category**:
One of the groups Unicode puts emoji in, such as Smileys & people or Flags, each with its own tab in the emoji panel.
_Avoid_: Emoji page, section

**Recent emoji**:
The emoji chosen since the keyboard last started, shown first in the emoji panel. They are never stored, so they are gone when Android restarts the keyboard.
_Avoid_: History, favourites, frequently used

**Skin tone**:
One of the five versions of an emoji showing a person in a different skin colour. Holding such an emoji in the emoji panel offers them; a tap types the emoji without a skin tone.
_Avoid_: Skin colour, variant, modifier

### Themes

**Theme**:
The set of colours the keyboard is drawn in: its background, keys and labels, the translate strip and the emoji panel. Akkhara has a fixed list of fifty: ten classic themes, twenty trending themes and twenty designed themes. The user picks one in the Akkhara app. A theme changes colours and key style, never the place or size of a key.
_Avoid_: Skin, style, colour scheme, palette (for what the user picks)

**Classic theme**:
One of Akkhara's first ten themes, five light and five dark, among them Light and Dark.
_Avoid_: Default theme, basic theme

**Trending theme**:
One of twenty themes whose colours are inspired by films, animation and cartoons that were popular when Akkhara made it. Its name is Akkhara's own, never a title, character or studio, and it has no pictures.
_Avoid_: Movie theme, film theme, cartoon theme, seasonal theme

**Designed theme**:
One of twenty themes that each come with their own key style, and whose colours are inspired by funny, animated and superhero films that were popular when Akkhara made it. Its background can fade from one colour to another. Its name is Akkhara's own, never a title, character, studio, team or hero, and it has no pictures, logos or costume patterns.
_Avoid_: Superhero theme, premium theme, custom theme

**Key style**:
The shape and fill every key is drawn with: Rounded, Square, Pill, Outline, Raised, Glass or Borderless. It never changes a key's place or size, the layout or the font. Every theme has its own key style; the user can keep it ("Theme's own") or pick one key style for every theme in the Akkhara app.
_Avoid_: Key shape, skin, button design, key theme

**Follow system**:
The choice in place until the user picks a theme: the keyboard is drawn in the Light theme while the phone is in light mode and in the Dark theme while it is in dark mode.
_Avoid_: Auto, default theme, dynamic colour
