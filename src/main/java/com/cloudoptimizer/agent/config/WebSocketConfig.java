package com.cloudoptimizer.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time optimization progress streaming.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to send messages to clients
        // Messages with destination prefix "/topic" will be routed to message broker
        config.enableSimpleBroker("/topic");
        
        // Messages with destination prefix "/app" will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint that clients will connect to
        registry.addEndpoint("/ws-optimize")
                .setAllowedOriginPatterns("*") // Use setAllowedOriginPatterns instead of setAllowedOrigins for local development
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket
    }
}
