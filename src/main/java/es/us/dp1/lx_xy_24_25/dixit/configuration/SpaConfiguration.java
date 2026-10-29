package es.us.dp1.lx_xy_24_25.dixit.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Explicit UI paths only. API, SockJS, management and missing assets never fall back. */
@Configuration
public class SpaConfiguration implements WebMvcConfigurer {
    static final String[] ROUTES = {
        "/login", "/register", "/logout", "/plans", "/docs", "/friends", "/stats",
        "/users", "/users/{username}", "/users/{userId}/achievements", "/games/History",
        "/games/{gameId}", "/games/{gameId}/join", "/games/{gameId}/game",
        "/games/{gameId}/spectate/{playerId}"
    };
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : ROUTES) registry.addViewController(route).setViewName("forward:/index.html");
    }
}
