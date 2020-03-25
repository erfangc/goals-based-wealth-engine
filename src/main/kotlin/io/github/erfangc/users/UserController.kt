package io.github.erfangc.users

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/users")
class UserController(private val userService: UserService) {
    @GetMapping("current-user")
    fun user(): User {
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
}