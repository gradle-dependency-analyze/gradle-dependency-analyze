package org.example;

import com.contrastsecurity.sdk.ContrastSDK;

public class App {
    public static void main(String[] args) {
        ContrastSDK contrast =
                new ContrastSDK.Builder("username", "my-service-key", "my-api-key").build();
        System.out.println(contrast);
    }
}
