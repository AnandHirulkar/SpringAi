package io.github.anandhirulkar.supportiq.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * MCP transports do not authenticate requests themselves, so this filter adds a shared-secret check
 * at the HTTP edge. Leave {@code app.mcp.api-key} empty for local experimentation; set it whenever
 * the endpoint is reachable by anything other than localhost.
 */
@Component
public class McpApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-MCP-API-Key";

    private final McpProperties properties;

    public McpApiKeyFilter(McpProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean mcpRequest = request.getRequestURI().equals("/mcp")
                || request.getRequestURI().startsWith("/mcp/");
        if (!mcpRequest || !StringUtils.hasText(properties.apiKey())) {
            filterChain.doFilter(request, response);
            return;
        }
        String supplied = request.getHeader(HEADER);
        if (supplied != null
                && MessageDigest.isEqual(
                        properties.apiKey().getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter()
                .write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Missing or invalid MCP API key\"}");
    }
}
