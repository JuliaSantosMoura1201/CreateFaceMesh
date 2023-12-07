package com.example.createmesh

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.face2face.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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

        viewModel.obterTokenDeAcesso(applicationContext)


        viewBinding.listFiles.setOnClickListener {
            viewModel.googleDriveList.clear()
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.getFilesListFromGoogleDrive("1ld0h6bCw73PoAfcqCdQtNssIvYsVZuYk")
            }
        }

        viewBinding.runRoutine.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.downloadAndProcessImages()
            }
        }

        viewBinding.stopRoutine.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.shouldStopRoutine = true
            }
        }

        viewModel.currentImage.observe(this) {
            Glide.with(this)
                .load(it)
                .into(viewBinding.imageView)
        }

        viewModel.jsonFile.observe(this){
            Log.i("Json - create mesh", it)
//            saveJsonToDownloadsDirectory(applicationContext, it, "data_set")
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

    companion object {
        private const val REQUEST_WRITE_STORAGE_PERMISSION = 1
    }

}