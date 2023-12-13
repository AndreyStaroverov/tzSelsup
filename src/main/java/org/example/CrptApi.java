package org.example;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create?pg=";
    private static final Gson gson = new Gson();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Semaphore requestSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestSemaphore = new Semaphore(requestLimit);
        scheduler.scheduleAtFixedRate(this::resetRequestCount, 0, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) {
        try {
            requestSemaphore.acquire();
            HttpRequest request = createRequest(document, signature);
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            requestSemaphore.release();
        }
    }

    private HttpRequest createRequest(Document document, String signature) throws IOException {
        String requestBody = gson.toJson(document);
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL + document.getProduct_group()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer <ТОКЕН>")
                .build();
    }

    private void handleResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
    }

    private void resetRequestCount() {
        requestCount.set(0);
    }

    private static class Document {
        private String document_format;
        private String product_document;
        private String product_group;
        private String signature;
        private String type;

        public String getDocument_format() {
            return document_format;
        }

        public void setDocument_format(String document_format) {
            this.document_format = document_format;
        }

        public String getProduct_group() {
            return product_group;
        }

        public void setProduct_group(String product_group) {
            this.product_group = product_group;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }


        public Document(String document_format, String product_document, String product_group, String signature, String type) {
            this.document_format = document_format;
            this.product_document = product_document;
            this.product_group = product_group;
            this.signature = signature;
            this.type = type;
        }

        public String getProduct_document() {
            return product_document;
        }

        public void setProduct_document(String product_document) {
            this.product_document = product_document;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        String signature = "<Открепленная подпись в base64>";
        Document document = new Document(
                "MANUAL",
                "<Документ в base64>",
                "group",
                signature,
                "LP_INTRODUCE_GOODS"
        );
        crptApi.createDocument(document, signature);
    }
}
