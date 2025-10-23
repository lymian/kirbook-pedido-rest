package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.model.DetallePedido;

@Repository
public interface IDetallePedidoRepository extends JpaRepository<DetallePedido, Integer>{

}
