# Keyboard themes: research findings

Research for the theme feature (ten built-in themes, picked in the Akkhara app). Gathered 2026-11-16 from primary sources only: the W3C spec, developer.android.com, AOSP source, and each project's own repository or site. Each claim is followed by the source that owns it. Where a primary source could not confirm something, the text says so; section 6 collects those gaps.

All contrast figures were computed, not estimated, with the WCAG formula in section 1. The script reproduces the figures in the `Palette` doc comment for the existing Light and Dark themes (lowest pairs 5.29, 7.45, 6.05 and 6.42), which is the check that it is right.

AOSP citations are to the `main` branch as read on that date. That is implementation, not contract: it can differ on a given Android release or a vendor build, and none of it was tried on a device.

## What this means for the design

1. **A theme picked in the app does not rebuild the keyboard.** The system rebuilds the input view on a `uiMode` change, so "follow system" keeps working with a palette read once at construction. Picking a theme in the app is not a configuration change, so nothing rebuilds the view. The service has to do it (build a new view, or have the view re-read the theme) when the keyboard next opens. See 2.3.
2. **The navigation bar's icons must be set from the theme, not left to the system.** With targetSdk 36 the bar's colour cannot be set; the keyboard's own background, already drawn behind the bar, is the bar's colour. What is left is the icon colour: the visible IME window is the window the system reads `APPEARANCE_LIGHT_NAVIGATION_BARS` from. Once a light theme can be shown while the phone is in dark mode (and the reverse), that flag has to follow the theme's `isDark`, or the icons can end up light on a light keyboard. API 30+ has `setSystemBarsAppearance`, API 26 to 29 the deprecated `SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR`, API 24 and 25 have nothing (dark icons cannot be asked for, so a light theme there may have poor icon contrast whatever is done). With three-button navigation the system also lays an 80% scrim over the bar unless `setNavigationBarContrastEnforced(false)` is called (API 29+). See 2.1 and 2.2.
3. **One fixed look per theme, and "follow system" as a choice between two of them, is what the reference keyboards do.** HeliBoard and FlorisBoard both store a day theme and a night theme and pick by the system's mode; neither has a theme that changes itself. `Theme.shown(chosen, systemIsDark)` is the same model with the two slots fixed to Light and Dark. See 3.
4. **Hold every theme to 4.5:1 for text and 3:1 for the accent against the background, unrounded.** Those are the WCAG AA figures. Key outlines against the background need no ratio when the key has a readable label. The 0.03928 constant in `ThemesTest` is the pre-2021 value; the spec now says 0.04045, and for 8-bit colours the two give identical results, so it is a wording matter only. See 1.
5. **The `Palette` doc comment's own figures (5.2, 7.4, 6, 6.4) cannot be promised "in every theme".** No scheme below reaches all four using only its official colours; all reach 4.5. Either the comment states the 4.5 floor for every theme, or the higher figures are said of Light and Dark only. See 5.3.
6. **Drop Monokai. Treat Gruvbox as usable with a caveat. The other seven are MIT with a clear copyright line.** Monokai is proprietary and its owner's rules do not allow this use. Gruvbox says "MIT/X11" in its README but has no licence file and no copyright line. Shipping any MIT scheme means shipping its copyright and permission notice in the app. See 4.
7. **Used the way each scheme intends, most fail.** Only Catppuccin Mocha and Rosé Pine pass every pair with their natural role-to-colour mapping. The usual failure is the hint: every scheme's "comment" or "muted" tone is under 4.5:1 on a key. All of them can be made to pass with official colours only, but Solarized only by giving up its look: its own body-text pairs are 4.13 to 4.86, and on Solarized Light no accent hue passes both as a fill under a label and as a mark on the background. See 5.
8. **The glossary says five light and five dark; the list asked about has three light variants.** Solarized Light, Gruvbox Light and Catppuccin Latte are the only light ones. Rosé Pine Dawn is added in section 5 as a fourth candidate (it passes), with one unresolved hex value. There are eight dark candidates for four places.

## 1. Contrast

### 1.1 The formulas

Relative luminance of an sRGB colour, with each 8-bit channel divided by 255 first:

```
L = 0.2126 * R + 0.7152 * G + 0.0722 * B
where, for each channel C (RsRGB, GsRGB, BsRGB):
  if CsRGB <= 0.04045 then C = CsRGB / 12.92
  else C = ((CsRGB + 0.055) / 1.055) ^ 2.4
```

Source: WCAG 2.2, definition of "relative luminance", https://www.w3.org/TR/WCAG22/#dfn-relative-luminance

Contrast ratio:

```
(L1 + 0.05) / (L2 + 0.05)
L1 = relative luminance of the lighter colour, L2 = of the darker
```

It ranges from 1 to 21. Source: WCAG 2.2, definition of "contrast ratio", https://www.w3.org/TR/WCAG22/#dfn-contrast-ratio

Two details that matter for a unit test:

- **The threshold constant.** The spec's note says that before May 2021 the value was 0.03928, taken from an older version of the sRGB specification, that 0.04045 is correct, and that the difference "has no practical effect on the calculations in the context of these guidelines" (same definition, https://www.w3.org/TR/WCAG22/#dfn-relative-luminance). Checked: no 8-bit channel value falls between the two (10/255 = 0.0392, 11/255 = 0.0431), so for ARGB ints the results are identical.
- **No rounding.** The ratios are thresholds and computed values are not to be rounded: "2.999:1 would not meet the 3:1 threshold". Source: Understanding SC 1.4.11, https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html. One proposed pair below is 5.998, which prints as 6.00 and is under 6.

The formula is defined for opaque colours. A colour with alpha has to be composited over what is under it before it is measured (the shimmer in the translate strip, for instance); WCAG gives no formula for that.

### 1.2 The minimums

| what | level | minimum | source |
|---|---|---|---|
| Text and images of text | AA | 4.5:1 | SC 1.4.3, https://www.w3.org/TR/WCAG22/#contrast-minimum |
| Large-scale text | AA | 3:1 | SC 1.4.3, same link |
| Text | AAA | 7:1 | SC 1.4.6, https://www.w3.org/TR/WCAG22/#contrast-enhanced |
| Large-scale text | AAA | 4.5:1 | SC 1.4.6, same link |
| Non-text: "visual information required to identify user interface components and states", and parts of graphics needed to understand the content | AA | 3:1 against adjacent colours | SC 1.4.11, https://www.w3.org/TR/WCAG22/#non-text-contrast |

- **Large scale** means "at least 18 point or 14 point bold", or the equivalent size for CJK fonts. Source: https://www.w3.org/TR/WCAG22/#dfn-large-scale. WCAG states this in points; it says nothing about Android's sp, and nothing about Myanmar script. I found no primary source that maps the definition onto sp, so the safe reading is the one the project already uses: treat every label and hint as normal text, 4.5:1.
- **Exempt from 1.4.3:** text in an inactive component, pure decoration, and logotypes. Source: SC 1.4.3, link above.
- **Key outlines against the background are not required to reach 3:1.** Understanding 1.4.11 says the criterion "does not require that controls have a visual boundary indicating the hit area", and that where a control has visible content such as text or a sufficiently contrasting icon, a border is not required. Source: https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html. So `key` against `background` (1.05 to 1.49 in every theme here, including Light at 1.23 and Dark at 1.45) is not a failure.
- **States are covered by 1.4.11.** Where colour alone shows a state, the mark needs 3:1 against what is next to it. In this keyboard that is the accent: shift lock, the selected alternate (accent against `callout`), and the underline of the selected emoji category (accent against `background`). `ThemesTest` already checks accent against background.

## 2. Android facts for a themed IME

developer.android.com has no guidance on edge-to-edge, the navigation bar or dark mode that is specific to an IME window. The guide "Create an input method" (https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method) covers none of them. What follows joins the general platform documentation to what AOSP source does for the IME window.

### 2.1 The colour of the navigation bar area

What the documentation says:

- Apps targeting API 35 are edge-to-edge on Android 15. Gesture navigation bar: "transparent by default"; `setNavigationBarColor` and `R.attr#navigationBarColor` "are deprecated and don't affect gesture navigation". Three-button navigation: "opacity set to 80% by default, with color possibly matching the window background"; `setNavigationBarColor` is "set to match the window background by default", "the window background must be a color drawable for this default to apply", the API "is deprecated but continues to affect 3-button navigation", and `setNavigationBarContrastEnforced` "is true by default, which adds an 80% opaque background across 3-button navigation". Source: https://developer.android.com/about/versions/15/behavior-changes-15
- For apps targeting API 36, the opt-out `R.attr#windowOptOutEdgeToEdgeEnforcement` is "deprecated and disabled". Source: https://developer.android.com/about/versions/16/behavior-changes-16. With targetSdk 36 there is no way back to the old behaviour.
- `Window.setNavigationBarColor`: added in API 21, deprecated in API 35. "Draw proper background behind WindowInsets.Type.navigationBars() or WindowInsets.Type.tappableElement() instead." "If the app targets VANILLA_ICE_CREAM or above, the color will be transparent and cannot be changed." Source: https://developer.android.com/reference/android/view/Window#setNavigationBarColor(int)
- `Window.setNavigationBarContrastEnforced`: added in API 29, not deprecated. "When the navigation bar color has a non-zero alpha value, the value of this property has no effect." Source: https://developer.android.com/reference/android/view/Window#setNavigationBarContrastEnforced(boolean)
- The edge-to-edge guide: once an app targets SDK 35 or higher "the system status bar and gesture navigation bars are transparent, but the three-button navigation bar is translucent"; set `window.isNavigationBarContrastEnforced = false` to remove that; and "you might need to manually update the system bar icon colors". Source: https://developer.android.com/develop/ui/views/layout/edge-to-edge

What AOSP does for the IME window (all under https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/):

- The IME window is a dialog with an ordinary `PhoneWindow`, of type `TYPE_INPUT_METHOD`, created with `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` set ("For ColorView in DecorView to work, FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS needs to be set by default"). Source: `core/java/android/inputmethodservice/InputMethodService.java`
- Edge-to-edge enforcement is decided from the application's `targetSdkVersion`, so it applies to the IME's window as it does to an activity's. When enforced, `getNavigationBarColor()` returns transparent; `setNavigationBarColor` still stores the colour because "it might be used to draw the scrim". Source: `core/java/com/android/internal/policy/PhoneWindow.java`
- The bar background is drawn for the IME window too ("IME is an exceptional floating window that requires color view"). In `calculateBarColor`, with contrast enforced and a fully transparent bar colour the result is a scrim: 90% white for a light bar, otherwise a semi-transparent dark colour; under enforced edge-to-edge a stored bar colour is turned into an 80%-opaque scrim of that colour; with contrast not enforced the result is transparent. The scrim is skipped when the system suppresses it for the bar, which is the gesture-navigation case. Source: `core/java/com/android/internal/policy/DecorView.java`

What to do, as a consequence:

- **API 35 and later:** draw the keyboard's own background behind the navigation-bar inset. `KeyboardView` already does this: it takes the bottom system-bar inset into its height and fills the whole view with `palette.background`. With gesture navigation nothing more is needed for colour.
- **Three-button navigation, API 35 and later:** by default an 80% scrim sits over the bar, so the bar area will not match the theme exactly. `setNavigationBarContrastEnforced(false)` on the IME's window (API 29+) removes it, after which the icon colour in 2.2 is the only thing keeping the icons readable.
- **API 24 to 34:** `setNavigationBarColor(palette.background)` still works there, and is how AOSP's own keyboard did it (`LatinIME.setNavigationBarVisibility`, https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/java/src/com/android/inputmethod/latin/LatinIME.java). HeliBoard gives the bar the keyboard background's colour (its `NAVIGATION_BAR` colour type; see 3.2). The app has no such call today. Whether the keyboard's view extends behind the bar on those releases was not checked.

Not confirmed from a primary source: what the system uses as the IME window's background drawable, and therefore what the default three-button scrim colour is for this keyboard.

### 2.2 Light or dark navigation-bar icons

- `WindowInsetsController.setSystemBarsAppearance(int appearance, int mask)`: added in API 30. `APPEARANCE_LIGHT_NAVIGATION_BARS`: added in API 30, "changes the foreground color for light navigation bars so that the items on the bar can be read clearly". Source: https://developer.android.com/reference/android/view/WindowInsetsController
- `View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR`: added in API 26, deprecated in API 30 in favour of the above. "For this to take effect, the window must request FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS but not FLAG_TRANSLUCENT_NAVIGATION." The matching theme attribute is `R.attr.windowLightNavigationBar`. Source: https://developer.android.com/reference/android/view/View#SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
- API 24 and 25 have no light-navigation-bar flag (the first one was added in API 26), so the keyboard cannot ask for dark icons there.

How the choice reaches the bar when the keyboard is showing (AOSP `main`):

- The window manager picks one "navigation colour window" and takes the light-navigation-bar bit from it. If the IME window is visible, there is a bottom navigation bar, and the IME window has `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`, the IME window is that window. Source: `chooseNavigationColorWindowLw` and `updateLightNavigationBarLw` in https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/wm/DisplayPolicy.java
- Where the IME draws the back and IME-switcher buttons itself (gesture navigation on recent releases), `NavigationBarController` listens for the window's appearance and tints them: dark icons when `APPEARANCE_LIGHT_NAVIGATION_BARS` is set and the window draws system-bar backgrounds. Source: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/inputmethodservice/NavigationBarController.java
- When edge-to-edge is enforced, no bar colour was specified and the window background is a `ColorDrawable`, `DecorView.setWindowBackground` sets a forced light-bar appearance from that colour's luminance (above 0.5 means light). The keyboard draws its background in its view, not as the window background, so this default cannot be relied on to track the theme. Source: `DecorView.java`, link in 2.1.

So, for a keyboard whose theme no longer follows the system:

| API | call on the IME window (`InputMethodService.getWindow().getWindow()`) |
|---|---|
| 30+ | `insetsController.setSystemBarsAppearance(if (light theme) APPEARANCE_LIGHT_NAVIGATION_BARS else 0, APPEARANCE_LIGHT_NAVIGATION_BARS)` |
| 26 to 29 | add or clear `SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR` in the decor view's `systemUiVisibility` |
| 24, 25 | nothing available; a light theme cannot get dark icons |

Not confirmed: the default value of the light-navigation-bar appearance on the IME window when the app sets nothing (it comes from the window's theme, which I did not trace), and whether the bit is honoured on every vendor build. Not tried on a device.

### 2.3 Does a `uiMode` change recreate the input view?

Yes, unless the IME says it handles that change itself.

- Documentation: "Subclasses of InputMethodService generally don't need to deal directly with this on their own; the standard implementation here takes care of regenerating the input method UI as a result of the configuration change, so you can rely on your onCreateInputView() and other methods being called as appropriate." Source: https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onConfigurationChanged(android.content.res.Configuration)
- AOSP: `onConfigurationChanged` hands the new configuration to a tracker, which calls `resetStateForNewConfiguration()` when the public diff contains any change the IME has not declared as handled (`(diff & ~handledConfigChanges) != 0`). The reset calls `initViews()`, restarts input, and shows the window again, which calls `onCreateInputView()`. Sources: `InputMethodService.java` and `ImsConfigurationTracker.java` in https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/inputmethodservice/
- An IME can declare the changes it handles itself with the `configChanges` attribute of its `<input-method>` XML. The reference says this works from `S` (API 31) to `TIRAMISU` (API 33) and again from a release it names `CINNAMON_BUN_1`, and that between `UPSIDE_DOWN_CAKE` (Android 14) and `CINNAMON_BUN` the declared changes "were not recognized due to an XML parsing error". Source: the `InputMethodService` reference, link above. This keyboard's `method.xml` declares none, so a `uiMode` change rebuilds the view on every release. That confirms the comment in `KeyboardView`.

What this does not cover: a theme written to SharedPreferences by the app is not a configuration change. `AkkharaImeService` reads settings in `onStartInputView`, and `KeyboardView` reads its palette once when constructed, so a newly picked theme needs one of: the service comparing the shown theme in `onStartInputView` and calling `setInputView(onCreateInputView())`, or the view (with `StripPainter`, `KeyCallout` and `EmojiPanel`, which each hold the palette) taking a new palette. HeliBoard and FlorisBoard both hold the current colours in one place and rebuild or recompose when the preference changes (3.2, 3.3).

## 3. How open-source keyboards model a theme

I read the source of each. Nothing here is copied from them, and their licences are noted so it stays that way.

| | AOSP LatinIME | HeliBoard | FlorisBoard |
|---|---|---|---|
| Licence | Apache-2.0 | GPL-3.0-only (files inherited from AOSP/OpenBoard are Apache-2.0) | Apache-2.0 |
| A theme is | an Android style resource plus an id | a style (shape) plus a set of about ten base colours | a stylesheet file (JSON, CSS-like) |
| Colour roles | about 20 theme attributes, some drawables | 7 required + 3 optional base colours, expanded to about 45 named roles | 15 variables, then about 100 rules per element and state |
| Stored as | theme id, as a string, in SharedPreferences | theme name strings in SharedPreferences | theme ids in preferences |
| Light and dark | each theme is one fixed look | each theme is one fixed look | each theme is one fixed look, flagged day or night |
| Follow system | none | a day theme and a night theme, with a switch that is on by default from API 29 | a mode (always day, always night, follow system, follow time) choosing between a day theme and a night theme |

### 3.1 AOSP LatinIME

Repository: https://android.googlesource.com/platform/packages/inputmethods/LatinIME/ (Apache-2.0; licence headers in each file).

- A theme is `(themeId, name, styleId, minApiVersion)`; there are four (ICS, KLP, LXX Light, LXX Dark). The chosen id is stored as a string under one SharedPreferences key; the default depends on the API level; an unknown id is removed and the default used. Source: `java/src/com/android/inputmethod/keyboard/KeyboardTheme.java`
- Colours are style attributes and drawables, not a palette object. The roles, from `java/res/values/attrs.xml`: `keyBackground`, `functionalKeyBackground`, `spacebarBackground`, `keyPreviewBackground` (drawables); `keyTextColor`, `keyTextShadowColor`, `keyTextInactivatedColor`, `functionalTextColor`, `keyHintLetterColor`, `keyHintLabelColor`, `keyShiftedLetterHintInactivatedColor`, `keyShiftedLetterHintActivatedColor`, `keyPreviewTextColor`, `languageOnSpacebarTextColor`; gesture trail and preview colours; four suggestion-strip text colours; and the emoji page indicator colours.
- No follow-system. Light and dark are two separate themes the user picks between.

### 3.2 HeliBoard

Repository: https://github.com/Helium314/HeliBoard (GPL-3.0-only; a fork of OpenBoard, itself from AOSP). GPL code must not be copied into this project.

- Shape and colour are separate settings: a style (Material, Holo, Rounded) plus a key-borders switch, and a colour theme. Source: `app/src/main/java/helium314/keyboard/keyboard/KeyboardTheme.kt`
- A built-in colour theme gives seven colours (accent, background, key background, functional key, space bar, key text, key hint text) and optionally three more (suggestion text, space bar text, gesture trail), which default to the others. Everything else is derived: pressed fills by brightening or darkening, the navigation bar from the background. Source: `app/src/main/java/helium314/keyboard/latin/common/Colors.kt`
- Drawing code asks for a role by name from an enum of about 45, among them `KEY_BACKGROUND`, `KEY_TEXT`, `KEY_HINT_TEXT`, `KEY_ICON`, `FUNCTIONAL_KEY_BACKGROUND`, `FUNCTIONAL_KEY_TEXT`, `ACTION_KEY_BACKGROUND`, `ACTION_KEY_ICON`, `SPACE_BAR_BACKGROUND`, `SPACE_BAR_TEXT`, `KEY_PREVIEW_BACKGROUND`, `KEY_PREVIEW_TEXT`, `POPUP_KEYS_BACKGROUND`, `POPUP_KEY_TEXT`, `STRIP_BACKGROUND`, `SUGGESTED_WORD`, `EMOJI_CATEGORY`, `EMOJI_CATEGORY_SELECTED`, `GESTURE_TRAIL`, `MAIN_BACKGROUND`, `NAVIGATION_BAR`, `TOOL_BAR_KEY`. Source: same file.
- Stored as theme-name strings: one preference for the day colours (default "light"), one for the night colours (default "dark"), and a boolean for following the system (default on from API 29). The current colours are chosen by: night if the system is in night mode and the boolean is on. Themes that only work in light are hidden from the night list. Sources: `KeyboardTheme.kt` and `app/src/main/java/helium314/keyboard/latin/settings/Defaults.kt`
- Its contrast check is a plain distance in RGB, not WCAG. Source: `app/src/main/java/helium314/keyboard/latin/utils/ColorUtil.kt`

### 3.3 FlorisBoard

Repository: https://github.com/florisboard/florisboard (Apache-2.0).

- A theme is one stylesheet inside a theme extension. The extension's manifest lists each theme with an id, a label, authors, an `isNight` flag and the stylesheet's path. Sources: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/theme/ThemeExtension.kt` and `app/src/main/assets/extensions/org.florisboard.themes/extension.json`
- The stylesheet defines variables first: `--primary`, `--primary-variant`, `--secondary`, `--secondary-variant`, `--background`, `--background-variant`, `--surface`, `--surface-variant`, `--popup-surface`, `--focused-popup-surface`, `--on-primary`, `--on-background`, `--on-background-disabled`, `--on-surface`, `--on-surface-variant`, plus shapes. Then rules per element and state give a background, a foreground, a shape and a font size: `window`, `key`, `key:pressed`, a key by code (enter, space), shift in caps lock, `key-hint`, `key-popup-box`, the focused popup element, and the smartbar and clipboard parts. Source: `app/src/main/assets/extensions/org.florisboard.themes/stylesheets/floris_day.json`
- Selection is three preferences: a mode (`ALWAYS_DAY`, `ALWAYS_NIGHT`, `FOLLOW_SYSTEM`, `FOLLOW_TIME`; default follow system), a day theme id and a night theme id. A controller turns the mode, the system's mode and the clock into day or night and loads that theme, falling back to a built-in style if loading fails. Sources: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/theme/PreferredThemeMode.kt`, `ThemeController.kt`, and `app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt`

### 3.4 What carries over

- A small set of named colours is enough. HeliBoard draws a whole keyboard from seven; this project's ten roles sit between HeliBoard's base colours and its derived roles.
- All three store the chosen theme as a name or id and fall back to a default when the stored value is unknown. `KeyboardSettings.inputLanguage` already does this with an enum name.
- None of them lets one theme change with the system. Follow-system is always a choice between two fixed themes.
- None of them checks contrast with the WCAG formula. A test that does is ahead of the field, not behind it.

## 4. Well-known colour schemes

Hex values are copied from each scheme's own files. "No trademark notice found" means only that the files and pages named here carry none; I did not search any trademark register, so whether a name is registered is not verified for any of them.

### 4.1 Licence and name, in short

| scheme | licence | copyright line | under its own name, with attribution? |
|---|---|---|---|
| Solarized | MIT | "Copyright (c) 2011 Ethan Schoonover" | yes |
| Nord | MIT | "Copyright (c) 2016-present Sven Greb" | yes |
| Dracula | MIT | "Copyright (c) 2023 Dracula Theme" | yes |
| Gruvbox | "MIT/X11" stated in the README only | none in the repository | yes, with a caveat (below) |
| Catppuccin | MIT | "Copyright (c) 2021 Catppuccin" | yes |
| Tokyo Night | MIT | "Copyright (c) 2018-present Enkia" | yes |
| Rosé Pine | MIT | "Copyright (c) 2023 Rosé Pine" | yes |
| One Dark | MIT | "Copyright (c) 2016 GitHub Inc." | yes |
| Monokai | proprietary | Monokai (Wimer Hazenberg) | no |

**What MIT asks for.** One condition: "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software." So each MIT scheme that ships needs its copyright line and the MIT text somewhere the user of the app can reach, for example a "Theme credits" page in the Akkhara app or a notices file packaged with it. Whether ten hex values are a "substantial portion" is a legal question this research cannot settle; including the notices makes the question moot. Nothing may be fetched for this: the notices have to be built in (ADR 0002).

**Gruvbox caveat.** The README says "License: MIT/X11", but the repository has no licence file, GitHub reports no licence for it, and the only authorship line is "Author: morhetz" in the Vim file. Sources: https://github.com/morhetz/gruvbox (README and file list), `colors/gruvbox.vim`. There is therefore no canonical copyright notice to reproduce. The closest honest attribution is "Gruvbox by morhetz (https://github.com/morhetz/gruvbox), MIT/X11 as stated in its README".

**Monokai.** The name is the alias and company name of its author. The licence page says the Monokai Pro extensions "are proprietary software" and "may not be sub-licensed, resold, or redistributed"; the contribute page says the colour schemes are copyrighted and allows community editions only where no official theme exists, named "Monokai Pro (CE)", free, non-commercial, open source, from its template, and with the right for Monokai to take them over. Sources: https://monokai.pro/license, https://monokai.pro/contribute, https://monokai.pro/history. I found no open licence for the original 2006 palette and no official table of its hex values (the 2006 blog post's address returns 404). Do not ship it, under that name or as a look-alike with that name.

**Usage rules found for the others.** Dracula's and Catppuccin's written rules are about ports submitted to their own organisations (https://draculatheme.com/contribute, https://github.com/catppuccin/catppuccin/blob/main/docs/port-creation.md); an app that only uses the colours is not submitting a port. Dracula's spec asks implementers to "use exact hex values" and to keep 4.5:1 (https://draculatheme.com/spec). "Dracula Pro" is a separate paid product; do not use that name. No trademark notice was found in the READMEs of Solarized, Nord, Dracula, Gruvbox, Catppuccin, Tokyo Night, Rosé Pine or One Dark, nor on the Nord home page.

Say in the credits that the themes are not made or endorsed by those projects. That is caution on my part, not a term any of the licences state.

### 4.2 Palettes

**Solarized.** Source: https://github.com/altercation/solarized (README, "The Values"; `LICENSE`).

| base03 | base02 | base01 | base00 | base0 | base1 | base2 | base3 |
|---|---|---|---|---|---|---|---|
| `#002b36` | `#073642` | `#586e75` | `#657b83` | `#839496` | `#93a1a1` | `#eee8d5` | `#fdf6e3` |

| yellow | orange | red | magenta | violet | blue | cyan | green |
|---|---|---|---|---|---|---|---|
| `#b58900` | `#cb4b16` | `#dc322f` | `#d33682` | `#6c71c4` | `#268bd2` | `#2aa198` | `#859900` |

Intended use, from the README: dark is background base03 with body text base0, and highlighted background base02 with text base1; light is the same with the base tones mirrored (base3 with base00, base2 with base01). Those four pairs compute to 4.75, 4.86, 4.13 and 4.39.

**Nord.** Source: https://github.com/nordtheme/nord (`src/nord.css`; `license`).

| nord0 | nord1 | nord2 | nord3 | nord4 | nord5 | nord6 | nord7 |
|---|---|---|---|---|---|---|---|
| `#2e3440` | `#3b4252` | `#434c5e` | `#4c566a` | `#d8dee9` | `#e5e9f0` | `#eceff4` | `#8fbcbb` |

| nord8 | nord9 | nord10 | nord11 | nord12 | nord13 | nord14 | nord15 |
|---|---|---|---|---|---|---|---|
| `#88c0d0` | `#81a1c1` | `#5e81ac` | `#bf616a` | `#d08770` | `#ebcb8b` | `#a3be8c` | `#b48ead` |

**Dracula.** Sources: https://draculatheme.com/spec and https://github.com/dracula/dracula-theme (`LICENSE`).

| Background | Selection | Foreground | Comment | Red | Orange | Yellow | Green | Cyan | Purple | Pink |
|---|---|---|---|---|---|---|---|---|---|---|
| `#282a36` | `#44475a` | `#f8f8f2` | `#6272a4` | `#ff5555` | `#ffb86c` | `#f1fa8c` | `#50fa7b` | `#8be9fd` | `#bd93f9` | `#ff79c6` |

UI palette from the spec (Dracula Classic):

| Floating interactive elements / Background Light | Background Lighter | Background Dark | Background Darker |
|---|---|---|---|
| `#343746` | `#424450` | `#21222c` | `#191a21` |

The spec also has a light variant, Alucard (background `#fffbeb`, foreground `#1f1f1f`), not mapped here.

**Gruvbox.** Source: https://github.com/morhetz/gruvbox (`colors/gruvbox.vim`). Dark mode uses dark0 to dark4 as backgrounds, light0 to light4 as foregrounds and the bright accents; light mode swaps them and uses the faded accents.

| dark0_hard | dark0 | dark0_soft | dark1 | dark2 | dark3 | dark4 | gray |
|---|---|---|---|---|---|---|---|
| `#1d2021` | `#282828` | `#32302f` | `#3c3836` | `#504945` | `#665c54` | `#7c6f64` | `#928374` |

| light0_hard | light0 | light0_soft | light1 | light2 | light3 | light4 |
|---|---|---|---|---|---|---|
| `#f9f5d7` | `#fbf1c7` | `#f2e5bc` | `#ebdbb2` | `#d5c4a1` | `#bdae93` | `#a89984` |

| | red | green | yellow | blue | purple | aqua | orange |
|---|---|---|---|---|---|---|---|
| bright | `#fb4934` | `#b8bb26` | `#fabd2f` | `#83a598` | `#d3869b` | `#8ec07c` | `#fe8019` |
| neutral | `#cc241d` | `#98971a` | `#d79921` | `#458588` | `#b16286` | `#689d6a` | `#d65d0e` |
| faded | `#9d0006` | `#79740e` | `#b57614` | `#076678` | `#8f3f71` | `#427b58` | `#af3a03` |

**Catppuccin.** Sources: https://github.com/catppuccin/palette (`palette.json`, version 1.8.0; `LICENSE`) and the style guide, https://github.com/catppuccin/catppuccin/blob/main/docs/style-guide.md. The style guide assigns: background pane Base; secondary panes Crust and Mantle; surface elements Surface 0 to 2; body copy Text; labels Subtext 0 and 1; on-accent Base; and adds "Legibility always comes first, so please use your own judgement."

| | text | subtext1 | subtext0 | overlay2 | overlay1 | overlay0 | surface2 | surface1 | surface0 | base | mantle | crust |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Latte | `#4c4f69` | `#5c5f77` | `#6c6f85` | `#7c7f93` | `#8c8fa1` | `#9ca0b0` | `#acb0be` | `#bcc0cc` | `#ccd0da` | `#eff1f5` | `#e6e9ef` | `#dce0e8` |
| Mocha | `#cdd6f4` | `#bac2de` | `#a6adc8` | `#9399b2` | `#7f849c` | `#6c7086` | `#585b70` | `#45475a` | `#313244` | `#1e1e2e` | `#181825` | `#11111b` |

| | rosewater | flamingo | pink | mauve | red | maroon | peach | yellow | green | teal | sky | sapphire | blue | lavender |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Latte | `#dc8a78` | `#dd7878` | `#ea76cb` | `#8839ef` | `#d20f39` | `#e64553` | `#fe640b` | `#df8e1d` | `#40a02b` | `#179299` | `#04a5e5` | `#209fb5` | `#1e66f5` | `#7287fd` |
| Mocha | `#f5e0dc` | `#f2cdcd` | `#f5c2e7` | `#cba6f7` | `#f38ba8` | `#eba0ac` | `#fab387` | `#f9e2af` | `#a6e3a1` | `#94e2d5` | `#89dceb` | `#74c7ec` | `#89b4fa` | `#b4befe` |

**Tokyo Night.** Source: https://github.com/tokyo-night/tokyo-night-vscode-theme (README "Color Palette"; `themes/tokyo-night-color-theme.json`; `LICENSE.txt`). The README lists syntax colours; the interface greys exist only as values in the theme file, so the names in the second table are mine, taken from the setting each value is used for.

| red | orange | yellow | green | teal | cyan | blue | magenta | terminal white | editor foreground | markdown text | comments | terminal black | background (Storm) | background (Night) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `#f7768e` | `#ff9e64` | `#e0af68` | `#9ece6a` | `#73daca` | `#7dcfff` | `#7aa2f7` | `#bb9af7` | `#c0caf5` | `#a9b1d6` | `#9aa5ce` | `#565f89` | `#414868` | `#24283b` | `#1a1b26` |

| side bar background | borders | interface foreground | input background | line highlight | list selection | line numbers, ANSI black | secondary button |
|---|---|---|---|---|---|---|---|
| `#16161e` | `#101014` | `#787c99` | `#14141b` | `#1e202e` | `#202330` | `#363b54` | `#3b3e52` |

**Rosé Pine.** Sources: https://rosepinetheme.com/palette and https://github.com/rose-pine/rose-pine-theme (`LICENSE`).

| | base | surface | overlay | muted | subtle | text | love | gold | rose | pine | foam | iris | highlight low | highlight med | highlight high |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| main | `#191724` | `#1f1d2e` | `#26233a` | `#6e6a86` | `#908caa` | `#e0def4` | `#eb6f92` | `#f6c177` | `#ebbcba` | `#31748f` | `#9ccfd8` | `#c4a7e7` | `#21202e` | `#403d52` | `#524f67` |
| Dawn | `#faf4ed` | `#fffaf3` | `#f2e9e1` | `#9893a5` | `#797593` | see note | `#b4637a` | `#ea9d34` | `#d7827e` | `#286983` | `#56949f` | `#907aa9` | `#f4ede8` | `#dfdad9` | `#cecacd` |

Note on Dawn's text: the site's palette page shows `#464261`; the project's palette repository (CSS build) has `#575279`. I could not tell which is current. The figures below use `#575279`, the lower-contrast of the two, so they hold either way.

**One Dark.** Sources: https://github.com/atom/one-dark-syntax (`styles/colors.less`, `styles/syntax-variables.less`) and https://github.com/atom/one-dark-ui (`styles/ui-variables.less`, `styles/ui-variables-custom.less`); both `LICENSE.md`; both repositories are archived. **The source defines every colour in HSL, not hex.** The hex values here are computed from it by the script and are not quoted from the project.

| name in the source | definition | computed hex |
|---|---|---|
| `@mono-1` (default text) | hsl(220, 14%, 71%) | `#abb2bf` |
| `@mono-2` | hsl(220, 9%, 55%) | `#828997` |
| `@mono-3` | hsl(220, 10%, 40%) | `#5c6370` |
| `@hue-1` cyan | hsl(187, 47%, 55%) | `#56b6c2` |
| `@hue-2` blue | hsl(207, 82%, 66%) | `#61afef` |
| `@hue-3` purple | hsl(286, 60%, 67%) | `#c678dd` |
| `@hue-4` green | hsl(95, 38%, 62%) | `#98c379` |
| `@hue-5` red | hsl(355, 65%, 65%) | `#e06c75` |
| `@hue-6` orange | hsl(29, 54%, 61%) | `#d19a66` |
| `@hue-6-2` orange | hsl(39, 67%, 69%) | `#e5c07b` |
| `@syntax-bg`, `@level-2-color` | hsl(220, 13%, 18%) | `#282c34` |
| `@level-1-color` (buttons) | lighten(bg, 6%) | `#353b45` |
| `@level-3-color` (app background) | darken(bg, 3%) | `#21252b` |
| `@background-color-highlight` | lighten(bg, 4%) | `#31363f` |
| `@background-color-selected` | lighten(bg, 8%) | `#3a3f4b` |
| `@syntax-selection-color` | lighten(bg, 10%) | `#3e4451` |
| `@input-background-color` | darken(bg, 6%) | `#1b1d23` |
| `@ui-fg` (interface text) | hsl(220, 13%, 66%) | `#9da5b4` |

## 5. Mapping each scheme onto the ten palette roles

Only official colours are used. Two mappings were computed per scheme: the *natural* one, which uses each colour for what its scheme says it is for, and the *proposed* one, which changes only what has to change to reach 4.5:1.

Pairs measured are the ones the `Palette` doc comment names: `label` on `key`, `keyPressed`, `actionKey` and `callout`; `actionLabel` on `actionKey`; `hint` on `key`; `onAccent` on `accent`. As the drawing code stands, `label` is never painted on `actionKey` (action keys and the translation tile use `actionLabel`), and `ThemesTest` leaves that pair out; it is included here because the comment names it.

### 5.1 What fails in the natural mappings

Bold is under 4.5:1. "Nearest" is the official colour closest to the natural one (by CIE76 distance) that reaches 4.5:1 on every fill that role sits on.

| scheme | pair | natural colours | ratio | nearest official colour that passes |
|---|---|---|---|---|
| Solarized Light | label / key | base00 on base3 | **4.13** | label = base02 `#073642` (lowest 4.86) |
| Solarized Light | label / keyPressed | base00 on base1 | **1.67** | label = base02 `#073642` |
| Solarized Light | label / actionKey | base00 on base2 | **3.64** | label = base02 `#073642` |
| Solarized Light | label / callout | base00 on base3 | **4.13** | label = base02 `#073642` |
| Solarized Light | actionLabel / actionKey | base01 on base2 | **4.39** | actionLabel = base02 `#073642` (10.61), or actionKey = base3 (4.99) |
| Solarized Light | hint / key | base1 on base3 | **2.48** | hint = base01 `#586e75` (4.99) |
| Solarized Light | onAccent / accent | base3 on blue | **3.41** | none as onAccent on blue; change the accent instead (see 5.4) |
| Solarized Dark | label / key | base0 on base02 | **4.11** | label = base3 `#fdf6e3` (lowest 4.99) |
| Solarized Dark | label / keyPressed | base0 on base01 | **1.70** | label = base3 `#fdf6e3` |
| Solarized Dark | label / callout | base0 on base02 | **4.11** | label = base3 `#fdf6e3` |
| Solarized Dark | hint / key | base01 on base02 | **2.42** | hint = base1 `#93a1a1` (4.86) |
| Solarized Dark | onAccent / accent | base03 on blue | **4.08** | none as onAccent on blue; accent = cyan `#2aa198` with base03 (4.75) |
| Nord | hint / key | nord9 on nord2 | **3.21** | hint = nord4 `#d8dee9` (6.39) |
| Dracula | label / keyPressed | foreground on comment | **4.41** | keyPressed = selection `#44475a` (8.59) |
| Dracula | hint / key | comment on selection | **1.94** | no muted tone passes; the lowest-contrast official colours that do are purple and pink |
| Gruvbox Light | hint / key | dark4 on light0_hard | **4.42** | hint = dark3 `#665c54` (5.92) |
| Gruvbox Dark | hint / key | light4 on dark1 | **4.17** | hint = light3 `#bdae93` (5.32) |
| Catppuccin Latte | label / keyPressed | text on surface1 | **4.39** | no official label colour passes on surface1 (text is the darkest); keyPressed = surface0 `#ccd0da` (5.17) |
| Catppuccin Latte | actionLabel / actionKey | subtext1 on surface0 | **4.05** | actionLabel = text `#4c4f69` (5.17), or actionKey = crust (4.73) |
| Catppuccin Latte | hint / key | subtext0 on base | **4.37** | hint = subtext1 `#5c5f77` (5.53) |
| Catppuccin Mocha | none | | | |
| Tokyo Night | hint / key | interface foreground on Storm background | **3.57** | hint = markdown text `#9aa5ce` (6.00) |
| Rosé Pine | none | | | |
| One Dark | hint / key | mono-2 on level-1 | **3.21** | hint = ui-fg `#9da5b4` (4.55) |

The natural mappings, for reference:

| scheme | background | key | keyPressed | actionKey | label | actionLabel | hint | callout | accent | onAccent |
|---|---|---|---|---|---|---|---|---|---|---|
| Solarized Light | base2 | base3 | base1 | base2 | base00 | base01 | base1 | base3 | blue | base3 |
| Solarized Dark | base03 | base02 | base01 | base03 | base0 | base1 | base01 | base02 | blue | base03 |
| Nord | nord0 | nord2 | nord3 | nord1 | nord6 | nord4 | nord9 | nord3 | nord8 | nord0 |
| Dracula | Background Dark | Selection | Comment | Background Light | Foreground | Foreground | Comment | Background Lighter | Purple | Background |
| Gruvbox Light | light1 | light0_hard | light3 | light2 | dark1 | dark2 | dark4 | light0_hard | faded blue | light0 |
| Gruvbox Dark | dark0_hard | dark1 | dark3 | dark0_soft | light1 | light2 | light4 | dark2 | bright yellow | dark0 |
| Catppuccin Latte | crust | base | surface1 | surface0 | text | subtext1 | subtext0 | base | mauve | base |
| Catppuccin Mocha | crust | surface0 | surface2 | base | text | subtext1 | subtext0 | surface1 | mauve | base |
| Tokyo Night | side bar background | Storm background | terminal black | Night background | terminal white | editor foreground | interface foreground | ANSI black | blue | Night background |
| Rosé Pine | base | overlay | highlight high | surface | text | text | subtle | highlight med | iris | base |
| One Dark | level-3 | level-1 | syntax selection | syntax-bg | mono-1 | mono-1 | mono-2 | selected background | hue-2 blue | level-3 |

### 5.2 Proposed mappings

Every text pair in every row reaches 4.5:1.

| theme | background | key | keyPressed | actionKey | label | actionLabel | hint | callout | accent | onAccent |
|---|---|---|---|---|---|---|---|---|---|---|
| Solarized Light | base2 `#eee8d5` | base3 `#fdf6e3` | base1 `#93a1a1` | base2 `#eee8d5` | base03 `#002b36` | base02 `#073642` | base01 `#586e75` | base3 `#fdf6e3` | base02 `#073642` | base3 `#fdf6e3` |
| Solarized Dark | base03 `#002b36` | base02 `#073642` | base01 `#586e75` | base03 `#002b36` | base3 `#fdf6e3` | base1 `#93a1a1` | base1 `#93a1a1` | base02 `#073642` | cyan `#2aa198` | base03 `#002b36` |
| Nord | nord0 `#2e3440` | nord2 `#434c5e` | nord3 `#4c566a` | nord1 `#3b4252` | nord6 `#eceff4` | nord4 `#d8dee9` | nord4 `#d8dee9` | nord3 `#4c566a` | nord8 `#88c0d0` | nord0 `#2e3440` |
| Dracula | Background Dark `#21222c` | Background Light `#343746` | Selection `#44475a` | Background `#282a36` | Foreground `#f8f8f2` | Foreground `#f8f8f2` | Purple `#bd93f9` | Background Lighter `#424450` | Pink `#ff79c6` | Background Dark `#21222c` |
| Gruvbox Light | light1 `#ebdbb2` | light0_hard `#f9f5d7` | light3 `#bdae93` | light2 `#d5c4a1` | dark1 `#3c3836` | dark1 `#3c3836` | dark3 `#665c54` | light0_hard `#f9f5d7` | faded blue `#076678` | light0 `#fbf1c7` |
| Gruvbox Dark | dark0_hard `#1d2021` | dark1 `#3c3836` | dark3 `#665c54` | dark0_soft `#32302f` | light1 `#ebdbb2` | light2 `#d5c4a1` | light3 `#bdae93` | dark2 `#504945` | bright yellow `#fabd2f` | dark0 `#282828` |
| Catppuccin Latte | mantle `#e6e9ef` | base `#eff1f5` | surface0 `#ccd0da` | crust `#dce0e8` | text `#4c4f69` | text `#4c4f69` | subtext1 `#5c5f77` | base `#eff1f5` | mauve `#8839ef` | base `#eff1f5` |
| Catppuccin Mocha | crust `#11111b` | surface0 `#313244` | surface2 `#585b70` | base `#1e1e2e` | text `#cdd6f4` | subtext1 `#bac2de` | subtext0 `#a6adc8` | surface1 `#45475a` | mauve `#cba6f7` | base `#1e1e2e` |
| Tokyo Night | side bar background `#16161e` | Storm background `#24283b` | terminal black `#414868` | Night background `#1a1b26` | terminal white `#c0caf5` | editor foreground `#a9b1d6` | markdown text `#9aa5ce` | ANSI black `#363b54` | blue `#7aa2f7` | Night background `#1a1b26` |
| Rosé Pine | base `#191724` | overlay `#26233a` | highlight high `#524f67` | surface `#1f1d2e` | text `#e0def4` | text `#e0def4` | subtle `#908caa` | highlight med `#403d52` | iris `#c4a7e7` | base `#191724` |
| One Dark | level-3 `#21252b` | highlight background `#31363f` | selected background `#3a3f4b` | syntax-bg `#282c34` | mono-1 `#abb2bf` | mono-1 `#abb2bf` | ui-fg `#9da5b4` | level-1 `#353b45` | hue-2 blue `#61afef` | level-3 `#21252b` |
| Rosé Pine Dawn (extra) | base `#faf4ed` | surface `#fffaf3` | highlight med `#dfdad9` | overlay `#f2e9e1` | text `#575279` | text `#575279` | pine `#286983` | surface `#fffaf3` | pine `#286983` | surface `#fffaf3` |

### 5.3 Contrast of the proposed mappings

Text pairs need 4.5:1. "Accent vs background" is the non-text check that `ThemesTest` makes, 3:1. "Key vs background" has no minimum (see 1.2) and is shown to compare with Light and Dark. Navigation icons: dark icons belong on a light background.

| theme | label / key | label / keyPressed | label / actionKey | label / callout | actionLabel / actionKey | hint / key | onAccent / accent | lowest | accent vs background | key vs background | navigation icons |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Light (existing) | 16.48 | 8.33 | 12.11 | 16.48 | 9.23 | 6.39 | 6.42 | 6.39 | 5.24 | 1.23 | dark |
| Dark (existing) | 9.28 | 5.29 | 7.87 | 7.01 | 7.45 | 6.05 | 9.37 | 5.29 | 9.99 | 1.45 | light |
| Solarized Light | 13.92 | 5.61 | 12.25 | 13.92 | 10.61 | 4.99 | 12.05 | 4.99 | 10.61 | 1.14 | dark |
| Solarized Dark | 12.05 | 4.99 | 13.92 | 12.05 | 5.61 | 4.86 | 4.75 | 4.75 | 4.75 | 1.15 | light |
| Nord | 7.49 | 6.40 | 8.73 | 6.40 | 7.45 | 6.39 | 6.24 | 6.24 | 6.24 | 1.45 | light |
| Dracula | 11.06 | 8.59 | 13.36 | 9.06 | 13.36 | 4.89 | 6.62 | 4.89 | 6.62 | 1.34 | light |
| Gruvbox Light | 10.53 | 5.32 | 6.76 | 10.53 | 6.76 | 5.92 | 5.82 | 5.32 | 4.81 | 1.25 | dark |
| Gruvbox Dark | 8.45 | 4.75 | 9.57 | 6.43 | 7.65 | 5.32 | 8.69 | 4.75 | 9.67 | 1.41 | light |
| Catppuccin Latte | 7.06 | 5.17 | 6.04 | 7.06 | 6.04 | 5.53 | 4.79 | 4.79 | 4.45 | 1.08 | dark |
| Catppuccin Mocha | 8.69 | 4.62 | 11.34 | 6.31 | 9.26 | 5.65 | 8.07 | 4.62 | 9.23 | 1.49 | light |
| Tokyo Night | 9.02 | 5.53 | 10.59 | 6.81 | 8.10 | 6.00 | 6.79 | 5.53 | 7.14 | 1.23 | light |
| Rosé Pine | 11.51 | 5.96 | 12.50 | 7.93 | 12.50 | 4.71 | 8.43 | 4.71 | 8.43 | 1.16 | light |
| One Dark | 5.69 | 4.94 | 6.57 | 5.29 | 6.57 | 4.90 | 6.51 | 4.90 | 6.51 | 1.27 | light |
| Rosé Pine Dawn (extra) | 7.00 | 5.25 | 6.07 | 7.00 | 6.07 | 5.88 | 5.88 | 5.25 | 5.59 | 1.05 | dark |

Against the figures the `Palette` doc comment gives (label 5.2, actionLabel 7.4, hint 6, onAccent 6.4), every proposed mapping falls short somewhere:

| theme | pairs under the doc comment's figure |
|---|---|
| Solarized Light | hint 4.99 |
| Solarized Dark | label on pressed key 4.99; actionLabel 5.61; hint 4.86; onAccent 4.75 |
| Nord | onAccent 6.24 |
| Dracula | hint 4.89 |
| Gruvbox Light | actionLabel 6.76; hint 5.92; onAccent 5.82 |
| Gruvbox Dark | label on pressed key 4.75; hint 5.32 |
| Catppuccin Latte | label on pressed key 5.17; actionLabel 6.04; hint 5.53; onAccent 4.79 |
| Catppuccin Mocha | label on pressed key 4.62; hint 5.65 |
| Tokyo Night | hint 5.998 |
| Rosé Pine | hint 4.71 |
| One Dark | label on pressed key 4.94; actionLabel 6.57; hint 4.90 |
| Rosé Pine Dawn | actionLabel 6.07; hint 5.88; onAccent 5.88 |

### 5.4 Notes per scheme

- **Solarized Light: passes, but no longer looks like Solarized.** Its body text (base00) and emphasis (base01) are under 4.5:1 on its own backgrounds, so the labels have to be base03 and base02, the dark variant's backgrounds. The accent is the harder problem: with a label on it, cyan with base03 is the best accent hue (4.75), but cyan against the background is 2.58, under 3:1; blue reaches 3.00 against the background but no official colour reaches 4.5:1 on blue (4.08 at best). No accent hue passes both, so the proposal uses base02, a near-black teal, as the accent. If a grey-teal accent is not acceptable, drop this theme.
- **Solarized Dark: passes narrowly, with the same loss.** Labels have to be base3 for the pressed key to be base01. Action keys share the background's colour because the scheme has only two dark tones. Blue cannot be the accent (4.08); cyan, green and yellow can (4.68 to 4.75 with base03). An alternative that stays closer to the scheme is label base1 with the pressed key drawn in base03 (5.61), at the cost of the hint and the label being the same colour.
- **Nord: clean.** One change, the hint from nord9 to nord4. Lowest pair 6.24.
- **Dracula: passes; the hint cannot be muted.** Comment `#6272a4`, the scheme's only muted tone, is 1.94 on Selection and 2.51 on Background Light. The proposal uses Purple for the hint (4.89, the least bright colour that passes) and Pink for the accent; Purple as the accent with Cyan (8.52) or Orange (6.92) as the hint also passes. The key takes the spec's own "floating interactive elements" colour.
- **Gruvbox Light and Dark: clean.** One step along the scheme's own scale for the hint in each (dark4 to dark3, light4 to light3); in Light the action label also moves from dark2 to dark1 for margin.
- **Catppuccin Latte: passes with a rearrangement.** Text, the darkest neutral, is 4.39 on surface1, so nothing darker than surface0 can be a fill under a label. That forces background mantle, action key crust, pressed key surface0. Keys are faint against the background (1.08), and onAccent on mauve is 4.79.
- **Catppuccin Mocha: clean, natural mapping unchanged.** Lowest pair 4.62 (label on the pressed key); using surface1 for the pressed key raises it to 6.31 and makes the pressed key and the key preview the same colour.
- **Tokyo Night: clean.** One change, the hint from the interface foreground to the markdown text colour. The neutral names are mine (see 4.2).
- **Rosé Pine: clean, natural mapping unchanged.** Lowest pair 4.71 (hint).
- **One Dark: passes with thin margins, on computed hex values.** The hint moves from mono-2 to the interface text colour, and the key from level-1 to the highlight background to give the hint room (4.90 instead of 4.55). The key fills sit within 10% lightness of each other, so the label on them is 4.94 to 6.57.
- **Rosé Pine Dawn (not in the list asked about): passes.** Subtle (4.23 on a key) and muted (2.87) both fail as the hint, so the hint is pine, the same colour as the accent. With the site's text value `#464261` the label pairs are higher (6.86 to 9.14).
- **Monokai: no mapping.** No official colours could be confirmed and the licence rules it out (4.1).

### 5.5 Reading the results

| verdict | themes |
|---|---|
| Pass with the natural mapping | Catppuccin Mocha, Rosé Pine |
| Pass after changing the hint only | Nord, Gruvbox Dark, Gruvbox Light (plus one action label), Tokyo Night |
| Pass after rearranging fills | Dracula, Catppuccin Latte, One Dark, Rosé Pine Dawn |
| Pass only by leaving the scheme's intended look | Solarized Dark, Solarized Light (no accent hue works) |
| Drop | Monokai |

## 6. What could not be verified

- **Nothing was run on a device or emulator.** Every statement about the navigation bar in section 2 comes from documentation and from AOSP `main`. Behaviour on Android 15 and 16 release builds and on vendor builds may differ, in particular: the default icon appearance of the IME window, the default three-button scrim, and whether the keyboard's view reaches behind the bar before API 35.
- **No IME-specific guidance from developer.android.com** on edge-to-edge, the navigation bar, or theming. None was found.
- **Large text in sp.** No primary source maps WCAG's "18 point or 14 point bold" onto Android's sp or onto Myanmar script.
- **Trademarks.** No register was searched. "No trademark notice found" describes the READMEs and pages cited, nothing more.
- **Whether using a palette's hex values needs the MIT notice at all.** A legal question; the recommendation to include the notices sidesteps it. This is not legal advice.
- **Gruvbox's licence.** Stated in the README only; no licence file or copyright line exists in the repository.
- **Monokai's hex values and the terms of the original 2006 palette.** No official static source found.
- **Rosé Pine Dawn's text colour.** Two published values, `#464261` (site) and `#575279` (palette repository).
- **One Dark's hex values.** Computed from the HSL definitions; they match the commonly quoted ones, but the project itself publishes no hex.
- **Tokyo Night's and One Dark's neutral names.** The projects name these only by the setting they are used for.
- **"Nearest" colour.** Ranked by CIE76 distance in the script, a convenience of mine, not something any source defines.

The script and its output are in the session's scratch directory and are not part of the repository; the formula in section 1 and the tables in section 5 are enough to reproduce them.
