package com.example.abren.configurations

class Constants {
    val ACCESS_TOKEN_VALIDITY_SECONDS = (5 * 60 * 60).toLong()
    var SIGNING_KEY : String = "abre13n"
    val TOKEN_PREFIX = "Bearer "
    val HEADER_STRING = "Authorization"
    val AUTHORITIES_KEY = "abre13nauthorities"
    val REQUIRED_KEYS = arrayListOf("profilePicture", "idCardPicture", "phoneNumber", "emergencyPhoneNumber", "role") //TODO: More Validation for each
    val REQUIRED_DRIVER_KEYS = arrayListOf("drivingLicensePicture", "ownershipDocPicture", "insuranceDocPicture", "vehiclePicture", "licensePlateNumber", "year", "make", "model", "kml")

}