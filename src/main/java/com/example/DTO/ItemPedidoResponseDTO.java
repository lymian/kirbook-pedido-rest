package com.example.DTO;

import lombok.Data;

@Data
public class ItemPedidoResponseDTO {
    private int id; // id del detalle del pedido
    private int cantidad;
    private Double precioUnitario;
    private Double subtotal;

    private LibroDTO libro; // información completa del libro
}