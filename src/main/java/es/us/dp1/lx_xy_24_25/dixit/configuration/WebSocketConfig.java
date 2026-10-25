package es.us.dp1.lx_xy_24_25.dixit.configuration; // Ajusta el paquete

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAccess access;
    public WebSocketConfig(StompAccess access) { this.access = access; }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(access.inbound());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(access.outbound());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic").setTaskScheduler(dixitHeartbeatScheduler()).setHeartbeatValue(new long[]{10000,10000});
    }

    @Bean(destroyMethod="shutdown")
    public ThreadPoolTaskScheduler dixitHeartbeatScheduler() {
        var scheduler=new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);scheduler.setThreadNamePrefix("dixit-stomp-heartbeat-");
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //http://localhost:8080/ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
