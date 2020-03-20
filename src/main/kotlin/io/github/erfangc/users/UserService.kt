package io.github.erfangc.users

import org.springframework.stereotype.Service

@Service
class UserService {
    fun getUser(): User {
        return User(
                id = "erfangc",
                address = "8710 51St Ave",
                email =  "erfangc@gmail.com",
                firmName = "Self Employed",
                firstName = "Erfang",
                lastName = "Chen",
                settings = Settings(
                        whiteList = listOf(
                                WhiteListItem("AGG"),
                                WhiteListItem("BNDX"),
                                WhiteListItem("VTV"),
                                WhiteListItem("VOE"),
                                WhiteListItem("VBR"),
                                WhiteListItem("MUB"),
                                WhiteListItem("EMB")
                        )
                )
        )
    }
    fun saveUser(user: User): User {
        TODO()
    }
}
