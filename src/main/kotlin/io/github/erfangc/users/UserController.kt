package io.github.erfangc.users

import io.github.erfangc.users.models.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/users")
class UserController(private val userService: UserService) {
    @GetMapping("current-user")
    fun currentUser(): User {
        return userService.currentUser()
    }

    @PostMapping("{id}")
    fun saveUser(@PathVariable id: String, @RequestBody user: User): User {
        return userService.saveUser(user.copy(id = id))
    }

    @PostMapping("_sign-up")
    fun signUp(@RequestBody req: SignUpRequest): SignUpResponse {
        return userService.signUp(req)
    }

    @PostMapping("_sign-in")
    fun signIn(@RequestBody req: SignInRequest): SignInResponse {
        return userService.signIn(req)
    }

    @PostMapping("reset-password-tickets")
    fun createResetPasswordTicket(@RequestBody req: CreateResetPasswordTicketRequest): CreateResetPasswordTicketResponse {
        return userService.createResetPasswordTicket(req)
    }

    @PostMapping("_reset-password")
    fun resetPassword(@RequestBody req: ResetPasswordRequest): ResetPasswordResponse {
        return userService.resetPassword(req)
    }
}