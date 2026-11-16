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
    ActionKey(Action.Globe),
    CharKey(comma),
    ActionKey(Action.Space, width = 4f),
    fullStop,
    enterKey,
)

private val symbolsBottomRow: Row = listOf(
    ActionKey(Action.Letters, WIDE),
    CharKey(","),
    ActionKey(Action.Space, width = 5f),
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
}

internal fun layoutFor(language: InputLanguage): Layout = when (language) {
    InputLanguage.English -> EnglishLayout
    InputLanguage.Myanmar -> MyanmarLayout
}

internal object EnglishLayout : Layout {
    override val name = "English"
    override val lettersLabel = "ABC"

    private val bottom = lettersBottomRow(
        comma = ",",
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
 * The standard Unicode Myanmar layout (Myanmar3), with the same key positions
 * as other phone keyboards. The standard layout has more keys than fit in ten
 * columns, so the letters left over sit on the shift layer's number row, with
 * ရ in its standard place above ၇.
 */
internal object MyanmarLayout : Layout {
    override val name = "မြန်မာ"
    override val lettersLabel = "ကခဂ"

    private val bottom = lettersBottomRow(
        comma = "၊",
        fullStop = CharKey("။", alternates = listOf("၊", "?", "!")),
    )

    /** Each Myanmar digit offers its Latin digit. */
    private val digits: Row = "၁၂၃၄၅၆၇၈၉၀".map { digit ->
        CharKey(digit.toString(), alternates = listOf(((digit - '၀') % 10).toString()))
    }

    override val base: List<Row> = listOf(
        digits,
        chars("ဆ တ န မ အ ပ က င သ စ").withAlternates("င", KINZI),
        chars("ေ ျ ိ ် ါ ့ ြ ု ူ း"),
        listOf(shiftKey) + chars("ဖ ထ ခ လ ဘ ည ာ") + backspaceKey,
        bottom,
    )

    override val shift: List<Row> = listOf(
        chars("ဍ ဎ ဋ ဟ ဧ ဩ ရ ဪ ၏ ?"),
        chars("ဈ ဝ ဣ ၎ ဤ ၌ ဥ ၍ ဿ ဏ"),
        chars("ဗ ှ ီ ္ ွ ံ ဲ ဒ ဓ ဂ"),
        listOf(shiftKey) + chars("ဇ ဌ ဃ ဠ ယ ဉ ဦ") + backspaceKey,
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
