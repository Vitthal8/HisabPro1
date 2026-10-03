package com.hisabpro.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Strengthened Unit Test verifying:
 * 1. All English string keys exist in values-hi and values-mr.
 * 2. Fail if a Hindi or Marathi translation is identical to the English value (except allowed brand names, numbers, and units).
 * 3. Fail if format placeholder counts (%1$s, %2$d, %d, %s) differ between languages.
 */
class StringTranslationCompletenessTest {

    private val allowlistIdenticalValues = setOf(
        "HisabPro",
        "English",
        "PDF",
        "CSV",
        "UPI",
        "GST",
        "GSTIN",
        "PAN",
        "IFSC",
        "POS",
        "WhatsApp",
        "CGST",
        "SGST",
        "IGST",
        "CGST:",
        "SGST:",
        "IGST:",
        "Pcs",
        "Kg",
        "Ltr",
        "0.00",
        "0",
        "1.0",
        "₹0",
        "Cards",
        "Table (Dr/Cr)",
        "हिंदी (Hindi)",
        "मराठी (Marathi)",
        "1:100000000000:android:0000000000000000"
    )

    @Test
    fun testAllEnglishStringKeysExistInHindiAndMarathi() {
        val (valuesDir, valuesHiDir, valuesMrDir) = resolveResourceFiles()

        val enMap = parseStringMap(valuesDir)
        val hiMap = parseStringMap(valuesHiDir)
        val mrMap = parseStringMap(valuesMrDir)

        val enKeys = enMap.keys
        val hiKeys = hiMap.keys
        val mrKeys = mrMap.keys

        val missingInHi = enKeys - hiKeys
        val missingInMr = enKeys - mrKeys

        assertTrue("Missing Hindi translations for keys: $missingInHi", missingInHi.isEmpty())
        assertTrue("Missing Marathi translations for keys: $missingInMr", missingInMr.isEmpty())
    }

    @Test
    fun testNoUntranslatedValuesInHindiAndMarathi() {
        val (valuesDir, valuesHiDir, valuesMrDir) = resolveResourceFiles()

        val enMap = parseStringMap(valuesDir)
        val hiMap = parseStringMap(valuesHiDir)
        val mrMap = parseStringMap(valuesMrDir)

        val identicalInHi = mutableListOf<String>()
        val identicalInMr = mutableListOf<String>()

        for ((key, enVal) in enMap) {
            if (allowlistIdenticalValues.contains(enVal.trim())) continue

            val hiVal = hiMap[key]
            if (hiVal != null && hiVal.trim() == enVal.trim()) {
                identicalInHi.add("$key = '$enVal'")
            }

            val mrVal = mrMap[key]
            if (mrVal != null && mrVal.trim() == enVal.trim()) {
                identicalInMr.add("$key = '$enVal'")
            }
        }

        assertTrue("Hindi values identical to English (untranslated): $identicalInHi", identicalInHi.isEmpty())
        assertTrue("Marathi values identical to English (untranslated): $identicalInMr", identicalInMr.isEmpty())
    }

    @Test
    fun testPlaceholderCountEqualityAcrossLanguages() {
        val (valuesDir, valuesHiDir, valuesMrDir) = resolveResourceFiles()

        val enMap = parseStringMap(valuesDir)
        val hiMap = parseStringMap(valuesHiDir)
        val mrMap = parseStringMap(valuesMrDir)

        val placeholderRegex = Regex("%[0-9]*\\$?[a-zA-Z]")

        for ((key, enVal) in enMap) {
            val enCount = placeholderRegex.findAll(enVal).count()

            val hiVal = hiMap[key]
            if (hiVal != null) {
                val hiCount = placeholderRegex.findAll(hiVal).count()
                assertEquals("Placeholder count mismatch for key '$key' in Hindi: en=$enCount vs hi=$hiCount", enCount, hiCount)
            }

            val mrVal = mrMap[key]
            if (mrVal != null) {
                val mrCount = placeholderRegex.findAll(mrVal).count()
                assertEquals("Placeholder count mismatch for key '$key' in Marathi: en=$enCount vs mr=$mrCount", enCount, mrCount)
            }
        }
    }

    private fun resolveResourceFiles(): Triple<File, File, File> {
        val rootDir = File(".").absoluteFile
        var valuesDir = File(rootDir, "src/main/res/values/strings.xml")
        var valuesHiDir = File(rootDir, "src/main/res/values-hi/strings.xml")
        var valuesMrDir = File(rootDir, "src/main/res/values-mr/strings.xml")

        if (!valuesDir.exists()) {
            valuesDir = File(rootDir, "app/src/main/res/values/strings.xml")
            valuesHiDir = File(rootDir, "app/src/main/res/values-hi/strings.xml")
            valuesMrDir = File(rootDir, "app/src/main/res/values-mr/strings.xml")
        }

        assertTrue("values/strings.xml missing at ${valuesDir.absolutePath}", valuesDir.exists())
        assertTrue("values-hi/strings.xml missing at ${valuesHiDir.absolutePath}", valuesHiDir.exists())
        assertTrue("values-mr/strings.xml missing at ${valuesMrDir.absolutePath}", valuesMrDir.exists())

        return Triple(valuesDir, valuesHiDir, valuesMrDir)
    }

    private fun parseStringMap(xmlFile: File): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val nodeList = doc.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) {
                map[name] = node.textContent ?: ""
            }
        }
        return map
    }
}
