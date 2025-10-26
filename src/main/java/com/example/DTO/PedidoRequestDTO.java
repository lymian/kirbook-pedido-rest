package com.example.DTO;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PedidoRequestDTO {

    @NotEmpty(message = "El pedido debe contener al menos un detalle.")
    @Valid
    private List<DetallePedidoRequestDTO> detalles;
}
