package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable

class DataExtra<T>(val key: String)

inline fun <reified T> Bundle.getExtra(extra: DataExtra<T>): T? {
    if (containsKey(extra.key))
        get(extra.key).let {
            if (it is T?)
                return it
        }
    return null
}

fun <T> Bundle.put(extra: DataExtra<T>, value: T) {
    when (value) {
        is IBinder -> putBinder(extra.key, value)
        is Bundle -> putBundle(extra.key, value)
        is Byte -> putByte(extra.key, value)
        is ByteArray -> putByteArray(extra.key, value)
        is Char -> putChar(extra.key, value)
        is CharArray -> putCharArray(extra.key, value)
        is CharSequence -> putCharSequence(extra.key, value)
        is Float -> putFloat(extra.key, value)
        is FloatArray -> putFloatArray(extra.key, value)
        is Parcelable -> putParcelable(extra.key, value)
        is Serializable -> putSerializable(extra.key, value)
        is Short -> putShort(extra.key, value)
        is ShortArray -> putShortArray(extra.key, value)
        is Size -> putSize(extra.key, value)
        is SizeF -> putSizeF(extra.key, value)
        //is SparseArray<Parcelable> -> putSparseParcelableArray(extra.key, value)
        is Array<*> -> {
            val charSequenceArray = arrayListOf<CharSequence>()
            val parcelableArray = arrayListOf<Parcelable>()
            for (v in value)
                if (v is CharSequence)
                    charSequenceArray.add(v)
                else if (v is Parcelable)
                    parcelableArray.add(v)
            if (charSequenceArray.isNotEmpty())
                putCharSequenceArray(extra.key, charSequenceArray.toTypedArray())
            if (parcelableArray.isNotEmpty())
                putParcelableArray(extra.key, parcelableArray.toTypedArray())
        }
        is ArrayList<*> -> {
            val charSequenceArray = arrayListOf<CharSequence>()
            val intArray = arrayListOf<Int>()
            val parcelableArray = arrayListOf<Parcelable>()
            val stringArray = arrayListOf<String>()
            for (v in value)
                when (v) {
                    is CharSequence -> charSequenceArray.add(v)
                    is Int -> intArray.add(v)
                    is Parcelable -> parcelableArray.add(v)
                    is String -> stringArray.add(v)
                }
            if (charSequenceArray.isNotEmpty())
                putCharSequenceArrayList(extra.key, charSequenceArray)
            if (intArray.isNotEmpty())
                putIntegerArrayList(extra.key, intArray)
            if (parcelableArray.isNotEmpty())
                putParcelableArrayList(extra.key, parcelableArray)
            if (stringArray.isNotEmpty())
                putStringArrayList(extra.key, stringArray)
        }
        is SparseArray<*> -> {
            val parcelableArray = SparseArray<Parcelable>()
            for (i in 0 until value.size())
                value[i].let { v ->
                    if (v is Parcelable)
                        parcelableArray.append(i, v)
                }
            if (parcelableArray.size() > 0)
                putSparseParcelableArray(extra.key, parcelableArray)
        }
    }
}
