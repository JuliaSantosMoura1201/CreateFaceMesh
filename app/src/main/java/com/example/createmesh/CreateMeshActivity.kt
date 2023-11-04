package com.example.createmesh

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.bumptech.glide.Glide
import com.example.face2face.databinding.ActivityMainBinding
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

@ExperimentalGetImage
class CreateMeshActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private val viewModel: CreateMeshViewModel = CreateMeshViewModel()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_PERMISSION)
        }


        processCurrentImage(currentImage = 0)

        viewModel.success.observe(this){
            Log.i("Current Image - success ", it.toString())
            processCurrentImage(it)
        }

        viewModel.failure.observe(this){
            Log.i("Current Image - failure", it.first.toString())
            Log.i("Current Image - failure", it.second.message.orEmpty())
            processCurrentImage(it.first)
        }

        viewModel.jsonFile.observe(this){
            Log.i("Json - create mesh", it)
            saveJsonToDownloadsDirectory(applicationContext, it, "data_set")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, você pode acessar a pasta
            } else {
                // Permissão negada, lide com isso de acordo com as necessidades do seu aplicativo
            }
        }
    }

    private fun processCurrentImage(currentImage: Int){
        val imageName = "user$currentImage.png"
        viewBinding.textView.text = imageName
        val inputStream = applicationContext.assets.open(imageName)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        Glide.with(this)
            .load(bitmap)
            .into(viewBinding.imageView)
        viewModel.addToDataSet(bitmap, currentImage)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveJsonToDownloadsDirectory(context: Context, json: String, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { outputStream ->
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(json.toByteArray())
            }
            Toast.makeText(context, "JSON file saved to Downloads directory", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to save JSON file", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE_PERMISSION = 1
    }

}