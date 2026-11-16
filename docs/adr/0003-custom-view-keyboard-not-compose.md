# The keyboard is a custom Canvas View, not Jetpack Compose

The keyboard surface inside the input method is drawn by a single custom `View`; Compose is used only for the settings and setup screens. An `InputMethodService` is not a lifecycle or saved-state owner, so Compose inside it needs hand-built lifecycle plumbing, and a single View gives direct control over touch handling and redraw cost on every key press. Layout and key-handling logic stay in plain Kotlin with no Android types so they can be unit tested on the JVM.
