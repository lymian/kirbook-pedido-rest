package com.example.DTO;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoDTO {
    // No id al crear
    @NotNull(message = "clienteId es requerido")
    private int clienteId;

    private String estado; // opcional, por defecto "pendiente"
    private List<DetallePedidoDTO> detalles;
}