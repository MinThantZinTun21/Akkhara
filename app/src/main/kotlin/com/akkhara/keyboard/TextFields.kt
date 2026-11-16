package com.akkhara.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.akkhara.keyboard.engine.EnterAction
import com.akkhara.keyboard.engine.Field
import com.akkhara.keyboard.engine.FieldKind

/** Describes Android's text field to the engine. */
fun EditorInfo.toField(): Field {
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val isText = inputClass == InputType.TYPE_CLASS_TEXT
    val isNumber = inputClass == InputType.TYPE_CLASS_NUMBER
    return Field(
        kind = when {
            isNumber ||
                inputClass == InputType.TYPE_CLASS_PHONE ||
                inputClass == InputType.TYPE_CLASS_DATETIME -> FieldKind.Number
            isText && variation in ADDRESS_VARIATIONS -> FieldKind.Address
            else -> FieldKind.Text
        },
        isPassword = (isText && variation in PASSWORD_VARIATIONS) ||
            (isNumber && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD),
        enterAction = enterAction(),
    )
}

private fun EditorInfo.enterAction(): EnterAction {
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
