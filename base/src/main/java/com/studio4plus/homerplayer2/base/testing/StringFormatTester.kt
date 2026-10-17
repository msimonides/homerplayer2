/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.studio4plus.homerplayer2.base.testing

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale

object StringFormatTester {

    class ValidationException(
        message: String,
        cause: Throwable
    ) : Exception("$message; ${cause.message}", originalCause(cause)) {
        override fun getStackTrace(): Array<StackTraceElement> = emptyArray()

        companion object {
            private fun originalCause(cause: Throwable) =
                if (cause is ValidationException) cause.cause else cause
        }
    }

    // This selection should cover all variants (one, other, few, many etc).
    private val PLURAL_QUANTITIES = intArrayOf(0, 1, 2, 4, 5, 7, 12, 16, 30)

    private data class StringResource(val id: Int, val text: String, val name: String)
    private data class PluralResource(val id: Int, val texts: Map<Int, String>, val name: String)
    private data class StringParam(val position: Int, val type: String) {
        override fun toString(): String = "%$position\$$type"
    }

    private val paramRegex = Regex("""%(\d+)\$([0-9.]*[a-z])""")

    fun test(app: Context, locales: Collection<String>, strings: Class<*>?, plurals: Class<*>?) {
        val usResources = resourcesForLocale("us", app)
        val referenceStringParamTypes = if (strings != null) {
            getStringResources(strings, usResources)
                .associate { it.id to extractParamTypes(it.text) }
        } else {
            null
        }
        val referencePluralParamTypes = plurals?.let { getPluralResources(plurals, usResources) }
            ?.associate {
                val pluralParamTypes = it.texts.mapValues { (_, text) -> extractParamTypes(text) }
                it.id to pluralParamTypes
            }

        val errors = locales.map { localeString ->
            try {
                val localizedResources = resourcesForLocale(localeString, app)
                val errors = buildMap {
                    if (strings != null && referenceStringParamTypes != null) {
                        putAll(validateStrings(strings, localizedResources, referenceStringParamTypes))
                    }
                    if (plurals != null && referencePluralParamTypes != null) {
                        putAll(validatePlurals(plurals, localizedResources, referencePluralParamTypes))
                    }
                }

                localeString to errors
            } catch (e: Throwable) {
                throw ValidationException("Locale: $localeString", e)
            }
        }
        val errorMessages = errors.flatMap { (locale, stringErrors) ->
            stringErrors.map { (resName, error) ->
                " - $locale: $resName: $error"
            }
        }
        if (errorMessages.isNotEmpty()) {
            throw AssertionError("Found errors in translation strings:\n${errorMessages.joinToString("\n")}")
        }
    }

    private fun validatePlurals(
        clazz: Class<*>,
        resources: Resources,
        referencePluralParamTypes: Map<Int, Map<Int, Set<StringParam>>>,
    ): Map<String, String> = buildMap {
        getPluralResources(clazz, resources)
            .forEach { plural ->
                val referenceParamTypes = referencePluralParamTypes[plural.id]!!
                plural.texts.forEach { (quantity, text) ->
                    try {
                        val error = validatePlural(plural, text, quantity, resources, referenceParamTypes[quantity]!!)
                        if (error != null) {
                            put("${plural.name} (${quantity})", error)
                        }
                    } catch (e: Throwable) {
                        throw ValidationException("pluralId: ${plural.name} with $quantity", e)
                    }
                }
            }
    }

    private fun validateStrings(
        clazz: Class<*>,
        resources: Resources,
        referenceParamTypes: Map<Int, Set<StringParam>>,
    ): Map<String, String> = buildMap {
        getStringResources(clazz, resources)
            .forEach { stringResource ->
                try {
                    val error = validateString(stringResource, resources, referenceParamTypes[stringResource.id]!!)
                    if (error != null)
                        put(stringResource.name, error)
                } catch (e: Throwable) {
                    throw ValidationException("stringId: ${stringResource.name}", e)
                }
            }
    }

    private fun validateString(
        string: StringResource,
        resources: Resources,
        referenceParamTypes: Set<StringParam>,
    ): String? {
        fun error(message: String) = "$message - '${string.text}'"

        val paramTypes = extractParamTypes(string.text)

        if (!referenceParamTypes.containsAll(paramTypes)) {
            return error("mismatched params, expected subset of $referenceParamTypes but got $paramTypes")
        }

        try {
            if (paramTypes.isNotEmpty()) {
                resources.getString(string.id, *referenceParamTypes.toValuesArray())
            }
        } catch (e: Exception) {
            return error("getString() throws: $e")
        }
        return null
    }

    private fun validatePlural(
        plural: PluralResource,
        text: String,
        quantity: Int,
        resources: Resources,
        referenceParamTypes: Set<StringParam>,
    ): String? {
        fun error(message: String) = "$message - '${text}'"

        val paramTypes = extractParamTypes(text)

        if (!referenceParamTypes.containsAll(paramTypes)) {
            return error("mismatched params, expected subset of $referenceParamTypes but got $paramTypes")
        }

        try {
            if (paramTypes.isNotEmpty()) {
                resources.getQuantityString(
                    plural.id,
                    quantity,
                    *referenceParamTypes.toValuesArray(quantity)
                )
            }
            return null
        } catch (e: Exception) {
            return error("getQuantityString() throws: $e")
        }
    }

    private fun extractParamTypes(text: String): Set<StringParam> {
        val matches = paramRegex.findAll(text)
        return buildSet {
            matches.forEach { match ->
                val (_, positionString, typeString) = match.groupValues

                val position = Integer.parseInt(positionString)
                add(StringParam(position, typeString))
            }
        }
    }


    private fun resourcesForLocale(localeString: String, appContext: Context): Resources {
        val parts = localeString.split("-r")
        val locale = if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(parts[0])
        val configuration = Configuration().apply { setLocale(locale) }
        val context = appContext.createConfigurationContext(configuration)
        return context.resources
    }

    private fun getStringResources(clazz: Class<*>, resources: Resources): Iterable<StringResource> =
        getResources(clazz) { resField ->
            val resId = resField.getInt(clazz)
            StringResource(id = resId, text = resources.getString(resId), name = resField.name)
        }

    private fun getPluralResources(clazz: Class<*>, resources: Resources): Iterable<PluralResource> =
        getResources(clazz) { resField ->
            val resId = resField.getInt(clazz)
            val texts = PLURAL_QUANTITIES.associateWith { quantity -> resources.getQuantityString(resId, quantity) }
            PluralResource(id = resId, texts = texts, name = resField.name)
        }

    private  fun <T> getResources(clazz: Class<*>, extractor: (Field) -> T): Iterable<T> =
        clazz.declaredFields
            .filter { Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) }
            .map(extractor)

    private fun Set<StringParam>.toValuesArray(intValue: Int = 5) =
        sortedBy { it.position }
            .map { valueForType(it.type, intValue) }
            .toTypedArray()

    // Use quantity for int value in plurals to make them less confusing when an error is reported.
    private fun valueForType(typeString: String, intValue: Int): Any = when (typeString) {
        "d" -> intValue
        "s" -> "text"
        "f" -> 1.123f
        else -> throw IllegalArgumentException("Unknown parameter type: $typeString")
    }
}