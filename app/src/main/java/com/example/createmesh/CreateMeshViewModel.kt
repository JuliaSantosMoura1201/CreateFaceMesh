package com.example.createmesh

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.createmesh.model.CustomFaceMesh
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class CreateMeshViewModel: ViewModel() {

    private val detector = FaceMeshDetection.getClient()
    private val gson = Gson()

    var customFaceMeshList: MutableList<CustomFaceMesh> = mutableListOf()

    private val _success: MutableLiveData<Int> = MutableLiveData()
    val success: LiveData<Int> = _success

    private val _failure: MutableLiveData<Pair<Int, Exception>> = MutableLiveData()
    val failure: LiveData<Pair<Int, Exception>> = _failure

    private val _jsonFile: MutableLiveData<String> = MutableLiveData()
    val jsonFile: LiveData<String> = _jsonFile

    private fun process(image: InputImage, nextImage: Int, fileName: String, isFraud: Boolean) {
        detector.process(image)
            .addOnSuccessListener{ handleSuccess(it, nextImage, fileName, isFraud) }
            .addOnFailureListener { handleFailure(it, nextImage) }
    }

    private fun handleSuccess(faceMeshes: List<FaceMesh>, nextImage: Int, fileName: String, isFraud: Boolean){
        customFaceMeshList.addAll(faceMeshes.map { faceMesh ->
            CustomFaceMesh(fileName, faceMesh, isFraud) }
        )

        if(nextImage == AMOUNT_OF_FACES){
            _jsonFile.value = gson.toJson(customFaceMeshList)
        } else{
            _success.value = nextImage
        }
    }

    private fun handleFailure(exception: Exception, nextImage: Int){
        if(nextImage == AMOUNT_OF_FACES){
            _jsonFile.value = gson.toJson(customFaceMeshList)
        } else{
            _failure.value = Pair(nextImage, exception)
        }
    }

    fun addToDataSet(bitmap: Bitmap, currentImage: Int, fileName: String, isFraud: Boolean = false){
        viewModelScope.launch {
            delay(100L)
            process(InputImage.fromBitmap(bitmap, 0), currentImage + 1, fileName, isFraud)
        }
    }

    companion object{
        private const val AMOUNT_OF_FACES = 25
    }
}