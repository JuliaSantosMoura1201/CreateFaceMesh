package com.example.createmesh

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.core.net.toUri
import com.example.face2face.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh

@ExperimentalGetImage
class CreateMeshActivity : AppCompatActivity(), FaceMeshListener {

    private val faceMeshAnalyzer = FaceMeshAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceMeshAnalyzer.faceMeshListener = this

        setContentView(R.layout.activity_main)

        val uri = "C:\\Users\\julia\\AndroidStudioProjects\\CreateMesh\\app\\src\\main\\assets\\user035_001.png".toUri()
        InputImage.fromFilePath(applicationContext, )
//            .let { image ->
//            faceMeshAnalyzer.process(image)
//        }
    }

    override fun onStart() {


    }



    override fun onSuccess(faceMeshes: List<FaceMesh>) {
        Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
    }

    override fun onFailure() {
        Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
    }
}