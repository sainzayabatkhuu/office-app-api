package com.sol.office.notification.config;

import com.sol.office.common.security.OfficeJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Browsers cannot set headers on the WebSocket handshake, so the access token travels in the STOMP CONNECT
 * frame ({@code Authorization: Bearer ...}) and is validated here. SUBSCRIBE to another user's queue is
 * impossible because user destinations are resolved from the authenticated principal.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final OfficeJwtAuthenticationConverter converter;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing access token");
            }
            try {
                accessor.setUser(converter.convert(jwtDecoder.decode(header.substring(7))));
            } catch (JwtException e) {
                throw new MessageDeliveryException("Invalid access token");
            }
        } else if (accessor.getCommand() != null && accessor.getUser() == null
                && accessor.getCommand() != StompCommand.DISCONNECT) {
            throw new MessageDeliveryException("Not authenticated");
        }
        return message;
    }
}
