package com.example.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.DTO.LibroDTO;

@FeignClient(name = "libroClient", url = "http://localhost:8082/libros") // URL del libro_service
public interface LibroClient {

    @GetMapping("/{id}")
    LibroDTO obtenerLibroPorId(@PathVariable("id") int id);
}