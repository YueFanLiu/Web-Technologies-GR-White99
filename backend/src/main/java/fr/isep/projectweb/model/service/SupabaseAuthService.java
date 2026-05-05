package fr.isep.projectweb.model.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class SupabaseAuthService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String supabasePublishableKey;
    private final String authBasePath;

    public SupabaseAuthService(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.publishable-key:}") String supabasePublishableKey) {
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("apikey", supabasePublishableKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + supabasePublishableKey)
                .build();
        this.supabasePublishableKey = supabasePublishableKey;
        this.authBasePath = "/auth/v1";
    }

    public Map<String, Object> signup(String email, String password, String fullName, String role) {
        ensureConfigured();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("full_name", fullName);
        metadata.put("role", role);
        payload.put("data", metadata);

        return post("/signup", payload);
    }

    public Map<String, Object> login(String email, String password) {
        ensureConfigured();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);

        return post("/token?grant_type=password", payload);
    }

    public Map<String, Object> forgotPassword(String email, String redirectTo) {
        ensureConfigured();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);

        String path = "/recover";
        if (redirectTo != null && !redirectTo.isBlank()) {
            path += "?redirect_to={redirectTo}";
        }

        return post(path, payload, redirectTo);
    }

    public Map<String, Object> verifyEmail(String tokenHash, String type) {
        ensureConfigured();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token_hash", tokenHash);
        payload.put("type", type);

        return post("/verify", payload);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        return post(path, payload, null);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload, String redirectTo) {
        try {
            RestClient.RequestBodyUriSpec request = restClient.post();
            RestClient.RequestBodySpec bodySpec = redirectTo != null && !redirectTo.isBlank()
                    ? request.uri(authBasePath + path, redirectTo)
                    : request.uri(authBasePath + path);
            String responseBody = bodySpec.body(payload).retrieve().body(String.class);
            return parseResponseBody(responseBody);
        } catch (RestClientResponseException exception) {
            Map<String, Object> errorBody = parseResponseBodyOrEmpty(exception.getResponseBodyAsString());
            String message = firstNonBlank(
                    asString(errorBody != null ? errorBody.get("msg") : null),
                    asString(errorBody != null ? errorBody.get("error_description") : null),
                    asString(errorBody != null ? errorBody.get("message") : null),
                    exception.getResponseBodyAsString(),
                    "Supabase authentication request failed"
            );
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.valueOf(exception.getStatusCode().value()),
                    message,
                    exception
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Unable to contact Supabase Auth", exception);
        }
    }

    private Map<String, Object> parseResponseBody(String responseBody) {
        Map<String, Object> parsed = parseResponseBodyOrEmpty(responseBody);
        if (!parsed.isEmpty()) {
            return parsed;
        }

        throw new ResponseStatusException(
                BAD_GATEWAY,
                firstNonBlank(responseBody, "Supabase authentication response was empty or not JSON")
        );
    }

    private Map<String, Object> parseResponseBodyOrEmpty(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(responseBody, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void ensureConfigured() {
        if (supabasePublishableKey == null || supabasePublishableKey.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Supabase publishable key is not configured");
        }
    }

    private String asString(Object value) {
        return value instanceof String string ? string : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
