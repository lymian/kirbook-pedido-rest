package com.example.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.DTO.ClienteSOAPDTO;
import com.example.DTO.DetallePedidoRequestDTO;
import com.example.DTO.ItemPedidoResponseDTO;
import com.example.DTO.LibroDTO;
import com.example.DTO.PedidoDTO;
import com.example.DTO.PedidoRequestDTO;
import com.example.DTO.PedidoResponseDTO;
import com.example.feign.LibroClient;
import com.example.model.DetallePedido;
import com.example.model.Pedido;
import com.example.repository.IDetallePedidoRepository;
import com.example.repository.IPedidoRepository;
import com.example.soap.AuthService;
import com.kirbook.auth.GetUserByIdResponse;
import com.kirbook.auth.ValidateTokenResponse;

import jakarta.transaction.Transactional;

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

    // Listar todos los pedidos
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // Obtener pedido por ID
    public Pedido obtenerPorId(int id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    // Crear pedido (solo ADMIN)
    public Pedido crearPedido(PedidoDTO dto, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");

        // valiadar si existe el usuario
        var userResp = authService.buscarUsuarioPorId((long) dto.getClienteId());
        if (userResp == null || !userResp.isExists()) {
            throw new RuntimeException("El cliente con ID " + dto.getClienteId() + " no existe en el sistema Auth.");
        }

        // Verificar que todos los libros existen antes de continuar
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

        // Crear pedido
        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setEstado(dto.getEstado() == null ? "pendiente" : dto.getEstado());
        pedido.setFecha(LocalDateTime.now());

        // Calcular total y detalles
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

    // Actualizar pedido (solo ADMIN)
    public Pedido actualizarPedido(int id, PedidoDTO dto, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // Verificar que todos los libros existen antes de continuar
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

    // Eliminar pedido (solo ADMIN)
    public void eliminarPedido(int id, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");
        pedidoRepository.deleteById(id);
    }

    // Finalizar pedido (solo ADMIN)
    public Pedido finalizarPedido(int id, String authorizationHeader) {
        validarTokenYRol(authorizationHeader, "ROLE_ADMIN");

        Pedido p = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        p.setEstado("finalizado");
        return pedidoRepository.save(p);
    }

    // Validar token y rol desde Auth SOAP
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

    public ResponseEntity<?> validarYAutorizarPedido(String tokenHeader, PedidoRequestDTO pedidoRequest) {

        // --- 1️⃣ Validar formato del token ---
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no proporcionado o formato inválido");
        }

        String token = tokenHeader.substring(7);

        // --- 2️⃣ Validar el token mediante SOAP ---
        ValidateTokenResponse resp = authService.validar(token);

        if (resp == null || !resp.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido o expirado");
        }

        // --- 3️⃣ Verificar el rol del usuario ---
        if (resp.getRol() == null || !resp.getRol().equals("ROLE_USER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado. Solo los usuarios con rol CLIENTE pueden generar pedidos.");
        }

        Long clienteId = resp.getId();

        // --- 4️⃣ Validar existencia y stock de libros ---
        List<String> errores = new ArrayList<>();
        Map<Integer, LibroDTO> librosValidados = new HashMap<>();

        for (DetallePedidoRequestDTO detalle : pedidoRequest.getDetalles()) {
            try {
                LibroDTO libro = libroClient.obtenerLibroPorId(detalle.getLibroId());

                if (libro == null || !libro.isEstado()) {
                    errores.add("El libro con ID " + detalle.getLibroId() + " no está disponible.");
                    continue;
                }

                if (detalle.getCantidad() > libro.getStock()) {
                    errores.add("Stock insuficiente para el libro '" + libro.getTitulo() +
                            "'. Stock disponible: " + libro.getStock());
                } else {
                    librosValidados.put(detalle.getLibroId(), libro);
                }

            } catch (Exception ex) {
                errores.add("Error al obtener el libro con ID " + detalle.getLibroId() +
                        ": " + ex.getMessage());
            }
        }

        if (!errores.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errores);
        }

        // --- 5️⃣ Crear y guardar el pedido ---
        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId.intValue());
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(LocalDateTime.now());

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;

        for (DetallePedidoRequestDTO detalleDTO : pedidoRequest.getDetalles()) {
            LibroDTO libro = librosValidados.get(detalleDTO.getLibroId());
            double precioFinal = libro.getPrecio() - (libro.getPrecio() * libro.getDescuento() / 100);
            double subtotal = precioFinal * detalleDTO.getCantidad();
            total += subtotal;

            DetallePedido detalle = new DetallePedido();
            detalle.setLibroId(libro.getId());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(precioFinal);
            detalle.setPedido(pedido);

            detalles.add(detalle);
        }

        pedido.setTotal(total);
        pedido.setDetalles(detalles);

        pedidoRepository.save(pedido);

        // --- 6️⃣ Construir respuesta DTO ---
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(pedido.getId());
        response.setFecha(pedido.getFecha());
        response.setEstado(pedido.getEstado());
        response.setTotal(pedido.getTotal());

        // cliente SOAP
        ClienteSOAPDTO clienteDTO = new ClienteSOAPDTO();
        clienteDTO.setId(resp.getId());
        clienteDTO.setUsername(resp.getUsername());
        clienteDTO.setEmail(resp.getEmail());
        clienteDTO.setNombre(resp.getNombre());
        clienteDTO.setApellido(resp.getApellido());
        clienteDTO.setRol(resp.getRol());

        response.setCliente(clienteDTO);

        // items del pedido
        List<ItemPedidoResponseDTO> items = new ArrayList<>();
        for (DetallePedido detalle : pedido.getDetalles()) {
            ItemPedidoResponseDTO item = new ItemPedidoResponseDTO();
            item.setId(detalle.getId());
            item.setCantidad(detalle.getCantidad());
            item.setPrecioUnitario(detalle.getPrecioUnitario());
            item.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            item.setLibro(librosValidados.get(detalle.getLibroId()));
            items.add(item);
        }

        response.setItems(items);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private PedidoResponseDTO construirPedidoResponseDTO(Pedido pedido, ValidateTokenResponse clienteSOAP) {
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(pedido.getId());
        response.setFecha(pedido.getFecha());
        response.setEstado(pedido.getEstado());
        response.setTotal(pedido.getTotal());

        // --- Datos del cliente ---
        ClienteSOAPDTO clienteDTO = new ClienteSOAPDTO();
        clienteDTO.setId(clienteSOAP.getId());
        clienteDTO.setUsername(clienteSOAP.getUsername());
        clienteDTO.setEmail(clienteSOAP.getEmail());
        clienteDTO.setNombre(clienteSOAP.getNombre());
        clienteDTO.setApellido(clienteSOAP.getApellido());
        clienteDTO.setRol(clienteSOAP.getRol());
        response.setCliente(clienteDTO);

        // --- Items del pedido ---
        List<ItemPedidoResponseDTO> items = new ArrayList<>();

        for (DetallePedido detalle : pedido.getDetalles()) {
            LibroDTO libro = null;
            try {
                libro = libroClient.obtenerLibroPorId(detalle.getLibroId());
            } catch (Exception e) {
                // en caso de error remoto, devolvemos null pero no interrumpimos
            }

            ItemPedidoResponseDTO item = new ItemPedidoResponseDTO();
            item.setId(detalle.getId());
            item.setCantidad(detalle.getCantidad());
            item.setPrecioUnitario(detalle.getPrecioUnitario());
            item.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            item.setLibro(libro);

            items.add(item);
        }

        response.setItems(items);
        return response;
    }

    public ResponseEntity<?> obtenerPedidosCliente(String tokenHeader) {

        // --- 1️⃣ Validar formato del token ---
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no proporcionado o formato inválido");
        }

        String token = tokenHeader.substring(7);

        // --- 2️⃣ Validar el token mediante SOAP ---
        ValidateTokenResponse resp = authService.validar(token);

        if (resp == null || !resp.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido o expirado");
        }

        // --- 3️⃣ Verificar el rol del usuario ---
        if (resp.getRol() == null || !resp.getRol().equals("ROLE_USER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado. Solo los usuarios con rol CLIENTE pueden ver sus pedidos.");
        }

        Long clienteId = resp.getId();

        // --- 4️⃣ Buscar pedidos del cliente ---
        List<Pedido> pedidos = pedidoRepository.findByClienteId(clienteId.intValue());

        if (pedidos.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // --- 5️⃣ Construir lista de respuestas ---
        List<PedidoResponseDTO> respuesta = pedidos.stream()
                .map(pedido -> construirPedidoResponseDTO(pedido, resp))
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<?> listarPedidosAdmin(String tokenHeader) {
        // --- 1️⃣ Validar formato del token ---
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no proporcionado o formato inválido");
        }

        String token = tokenHeader.substring(7);

        // --- 2️⃣ Validar el token mediante SOAP ---
        ValidateTokenResponse resp = authService.validar(token);

        if (resp == null || !resp.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido o expirado");
        }

        // --- 3️⃣ Verificar el rol ---
        if (resp.getRol() == null || !resp.getRol().equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado. Solo los administradores pueden listar pedidos.");
        }

        // --- 4️⃣ Obtener los pedidos desde la BD ---
        List<Pedido> pedidos = pedidoRepository.findAll();

        if (pedidos.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // --- 5️⃣ Construir la respuesta con tus DTOs existentes ---
        List<PedidoResponseDTO> lista = pedidos.stream()
                .map(pedido -> {
                    GetUserByIdResponse clienteSOAP = authService.obtenerUsuarioPorId(pedido.getClienteId());
                    return construirPedidoResponseDTOAdmin(pedido, clienteSOAP);
                })
                .toList();

        return ResponseEntity.ok(lista);
    }

    private PedidoResponseDTO construirPedidoResponseDTOAdmin(Pedido pedido, GetUserByIdResponse clienteSOAP) {
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(pedido.getId());
        response.setFecha(pedido.getFecha());
        response.setEstado(pedido.getEstado());
        response.setTotal(pedido.getTotal());

        // --- Cliente ---
        ClienteSOAPDTO clienteDTO = new ClienteSOAPDTO();
        clienteDTO.setId(clienteSOAP.getId());
        clienteDTO.setUsername(clienteSOAP.getUsername());
        clienteDTO.setEmail(clienteSOAP.getEmail());
        clienteDTO.setRol(clienteSOAP.getRol());
        response.setCliente(clienteDTO);

        // --- Items ---
        List<ItemPedidoResponseDTO> items = new ArrayList<>();

        for (DetallePedido detalle : pedido.getDetalles()) {
            LibroDTO libro = null;
            try {
                libro = libroClient.obtenerLibroPorId(detalle.getLibroId());
            } catch (Exception e) {
                // Evita interrumpir si el microservicio libro no responde
            }

            ItemPedidoResponseDTO item = new ItemPedidoResponseDTO();
            item.setId(detalle.getId());
            item.setCantidad(detalle.getCantidad());
            item.setPrecioUnitario(detalle.getPrecioUnitario());
            item.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            item.setLibro(libro);

            items.add(item);
        }

        response.setItems(items);
        return response;
    }

    @Transactional
    public ResponseEntity<?> finalizarPedido(String tokenHeader, int pedidoId) {

        // 1️⃣ Validar token
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no proporcionado o formato inválido");
        }

        String token = tokenHeader.substring(7);
        ValidateTokenResponse resp = authService.validar(token);

        if (resp == null || !resp.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido o expirado");
        }

        if (!"ROLE_ADMIN".equals(resp.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo administradores pueden finalizar pedidos.");
        }

        // 2️⃣ Buscar el pedido
        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);
        if (optionalPedido.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Pedido no encontrado con ID: " + pedidoId);
        }

        Pedido pedido = optionalPedido.get();

        if (!"PENDIENTE".equalsIgnoreCase(pedido.getEstado())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El pedido ya fue finalizado o no está en estado PENDIENTE.");
        }

        // 3️⃣ Actualizar stock en servicio de libros
        List<String> errores = new ArrayList<>();

        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                libroClient.restarStock(
                        detalle.getLibroId(),
                        detalle.getCantidad());
            } catch (Exception e) {
                throw new RuntimeException("No se pudo actualizar el stock del libro ID " + detalle.getLibroId());
            }
        }

        if (!errores.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errores);
        }

        // 4️⃣ Cambiar estado y guardar
        pedido.setEstado("FINALIZADO");
        pedidoRepository.save(pedido);

        // 5️⃣ Construir respuesta DTO reutilizando tu método existente
        GetUserByIdResponse clienteSOAP = authService.obtenerUsuarioPorId(pedido.getClienteId());
        PedidoResponseDTO response = construirPedidoResponseDTOAdmin(pedido, clienteSOAP);

        return ResponseEntity.ok(response);
    }

}