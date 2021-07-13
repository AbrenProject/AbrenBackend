package com.example.abren.models

data class VehicleInformation(
    var year: String,
    var make: String,
    var model: String,
    var licensePlateNumber: String,
    var vehiclePictureUrl: String?,
    var licenseUrl: String?,
    var ownershipDocUrl: String?,
    var insuranceDocUrl: String?
)