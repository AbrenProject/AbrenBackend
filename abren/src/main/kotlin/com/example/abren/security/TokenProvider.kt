package com.example.abren.security

import com.example.abren.models.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable
import java.util.*


class JwtTokenUtil : Serializable {
    private val constants = Constants()

    fun getPhoneNumberFromToken(token: String?): String {
        val claims = getAllClaimsFromToken(token)
        return (Claims::getSubject)(claims)
    }

    fun getExpirationDateFromToken(token: String?): Date {
        val claims = getAllClaimsFromToken(token)
        return (Claims::getExpiration)(claims)
    }

//    fun <T> getClaimFromToken(token: String?, claimsResolver: () -> T): T {
//        val claims = getAllClaimsFromToken(token)
//        return claimsResolver.apply(claims)
//    }

    private fun getAllClaimsFromToken(token: String?): Claims {
        return Jwts.parser()
            .setSigningKey(constants.SIGNING_KEY)
            .parseClaimsJws(token)
            .body
    }

    private fun isTokenExpired(token: String?): Boolean? {
        val expiration: Date = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    fun generateToken(user: User): String? {
        return doGenerateToken(user.phoneNumber)
    }

    private fun doGenerateToken(subject: String): String? {
        val claims = Jwts.claims().setSubject(subject)
        claims["scopes"] = Arrays.asList(SimpleGrantedAuthority("ROLE_ADMIN"))
        return Jwts.builder()
            .setClaims(claims)
//            .setIssuer("http://devglan.com")
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + constants.ACCESS_TOKEN_VALIDITY_SECONDS * 1000))
            .signWith(SignatureAlgorithm.HS256, constants.SIGNING_KEY)
            .compact()
    }

    fun validateToken(token: String?, user: User): Boolean? {
        val phoneNumber = getPhoneNumberFromToken(token)
        return phoneNumber == user.phoneNumber && !isTokenExpired(token)!!
    }
}