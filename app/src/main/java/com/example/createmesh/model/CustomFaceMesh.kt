package com.example.createmesh.model

import com.google.mlkit.vision.facemesh.FaceMesh
import java.io.Serializable

class CustomFaceMesh(fileName: String, faceMesh: FaceMesh, isFraud: Boolean) {
    val fileName: String
    val allPoints: MutableMap<Int, Point> = mutableMapOf()
    val isFraud: Boolean

    init {
        this.fileName = fileName
        this.isFraud = isFraud

        faceMesh.allPoints.map { faceMeshPoint ->
            this.allPoints[faceMeshPoint.index] = Point(
                faceMeshPoint.position.x,
                faceMeshPoint.position.y,
                faceMeshPoint.position.z
            )
        }
    }
}

data class Point(
    val x: Float,
    val y: Float,
    val z: Float
) : Serializable