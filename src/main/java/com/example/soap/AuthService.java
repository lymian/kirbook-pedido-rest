package com.example.soap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kirbook.auth.GetUserByIdResponse;
import com.kirbook.auth.ValidateTokenResponse;

@Service
public class AuthService {

    @Autowired
    private AuthClient authClient;

    public ValidateTokenResponse validar(String token) {
        return authClient.validateToken(token);
    }

    public GetUserByIdResponse buscarUsuarioPorId(Long id) {
        return authClient.getUserById(id);
    }

    public GetUserByIdResponse obtenerUsuarioPorId(int id) {
        return authClient.getUserById((long) id);
    }
}