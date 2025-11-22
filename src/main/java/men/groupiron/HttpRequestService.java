package men.groupiron;

import com.google.gson.Gson;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import okhttp3.*;

@Slf4j
@Singleton
public class HttpRequestService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String USER_AGENT =
            "GroupIronmenTracker/1.6.0 " + "RuneLite/" + RuneLiteProperties.getVersion();
    private static final String PUBLIC_BASE_URL = "https://groupiron.men";

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private GroupIronmenTrackerConfig config;

    @Inject
    private Gson gson;

    public HttpResponse get(String url, String authToken) {
        Request request = buildRequest(url, authToken).get().build();

        return executeRequest(request, "GET", url, null);
    }

    public HttpResponse post(String url, String authToken, Object requestBody) {
        String requestJson = gson.toJson(requestBody);
        RequestBody body = RequestBody.create(JSON, requestJson);

        Request request = buildRequest(url, authToken).post(body).build();

        return executeRequest(request, "POST", url, requestJson);
    }

    private Request.Builder buildRequest(String url, String authToken) {
        Request.Builder requestBuilder = new Request.Builder().url(url).header("User-Agent", USER_AGENT);

        if (isInternalUrl(url)) {
            if (authToken != null && !authToken.trim().isEmpty()) {
                requestBuilder.header("Authorization", authToken);
            }
            requestBuilder.header("Accept", "application/json");
        }

        return requestBuilder;
    }

    private boolean isInternalUrl(String url) {
        return url.startsWith(getBaseUrl());
    }

    private HttpResponse executeRequest(Request request, String method, String url, String requestBody) {
        Call call = okHttpClient.newCall(request);

        try (Response response = call.execute()) {
            String responseBody = readBodySafe(response);
            logRequest(method, url, requestBody, response, responseBody);
            return new HttpResponse(response.isSuccessful(), response.code(), responseBody);
        } catch (IOException ex) {
            log.warn("{} {} failed: {}", method, url, ex.toString());
            return new HttpResponse(false, -1, ex.getMessage());
        }
    }

    private void logRequest(String method, String url, String requestBody, Response response, String responseBody) {
        if (!log.isDebugEnabled()) {
            return;
        }

        switch (method) {
            case "GET":
                log.debug("GET {} -> {}\nResponse: {}", url, response.code(), responseBody);
                break;
            case "POST":
                log.debug("POST {}\nRequest: {}\nResponse({}): {}", url, requestBody, response.code(), responseBody);
                break;
            default:
                log.debug("{} {} -> {}\nResponse: {}", method, url, response.code(), responseBody);
        }
    }

    private static String readBodySafe(Response response) {
        try {
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.string() : "<no body>";
        } catch (Exception e) {
            return "<unavailable: " + e.getMessage() + ">";
        }
    }

    public String getBaseUrl() {
        String baseUrlOverride = config.baseUrlOverride().trim();
        if (!baseUrlOverride.isEmpty()) {
            return baseUrlOverride;
        }
        return PUBLIC_BASE_URL;
    }

    @Getter
    public static class HttpResponse {
        private final boolean successful;
        private final int code;
        private final String body;

        public HttpResponse(boolean successful, int code, String body) {
            this.successful = successful;
            this.code = code;
            this.body = body;
        }
    }
}
