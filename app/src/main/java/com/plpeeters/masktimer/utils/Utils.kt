package com.plpeeters.masktimer.utils

import android.content.Context
import android.widget.EditText
import java.text.Normalizer


private val UNIDECODE_REGEX = """\p{InCombiningDiacriticalMarks}+""".toRegex()

fun CharSequence.unidecode(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)

    return UNIDECODE_REGEX.replace(temp, "")
}

fun CharSequence.normalize(): String {
    return unidecode().lowercase().trim()
}

fun EditText.getTrimmedText(): String {
    return text.toString().trim()
}

fun Context.getVersionName(): String? {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)

    return packageInfo?.versionName
}
