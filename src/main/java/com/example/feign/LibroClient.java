package com.example.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.DTO.LibroDTO;

@FeignClient(name = "libroClient", url = "http://localhost:8082/libros") // URL del libro_service
public interface LibroClient {

    @GetMapping("/{id}")
    LibroDTO obtenerLibroPorId(@PathVariable("id") int id);

    @PutMapping("/actualizar/{id}")
    ResponseEntity<?> actualizarLibro(
            @RequestHeader("Authorization") String token,
            @PathVariable int id,
            @RequestBody LibroDTO libroDTO);

    @PutMapping("/restar-stock/{id}/{cantidad}")
    ResponseEntity<Void> restarStock(
            @PathVariable("id") int id,
            @PathVariable("cantidad") int cantidad);

}