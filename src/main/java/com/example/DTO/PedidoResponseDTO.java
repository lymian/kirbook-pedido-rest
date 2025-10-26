package com.example.DTO;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PedidoResponseDTO {
    private int id;
    private LocalDateTime fecha;
    private String estado;
    private Double total;

    private ClienteSOAPDTO cliente; // obtenido del servicio SOAP
    private List<ItemPedidoResponseDTO> items; // detalle del pedido
}