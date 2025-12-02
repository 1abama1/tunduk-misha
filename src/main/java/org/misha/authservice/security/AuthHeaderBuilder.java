package org.misha.authservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class AuthHeaderBuilder {

    private static final String EXPOSED_HEADERS = "Authorization,Access-Token,Token-Type,Uid,Client,Cache-Control,Content-Type";

    public void apply(HttpHeaders headers,
                      String opaqueAccessToken,
                      String uid,
                      String clientId) {

        headers.add("Authorization", "Bearer " + opaqueAccessToken);
        headers.add("Access-Token", opaqueAccessToken);
        headers.add("Token-Type", "Bearer");

        if (uid != null && !uid.isBlank()) {
            headers.add("Uid", uid);
        }

        headers.add("Client", clientId);
        headers.add("Cache-Control", "max-age=0, private, must-revalidate");
        headers.add("Access-Control-Expose-Headers", EXPOSED_HEADERS);
    }
}

