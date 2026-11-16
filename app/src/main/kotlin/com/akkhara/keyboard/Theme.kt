package com.akkhara.keyboard

/**
 * A look the user can pick for the keyboard: one fixed [palette], light or
 * dark, in the classic or the trending [group]. See GLOSSARY.md, "Theme". The
 * list is built into the app, in the order the picker shows it; ThemesTest
 * holds every theme to the palette's contrast floors.
 *
 * The trending themes' colours come from docs/research/trending-film-themes.md.
 * Their names are Akkhara's own: never a title, character or studio.
 */
enum class Theme(val isDark: Boolean, val palette: Palette, val group: Group = Group.Classic) {
    /** Grey and white with a green accent; what following the system draws by day. */
    Light(
        isDark = false,
        Palette(
            background = 0xFFE6E9E3.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFB4BAB2.toInt(),
            actionKey = 0xFFD0E2CC.toInt(),
            label = 0xFF1F1F1F.toInt(),
            actionLabel = 0xFF1B3A20.toInt(),
            hint = 0xFF5C605D.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF2E6B34.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
    ),

    /** Warm beige. */
    Sand(
        isDark = false,
        Palette(
            background = 0xFFEDE4D3.toInt(),
            key = 0xFFFFFBF2.toInt(),
            keyPressed = 0xFFC9BCA3.toInt(),
            actionKey = 0xFFE0D0B0.toInt(),
            label = 0xFF2A2418.toInt(),
            actionLabel = 0xFF4A3A14.toInt(),
            hint = 0xFF6B604C.toInt(),
            callout = 0xFFFFFBF2.toInt(),
            accent = 0xFF8A5A00.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
    ),

    /** Soft pink. */
    Rose(
        isDark = false,
        Palette(
            background = 0xFFF4DCE0.toInt(),
            key = 0xFFFFF7F8.toInt(),
            keyPressed = 0xFFD9AEB6.toInt(),
            actionKey = 0xFFF0C4CC.toInt(),
            label = 0xFF2B1A1D.toInt(),
            actionLabel = 0xFF5A1A2A.toInt(),
            hint = 0xFF6E5257.toInt(),
            callout = 0xFFFFF7F8.toInt(),
            accent = 0xFFA8324E.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
    ),

    /** Pale blue. */
    Sky(
        isDark = false,
        Palette(
            background = 0xFFD9E8F5.toInt(),
            key = 0xFFF8FCFF.toInt(),
            keyPressed = 0xFFA9C2D9.toInt(),
            actionKey = 0xFFC2DAF0.toInt(),
            label = 0xFF16212B.toInt(),
            actionLabel = 0xFF0F3554.toInt(),
            hint = 0xFF4F6172.toInt(),
            callout = 0xFFF8FCFF.toInt(),
            accent = 0xFF1F5F99.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
    ),

    /** Pale purple. */
    Lavender(
        isDark = false,
        Palette(
            background = 0xFFE6DFF5.toInt(),
            key = 0xFFFCFAFF.toInt(),
            keyPressed = 0xFFBBAEDB.toInt(),
            actionKey = 0xFFD5C9F0.toInt(),
            label = 0xFF1F1A2B.toInt(),
            actionLabel = 0xFF34206B.toInt(),
            hint = 0xFF5D5670.toInt(),
            callout = 0xFFFCFAFF.toInt(),
            accent = 0xFF5B3FA8.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
    ),

    /** Near-black grey with a pale green accent; what following the system draws in dark mode. */
    Dark(
        isDark = true,
        Palette(
            background = 0xFF1F1F1F.toInt(),
            key = 0xFF3A3A3A.toInt(),
            keyPressed = 0xFF5E5E5E.toInt(),
            actionKey = 0xFF37493A.toInt(),
            label = 0xFFE8E8E8.toInt(),
            actionLabel = 0xFFD3E8CF.toInt(),
            hint = 0xFFBDBDBD.toInt(),
            callout = 0xFF4C4C4C.toInt(),
            accent = 0xFFA5D6A0.toInt(),
            onAccent = 0xFF0E2A12.toInt(),
        ),
    ),

    /** Deep navy. */
    Midnight(
        isDark = true,
        Palette(
            background = 0xFF0F1626.toInt(),
            key = 0xFF1F2A44.toInt(),
            keyPressed = 0xFF3A4A70.toInt(),
            actionKey = 0xFF1A3358.toInt(),
            label = 0xFFE6ECF8.toInt(),
            actionLabel = 0xFFCFE0FF.toInt(),
            hint = 0xFFB0BCD6.toInt(),
            callout = 0xFF2C3A5C.toInt(),
            accent = 0xFF8AB4F8.toInt(),
            onAccent = 0xFF0A1F3D.toInt(),
        ),
    ),

    /** Deep green. */
    Forest(
        isDark = true,
        Palette(
            background = 0xFF0F1F17.toInt(),
            key = 0xFF1E3328.toInt(),
            keyPressed = 0xFF3A5546.toInt(),
            actionKey = 0xFF22492F.toInt(),
            label = 0xFFE4F0E8.toInt(),
            actionLabel = 0xFFCDEBD6.toInt(),
            hint = 0xFFB0C7B8.toInt(),
            callout = 0xFF2B4536.toInt(),
            accent = 0xFF7FD99A.toInt(),
            onAccent = 0xFF07301A.toInt(),
        ),
    ),

    /** Dark purple. */
    Plum(
        isDark = true,
        Palette(
            background = 0xFF1E1424.toInt(),
            key = 0xFF33243D.toInt(),
            keyPressed = 0xFF574068.toInt(),
            actionKey = 0xFF472A5A.toInt(),
            label = 0xFFEFE6F5.toInt(),
            actionLabel = 0xFFE7D2F7.toInt(),
            hint = 0xFFC6B6D1.toInt(),
            callout = 0xFF44314F.toInt(),
            accent = 0xFFD6A4F5.toInt(),
            onAccent = 0xFF2E0F45.toInt(),
        ),
    ),

    /** Pure black with white labels: the strongest contrast, and the least light from an OLED screen. */
    Black(
        isDark = true,
        Palette(
            background = 0xFF000000.toInt(),
            key = 0xFF1C1C1C.toInt(),
            keyPressed = 0xFF484848.toInt(),
            actionKey = 0xFF2A2A2A.toInt(),
            label = 0xFFFFFFFF.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFFC8C8C8.toInt(),
            callout = 0xFF333333.toInt(),
            accent = 0xFFFFFFFF.toInt(),
            onAccent = 0xFF000000.toInt(),
        ),
    ),

    /** Olive moss with an olive-gold accent. */
    Moss(
        isDark = false,
        Palette(
            background = 0xFFD9DCAE.toInt(),
            key = 0xFFF7F8EC.toInt(),
            keyPressed = 0xFFBDC28A.toInt(),
            actionKey = 0xFFCCCF94.toInt(),
            label = 0xFF22240F.toInt(),
            actionLabel = 0xFF3A3D0C.toInt(),
            hint = 0xFF56583E.toInt(),
            callout = 0xFFF7F8EC.toInt(),
            accent = 0xFF5C6410.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Ash slate with an electric-cyan accent. */
    Cinder(
        isDark = true,
        Palette(
            background = 0xFF19232B.toInt(),
            key = 0xFF2C373F.toInt(),
            keyPressed = 0xFF4E565E.toInt(),
            actionKey = 0xFF1F3A48.toInt(),
            label = 0xFFE9ECEF.toInt(),
            actionLabel = 0xFFCDEFFA.toInt(),
            hint = 0xFFB6BCC2.toInt(),
            callout = 0xFF3E454C.toInt(),
            accent = 0xFF45CFF5.toInt(),
            onAccent = 0xFF04262F.toInt(),
        ),
        group = Group.Trending,
    ),
    ;

    /** Where a theme sits in the picker. See GLOSSARY.md, "Classic theme" and "Trending theme". */
    enum class Group { Classic, Trending }

    companion object {
        /**
         * The theme the keyboard is drawn in: the one the user [chosen], or,
         * while following the system, Light or Dark from the phone's dark mode.
         */
        fun shown(chosen: Theme?, systemIsDark: Boolean): Theme = chosen ?: if (systemIsDark) Dark else Light
    }
}
