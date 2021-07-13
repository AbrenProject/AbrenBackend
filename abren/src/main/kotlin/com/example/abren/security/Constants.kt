package com.example.abren.security

class Constants {
    val ACCESS_TOKEN_VALIDITY_SECONDS = (5 * 60 * 60).toLong()
    var SIGNING_KEY : String = "abre13n"
    val TOKEN_PREFIX = "Bearer "
    val HEADER_STRING = "Authorization"
    val AUTHORITIES_KEY = "abre13nauthorities"

}