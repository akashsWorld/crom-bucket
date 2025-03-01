package com.cromxt.clusterrouter.controller;

import com.cromxt.clusterrouter.dtos.BucketInformationDTO;
import com.cromxt.clusterrouter.service.RoutingAdminServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/admin")
public record AdminController (
        RoutingAdminServices routingAdminServices
){
    @GetMapping("/online-routes")
    public Flux<BucketInformationDTO> getAllOnlineBuckets() {
        return routingAdminServices.getAllOnlineBuckets();
    }
}
