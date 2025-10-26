package com.example.DTO;

import lombok.Data;

@Data
public class ClienteSOAPDTO {
    private Long id;
    private String username;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
}