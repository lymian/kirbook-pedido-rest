package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LibroDTO {
    private int id;
    private String titulo;
    private String sinopsis;
    private String autor;
    private String categoria;
    private String fechaPublicacion;
    private double precio;
    private double descuento;
    private int stock;
    private boolean estado;
}