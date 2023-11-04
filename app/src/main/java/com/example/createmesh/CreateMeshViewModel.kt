package com.example.createmesh

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val _facesLiveData: MutableLiveData<MutableList<List<FaceMesh>>> = MutableLiveData(
        mutableListOf()
    )
    private val _success: MutableLiveData<Int> = MutableLiveData()
    val success: LiveData<Int> = _success

    private val _failure: MutableLiveData<Pair<Int, Exception>> = MutableLiveData()
    val failure: LiveData<Pair<Int, Exception>> = _failure

    private val _jsonFile: MutableLiveData<String> = MutableLiveData()
    val jsonFile: LiveData<String> = _jsonFile

    private fun process(image: InputImage, nextImage: Int) {
        detector.process(image)
            .addOnSuccessListener{ handleSuccess(it, nextImage) }
            .addOnFailureListener { handleFailure(it, nextImage) }
    }

    private fun handleSuccess(faceMeshes: List<FaceMesh>, nextImage: Int){
        _facesLiveData.value?.add(faceMeshes)

        if(nextImage == AMOUNT_OF_FACES){
            _jsonFile.value = gson.toJson(_facesLiveData.value)
        } else{
            _success.value = nextImage
        }
    }

    private fun handleFailure(exception: Exception, nextImage: Int){
        if(nextImage == AMOUNT_OF_FACES){
            _jsonFile.value = gson.toJson(_facesLiveData.value)
        } else{
            _failure.value = Pair(nextImage, exception)
        }
    }

    fun addToDataSet(bitmap: Bitmap, currentImage: Int){
        viewModelScope.launch {
            delay(1000L)
            process(InputImage.fromBitmap(bitmap, 0), currentImage + 1)
        }
    }

    companion object{
        private const val AMOUNT_OF_FACES = 25
    }
}