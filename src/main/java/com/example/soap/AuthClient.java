package com.example.soap;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import com.kirbook.auth.GetUserByIdRequest;
import com.kirbook.auth.GetUserByIdResponse;
import com.kirbook.auth.ValidateTokenRequest;
import com.kirbook.auth.ValidateTokenResponse;

public class AuthClient extends WebServiceGatewaySupport {

    public ValidateTokenResponse validateToken(String token) {
        ValidateTokenRequest req = new ValidateTokenRequest();
        req.setToken(token);

        Object resp = getWebServiceTemplate()
                .marshalSendAndReceive(req,
                        new SoapActionCallback("http://kirbook.com/auth/ValidateTokenRequest"));

        return (ValidateTokenResponse) resp;
    }
    
    public GetUserByIdResponse getUserById(Long id) {
        GetUserByIdRequest req = new GetUserByIdRequest();
        req.setId(id);

        Object resp = getWebServiceTemplate()
                .marshalSendAndReceive(req,
                        new SoapActionCallback("http://kirbook.com/auth/GetUserByIdRequest"));

        return (GetUserByIdResponse) resp;
    }
}