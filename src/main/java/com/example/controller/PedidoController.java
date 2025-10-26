package com.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.PedidoDTO;
import com.example.DTO.PedidoRequestDTO;
import com.example.model.Pedido;
import com.example.service.PedidoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable int id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader("Authorization") String authorization,
            @Valid @RequestBody PedidoDTO dto) {
        try {
            Pedido creado = pedidoService.crearPedido(dto, authorization);
            return ResponseEntity.status(201).body(creado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@RequestHeader("Authorization") String authorization,
            @PathVariable int id,
            @Valid @RequestBody PedidoDTO dto) {
        try {
            Pedido actualizado = pedidoService.actualizarPedido(id, dto, authorization);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader("Authorization") String authorization,
            @PathVariable int id) {
        try {
            pedidoService.eliminarPedido(id, authorization);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<?> finalizar(@RequestHeader("Authorization") String authorization,
            @PathVariable int id) {
        try {
            Pedido p = pedidoService.finalizarPedido(id, authorization);
            return ResponseEntity.ok(p);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/generar-pedido")
    public ResponseEntity<?> generarPedido(
            @RequestHeader("Authorization") String tokenHeader,
            @Valid @RequestBody PedidoRequestDTO pedidoRequest) {

        // El controlador solo delega la lógica al servicio
        return pedidoService.validarYAutorizarPedido(tokenHeader, pedidoRequest);
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<?> obtenerMisPedidos(
            @RequestHeader("Authorization") String tokenHeader) {
        return pedidoService.obtenerPedidosCliente(tokenHeader);
    }

    @GetMapping("/listar-todos")
    public ResponseEntity<?> listarTodosLosPedidos(
            @RequestHeader("Authorization") String tokenHeader) {

        return pedidoService.listarPedidosAdmin(tokenHeader);
    }

    @PutMapping("/finalizar/{id}")
    public ResponseEntity<?> finalizarPedido(
            @RequestHeader("Authorization") String tokenHeader,
            @PathVariable("id") int pedidoId) {

        return pedidoService.finalizarPedido(tokenHeader, pedidoId);
    }

}