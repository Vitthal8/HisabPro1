package com.hisabpro.app.util

import com.hisabpro.app.data.model.GstMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Indian State Information for GST Compliance
 */
data class IndianStateInfo(
    val code: String,          // 2-digit GST State Code (e.g. "27", "24")
    val name: String,          // Official State Name (e.g. "Maharashtra", "Gujarat")
    val shortCode: String,     // 2-letter Postal Code (e.g. "MH", "GJ")
    val majorCities: List<String> = emptyList()
)

/**
 * Standard Indian Accounting Utilities:
 * 1. Financial Year (April 1 to March 31)
 * 2. Indian Numbering System (Lakhs & Crores e.g. ₹1,23,456.00)
 * 3. Dr / Cr (Debit / Credit) Ledger labels
 * 4. DD/MM/YYYY Indian Date formatting
 * 5. Multi-language dictionary (English, Hindi, Marathi)
 * 6. Pan-India State Identification & Inter-State (IGST) vs Intra-State (CGST+SGST) resolution
 */
object IndianAccountingFormat {

    val INDIAN_STATES = listOf(
        IndianStateInfo("01", "Jammu and Kashmir", "JK", listOf("Srinagar", "Jammu")),
        IndianStateInfo("02", "Himachal Pradesh", "HP", listOf("Shimla", "Dharamshala", "Manali", "Mandi")),
        IndianStateInfo("03", "Punjab", "PB", listOf("Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Mohali")),
        IndianStateInfo("04", "Chandigarh", "CH", listOf("Chandigarh")),
        IndianStateInfo("05", "Uttarakhand", "UK", listOf("Dehradun", "Haridwar", "Rishikesh", "Haldwani")),
        IndianStateInfo("06", "Haryana", "HR", listOf("Gurgaon", "Gurugram", "Faridabad", "Panipat", "Ambala")),
        IndianStateInfo("07", "Delhi", "DL", listOf("Delhi", "New Delhi", "Noida")),
        IndianStateInfo("08", "Rajasthan", "RJ", listOf("Jaipur", "Jodhpur", "Udaipur", "Kota", "Bikaner")),
        IndianStateInfo("09", "Uttar Pradesh", "UP", listOf("Lucknow", "Kanpur", "Varanasi", "Agra", "Prayagraj", "Noida", "Ghaziabad")),
        IndianStateInfo("10", "Bihar", "BR", listOf("Patna", "Gaya", "Bhagalpur", "Muzaffarpur")),
        IndianStateInfo("11", "Sikkim", "SK", listOf("Gangtok")),
        IndianStateInfo("12", "Arunachal Pradesh", "AR", listOf("Itanagar")),
        IndianStateInfo("13", "Nagaland", "NL", listOf("Kohima", "Dimapur")),
        IndianStateInfo("14", "Manipur", "MN", listOf("Imphal")),
        IndianStateInfo("15", "Mizoram", "MZ", listOf("Aizawl")),
        IndianStateInfo("16", "Tripura", "TR", listOf("Agartala")),
        IndianStateInfo("17", "Meghalaya", "ML", listOf("Shillong")),
        IndianStateInfo("18", "Assam", "AS", listOf("Guwahati", "Silchar", "Dibrugarh", "Jorhat")),
        IndianStateInfo("19", "West Bengal", "WB", listOf("Kolkata", "Howrah", "Siliguri", "Durgapur", "Asansol")),
        IndianStateInfo("20", "Jharkhand", "JH", listOf("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro")),
        IndianStateInfo("21", "Odisha", "OD", listOf("Bhubaneswar", "Cuttack", "Rourkela", "Puri")),
        IndianStateInfo("22", "Chhattisgarh", "CG", listOf("Raipur", "Bhilai", "Bilaspur", "Korba")),
        IndianStateInfo("23", "Madhya Pradesh", "MP", listOf("Bhopal", "Indore", "Gwalior", "Jabalpur", "Ujjain")),
        IndianStateInfo("24", "Gujarat", "GJ", listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Gandhinagar")),
        IndianStateInfo("25", "Daman and Diu", "DD", listOf("Daman", "Diu")),
        IndianStateInfo("26", "Dadra and Nagar Haveli", "DN", listOf("Silvassa")),
        IndianStateInfo("27", "Maharashtra", "MH", listOf("Mumbai", "Pune", "Nagpur", "Thane", "Nashik", "Aurangabad", "Solapur", "Kolhapur")),
        IndianStateInfo("28", "Andhra Pradesh (Old)", "AP", emptyList()),
        IndianStateInfo("29", "Karnataka", "KA", listOf("Bengaluru", "Bangalore", "Mysuru", "Mysore", "Hubli", "Mangaluru", "Belagavi")),
        IndianStateInfo("30", "Goa", "GA", listOf("Panaji", "Margao", "Vasco")),
        IndianStateInfo("31", "Lakshadweep", "LD", listOf("Kavaratti")),
        IndianStateInfo("32", "Kerala", "KL", listOf("Thiruvananthapuram", "Kochi", "Cochin", "Kozhikode", "Thrissur")),
        IndianStateInfo("33", "Tamil Nadu", "TN", listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem")),
        IndianStateInfo("34", "Puducherry", "PY", listOf("Puducherry", "Pondicherry")),
        IndianStateInfo("35", "Andaman and Nicobar Islands", "AN", listOf("Port Blair")),
        IndianStateInfo("36", "Telangana", "TS", listOf("Hyderabad", "Warangal", "Nizamabad", "Karimnagar")),
        IndianStateInfo("37", "Andhra Pradesh", "AP", listOf("Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Tirupati")),
        IndianStateInfo("38", "Ladakh", "LA", listOf("Leh", "Kargil")),
        IndianStateInfo("97", "Other Territory", "OT", emptyList())
    )

    /**
     * Resolves canonical 2-digit GST state code from any representation:
     * - GSTIN (first 2 digits e.g. "24AAAAA1234A1Z5" -> "24")
     * - State Name (e.g. "Gujarat", "Maharashtra")
     * - State Code string (e.g. "24", "27")
     * - State Code with Parentheses (e.g. "Gujarat (24)")
     * - 2-letter postal code (e.g. "GJ", "MH")
     * - Address substring containing state name or abbreviation or major city
     */
    fun resolveStateCode(
        state: String? = null,
        stateCode: String? = null,
        gstin: String? = null,
        address: String? = null
    ): String? {
        // 1. Try GSTIN first (highest priority, legal standard in Indian GST)
        val cleanGstin = gstin?.trim()?.uppercase() ?: ""
        if (cleanGstin.length >= 2 && cleanGstin.take(2).all { it.isDigit() }) {
            val candidateCode = cleanGstin.take(2)
            if (INDIAN_STATES.any { it.code == candidateCode }) {
                return candidateCode
            }
        }

        // 2. Try explicit stateCode
        val cleanCode = stateCode?.trim() ?: ""
        if (cleanCode.isNotBlank()) {
            val padded = if (cleanCode.length == 1) "0$cleanCode" else cleanCode
            if (INDIAN_STATES.any { it.code == padded }) {
                return padded
            }
        }

        // 3. Try state string (e.g. "Gujarat", "GJ", "24", "Gujarat (24)")
        val cleanState = state?.trim() ?: ""
        if (cleanState.isNotBlank()) {
            // Check parentheses e.g. "Gujarat (24)"
            val insideParens = cleanState.substringAfter("(", "").substringBefore(")", "").trim()
            if (insideParens.isNotBlank()) {
                val padded = if (insideParens.length == 1) "0$insideParens" else insideParens
                if (INDIAN_STATES.any { it.code == padded }) return padded
            }

            // Direct digit code match: "24", "27"
            if (cleanState.all { it.isDigit() }) {
                val padded = if (cleanState.length == 1) "0$cleanState" else cleanState
                if (INDIAN_STATES.any { it.code == padded }) return padded
            }

            // Match full state name (case-insensitive)
            val nameMatch = INDIAN_STATES.find {
                it.name.equals(cleanState, ignoreCase = true) ||
                cleanState.startsWith(it.name, ignoreCase = true)
            }
            if (nameMatch != null) return nameMatch.code

            // Match 2-letter postal code: "GJ", "MH"
            val shortMatch = INDIAN_STATES.find {
                it.shortCode.equals(cleanState, ignoreCase = true)
            }
            if (shortMatch != null) return shortMatch.code
        }

        // 4. Try scanning address for state name or state code or major cities
        val cleanAddress = address?.trim() ?: ""
        if (cleanAddress.isNotBlank()) {
            // Check for state name in address
            for (st in INDIAN_STATES) {
                val pattern = "\\b${Regex.escape(st.name)}\\b".toRegex(RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(cleanAddress)) {
                    return st.code
                }
            }
            // Check for state 2-letter code in address (e.g. "Surat, GJ", "Pune, MH")
            for (st in INDIAN_STATES) {
                val pattern = "\\b${st.shortCode}\\b".toRegex(RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(cleanAddress)) {
                    return st.code
                }
            }
            // Check for major cities in address
            for (st in INDIAN_STATES) {
                for (city in st.majorCities) {
                    val pattern = "\\b${Regex.escape(city)}\\b".toRegex(RegexOption.IGNORE_CASE)
                    if (pattern.containsMatchIn(cleanAddress)) {
                        return st.code
                    }
                }
            }
        }

        return null
    }

    /**
     * Determines whether a supply is Inter-State (IGST) or Intra-State (CGST + SGST).
     * Compares the resolved business state code with customer state code.
     */
    fun isInterStateSupply(
        businessState: String? = null,
        businessStateCode: String? = null,
        businessGstin: String? = null,
        customerState: String? = null,
        customerStateCode: String? = null,
        customerGstin: String? = null,
        customerAddress: String? = null
    ): Boolean {
        val bCode = resolveStateCode(state = businessState, stateCode = businessStateCode, gstin = businessGstin) ?: "27"
        val cCode = resolveStateCode(state = customerState, stateCode = customerStateCode, gstin = customerGstin, address = customerAddress)
        return cCode != null && cCode != bCode
    }

    /**
     * Determines GstMode (INTER_STATE vs INTRA_STATE) based on business and customer state data.
     */
    fun determineGstMode(
        businessState: String? = null,
        businessStateCode: String? = null,
        businessGstin: String? = null,
        customerState: String? = null,
        customerStateCode: String? = null,
        customerGstin: String? = null,
        customerAddress: String? = null
    ): GstMode {
        return if (isInterStateSupply(
                businessState = businessState,
                businessStateCode = businessStateCode,
                businessGstin = businessGstin,
                customerState = customerState,
                customerStateCode = customerStateCode,
                customerGstin = customerGstin,
                customerAddress = customerAddress
            )
        ) {
            GstMode.INTER_STATE
        } else {
            GstMode.INTRA_STATE
        }
    }

    /**
     * Calculates the Indian Financial Year string, e.g. "2025-26".
     * In India, FY starts on April 1 (Month index 3 in Calendar) and ends on March 31.
     */
    fun getFinancialYear(dateMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-indexed: 3 is April

        return if (month >= Calendar.APRIL) {
            val nextYearShort = (year + 1) % 100
            "$year-${String.format(Locale.ROOT, "%02d", nextYearShort)}"
        } else {
            val prevYear = year - 1
            val currentYearShort = year % 100
            "$prevYear-${String.format(Locale.ROOT, "%02d", currentYearShort)}"
        }
    }

    /**
     * Generates a standard FY-prefixed invoice number e.g. "2025-26/INV/001" or "2025-26/BILL/001"
     */
    fun formatInvoiceNumberWithFY(
        prefix: String,
        sequenceNumber: Int,
        dateMillis: Long = System.currentTimeMillis()
    ): String {
        val fy = getFinancialYear(dateMillis)
        val seqPadded = String.format(Locale.ROOT, "%03d", sequenceNumber)
        return "$fy/$prefix/$seqPadded"
    }

    /**
     * Formats amount according to Indian numbering system (Lakhs, Crores):
     * e.g. 1234567.50 -> ₹12,34,567.50
     */
    fun formatIndianCurrency(amount: Double, showSymbol: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val longPart = absAmount.toLong()
        val decimalPart = kotlin.math.round((absAmount - longPart) * 100).toInt()

        val s = longPart.toString()
        val formattedLong = if (s.length <= 3) {
            s
        } else {
            val lastThree = s.substring(s.length - 3)
            val rest = s.substring(0, s.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count % 2 == 0 && i > 0) {
                    sb.append(',')
                }
            }
            sb.reverse().append(',').append(lastThree).toString()
        }

        val formatted = if (decimalPart > 0) {
            "$formattedLong.${String.format(Locale.ROOT, "%02d", decimalPart)}"
        } else {
            formattedLong
        }

        val symbolPrefix = if (showSymbol) "₹" else ""
        return if (isNegative) "-$symbolPrefix$formatted" else "$symbolPrefix$formatted"
    }

    /**
     * Formats date as DD/MM/YYYY (Indian Standard)
     */
    fun formatIndianDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return sdf.format(Date(dateMillis))
    }

    /**
     * Returns Dr / Cr descriptor for party running balances:
     * - Customer positive balance: Dr (Debit = Money to Receive)
     * - Supplier positive balance: Cr (Credit = Money to Pay)
     */
    fun getDrCrIndicator(balance: Double, isCustomer: Boolean = true): String {
        if (kotlin.math.abs(balance) < 0.01) return "-"
        return if (isCustomer) {
            if (balance > 0) "Dr" else "Cr"
        } else {
            if (balance > 0) "Cr" else "Dr"
        }
    }

    fun getDrCrBalanceLabel(balance: Double, isCustomer: Boolean): String {
        val formatted = formatIndianCurrency(kotlin.math.abs(balance))
        return when {
            balance > 0 -> {
                if (isCustomer) "$formatted Dr (To Collect)" else "$formatted Cr (To Pay)"
            }
            balance < 0 -> {
                if (isCustomer) "$formatted Cr (Advance Paid)" else "$formatted Dr (Advance Given)"
            }
            else -> "₹0 (Settled)"
        }
    }

    /**
     * Converts a numeric rupee amount to words according to the Indian numbering system
     * e.g. 125000.0 -> "Rupees One Lakh Twenty-Five Thousand Only"
     */
    fun numberToWordsIndian(amount: Double): String {
        if (amount < 0.01) return "Rupees Zero Only"

        val absAmount = kotlin.math.abs(amount)
        val rupees = absAmount.toLong()
        val paise = kotlin.math.round((absAmount - rupees) * 100).toInt()

        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertLessThanThousand(number: Int): String {
            var n = number
            var result = ""
            if (n >= 100) {
                result += "${units[n / 100]} Hundred "
                n %= 100
            }
            if (n in 1..19) {
                result += units[n]
            } else if (n >= 20) {
                val t = tens[n / 10]
                val u = units[n % 10]
                result += if (u.isNotBlank()) "$t-$u" else t
            }
            return result.trim()
        }

        var num = rupees
        var words = ""

        val crore = (num / 10000000L).toInt()
        num %= 10000000L
        val lakh = (num / 100000L).toInt()
        num %= 100000L
        val thousand = (num / 1000L).toInt()
        num %= 1000L
        val hundredRemainder = num.toInt()

        if (crore > 0) {
            words += "${convertLessThanThousand(crore)} Crore "
        }
        if (lakh > 0) {
            words += "${convertLessThanThousand(lakh)} Lakh "
        }
        if (thousand > 0) {
            words += "${convertLessThanThousand(thousand)} Thousand "
        }
        if (hundredRemainder > 0) {
            words += "${convertLessThanThousand(hundredRemainder)} "
        }

        words = words.trim()
        val paiseWords = if (paise > 0) " and ${convertLessThanThousand(paise)} Paise" else ""

        return if (words.isBlank() && paise > 0) {
            "${convertLessThanThousand(paise)} Paise Only"
        } else {
            "Rupees $words$paiseWords Only"
        }
    }

    /**
     * Multi-language dictionary for Indian SMB accounting terms (English, Hindi, Marathi)
     */
    fun t(key: String, lang: String = "en"): String {
        val dict = mapOf(
            "dashboard" to mapOf("en" to "Dashboard", "hi" to "डैशबोर्ड", "mr" to "डॅशबोर्ड"),
            "today_sales" to mapOf("en" to "Today's Sales", "hi" to "आज की बिक्री", "mr" to "आजची विक्री"),
            "to_collect" to mapOf("en" to "To Collect (Receivables)", "hi" to "लेना बाकी (उधार)", "mr" to "येणे बाकी (उधारी)"),
            "to_pay" to mapOf("en" to "To Pay (Payables)", "hi" to "देना बाकी", "mr" to "देणे बाकी"),
            "cash_in_hand" to mapOf("en" to "Cash in Hand", "hi" to "रोकड़ (नकद)", "mr" to "हातातील रोख"),
            "bank_balance" to mapOf("en" to "Bank Balance", "hi" to "बैंक बैलेंस", "mr" to "बँक शिल्लक"),
            "new_sale" to mapOf("en" to "New Sale", "hi" to "नई बिक्री", "mr" to "नवीन विक्री"),
            "payment_in" to mapOf("en" to "Payment In", "hi" to "पेमेंट जमा", "mr" to "पेमेंट जमा"),
            "add_expense" to mapOf("en" to "Expense Out", "hi" to "खर्च", "mr" to "खर्च"),
            "add_party" to mapOf("en" to "Add Party", "hi" to "पार्टी जोड़ें", "mr" to "पार्टी जोडा"),
            "daybook" to mapOf("en" to "Day Book (Roznamcha)", "hi" to "रोजनामचा", "mr" to "रोजकीर्द"),
            "cashbook" to mapOf("en" to "Cash Book", "hi" to "रोकड़ बही", "mr" to "रोख वही"),
            "parties" to mapOf("en" to "Parties / Khata", "hi" to "पार्टियां (खाता)", "mr" to "खातेदार (खाते)"),
            "sales" to mapOf("en" to "Sales / Invoices", "hi" to "बिक्री / बिल", "mr" to "विक्री / बिले"),
            "reports" to mapOf("en" to "Reports & GST", "hi" to "रिपोर्ट्स और जीएसटी", "mr" to "अहवाल व जीएसटी"),
            "items" to mapOf("en" to "Items & Stock", "hi" to "आइटम और स्टॉक", "mr" to "वस्तू व स्टॉक"),
            "non_gst_mode" to mapOf("en" to "Non-GST Business", "hi" to "नॉन-जीएसटी व्यापार", "mr" to "विना-जीएसटी व्यवसाय"),
            "gst_registered" to mapOf("en" to "GST Registered", "hi" to "जीएसटी पंजीकृत", "mr" to "जीएसटी नोंदणीकृत")
        )
        return dict[key]?.get(lang) ?: dict[key]?.get("en") ?: key
    }
}
