package com.akkhara.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.akkhara.keyboard.engine.EnterAction
import com.akkhara.keyboard.engine.Field
import com.akkhara.keyboard.engine.FieldKind

/** Describes Android's text field to the engine. */
fun EditorInfo.toField(): Field = fieldOf(inputType, imeOptions)

/** Reads a text field's [inputType] and [imeOptions], as Android gives them, into what the engine needs. */
fun fieldOf(inputType: Int, imeOptions: Int): Field {
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val isText = inputClass == InputType.TYPE_CLASS_TEXT
    val isNumber = inputClass == InputType.TYPE_CLASS_NUMBER
    return Field(
        kind = when {
            isNumber -> FieldKind.Number(
                allowsNegative = inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0,
                allowsDecimal = inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0,
            )
            inputClass == InputType.TYPE_CLASS_PHONE -> FieldKind.Phone
            inputClass == InputType.TYPE_CLASS_DATETIME -> FieldKind.DateTime
            isText && variation in ADDRESS_VARIATIONS -> FieldKind.Address
            else -> FieldKind.Text
        },
        isPassword = (isText && variation in PASSWORD_VARIATIONS) ||
            (isNumber && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD),
        enterAction = enterAction(imeOptions),
    )
}

private fun enterAction(imeOptions: Int): EnterAction {
    if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EnterAction.NewLine
    return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_GO -> EnterAction.Go
        EditorInfo.IME_ACTION_SEARCH -> EnterAction.Search
        EditorInfo.IME_ACTION_SEND -> EnterAction.Send
        EditorInfo.IME_ACTION_NEXT -> EnterAction.Next
        EditorInfo.IME_ACTION_DONE -> EnterAction.Done
        else -> EnterAction.NewLine
    }
}

private val ADDRESS_VARIATIONS = setOf(
    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
    InputType.TYPE_TEXT_VARIATION_URI,
)

private val PASSWORD_VARIATIONS = setOf(
    InputType.TYPE_TEXT_VARIATION_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
)
