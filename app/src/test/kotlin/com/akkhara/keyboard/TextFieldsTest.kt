package com.akkhara.keyboard

import android.text.InputType.TYPE_CLASS_DATETIME
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_PHONE
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_DATETIME_VARIATION_DATE
import android.text.InputType.TYPE_DATETIME_VARIATION_TIME
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
import android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import com.akkhara.keyboard.engine.EnterAction
import com.akkhara.keyboard.engine.Field
import com.akkhara.keyboard.engine.FieldKind
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFieldsTest {
    @Test
    fun `a number field takes a plain number`() {
        assertEquals(Field(FieldKind.Number()), fieldOf(TYPE_CLASS_NUMBER, 0))
    }

    @Test
    fun `a signed number field takes a minus sign`() {
        assertEquals(
            Field(FieldKind.Number(allowsNegative = true)),
            fieldOf(TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_SIGNED, 0),
        )
    }

    @Test
    fun `a decimal number field takes a decimal point`() {
        assertEquals(
            Field(FieldKind.Number(allowsDecimal = true)),
            fieldOf(TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL, 0),
        )
    }

    @Test
    fun `a signed decimal field takes both`() {
        assertEquals(
            Field(FieldKind.Number(allowsNegative = true, allowsDecimal = true)),
            fieldOf(TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL, 0),
        )
    }

    @Test
    fun `a PIN is a number field that is a password`() {
        assertEquals(
            Field(FieldKind.Number(), isPassword = true),
            fieldOf(TYPE_CLASS_NUMBER or TYPE_NUMBER_VARIATION_PASSWORD, 0),
        )
    }

    @Test
    fun `a phone field is told apart from a number field`() {
        assertEquals(FieldKind.Phone, fieldOf(TYPE_CLASS_PHONE, 0).kind)
    }

    @Test
    fun `date and time fields are told apart`() {
        assertEquals(FieldKind.Date, fieldOf(TYPE_CLASS_DATETIME or TYPE_DATETIME_VARIATION_DATE, 0).kind)
        assertEquals(FieldKind.Time, fieldOf(TYPE_CLASS_DATETIME or TYPE_DATETIME_VARIATION_TIME, 0).kind)
        assertEquals(FieldKind.DateAndTime, fieldOf(TYPE_CLASS_DATETIME, 0).kind)
    }

    @Test
    fun `text and address fields keep the letters`() {
        assertEquals(FieldKind.Text, fieldOf(TYPE_CLASS_TEXT, 0).kind)
        assertEquals(FieldKind.Address, fieldOf(TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 0).kind)
    }

    @Test
    fun `enter shows the field's action`() {
        assertEquals(EnterAction.Done, fieldOf(TYPE_CLASS_NUMBER, IME_ACTION_DONE).enterAction)
    }
}
