package com.example.createmesh

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.face2face.R
import com.example.face2face.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

@ExperimentalGetImage
class CreateMeshActivity : AppCompatActivity(), FaceMeshListener {

    private val faceMeshAnalyzer = FaceMeshAnalyzer()
    private val gson = Gson()
    private val faces: MutableList<List<FaceMesh>> = mutableListOf()
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceMeshAnalyzer.faceMeshListener = this


        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val assetManager = applicationContext.assets

        for(i in 0 until AMOUNT_OF_FACES){
            val inputStream = assetManager.open("user$i.png")
            val inputImage = inputStream.toInputImage()
            Glide.with(this)
                .load(inputImage)
                .into(viewBinding.imageView)
            faceMeshAnalyzer.process(inputImage)
        }

    }

    private fun InputStream.toInputImage(): InputImage {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (read(buffer).also { bytesRead = it } != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead)
        }
        val byteArray = byteArrayOutputStream.toByteArray()
        return InputImage.fromByteBuffer(ByteBuffer.wrap(byteArray), 338, 601,   0, InputImage.IMAGE_FORMAT_NV21)
    }
    override fun onSuccess(faceMeshes: List<FaceMesh>) {
        faces.add(faceMeshes)
        if (faces.size == AMOUNT_OF_FACES){
            val jsonFile = gson.toJson(faces)
            Log.i("Face mesh json", jsonFile)
        }
        Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
    }

    override fun onFailure() {
        Toast.makeText(applicationContext, "Failure", Toast.LENGTH_SHORT).show()
    }

    companion object{
        private const val AMOUNT_OF_FACES = 25
    }
}