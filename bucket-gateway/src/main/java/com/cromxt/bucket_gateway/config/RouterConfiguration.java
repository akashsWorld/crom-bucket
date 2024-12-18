package com.cromxt.bucket_gateway.config;


import com.cromxt.bucket_gateway.client.RouteServerClient;
import com.cromxt.bucket_gateway.service.impl.DynamicRouteService;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RouterConfiguration {

    @Bean
    public DynamicRouteService bucketServerClient(RouteServerClient routeServerClient,
                                                  Environment environment,
                                                  RouteDefinitionWriter routeDefinitionWriter,
                                                  ApplicationEventPublisher applicationEventPublisher) {
        Boolean isSecure =  environment.getProperty("BUCKET_GATEWAY.BUCKETS_PROTOCOL", Boolean.class, false);
        return new DynamicRouteService(routeServerClient,isSecure,routeDefinitionWriter,applicationEventPublisher);
    }

    @Bean
    RouteDefinitionLocator dynamicRouteLocator(DynamicRouteService dynamicRouteService) {
        return dynamicRouteService;
    }

}
