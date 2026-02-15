package com.biswariapps.pdfgenerator


import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    //view variables
    lateinit var main: RelativeLayout
    lateinit var date: EditText
    lateinit var markername: EditText
    lateinit var vehicle: EditText
    lateinit var buyer: EditText
    lateinit var loadrate: EditText
    lateinit var weight: EditText
    lateinit var rate: EditText
    lateinit var billamt: EditText
    lateinit var calculate: Button
    lateinit var pdfgen: Button
    //output variables
    lateinit var loadcost: TextView
    lateinit var gst: TextView
    lateinit var cash: TextView
    lateinit var totalcost: TextView

    //pdf function dimensions
    var pageHeight = 1120
    var pageWidth = 792

    //datepicker
    lateinit var datePickerDialog: DatePickerDialog

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if(Environment.isExternalStorageManager()){
            //permission granted
        } else{
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:" + packageName)
            startActivity(intent)
        }

        main = findViewById(R.id.main)
        date = findViewById(R.id.datepick)
        markername = findViewById(R.id.nameedit)
        vehicle = findViewById(R.id.vehicleedit)
        buyer = findViewById(R.id.buyeredit)
        loadrate = findViewById(R.id.loadrateedit)
        weight = findViewById(R.id.weightedit)
        rate = findViewById(R.id.rateedit)
        billamt = findViewById(R.id.billamtedit)
        calculate = findViewById(R.id.calbutton)
        pdfgen = findViewById(R.id.pdfbutton)

        loadcost = findViewById(R.id.loadingedit)
        gst = findViewById(R.id.gstedit)
        cash = findViewById(R.id.cashedit)
        totalcost = findViewById(R.id.totalcedit)

        loadrate.setText("200")
        setdate()

        main.setOnClickListener(){
            closekeypad(main)
        }

        date.setOnClickListener(){
            datePickerDialog.show()
            date.error = null
        }

        calculate.setOnClickListener(){
            var fieldcheckflag: Boolean = checkallfields()
            if(fieldcheckflag){
                calculate()
            }
        }

        pdfgen.setOnClickListener(){
            var fieldcheckflag: Boolean = checkallfields()
            if(fieldcheckflag){
                calculate()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    generatepdf()
                }
            }
        }
    }

    private fun closekeypad(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setdate() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        datePickerDialog = DatePickerDialog(
            this,
            { view, year, monthOfYear, dayOfMonth ->
                date.setText(dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
            },
            year,
            month,
            day
        )
    }

    private fun calculate(){
        var loadcost1 = weight.getText().toString().toInt() * loadrate.getText().toString().toInt()
        var gst1 = (billamt.getText().toString().toInt()/1.12)*0.12
        var cash1 = weight.getText().toString().toInt() * rate.getText().toString().toInt() + gst1.toDouble() - billamt.getText().toString().toInt()
        var totalc = weight.getText().toString().toInt() * rate.getText().toString().toInt()
        loadcost.setText(loadcost1.toString())
        gst.setText(String.format("%.2f", gst1))
        cash.setText(String.format("%.2f", cash1))
        totalcost.setText(totalc.toString())
    }

    private fun checkallfields(): Boolean {
        var flag: Boolean = true
        if(date.length() == 0){
            date.error = "This field is Required"
            flag = false
        }
        if(markername.length() == 0){
            markername.error = "This field is Required"
            flag = false
        }
        if(vehicle.length() == 0){
            vehicle.error = "This field is Required"
            flag = false
        }
        if(buyer.length() == 0){
            buyer.error = "This field is Required"
            flag = false
        }
        if(loadrate.length() == 0){
            loadrate.error = "This field is Required"
            flag = false
        }
        if(weight.length() == 0){
            weight.error = "This field is Required"
            flag = false
        }
        if(rate.length() == 0){
            rate.error = "This field is Required"
            flag = false
        }
        if(billamt.length() == 0){
            billamt.error = "This field is Required"
            flag = false
        }
        return flag
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun generatepdf() {
        var pdfDocument: PdfDocument = PdfDocument()

        // for adding text in our PDF file.
        var title: Paint = Paint()

        // we are adding page info to our PDF file
        // in which we will be passing our pageWidth,
        // pageHeight and number of pages and after that
        // we are calling it to create our PDF.
        var myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // below line is used for setting
        // start page for our PDF file.
        var myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)

        // creating a variable for canvas
        // from our page of PDF.
        var canvas: Canvas = myPage.canvas


        // below line is used for adding typeface for
        // our text which we will be adding in our PDF file.
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))

        // below line is used for setting text size
        // which we will be displaying in our PDF file.
        title.textSize = 15F

        // below line is sued for setting color
        // of our text inside our PDF file.
        title.setColor(ContextCompat.getColor(this, R.color.black))

        // below line is used to draw text in our PDF file.
        // the first parameter is our text, second parameter
        // is position from start, third parameter is position from top
        // and then we are passing our variable of paint which is title.
        canvas.drawText("Receipt", 20F, 80F, title)
        canvas.drawText("1. Date : " + date.text.toString(), 20F, 100F, title)
        canvas.drawText("2. Marker Name : " + markername.text.toString(), 20F, 120F, title)
        canvas.drawText("3. Vehicle Number : " + vehicle.text.toString(), 20F, 140F, title)
        canvas.drawText("4. Buyer Name : " + buyer.text.toString(), 20F, 160F, title)
        canvas.drawText("5. Weight : " + weight.text.toString(), 20F, 200F, title)
        canvas.drawText("6. Rate (per Ton) : " + rate.text.toString(), 20F, 220F, title)
        canvas.drawText("7. Total Cost : " + totalcost.text.toString(), 20F, 260F, title)
        canvas.drawText("8. GST : " + gst.text.toString(), 20F, 280F, title)
        canvas.drawText("9. Bill Amount : " + billamt.text.toString(), 20F, 300F, title)
        canvas.drawText("10. Cash Amount : " + cash.text.toString(), 20F, 320F, title)
        canvas.drawText("11. Loading Cost : " + loadcost.text.toString(), 20F, 340F, title)
        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        title.setColor(ContextCompat.getColor(this, R.color.black))
        title.textSize = 20F

        // below line is used for setting
        // our text to center of PDF.

        // after adding all attributes to our
        // PDF file we will be finishing our page.
        pdfDocument.finishPage(myPage)

        // below line is used to set the name of
        // our PDF file and its path.
        val docsfolder = Environment.getExternalStorageDirectory()
        if(!docsfolder.exists()){
            docsfolder.mkdir()
        }
        val file: File = File(docsfolder.absolutePath , "Receipt_" + markername.text + "_" + date.text + ".pdf")

        try {
            // after creating a file name we will
            // write our PDF file to that location.
            pdfDocument.writeTo(FileOutputStream(file))
            // on below line we are displaying a toast message as PDF file generated..
            Toast.makeText(applicationContext, "PDF file generated..", Toast.LENGTH_SHORT).show()
            try {
                val connection: Boolean = isNetworkAvailable()
                if(connection){
                    insertdatatoonlinesheet()
                } else{
                    Toast.makeText(applicationContext, "Internet Connection is not working..", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            clearallfields()
        } catch (e: Exception) {
            // below line is used
            // to handle error
            e.printStackTrace()
            // on below line we are displaying a toast message as fail to generate PDF
            Toast.makeText(applicationContext, "Fail to generate PDF file..", Toast.LENGTH_SHORT)
                .show()
        }
        // after storing our pdf to that
        // location we are closing our PDF file.
        pdfDocument.close()

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val connected = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!
            .state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED)

        return connected
    }

    private fun insertdatatoonlinesheet() {
        //https://script.google.com/macros/s/AKfycbxNerxPp40jlY4yxyEieizv0MODrD9bEVBXdT6h6Pee-8brzxDw05eDT9btWZRPfg/exec
        var url: String = "https://script.google.com/macros/s/AKfycbxNerxPp40jlY4yxyEieizv0MODrD9bEVBXdT6h6Pee-8brzxDw05eDT9btWZRPfg/exec?"

        val mname: String = markername.text.toString().replace(" ", "_")
        val bname: String = buyer.text.toString().replace(" ", "_")
        url = url+"action=create&date="+date.text+"&markername="+mname+"&vehicle="+vehicle.text+"&buyer="+bname+"&loadrate="+loadrate.text+
                "&weight="+weight.text+"&rate="+rate.text+"&billamt="+billamt.text+"&loadcost="+loadcost.text+"&gst="+gst.text+"&cash="+cash.text+"&totalcost="+totalcost.text

        Thread {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val res = client.newCall(request).execute()
            runOnUiThread {
                // Post the result to the main thread
                Toast.makeText(applicationContext, res.body?.string() , Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun clearallfields() {
        markername.text = null
        vehicle.text = null
        buyer.text = null
        weight.text = null
        rate.text = null
        billamt.text = null
        loadcost.text = null
        gst.text = null
        cash.text = null
        totalcost.text = null
    }

}