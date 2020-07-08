package com.example.colorpaint

import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children

import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileOutputStream
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {
    private val WRITE_REQUEST_CODE = 1300
    private var imageTitle = "MyDrawing"
    private var buttonsVisible: Boolean = false

    private lateinit var mSizeBar: SeekBar
    private lateinit var mPaintView: PaintView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mPaintView = findViewById(R.id.paintView)

        // init canvas size to match display
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        mPaintView.initDisplay(displayMetrics)

        //init button visibility
        setOverlayVisibility(false)

        //seeker init
        mSizeBar = findViewById(R.id.sizeBar)
        mSizeBar.progress =  mPaintView.brushSize.toInt()
        mSizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                mPaintView.brushSize = i.toFloat()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        //palette init
        val paletteLayout = findViewById<GridLayout>(R.id.paletteLayout)
        for (childView in paletteLayout.children) {
            childView.setOnClickListener(object: View.OnClickListener {
                override fun onClick(view: View?) {
                    val color = (childView as ImageView).imageTintList?.defaultColor
                    if (color != null) mPaintView.brushColor = color
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.paint_menu, menu)
        return true
    }

    fun toggleButtonOverlay(view: View) {
        setOverlayVisibility(!buttonsVisible)
    }

    private fun setOverlayVisibility(boolean: Boolean) {
        buttonsVisible = boolean
        val button1 = findViewById<FloatingActionButton>(R.id.brushButton1)
        val button2 = findViewById<FloatingActionButton>(R.id.brushButton2)
        val button3 = findViewById<FloatingActionButton>(R.id.brushButton3)

        val sliderGroup = findViewById<View>(R.id.sliderLayout)
        val paletteGroup = findViewById<View>(R.id.paletteLayout)

        if (buttonsVisible) {
            sliderGroup.visibility = View.VISIBLE
            paletteLayout.visibility = View.VISIBLE
            button1.show()
            button2.show()
            button3.show()
        } else {
            sliderGroup.visibility = View.GONE
            paletteGroup.visibility = View.GONE
            button1.hide()
            button2.hide()
            button3.hide()
        }
    }

    fun setMarker(view: View) {
        mPaintView.brushType = PaintView.TYPE_MARKER
    }

    fun setPencil(view: View) {
        mPaintView.brushType = PaintView.TYPE_PENCIL
    }

    fun setPen(view: View) {
        mPaintView.brushType = PaintView.TYPE_PEN
    }

    fun undo(menuItem: MenuItem) {
        mPaintView.undo()
    }

    fun redo(menuITem: MenuItem) {
        mPaintView.redo()
    }

    fun clear(menuItem: MenuItem) {
        mPaintView.clear()
    }

    fun save(menuItem: MenuItem) {
        val contentLayout = layoutInflater.inflate(R.layout.text_input_layout, null)
        val editText = contentLayout.findViewById<EditText>(R.id.save_name_text_box)

        val alertDialog = AlertDialog.Builder(this, R.style.AlertMenu)
            .setView(contentLayout)
            .setPositiveButton("OK", object: DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    imageTitle = editText.text.toString()
                    dialogInterface.dismiss()
                    if (checkPermissions()) saveAs(imageTitle)
                }
            })
            .setNegativeButton("CANCEL", object: DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    dialogInterface.dismiss();
                }
            })
            .create()
        alertDialog.show()

    }

    private fun checkPermissions(): Boolean {
        // if permission to read and write is not given request it
        val writePermission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE),
                WRITE_REQUEST_CODE)

            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WRITE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveAs(imageTitle)
                } else {
                    val toast = Toast.makeText(this,
                        "permissions denied, you must enable permissions to save", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    private fun saveAs(title: String) {
        val mediaValues = ContentValues()
        mediaValues.put(MediaStore.Images.Media.TITLE, "$title.jpg")
        mediaValues.put(MediaStore.Images.Media.DISPLAY_NAME, "$title.jpg")
        mediaValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        val time = System.currentTimeMillis() / 1000
        mediaValues.put(MediaStore.Images.Media.DATE_ADDED, time)
        mediaValues.put(MediaStore.Images.Media.DATE_MODIFIED, time)

        val cr = contentResolver
        val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaValues)
        val bmp = mPaintView.mBitmap

        if (uri != null) {
            val out = cr.openOutputStream(uri)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            val toast = Toast.makeText(applicationContext, "Image Saved", Toast.LENGTH_SHORT)
            toast.show()
        } else {
            val toast = Toast.makeText(applicationContext, "Failed to Save Image", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

}
