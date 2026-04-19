package com.example.apigateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DiscoveryController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/services")
    public Object listServices() {
        return discoveryClient.getServices().stream()
                .collect(java.util.stream.Collectors.toMap(
                        service -> service,
                        service -> discoveryClient.getInstances(service)));
    }
}
