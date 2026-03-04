package org.union;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class UnionApiClient
{
    private final String baseUrl;
    private final Gson gson = new Gson();
    // Background thread so HTTP calls never freeze the game
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UnionApiClient(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    /**
     * Fire-and-forget: POST a contribution to the union service in the background.
     */
    public void recordContribution(String userId, String unionId, int deltaPoints, int deltaMissions)
    {
        executor.submit(() -> {
            try
            {
                ContributionRequest body = new ContributionRequest(
                        UUID.randomUUID().toString(), // session_id
                        userId,
                        unionId,
                        deltaPoints,
                        deltaMissions,
                        UUID.randomUUID().toString()  // idempotency_key — unique per contribution
                );

                String json = gson.toJson(body);
                URL url = new URL(baseUrl + "/v1/unions/" + unionId + "/contributions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream())
                {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                if (status == 201)
                {
                    log.debug("Contribution recorded successfully!");
                }
                else
                {
                    log.warn("Contribution POST returned status: {}", status);
                }
                conn.disconnect();
            }
            catch (Exception e)
            {
                log.error("Failed to record contribution", e);
            }
        });
    }

    public void shutdown()
    {
        executor.shutdown();
    }

    // ---- Inner request DTO ----
    private static class ContributionRequest
    {
        String session_id;
        String user_id;
        String union_id;
        int delta_points;
        int delta_missions;
        String idempotency_key;

        ContributionRequest(String sessionId, String userId, String unionId,
                            int deltaPoints, int deltaMissions, String idempotencyKey)
        {
            this.session_id = sessionId;
            this.user_id = userId;
            this.union_id = unionId;
            this.delta_points = deltaPoints;
            this.delta_missions = deltaMissions;
            this.idempotency_key = idempotencyKey;
        }
    }
}