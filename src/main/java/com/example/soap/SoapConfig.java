package com.example.soap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class SoapConfig {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller m = new Jaxb2Marshaller();
        // paquete generado por JAXB desde auth.xsd — según tu ValidateTokenResponse.java anterior: com.kirbook.auth
        m.setContextPath("com.kirbook.auth");
        return m;
    }

    @Bean
    public AuthClient authClient(Jaxb2Marshaller marshaller) {
        AuthClient client = new AuthClient();
        client.setDefaultUri("http://localhost:8081/ws"); // <- tu auth service endpoint
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}