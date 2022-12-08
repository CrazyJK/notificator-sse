package com.hs.common.notificator.sse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class NotifyRouter {

  @Bean
  public RouterFunction<ServerResponse> route(NotifyHandler notifyHandler) {
    return RouterFunctions
        .route(POST("/webNotification"), notifyHandler::receive)
        .andRoute(GET("/sse/{userid}"), notifyHandler::sse);
  }

  @Bean
  CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowedOrigins(Arrays.asList("*"));
    corsConfig.setMaxAge(8000L);
    corsConfig.addAllowedMethod("POST,GET");
    // corsConfig.addAllowedHeader("x-requested-with");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }

}
