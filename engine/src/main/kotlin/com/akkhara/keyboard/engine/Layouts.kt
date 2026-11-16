package com.akkhara.keyboard.engine

internal typealias Row = List<Key>

/** Turns a space-separated string into a row of character keys. */
internal fun chars(keys: String): Row = keys.split(' ').map { CharKey(it) }

private const val WIDE = 1.5f

private val shiftKey = ActionKey(Action.Shift, WIDE)
private val backspaceKey = ActionKey(Action.Backspace, WIDE)
private val enterKey = ActionKey(Action.Enter, WIDE)

private val latinDigits = chars("1 2 3 4 5 6 7 8 9 0")

/** Gives the key that types [text] these [alternates]. */
private fun Row.withAlternates(text: String, vararg alternates: String): Row =
    map { if (it is CharKey && it.text == text) it.copy(alternates = alternates.toList()) else it }

/** The row under the letters, with the layout's own [comma] and [fullStop] key. */
private fun lettersBottomRow(comma: String, fullStop: CharKey): Row = listOf(
    ActionKey(Action.Symbols, WIDE),
    CharKey(comma),
    ActionKey(Action.Globe),
    ActionKey(Action.Space, width = 4f),
    fullStop,
    enterKey,
)

private val symbolsBottomRow: Row = listOf(
    ActionKey(Action.Letters, WIDE),
    CharKey(","),
    ActionKey(Action.Emoji),
    ActionKey(Action.Space, width = 4f),
    CharKey("."),
    enterKey,
)

/** The keys of one input language: its base layer and its shift layer. */
internal interface Layout {
    val base: List<Row>
    val shift: List<Row>

    /** The layout's name in its own language, shown on the space bar. */
    val name: String

    /** A few of the layout's letters, shown on the key that comes back to it from the symbols layers. */
    val lettersLabel: String

    /** Shown on the key that opens the symbols layers, in the layout's own digits. */
    val symbolsLabel: String

    /** What the layout's comma key types. Held down, that key offers the emoji key. */
    val comma: String
}

internal fun layoutFor(language: InputLanguage): Layout = when (language) {
    InputLanguage.English -> EnglishLayout
    InputLanguage.Myanmar -> MyanmarLayout
}

internal object EnglishLayout : Layout {
    override val name = "English"
    override val lettersLabel = "ABC"
    override val symbolsLabel = "?123"
    override val comma = ","

    private val bottom = lettersBottomRow(
        comma = comma,
        fullStop = CharKey(".", alternates = listOf(",", "?", "!", "'", "\"", ":", ";")),
    )

    override val base: List<Row> = listOf(
        latinDigits,
        chars("q w e r t y u i o p"),
        chars("a s d f g h j k l"),
        listOf(shiftKey) + chars("z x c v b n m") + backspaceKey,
        bottom,
    )

    override val shift: List<Row> = listOf(
        latinDigits,
        chars("Q W E R T Y U I O P"),
        chars("A S D F G H J K L"),
        listOf(shiftKey) + chars("Z X C V B N M") + backspaceKey,
        bottom,
    )
}

/**
 * The Unicode Myanmar layout as Gboard arranges it, so that the keys are where
 * people already look for them on their phone. The top row of the base layer
 * holds letters and whole vowels, each with a Latin digit as its hint; the
 * Myanmar digits are the top row of the shift layer.
 */
internal object MyanmarLayout : Layout {
    override val name = "မြန်မာ"
    override val lettersLabel = "ကခဂ"
    override val symbolsLabel = "?၁၂၃"

    override val comma = "၊"

    private val bottom = lettersBottomRow(
        comma = comma,
        fullStop = CharKey("။", alternates = listOf("၊", "?", "!")),
    )

    /** Each key of the top row offers the Latin digit of its column, and shows it as its hint. */
    private fun Row.withLatinDigitHints(): Row = mapIndexed { column, key ->
        val digit = ((column + 1) % 10).toString()
        (key as CharKey).copy(alternates = listOf(digit), hint = digit)
    }

    override val base: List<Row> = listOf(
        chars("ဈ ဝ ဋ ုံ ော ော် ရ ဂ ဟ ၏").withLatinDigitHints(),
        chars("ဆ တ န မ အ ပ က င သ စ").withAlternates("င", KINZI).withAlternates("သ", "ဩ", "ဪ"),
        chars("ေ ျ ိ ် ါ ့ ြ ု ူ း"),
        listOf(ActionKey(Action.Shift)) + chars("ဖ ထ ခ လ ဘ ည ာ ယ") + ActionKey(Action.Backspace),
        bottom,
    )

    override val shift: List<Row> = listOf(
        chars("၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀").withLatinDigitHints(),
        chars("ဍ ဏ္ဍ ဣ ၎င်း ဤ ၌ ဥ ၍ ဿ ဏ").withAlternates("၎င်း", "၎"),
        chars("ဗ ှ ီ ္ ွ ံ ဲ ဒ ဓ ဏ္ဌ"),
        listOf(ActionKey(Action.Shift)) + chars("ဇ ဌ ဃ ဠ ဎ ဉ ဦ ဧ") + ActionKey(Action.Backspace),
        bottom,
    )
}

/** The two layers of digits and punctuation shared by every layout. */
internal object SymbolsLayout {
    val first: List<Row> = listOf(
        latinDigits,
        chars("@ # $ % & - + ( ) /"),
        chars("* \" ' : ; ! ? _ = ~"),
        listOf(ActionKey(Action.MoreSymbols, WIDE)) + chars("\\ | < > [ ] ^") + backspaceKey,
        symbolsBottomRow,
    )

    val second: List<Row> = listOf(
        chars("` • √ π ÷ × ¶ ∆ ° ¿"),
        chars("£ ¢ € ¥ ₩ ₹ ฿ { } §"),
        chars("© ® ™ ✓ « » ‹ › … ¡"),
        listOf(ActionKey(Action.Symbols, WIDE)) + chars("‘ ’ “ ” — – ·") + backspaceKey,
        symbolsBottomRow,
    )
}

/** The keys of the emoji panel's bar, either side of its category tabs. */
internal object EmojiPanelLayout {
    val bar: List<Row> = listOf(listOf(ActionKey(Action.Letters, WIDE), backspaceKey))
}
