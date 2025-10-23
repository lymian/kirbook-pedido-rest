package com.example.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.LibroDTO;
import com.example.DTO.PedidoDTO;
import com.example.feign.LibroClient;
import com.example.model.DetallePedido;
import com.example.model.Pedido;
import com.example.repository.IDetallePedidoRepository;
import com.example.repository.IPedidoRepository;
import com.example.soap.AuthService;
import com.kirbook.auth.ValidateTokenResponse;

@Service
public class PedidoService {

	@Autowired
    private IPedidoRepository pedidoRepository;
	
	@Autowired
    private IDetallePedidoRepository detalleRepo;
	
	@Autowired
    private AuthService authService;
	
	@Autowired
    private LibroClient libroClient;

   
	//Listar todos los pedidos
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    //Obtener pedido por ID
    public Pedido obtenerPorId(int id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    //Crear pedido (solo ADMIN)
    public Pedido crearPedido(PedidoDTO dto, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");
        
        //valiadar si existe el usuario
        var userResp = authService.buscarUsuarioPorId((long) dto.getClienteId());
        if (userResp == null || !userResp.isExists()) {
            throw new RuntimeException("El cliente con ID " + dto.getClienteId() + " no existe en el sistema Auth.");
        }

        //Verificar que todos los libros existen antes de continuar
        for (var detalle : dto.getDetalles()) {
            try {
                LibroDTO libro = libroClient.obtenerLibroPorId(detalle.getLibroId());
                if (libro == null) {
                    throw new RuntimeException("Libro con ID " + detalle.getLibroId() + " no existe");
                }
            } catch (Exception e) {
                throw new RuntimeException("Libro con ID " + detalle.getLibroId() + " no existe o no disponible");
            }
        }

        //Crear pedido
        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setEstado(dto.getEstado() == null ? "pendiente" : dto.getEstado());
        pedido.setFecha(LocalDateTime.now());

        //Calcular total y detalles
        double total = 0.0;
        List<DetallePedido> detalles = dto.getDetalles().stream().map(d -> {
            DetallePedido det = new DetallePedido();
            det.setLibroId(d.getLibroId());
            det.setCantidad(d.getCantidad());

            LibroDTO libro = libroClient.obtenerLibroPorId(d.getLibroId());
            if (libro == null) {
                throw new RuntimeException("Libro no encontrado: " + d.getLibroId());
            }

            det.setPrecioUnitario(libro.getPrecio());
            det.setPedido(pedido);
            return det;
        }).collect(Collectors.toList());

        total = detalles.stream()
                .mapToDouble(det -> det.getCantidad() * det.getPrecioUnitario())
                .sum();

        pedido.setTotal(total);
        pedido.setDetalles(detalles);

        return pedidoRepository.save(pedido);
    }

    //Actualizar pedido (solo ADMIN)
    public Pedido actualizarPedido(int id, PedidoDTO dto, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        //Verificar que todos los libros existen antes de continuar
        for (var detalle : dto.getDetalles()) {
            try {
                LibroDTO libro = libroClient.obtenerLibroPorId(detalle.getLibroId());
                if (libro == null) {
                    throw new RuntimeException("Libro con ID " + detalle.getLibroId() + " no existe");
                }
            } catch (Exception e) {
                throw new RuntimeException("Libro con ID " + detalle.getLibroId() + " no existe o no disponible");
            }
        }

        existente.setEstado(dto.getEstado() == null ? existente.getEstado() : dto.getEstado());

        detalleRepo.deleteAll(existente.getDetalles());
        List<DetallePedido> detalles = dto.getDetalles().stream().map(d -> {
            DetallePedido det = new DetallePedido();
            det.setLibroId(d.getLibroId());
            det.setCantidad(d.getCantidad());

            LibroDTO libro = libroClient.obtenerLibroPorId(d.getLibroId());
            if (libro == null) {
                throw new RuntimeException("Libro no encontrado: " + d.getLibroId());
            }

            det.setPrecioUnitario(libro.getPrecio());
            det.setPedido(existente);
            return det;
        }).collect(Collectors.toList());

        double total = detalles.stream()
                .mapToDouble(det -> det.getCantidad() * det.getPrecioUnitario())
                .sum();

        existente.setTotal(total);
        existente.setDetalles(detalles);

        return pedidoRepository.save(existente);
    }

    //Eliminar pedido (solo ADMIN)
    public void eliminarPedido(int id, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");
        pedidoRepository.deleteById(id);
    }

    //Finalizar pedido (solo ADMIN)
    public Pedido finalizarPedido(int id, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");

        Pedido p = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        p.setEstado("finalizado");
        return pedidoRepository.save(p);
    }

    //Validar token y rol desde Auth SOAP
    private String validarTokenYRol(String authorizationHeader, String rolRequerido) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token no proporcionado");
        }

        String token = authorizationHeader.substring(7);

        ValidateTokenResponse v = authService.validar(token);
        if (v == null || !v.isValid()) {
            throw new RuntimeException("Token inválido");
        }

        String rol = v.getRol();
        if (!rol.equalsIgnoreCase(rolRequerido)) {
            throw new RuntimeException("Acceso denegado: se requiere rol " + rolRequerido);
        }
        return token;
    }
}