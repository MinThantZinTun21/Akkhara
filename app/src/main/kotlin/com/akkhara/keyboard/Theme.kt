package com.akkhara.keyboard

import androidx.annotation.StringRes

/**
 * A look the user can pick for the keyboard: its name, one fixed [palette],
 * light or dark, in the classic, trending or designed [group], with its own [keyStyle]. See GLOSSARY.md, "Theme". The
 * list is built into the app, in the order the picker shows it; ThemesTest
 * holds every theme to the palette's contrast floors.
 *
 * The trending themes' colours come from docs/research/trending-film-themes.md,
 * and the designed themes' from docs/research/designed-film-themes.md. Their
 * names are Akkhara's own: never a title, character, studio, team or hero.
 */
enum class Theme(
    @StringRes val nameRes: Int,
    val isDark: Boolean,
    val palette: Palette,
    val group: Group,
    /** The shape this theme draws its keys in, unless the user picks another. See GLOSSARY.md, "Key style". */
    val keyStyle: KeyStyle = KeyStyle.Rounded,
) {
    /** Grey and white with a green accent; what following the system draws in light mode. */
    Light(
        nameRes = R.string.theme_light,
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
        group = Group.Classic,
    ),

    /** Warm beige. */
    Sand(
        nameRes = R.string.theme_sand,
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
        group = Group.Classic,
    ),

    /** Soft pink. */
    Rose(
        nameRes = R.string.theme_rose,
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
        group = Group.Classic,
    ),

    /** Pale blue. */
    Sky(
        nameRes = R.string.theme_sky,
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
        group = Group.Classic,
    ),

    /** Pale purple. */
    Lavender(
        nameRes = R.string.theme_lavender,
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
        group = Group.Classic,
    ),

    /** Near-black grey with a pale green accent; what following the system draws in dark mode. */
    Dark(
        nameRes = R.string.theme_dark,
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
        group = Group.Classic,
    ),

    /** Deep navy. */
    Midnight(
        nameRes = R.string.theme_midnight,
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
        group = Group.Classic,
    ),

    /** Deep green. */
    Forest(
        nameRes = R.string.theme_forest,
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
        group = Group.Classic,
    ),

    /** Dark purple. */
    Plum(
        nameRes = R.string.theme_plum,
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
        group = Group.Classic,
    ),

    /** Pure black with white labels: the strongest contrast, and the least light from an OLED screen. */
    Black(
        nameRes = R.string.theme_black,
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
        group = Group.Classic,
    ),

    /** Olive moss with an olive-gold accent. */
    Moss(
        nameRes = R.string.theme_moss,
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

    /** Pale mint with pastel-pink action keys and a magenta accent. */
    Prism(
        nameRes = R.string.theme_prism,
        isDark = false,
        Palette(
            background = 0xFFDDF2EA.toInt(),
            key = 0xFFFBFFFD.toInt(),
            keyPressed = 0xFFA9D6C8.toInt(),
            actionKey = 0xFFF3D3EC.toInt(),
            label = 0xFF1C1A2B.toInt(),
            actionLabel = 0xFF6A0F4E.toInt(),
            hint = 0xFF545066.toInt(),
            callout = 0xFFFBFFFD.toInt(),
            accent = 0xFFA3166A.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Butter yellow with denim-blue action keys and accent. */
    Marquee(
        nameRes = R.string.theme_marquee,
        isDark = false,
        Palette(
            background = 0xFFF2DE8A.toInt(),
            key = 0xFFFFFBE6.toInt(),
            keyPressed = 0xFFDDC56A.toInt(),
            actionKey = 0xFFC9D6EE.toInt(),
            label = 0xFF2A2410.toInt(),
            actionLabel = 0xFF1D3566.toInt(),
            hint = 0xFF615A3A.toInt(),
            callout = 0xFFFFFBE6.toInt(),
            accent = 0xFF2C4A8C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Apricot saffron with a deep vermilion accent. */
    Saffron(
        nameRes = R.string.theme_saffron,
        isDark = false,
        Palette(
            background = 0xFFF6D4A2.toInt(),
            key = 0xFFFFF8EE.toInt(),
            keyPressed = 0xFFE3B57A.toInt(),
            actionKey = 0xFFF0BE84.toInt(),
            label = 0xFF2B1A0C.toInt(),
            actionLabel = 0xFF5A220C.toInt(),
            hint = 0xFF6A5038.toInt(),
            callout = 0xFFFFF8EE.toInt(),
            accent = 0xFF9E2B12.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Pale jade with a rouge-red accent. */
    Jade(
        nameRes = R.string.theme_jade,
        isDark = false,
        Palette(
            background = 0xFFC4DFD0.toInt(),
            key = 0xFFF3FAF6.toInt(),
            keyPressed = 0xFF9CC4AF.toInt(),
            actionKey = 0xFFAED3BF.toInt(),
            label = 0xFF132A20.toInt(),
            actionLabel = 0xFF0F3F2A.toInt(),
            hint = 0xFF455E52.toInt(),
            callout = 0xFFF3FAF6.toInt(),
            accent = 0xFFA3243B.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Sepia paper with tropical-teal action keys and accent. */
    Sonata(
        nameRes = R.string.theme_sonata,
        isDark = false,
        Palette(
            background = 0xFFD6BF9C.toInt(),
            key = 0xFFFBF4E8.toInt(),
            keyPressed = 0xFFBFA47A.toInt(),
            actionKey = 0xFFB9CFC8.toInt(),
            label = 0xFF2B1F12.toInt(),
            actionLabel = 0xFF163F42.toInt(),
            hint = 0xFF5A4C35.toInt(),
            callout = 0xFFFBF4E8.toInt(),
            accent = 0xFF1F5A5E.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Spotlight peach with a curtain-violet accent. */
    Encore(
        nameRes = R.string.theme_encore,
        isDark = false,
        Palette(
            background = 0xFFFAD0BD.toInt(),
            key = 0xFFFFF6F1.toInt(),
            keyPressed = 0xFFE8AE95.toInt(),
            actionKey = 0xFFF2BBA6.toInt(),
            label = 0xFF2C1A16.toInt(),
            actionLabel = 0xFF4C1F5E.toInt(),
            hint = 0xFF6A4F48.toInt(),
            callout = 0xFFFFF6F1.toInt(),
            accent = 0xFF5E2A8A.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** White keys, toy-blue action keys and a toy-red enter key. */
    Crayon(
        nameRes = R.string.theme_crayon,
        isDark = false,
        Palette(
            background = 0xFFF2F2F4.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFC5C8D0.toInt(),
            actionKey = 0xFF2B5FC1.toInt(),
            label = 0xFF1C1C1C.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF565A60.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFFC62828.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Sage stone with earthy action keys and a sky-blue accent. */
    Wander(
        nameRes = R.string.theme_wander,
        isDark = false,
        Palette(
            background = 0xFFCBCFC0.toInt(),
            key = 0xFFF5F6F0.toInt(),
            keyPressed = 0xFFA9AE9A.toInt(),
            actionKey = 0xFFDCCFB0.toInt(),
            label = 0xFF1E211A.toInt(),
            actionLabel = 0xFF4A3A14.toInt(),
            hint = 0xFF50554A.toInt(),
            callout = 0xFFF5F6F0.toInt(),
            accent = 0xFF345E8C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Clear water blue with a deep pond-green accent. */
    Pond(
        nameRes = R.string.theme_pond,
        isDark = false,
        Palette(
            background = 0xFFBEE3E6.toInt(),
            key = 0xFFF2FBFB.toInt(),
            keyPressed = 0xFF93C8CD.toInt(),
            actionKey = 0xFFA6D6D9.toInt(),
            label = 0xFF112A2D.toInt(),
            actionLabel = 0xFF0B4048.toInt(),
            hint = 0xFF3F5D60.toInt(),
            callout = 0xFFF2FBFB.toInt(),
            accent = 0xFF0E6A5C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Ash slate with an electric-cyan accent. */
    Cinder(
        nameRes = R.string.theme_cinder,
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

    /** Sooty sepia night with gas-lamp amber. */
    Lamplight(
        nameRes = R.string.theme_lamplight,
        isDark = true,
        Palette(
            background = 0xFF221B0F.toInt(),
            key = 0xFF362C1B.toInt(),
            keyPressed = 0xFF584A30.toInt(),
            actionKey = 0xFF45381F.toInt(),
            label = 0xFFF3E9D8.toInt(),
            actionLabel = 0xFFF5DDB0.toInt(),
            hint = 0xFFCBBDA4.toInt(),
            callout = 0xFF463A25.toInt(),
            accent = 0xFFE9A93A.toInt(),
            onAccent = 0xFF2A1A00.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Near-black magenta with a hot-pink accent. */
    Neon(
        nameRes = R.string.theme_neon,
        isDark = true,
        Palette(
            background = 0xFF140812.toInt(),
            key = 0xFF2C1328.toInt(),
            keyPressed = 0xFF5A2D52.toInt(),
            actionKey = 0xFF4C1240.toInt(),
            label = 0xFFF9E9F4.toInt(),
            actionLabel = 0xFFFFD3EC.toInt(),
            hint = 0xFFD8BCD0.toInt(),
            callout = 0xFF3E1D38.toInt(),
            accent = 0xFFFF4FA8.toInt(),
            onAccent = 0xFF3A0020.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Moonlit deep sea with a moon-cream accent. */
    Lagoon(
        nameRes = R.string.theme_lagoon,
        isDark = true,
        Palette(
            background = 0xFF0A2B33.toInt(),
            key = 0xFF15404A.toInt(),
            keyPressed = 0xFF2B5E6A.toInt(),
            actionKey = 0xFF12494F.toInt(),
            label = 0xFFE3F6F7.toInt(),
            actionLabel = 0xFFCFF2EE.toInt(),
            hint = 0xFFACCBCE.toInt(),
            callout = 0xFF1F5059.toInt(),
            accent = 0xFFF4E3A3.toInt(),
            onAccent = 0xFF2A2300.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Deep indigo space with star gold. */
    Comet(
        nameRes = R.string.theme_comet,
        isDark = true,
        Palette(
            background = 0xFF18154A.toInt(),
            key = 0xFF2A2666.toInt(),
            keyPressed = 0xFF4A4590.toInt(),
            actionKey = 0xFF332A72.toInt(),
            label = 0xFFEEEBFF.toInt(),
            actionLabel = 0xFFFFE9A8.toInt(),
            hint = 0xFFC3BEE6.toInt(),
            callout = 0xFF3A3580.toInt(),
            accent = 0xFFFFCC33.toInt(),
            onAccent = 0xFF2B1E00.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Wet clay and terracotta. */
    Umber(
        nameRes = R.string.theme_umber,
        isDark = true,
        Palette(
            background = 0xFF341C18.toInt(),
            key = 0xFF4D2F28.toInt(),
            keyPressed = 0xFF70493E.toInt(),
            actionKey = 0xFF5C2F24.toInt(),
            label = 0xFFF4E4DA.toInt(),
            actionLabel = 0xFFF8D2BC.toInt(),
            hint = 0xFFD0B9AB.toInt(),
            callout = 0xFF5C3C33.toInt(),
            accent = 0xFFE38A57.toInt(),
            onAccent = 0xFF2B1103.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Witchy violet with teal action keys and a magenta accent. */
    Potion(
        nameRes = R.string.theme_potion,
        isDark = true,
        Palette(
            background = 0xFF2C1236.toInt(),
            key = 0xFF42214F.toInt(),
            keyPressed = 0xFF653A75.toInt(),
            actionKey = 0xFF1F3B3A.toInt(),
            label = 0xFFF7ECFA.toInt(),
            actionLabel = 0xFFC9F2EC.toInt(),
            hint = 0xFFD6C0DD.toInt(),
            callout = 0xFF54305F.toInt(),
            accent = 0xFFE25AC8.toInt(),
            onAccent = 0xFF2A0326.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Night olive with firefly-lime light and blood-red action keys. */
    Lumen(
        nameRes = R.string.theme_lumen,
        isDark = true,
        Palette(
            background = 0xFF17170D.toInt(),
            key = 0xFF2A2A1C.toInt(),
            keyPressed = 0xFF484833.toInt(),
            actionKey = 0xFF3A1F22.toInt(),
            label = 0xFFEFF1E2.toInt(),
            actionLabel = 0xFFF6D6D6.toInt(),
            hint = 0xFFC3C5AF.toInt(),
            callout = 0xFF35352A.toInt(),
            accent = 0xFFCBEA5E.toInt(),
            onAccent = 0xFF1E2A00.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Dark oxblood with a warning-red accent. */
    Nightshift(
        nameRes = R.string.theme_nightshift,
        isDark = true,
        Palette(
            background = 0xFF241015.toInt(),
            key = 0xFF3A1E25.toInt(),
            keyPressed = 0xFF63404A.toInt(),
            actionKey = 0xFF4A1A24.toInt(),
            label = 0xFFF7E9EC.toInt(),
            actionLabel = 0xFFFFD0D6.toInt(),
            hint = 0xFFD1B7BD.toInt(),
            callout = 0xFF472A32.toInt(),
            accent = 0xFFF0585E.toInt(),
            onAccent = 0xFF2B0003.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Deep blue with an energy-orange accent. */
    Blaze(
        nameRes = R.string.theme_blaze,
        isDark = true,
        Palette(
            background = 0xFF101C38.toInt(),
            key = 0xFF1E2C52.toInt(),
            keyPressed = 0xFF3A4C7C.toInt(),
            actionKey = 0xFF3A2414.toInt(),
            label = 0xFFEAF0FF.toInt(),
            actionLabel = 0xFFFFD9B8.toInt(),
            hint = 0xFFB8C4E0.toInt(),
            callout = 0xFF2A3A66.toInt(),
            accent = 0xFFFF8A2A.toInt(),
            onAccent = 0xFF2B1000.toInt(),
        ),
        group = Group.Trending,
    ),

    /** Ivory and ink with a deep wine accent. */
    Atelier(
        nameRes = R.string.theme_atelier,
        isDark = false,
        Palette(
            background = 0xFFF4EFE6.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE4DCCF.toInt(),
            actionKey = 0xFF6E1F2E.toInt(),
            label = 0xFF1A1717.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF5E5650.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF8A2436.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Square,
    ),

    /** Midnight violet with a slime-green accent. */
    Gasp(
        nameRes = R.string.theme_gasp,
        isDark = true,
        Palette(
            background = 0xFF120E1A.toInt(),
            key = 0xFF241D30.toInt(),
            keyPressed = 0xFF3A2F4C.toInt(),
            actionKey = 0xFF9BE34A.toInt(),
            label = 0xFFF2EEF8.toInt(),
            actionLabel = 0xFF101208.toInt(),
            hint = 0xFFB3A8C4.toInt(),
            callout = 0xFF2E2540.toInt(),
            accent = 0xFFB6F25A.toInt(),
            onAccent = 0xFF141A06.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Raised,
    ),

    /** Night asphalt with a rally-orange accent. */
    Rally(
        nameRes = R.string.theme_rally,
        isDark = true,
        Palette(
            background = 0xFF16181B.toInt(),
            key = 0xFF1E2124.toInt(),
            keyPressed = 0xFF2E3236.toInt(),
            actionKey = 0xFFF28C28.toInt(),
            label = 0xFFF5F1EA.toInt(),
            actionLabel = 0xFF1A1208.toInt(),
            hint = 0xFFB9B2A6.toInt(),
            callout = 0xFF2A2D31.toInt(),
            accent = 0xFFFFA640.toInt(),
            onAccent = 0xFF1E1406.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Borderless,
    ),

    /** Desert sky fading to sand, with terracotta keys. */
    Mesa(
        nameRes = R.string.theme_mesa,
        isDark = false,
        Palette(
            background = 0xFFBFE0F2.toInt(),
            backgroundBottom = 0xFFF2D3A8.toInt(),
            key = 0xFFFFF8EE.toInt(),
            keyPressed = 0xFFF1DFC6.toInt(),
            actionKey = 0xFFB4471E.toInt(),
            label = 0xFF2B1D14.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF6E5544.toInt(),
            callout = 0xFFFFFBF5.toInt(),
            accent = 0xFFA5401A.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Rounded,
    ),

    /** Wool cream on wet-field green. */
    Pasture(
        nameRes = R.string.theme_pasture,
        isDark = false,
        Palette(
            background = 0xFFE3EBD9.toInt(),
            key = 0xFFFAF8F1.toInt(),
            keyPressed = 0xFFE6E1D2.toInt(),
            actionKey = 0xFF3F6B3A.toInt(),
            label = 0xFF1F2A1C.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF5A6455.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF3B6A35.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Pill,
    ),

    /** Warm cream with cranberry and pumpkin. */
    Hearth(
        nameRes = R.string.theme_hearth,
        isDark = false,
        Palette(
            background = 0xFFFBF1E4.toInt(),
            key = 0xFFF6E6D2.toInt(),
            keyPressed = 0xFFEBD3B6.toInt(),
            actionKey = 0xFF9C3B1B.toInt(),
            label = 0xFF2E1A10.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF6F5240.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFFB0441C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Outline,
    ),

    /** Swamp-water teal under glass keys, with a lantern-orange accent. */
    Bayou(
        nameRes = R.string.theme_bayou,
        isDark = true,
        Palette(
            background = 0xFF0B3437.toInt(),
            backgroundBottom = 0xFF062124.toInt(),
            key = 0xFF355054.toInt(),
            keyPressed = 0xFF4B6467.toInt(),
            actionKey = 0xFFF2A541.toInt(),
            label = 0xFFF4FAF7.toInt(),
            actionLabel = 0xFF2A1604.toInt(),
            hint = 0xFFD0E6E2.toInt(),
            callout = 0xFF1C4E52.toInt(),
            accent = 0xFFF7B24E.toInt(),
            onAccent = 0xFF2A1604.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Glass,
    ),

    /** Fire fading into ice, with ember action keys. */
    Kiln(
        nameRes = R.string.theme_kiln,
        isDark = true,
        Palette(
            background = 0xFF5A1A0A.toInt(),
            backgroundBottom = 0xFF0B2233.toInt(),
            key = 0xFF3A1F2A.toInt(),
            keyPressed = 0xFF55303E.toInt(),
            actionKey = 0xFFE8452C.toInt(),
            label = 0xFFFFF1E8.toInt(),
            actionLabel = 0xFF1A0503.toInt(),
            hint = 0xFFE0BFC0.toInt(),
            callout = 0xFF2A2440.toInt(),
            accent = 0xFF8FD3E8.toInt(),
            onAccent = 0xFF06192A.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Raised,
    ),

    /** Lacquered dark wood with wisteria and lantern amber. */
    Lacquer(
        nameRes = R.string.theme_lacquer,
        isDark = true,
        Palette(
            background = 0xFF1A1012.toInt(),
            key = 0xFF2B1C1E.toInt(),
            keyPressed = 0xFF43302F.toInt(),
            actionKey = 0xFFB08BD6.toInt(),
            label = 0xFFF6EBDD.toInt(),
            actionLabel = 0xFF1E1028.toInt(),
            hint = 0xFFC9B49E.toInt(),
            callout = 0xFF352426.toInt(),
            accent = 0xFFE0A050.toInt(),
            onAccent = 0xFF241405.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Square,
    ),

    /** Deep violet space under glass keys, with a glowing pink accent. */
    Orbit(
        nameRes = R.string.theme_orbit,
        isDark = true,
        Palette(
            background = 0xFF2A1458.toInt(),
            backgroundBottom = 0xFF0D0B2A.toInt(),
            key = 0xFF453B63.toInt(),
            keyPressed = 0xFF61587A.toInt(),
            actionKey = 0xFF6FE3D2.toInt(),
            label = 0xFFF6F0FF.toInt(),
            actionLabel = 0xFF0B2A27.toInt(),
            hint = 0xFFD2C6F0.toInt(),
            callout = 0xFF3A2470.toInt(),
            accent = 0xFFFFB6E1.toInt(),
            onAccent = 0xFF3A0A2A.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Glass,
    ),

    /** Night-purple candy with pink and mint. */
    Sherbet(
        nameRes = R.string.theme_sherbet,
        isDark = true,
        Palette(
            background = 0xFF140B24.toInt(),
            key = 0xFF2A1A44.toInt(),
            keyPressed = 0xFF41296A.toInt(),
            actionKey = 0xFFFF5FA8.toInt(),
            label = 0xFFFFF5FB.toInt(),
            actionLabel = 0xFF2A0618.toInt(),
            hint = 0xFFC9B8EA.toInt(),
            callout = 0xFF33204F.toInt(),
            accent = 0xFF3CF0C8.toInt(),
            onAccent = 0xFF04241D.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Pill,
    ),

    /** Rice paper and ink with a cinnabar accent. */
    Scroll(
        nameRes = R.string.theme_scroll,
        isDark = false,
        Palette(
            background = 0xFFF4F1E8.toInt(),
            backgroundBottom = 0xFFE6E3D9.toInt(),
            key = 0xFFEDEAE0.toInt(),
            keyPressed = 0xFFDAD6CA.toInt(),
            actionKey = 0xFF2E2F33.toInt(),
            label = 0xFF1D1E21.toInt(),
            actionLabel = 0xFFF4F1E8.toInt(),
            hint = 0xFF5C5E60.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFFB23A2A.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Borderless,
    ),

    /** Night-heist navy with getaway gold. */
    Caper(
        nameRes = R.string.theme_caper,
        isDark = true,
        Palette(
            background = 0xFF101A33.toInt(),
            backgroundBottom = 0xFF070C1C.toInt(),
            key = 0xFF16213D.toInt(),
            keyPressed = 0xFF24335A.toInt(),
            actionKey = 0xFFE8B84A.toInt(),
            label = 0xFFF3EEDF.toInt(),
            actionLabel = 0xFF1F1606.toInt(),
            hint = 0xFFB8BED2.toInt(),
            callout = 0xFF1D2A4C.toInt(),
            accent = 0xFFF0C35A.toInt(),
            onAccent = 0xFF241A04.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Outline,
    ),

    /** Brick dusk over steel, with an apricot sunrise accent. */
    Borough(
        nameRes = R.string.theme_borough,
        isDark = true,
        Palette(
            background = 0xFF4A2E2A.toInt(),
            backgroundBottom = 0xFF181A1F.toInt(),
            key = 0xFF2C2B30.toInt(),
            keyPressed = 0xFF45403F.toInt(),
            actionKey = 0xFFF4A56A.toInt(),
            label = 0xFFF8F1E9.toInt(),
            actionLabel = 0xFF2A1308.toInt(),
            hint = 0xFFC8BDB4.toInt(),
            callout = 0xFF363238.toInt(),
            accent = 0xFFFFB37A.toInt(),
            onAccent = 0xFF2A1308.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Rounded,
    ),

    /** Magic-hour browns and gold. */
    Sepia(
        nameRes = R.string.theme_sepia,
        isDark = true,
        Palette(
            background = 0xFF1E1A14.toInt(),
            key = 0xFF2F2A22.toInt(),
            keyPressed = 0xFF463E31.toInt(),
            actionKey = 0xFFD9A441.toInt(),
            label = 0xFFF3EADA.toInt(),
            actionLabel = 0xFF22180A.toInt(),
            hint = 0xFFC2B6A0.toInt(),
            callout = 0xFF3A3428.toInt(),
            accent = 0xFFE2B455.toInt(),
            onAccent = 0xFF241906.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Raised,
    ),

    /** Clear sky under glass keys, with a sea-teal accent. */
    Zenith(
        nameRes = R.string.theme_zenith,
        isDark = false,
        Palette(
            background = 0xFF8FD0F5.toInt(),
            backgroundBottom = 0xFFE4F4FD.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFD1ECFB.toInt(),
            actionKey = 0xFF14507A.toInt(),
            label = 0xFF0D2238.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF2F4A60.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF0A6A60.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Glass,
    ),

    /** Mid-century pale blue and cream, with teal and orange. */
    Atomic(
        nameRes = R.string.theme_atomic,
        isDark = false,
        Palette(
            background = 0xFFD8E9EE.toInt(),
            key = 0xFFF7F3E8.toInt(),
            keyPressed = 0xFFE5DED0.toInt(),
            actionKey = 0xFF2E7D74.toInt(),
            label = 0xFF1E2A30.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF4E5B60.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFFB84F1B.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Pill,
    ),

    /** Desaturated slate with a muted sand accent. */
    Overcast(
        nameRes = R.string.theme_overcast,
        isDark = true,
        Palette(
            background = 0xFF1F2224.toInt(),
            key = 0xFF26292C.toInt(),
            keyPressed = 0xFF35393D.toInt(),
            actionKey = 0xFF8C9AA0.toInt(),
            label = 0xFFE8E6E1.toInt(),
            actionLabel = 0xFF15181A.toInt(),
            hint = 0xFFA9ADAE.toInt(),
            callout = 0xFF303438.toInt(),
            accent = 0xFFC9B79A.toInt(),
            onAccent = 0xFF1B1712.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Borderless,
    ),

    /** Dusty rust fading to dark, with ochre. */
    Sirocco(
        nameRes = R.string.theme_sirocco,
        isDark = true,
        Palette(
            background = 0xFF3B2418.toInt(),
            backgroundBottom = 0xFF1A1412.toInt(),
            key = 0xFF2E221C.toInt(),
            keyPressed = 0xFF48362C.toInt(),
            actionKey = 0xFFD9824A.toInt(),
            label = 0xFFF6E9DC.toInt(),
            actionLabel = 0xFF22100A.toInt(),
            hint = 0xFFCDB5A2.toInt(),
            callout = 0xFF3A2C24.toInt(),
            accent = 0xFFE89A5E.toInt(),
            onAccent = 0xFF2A1408.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Square,
    ),

    /** Blossom pink with olive action keys. */
    Petal(
        nameRes = R.string.theme_petal,
        isDark = false,
        Palette(
            background = 0xFFFBEFF1.toInt(),
            key = 0xFFF8E4E8.toInt(),
            keyPressed = 0xFFEFCFD6.toInt(),
            actionKey = 0xFF4A4A2E.toInt(),
            label = 0xFF2A1E22.toInt(),
            actionLabel = 0xFFFFFFFF.toInt(),
            hint = 0xFF6A545B.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFFB2476A.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        ),
        group = Group.Designed,
        keyStyle = KeyStyle.Outline,
    ),
    ;

    /** Where a theme sits in the picker. See GLOSSARY.md, "Classic theme" and "Trending theme". */
    enum class Group(@StringRes val nameRes: Int) {
        Classic(R.string.theme_group_classic),
        Trending(R.string.theme_group_trending),
        Designed(R.string.theme_group_designed),
    }

    companion object {
        /**
         * The theme the keyboard is drawn in: the one the user [chosen], or,
         * while following the system, Light or Dark from the phone's dark mode.
         */
        fun shown(chosen: Theme?, systemIsDark: Boolean): Theme = chosen ?: if (systemIsDark) Dark else Light
    }
}
