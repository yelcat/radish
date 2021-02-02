package com.radishframework.grpc.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Charsets;

import io.kubernetes.client.proto.V1.EndpointSubset;
import io.kubernetes.client.proto.V1.Endpoints;
import io.kubernetes.client.proto.V1.EndpointsList;
import sun.net.www.http.ChunkedInputStream;

public class KubernetesClient {

    private final String kubeApiUri;

    public KubernetesClient(String kubeApiUri) {
        this.kubeApiUri = kubeApiUri;
    }

    public List<EndpointSubset> getEndpointSubsets(String namespace, String name) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.kubeApiUri 
                            + "/api/v1/namespaces/" 
                            + namespace 
                            + "/endpoints?filedSelector="
                            + URLEncoder.encode("name=" + name, Charsets.UTF_8)))
                .header("Accept", "application/vnd.kubernetes.protobuf").version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());

        List<EndpointSubset> subsets = Collections.emptyList();

        if (response.statusCode() == 200) {
            EndpointsList endpointsList = EndpointsList.parseFrom(response.body());
            if (endpointsList.getItemsCount() > 0) {
                Endpoints endpoints = endpointsList.getItems(0);
                if (endpoints.getSubsetsCount() > 0) {
                    subsets = endpoints.getSubsetsList();
                }
            }
        }

        return subsets;
    }

    public void watchEndpointSubsets(String namespace, String name) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(this.kubeApiUri 
                        + "/api/v1/namespaces/"
                        + namespace 
                        + "/endpoints?watch=true&filedSelector=" 
                        + URLEncoder.encode("name=" + name, Charsets.UTF_8)))
            .header("Accept", "application/vnd.kubernetes.protobuf")
            .version(HttpClient.Version.HTTP_1_1)
            .GET()
            .timeout(Duration.ofSeconds(20))
            .build();
        client.sendAsync(request, BodyHandlers.ofInputStream())
            .thenAccept(response -> {
                ChunkedInputStream in = new ChunkedInputStream(response.body());
            });
    }

}
