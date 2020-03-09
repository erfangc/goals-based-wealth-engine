package io.github.erfangc.users

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/users")
class UserController(private val userService: UserService) {
    @GetMapping("current-user")
    fun user(): User {
        return userService.getUser()
    }
    @PostMapping("{id}")
    fun saveUser(@PathVariable id: String, @RequestBody user: User): User {
        return userService.saveUser(user.copy(id = id))
    }
}