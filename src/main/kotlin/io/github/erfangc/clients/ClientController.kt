package io.github.erfangc.clients

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/clients")
class ClientController(private val clientService: ClientService) {
    @GetMapping("{id}")
    fun getClient(@PathVariable id: String): Client? {
        return clientService.getClient(id)
    }
    @PostMapping
    fun saveClient(@RequestBody client: Client): Client {
        return clientService.saveClient(client)
    }
    @DeleteMapping("{id}")
    fun deleteClient(@PathVariable id: String): Client? {
        return clientService.deleteClient(id)
    }
}