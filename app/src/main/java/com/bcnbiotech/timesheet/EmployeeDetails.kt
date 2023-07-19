package com.bcnbiotech.timesheet

object EmployeeDetails {

    lateinit var RFIDToNameHashMap: HashMap<String, String>

    init {
        initRFIDToNameHashMap()
    }

    private fun initRFIDToNameHashMap() {
        RFIDToNameHashMap = HashMap()

        RFIDToNameHashMap["0004785350"] = "B.SRINIVASAN"
        RFIDToNameHashMap["0004785355"] = "A.REETA"
        RFIDToNameHashMap["0009921045"] = "M.THILAKA"
        RFIDToNameHashMap["0009921050"] = "V.GOPI"
        RFIDToNameHashMap["0009921055"] = "N.KUMARAN"
        RFIDToNameHashMap["0002297787"] = "A.SULOCHANA"
        RFIDToNameHashMap["0002323331"] = "T.KOMALEESWARI"
        RFIDToNameHashMap["0002444325"] = "R.RAJENDIRAN"
        RFIDToNameHashMap["0002487444"] = "G.LAVANYA"
        RFIDToNameHashMap["0002415089"] = "V.KAVITHA"
        RFIDToNameHashMap["0002482111"] = "M.SANTHOSH"
        RFIDToNameHashMap["0002402770"] = "E.INDURANI"
        RFIDToNameHashMap["0002460710"] = "S.ESWARI"
        RFIDToNameHashMap["0002446730"] = "K.MAHENDIRAN"
        RFIDToNameHashMap["0002457562"] = "K.MOHANAPRIYA"
        RFIDToNameHashMap["0004192330"] = "M.SANTHOSHKUMAR"
        RFIDToNameHashMap["0004336632"] = "SATISH A.KAMATH"
        RFIDToNameHashMap["0009988496"] = "S.SEENIVASAN"
        RFIDToNameHashMap["0009817679"] = "M.NARASIMMAN"
        RFIDToNameHashMap["0004246881"] = "C. RAVI"
        RFIDToNameHashMap["0009818672"] = "K.RAVISHANKAR"
        RFIDToNameHashMap["0004718400"] = "V. NAVEENKUMAR"
    }

}