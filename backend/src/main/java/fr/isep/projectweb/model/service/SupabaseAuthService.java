package fr.isep.projectweb.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String supabasePublishableKey;
    private final String authBasePath;

    public SupabaseAuthService(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.publishable-key:}") String supabasePublishableKey) {
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

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        return post(path, payload, null);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload, String redirectTo) {
        try {
            RestClient.RequestBodyUriSpec request = restClient.post();
            RestClient.RequestBodySpec bodySpec = redirectTo != null && !redirectTo.isBlank()
                    ? request.uri(authBasePath + path, redirectTo)
                    : request.uri(authBasePath + path);
            return bodySpec.body(payload).retrieve().body(MAP_TYPE);
        } catch (RestClientResponseException exception) {
            Map<String, Object> errorBody = exception.getResponseBodyAs(MAP_TYPE);
            String message = firstNonBlank(
                    asString(errorBody != null ? errorBody.get("msg") : null),
                    asString(errorBody != null ? errorBody.get("error_description") : null),
                    asString(errorBody != null ? errorBody.get("message") : null),
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
