package com.ga.leave.service;

import com.ga.leave.model.SecureToken;
import org.springframework.stereotype.Service;

public interface SecureTokenService {

    SecureToken createToken();
    void saveSecureToken(SecureToken secureToken);
    SecureToken findByToken(String token);
    void removeToken(SecureToken token);

}