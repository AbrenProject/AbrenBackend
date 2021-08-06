package com.example.abren.security

import com.example.abren.configurations.Constants
import com.example.abren.models.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*

@Component
class TokenProvider : Serializable {
    private val constants = Constants()

    fun getPhoneNumberFromToken(token: String?): String {
        val claims = getAllClaimsFromToken(token)
        return (Claims::getSubject)(claims)
    }

    fun getExpirationDateFromToken(token: String?): Date {
        val claims = getAllClaimsFromToken(token)
        return (Claims::getExpiration)(claims)
    }

    fun getAllClaimsFromToken(token: String?): Claims {
        return Jwts.parser()
            .setSigningKey(constants.SIGNING_KEY)
            .parseClaimsJws(token)
            .body
    }

    fun isTokenExpired(token: String?): Boolean? {
        val expiration: Date = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    fun generateToken(user: User?): String {
        val authorities: List<String?> = List(1) { user?.role }
        return if (user != null) {
            Jwts.builder()
                .setSubject(user.phoneNumber)
                .claim(constants.AUTHORITIES_KEY, authorities)
                .signWith(SignatureAlgorithm.HS256, constants.SIGNING_KEY)
                .setIssuedAt(Date(System.currentTimeMillis()))
                .setExpiration(Date(System.currentTimeMillis() + constants.ACCESS_TOKEN_VALIDITY_SECONDS * 1000)) //TODO: Don't let it expire?
                .compact()
        } else {
            ""
        }
    }

    fun validateToken(token: String?, user: User): Boolean {
        val phoneNumber = getPhoneNumberFromToken(token)
        return phoneNumber == user.phoneNumber && !isTokenExpired(token)!!
    }
}