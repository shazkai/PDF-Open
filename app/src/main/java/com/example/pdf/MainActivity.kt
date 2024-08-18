package com.example.pdf

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdf.databinding.ActivityMainBinding
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var btnCapture: Button
    private lateinit var btnCreatePDF: Button
    private lateinit var recyclerView: RecyclerView
    private val capturedImages = mutableListOf<File>()

    // Using ActivityResultLauncher for the camera
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI elements
        btnCapture = binding.btnCapture
        btnCreatePDF = binding.btnCreatePDF
        recyclerView = binding.recyclerView

        // Set up RecyclerView (layout manager and adapter)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ImageAdapter(capturedImages)

        // Initialize ActivityResultLauncher
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageBitmap = data?.extras?.getParcelable<Bitmap>("data")
                imageBitmap?.let {
                    val imageFile = saveBitmapToFile(it)
                    if (imageFile != null) {
                        capturedImages.add(imageFile)
                        recyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        // Set up button click listeners
        btnCapture.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        btnCreatePDF.setOnClickListener {
            createPDF(capturedImages)
        }
    }

    private fun checkCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is required to capture documents.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startForResult.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        val imageFile = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        return try {
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            imageFile
        } catch (e: IOException) {
            Log.e("MainActivity", "Error saving image file", e)
            null
        }
    }

    private fun createPDF(images: List<File>) {
        if (images.isEmpty()) {
            Toast.makeText(this, "No images captured to create a PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfFile = File(externalMediaDirs.first(), "document.pdf")
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            for (image in images) {
                val img = Image.getInstance(image.absolutePath)
                img.scaleToFit(PageSize.A4.width, PageSize.A4.height)
                document.add(img)
            }

            document.close()
            Toast.makeText(this, "PDF created: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating PDF", e)
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
