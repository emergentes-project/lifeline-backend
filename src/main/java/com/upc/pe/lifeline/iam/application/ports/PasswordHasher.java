package com.upc.pe.lifeline.iam.application.ports;

public interface PasswordHasher {
    String hash(String password);
    boolean matches(String password, String hash);
}
