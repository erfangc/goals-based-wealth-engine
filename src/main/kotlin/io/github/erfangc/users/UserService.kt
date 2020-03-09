package io.github.erfangc.users

import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService {
    fun getUser(): User {
        return User(
                id = UUID.randomUUID().toString(),
                address = "8710 51St Ave",
                email =  "erfangc@gmail.com",
                firmName = "Self Employed",
                firstName = "Erfang",
                lastName = "Chen",
                overrides = Overrides(
                        whiteList = listOf(
                                WhiteListItem("BND"),
                                WhiteListItem("BNDX"),
                                WhiteListItem("VTI"),
                                WhiteListItem("VEA"),
                                WhiteListItem("VWO"),
                                WhiteListItem("VXF")
                        )
                )
        )
    }
    fun saveUser(user: User): User {
        TODO()
    }
}
