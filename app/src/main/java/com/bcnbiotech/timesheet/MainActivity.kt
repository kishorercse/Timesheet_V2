package com.bcnbiotech.timesheet

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask

@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity() {

    private lateinit var timeModeTextView: TextView
    private lateinit var employeeRFIDEditText: EditText
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var responseImageView: ImageView

    private val TIME_IN_MODE = "Time In"
    private val TIME_OUT_MODE = "Time Out"

    private lateinit var dayOfWeek: DayOfWeek

    private lateinit var dateFormatter: DateTimeFormatter
    private lateinit var timeFormatter: DateTimeFormatter

    private lateinit var dateTimeLiveData: MutableLiveData<LocalDateTime>

    private lateinit var timer: Timer

    private lateinit var filePath: String

    // column numbers in excel file
    private val DATE_IN_COLUMN = 0
    private val DAY_COLUMN = 1
    private val TIME_IN_COLUMN = 2
    private val DATE_OUT_COLUMN = 3
    private val TIME_OUT_COLUMN = 4
    private val LOG_HOURS_COLUMN = 5
    private val REGULAR_WORKING_HOURS_COLUMN = 6
    private val OT_HOURS_COLUMN = 7
    private val REGULAR_WORKING_HOURS_TOTAL_COLUMN = 12
    private val OT_HOURS_TOTAL_COLUMN = 14

    private fun initDateTimeAndDay() {
        dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        timer = Timer("date time timer")
        dateTimeLiveData = MutableLiveData()

        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                dateTimeLiveData.postValue(LocalDateTime.now())
            }
        }, 0, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initDateTimeAndDay()
    }

    override fun onStart() {
        super.onStart()
        setDateTimeObserver()
        setEmployeeRFIDTextChangeListener()
    }

    override fun onResume() {
        super.onResume()
        employeeRFIDEditText.requestFocus()
    }

    private fun setEmployeeRFIDTextChangeListener() {
        employeeRFIDEditText.addTextChangedListener (object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                onRFIDScanned()
            }
        })
    }

    private fun onRFIDScanned() {
        val employeeRFID = employeeRFIDEditText.text.toString()
        if (employeeRFID.length == 10) {
            if (!EmployeeDetails.RFIDToNameHashMap.containsKey(employeeRFID)) {
                Toast.makeText(this, "Employee RFID is not available in database", Toast.LENGTH_SHORT).show()
            } else {
                val currentDateValue = dateTextView.text.toString()
                val currentTimeValue = timeTextView.text.toString().substring(0, 5)

                filePath =
                    this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath.toString()
                val file = File(filePath, "Timesheet.xlsx")

                if (!file.exists()) {
                    val workbook = XSSFWorkbook()
                    val fileOut = FileOutputStream(file)
                    workbook.write(fileOut)
                    fileOut.close()
                    workbook.close()
                }

                val workbook = XSSFWorkbook(FileInputStream(file))

                val sheetName =
                    EmployeeDetails.RFIDToNameHashMap[employeeRFIDEditText.text.toString()]

                var sheet = workbook.getSheet(sheetName)

                if (sheet == null) {
                    sheet = workbook.createSheet(sheetName)

                    val row = sheet.createRow(0)
                    row.createCell(DATE_IN_COLUMN).setCellValue("Date In")
                    row.createCell(DAY_COLUMN).setCellValue("Day")
                    row.createCell(TIME_IN_COLUMN).setCellValue("Time In")
                    row.createCell(DATE_OUT_COLUMN).setCellValue("Date Out")
                    row.createCell(TIME_OUT_COLUMN).setCellValue("Time Out")
                    row.createCell(LOG_HOURS_COLUMN).setCellValue("Log hours")
                    row.createCell(REGULAR_WORKING_HOURS_COLUMN)
                        .setCellValue("Regular working hours")
                    row.createCell(OT_HOURS_COLUMN).setCellValue("Ot hours")

                    row.createCell(REGULAR_WORKING_HOURS_TOTAL_COLUMN)
                        .setCellValue("Regular working hours total")
                    row.createCell(OT_HOURS_TOTAL_COLUMN).setCellValue("Ot hours total")
                }

                val timeMode = timeModeTextView.text.toString()
                when (timeMode) {
                    TIME_IN_MODE -> {
                        val dateInValue = currentDateValue
                        val timeInValue = currentTimeValue
                        val row = sheet.createRow(sheet.lastRowNum + 1)
                        row.createCell(DATE_IN_COLUMN).setCellValue(dateInValue)
                        row.createCell(DAY_COLUMN).setCellValue(dayOfWeek.name)
                        row.createCell(TIME_IN_COLUMN).setCellValue(timeInValue)
                    }

                    TIME_OUT_MODE -> {
                        val row = sheet.getRow(sheet.lastRowNum)
                        val dateInValue = row.getCell(DATE_IN_COLUMN).stringCellValue
                        val timeInValue = row.getCell(TIME_IN_COLUMN).stringCellValue
                        val dateOutValue = currentDateValue
                        val timeOutValue = currentTimeValue

                        if (sheet.lastRowNum == 0 || timeInValue.isNullOrEmpty()) {
                            // TODO -> if no check-in detected
                            val dialogFragment = DialogFragment(R.layout.check_in_dialog_fragment)
                            dialogFragment.show(supportFragmentManager, null)
                            return
                        }

                        row.createCell(DATE_OUT_COLUMN).setCellValue(dateOutValue)
                        row.createCell(TIME_OUT_COLUMN).setCellValue(timeOutValue)

                        val logTime = calculateTimeDifference(
                            dateInValue,
                            timeInValue,
                            dateOutValue,
                            timeOutValue
                        )
                        val regularWorkingTime =
                            if (dayOfWeek == DayOfWeek.SUNDAY)
                                "00:00"
                            else if (logTime.substring(0, 2).toInt() < 8)
                                "${logTime.substring(0, 2)}:00"
                            else
                                "08:00"

                        val logHours = logTime.substring(0, 2).toInt()
                        val otHours = if (logHours > 8) {
                            calculateTimeDifference(regularWorkingTime, logTime)
                        } else if (logHours < 8) {
                            calculateTimeDifference("08:00", regularWorkingTime)
                        } else {
                            "00:00"
                        }

                        row.createCell(LOG_HOURS_COLUMN).setCellValue(logTime)
                        row.createCell(REGULAR_WORKING_HOURS_COLUMN)
                            .setCellValue(regularWorkingTime)
                        row.createCell(OT_HOURS_COLUMN).setCellValue(otHours)
                    }
                }


                val fileOut = FileOutputStream(file)
                workbook.write(fileOut)
                fileOut.close()

                workbook.close()

//            Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show()
                responseImageView.setImageResource(R.drawable.green_tick)

                employeeRFIDEditText.text.clear()
            }
        }
    }

    private fun setDateTimeObserver() {
        val dateTimeObserver = Observer<LocalDateTime> {
            val formattedDate = it.format(dateFormatter)
            val formattedTime = it.format(timeFormatter)

            dayOfWeek = it.dayOfWeek
            dateTextView.text = formattedDate
            timeTextView.text = formattedTime
        }
        dateTimeLiveData.observe(this, dateTimeObserver)
    }

    // Minutes is not rounded-off
    private fun calculateTimeDifference(dateIn: String, timeIn: String, dateOut: String, timeOut: String): String {
        val dateInArray = dateIn.split("-")
        val timeInArray = timeIn.split(":")
        val dateOutArray = dateOut.split("-")
        val timeOutArray = timeOut.split(":")

        val dateTimeIn = LocalDateTime.of(dateInArray[2].toInt(), dateInArray[1].toInt(), dateInArray[0].toInt(), timeInArray[0].toInt(), timeInArray[1].toInt())
        val dateTimeOut = LocalDateTime.of(dateOutArray[2].toInt(), dateOutArray[1].toInt(), dateOutArray[0].toInt(), timeOutArray[0].toInt(), timeOutArray[1].toInt())

        val duration = Duration.between(dateTimeIn, dateTimeOut)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return String.format("%02d:%02d",hours, minutes)
    }

    // minutes is set to zero, after finding difference
    private fun calculateTimeDifference(timeIn: String, timeOut:String): String {
        val startTimeArray = timeIn.split(":")
        val endTimeArray = timeOut.split(":")

        val startHours = startTimeArray[0].toInt()
        val startMinutes = startTimeArray[1].toInt()
        val endHours = endTimeArray[0].toInt()
        val endMinutes = endTimeArray[1].toInt()

        val startTime = startHours * 60 + startMinutes
        val endTime = endHours * 60 + endMinutes

        val diff = endTime - startTime

        var diffHours = diff / 60
        var diffMinutes = diff % 60

        if (diffMinutes > 40) {
            diffHours += 1
        }
        diffMinutes = 0

        return String.format("%02d:%02d",diffHours, diffMinutes)
    }

    private fun initViews() {
        timeModeTextView = findViewById(R.id.time_mode_textview)
        dateTextView = findViewById(R.id.date_value)
        timeTextView = findViewById(R.id.time_value)
        employeeRFIDEditText = findViewById(R.id.employee_id_value)
        responseImageView = findViewById(R.id.response_image_view)

        setTimeMode(TIME_IN_MODE)
        timeModeTextView.setOnClickListener {
            val timeMode = timeModeTextView.text.toString()
            when (timeMode) {
                TIME_IN_MODE -> setTimeMode(TIME_OUT_MODE)
                TIME_OUT_MODE -> setTimeMode(TIME_IN_MODE)
            }
        }
    }

    private fun setTimeMode(timeMode: String) {
        val backgroundColor = when(timeMode) {
            TIME_IN_MODE -> Color.GREEN
            TIME_OUT_MODE -> Color.RED
            else -> Color.WHITE
        }
        timeModeTextView.setBackgroundColor(backgroundColor)
        timeModeTextView.text = timeMode
    }

}