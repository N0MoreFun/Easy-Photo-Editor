package com.example.photoeditor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {

            } else {
                // Permission denied
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val copiedUri = savePhotoToCache(uri)
                navigateToEditingActivity(copiedUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }

        requestGalleryPermission()
    }

    @Composable
    fun MainScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.roundicon),
                    contentDescription = "Logo",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Easy Photo Editor",
                    style = MaterialTheme.typography.h5,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        openCamera()
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(text = "Сделать фото")
                }
                Button(
                    onClick = {
                        openGallery()
                    }
                ) {
                    Text(text = "Использовать фото из хранилища телефона",
                        textAlign = TextAlign.Center)
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(this@MainActivity, PhotoCaptureActivity::class.java)
        startActivity(intent)
    }

    private fun requestGalleryPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {

            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                // Show permission rationale
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent.type)
    }

    private fun savePhotoToCache(originalUri: Uri): Uri? {
        val inputStream = contentResolver.openInputStream(originalUri)
        val copyFile = File(cacheDir, "copied_photo.jpg")

        try {
            val outputStream = FileOutputStream(copyFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return Uri.fromFile(copyFile)
    }

    private fun navigateToEditingActivity(photoUri: Uri?) {
        val intent = Intent(this@MainActivity, EditPhotoActivity::class.java)
        intent.putExtra("photoUri", photoUri)
        startActivity(intent)
    }
}

