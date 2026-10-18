package com.sol.office_app.config;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Browsers cannot add headers to the WebSocket handshake, so the token travels in the STOMP CONNECT frame.
 * The authenticated user becomes the session principal, which is what {@code /user/...} destinations use.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    public StompAuthChannelInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing access token");
            }
            CustomUserPrincipal principal = jwtUtils.parse(header.substring(7))
                    .map(JwtUtils::toPrincipal)
                    .orElseThrow(() -> new MessageDeliveryException("Invalid access token"));
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        } else if (accessor.getUser() == null && !StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            throw new MessageDeliveryException("Not authenticated");
        }
        return message;
    }
}
