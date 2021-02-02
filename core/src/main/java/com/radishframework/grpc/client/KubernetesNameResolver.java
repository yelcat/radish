package com.radishframework.grpc.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.base.Charsets;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.SharedResourceHolder;

import io.kubernetes.client.proto.V1.EndpointsList;
import io.kubernetes.client.proto.V1.Endpoints;
import io.kubernetes.client.proto.Meta.WatchEvent;
import io.kubernetes.client.proto.V1.EndpointAddress;
import io.kubernetes.client.proto.V1.EndpointPort;
import io.kubernetes.client.proto.V1.EndpointSubset;

public class KubernetesNameResolver extends NameResolver {

    private final String namespace;
    private final String name;
    private final int port;
    private final URI kubeApiUri;
	private final KubernetesClient kubernetesClient;
    private final SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource;

    private Listener listener;

    private volatile boolean refreshing = false;
    private volatile boolean watching = false;

    public KubernetesNameResolver(final String namespace, final String name, final int port,
            final SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource,
            final String kubeApiUri) {
        this.namespace = namespace;
        this.name = name;
        this.port = port;
        this.timerServiceResource = timerServiceResource;
        this.kubeApiUri = URI.create(kubeApiUri);
        this.kubernetesClient = new KubernetesClient(kubeApiUri);
    }

    @Override
    public String getServiceAuthority() {
        return kubeApiUri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        refresh();
    }

    @Override
    public void shutdown() {
    }

    @Override
    @GuardedBy("this")
    public void refresh() {
        if (refreshing)
            return;
        try {
            refreshing = true;

            List<EndpointSubset> subsets = kubernetesClient.getEndpointSubsets(namespace, name);
            if (subsets.isEmpty()) {
                // not foud subsets, retrying 30 senconds later
                final ScheduledExecutorService timerService = SharedResourceHolder.get(timerServiceResource);
                timerService.schedule(this::refresh, 30, TimeUnit.SECONDS);
                return;
            }

            update(subsets);
            watch();
        } finally {
            refreshing = false;
        }
    }

    private void update(List<EndpointSubset> subsets) {
        List<EquivalentAddressGroup> servers = subsets.stream().filter(
                subset -> subset.getPortsList().stream().anyMatch(endpointPort -> port == endpointPort.getPort()))
                .flatMap(
                        subset -> subset.getAddressesList().stream()
                                .map(endpointAddress -> new EquivalentAddressGroup(
                                        new InetSocketAddress(endpointAddress.getIp(), port))))
                .collect(Collectors.toList());
        listener.onAddresses(servers, Attributes.EMPTY);
    }

    @GuardedBy("this")
    protected void watch() {
        if (watching)
            return;
        watching = true;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(this.kubeApiUrl 
                        + this.namespace 
                        + "/endpoints?watch=true&filedSelector=" 
                        + URLEncoder.encode("name=" + this.name, Charsets.UTF_8)))
            .header("Accept", "application/vnd.kubernetes.protobuf")
            .version(HttpClient.Version.HTTP_1_1)
            .GET()
            .timeout(Duration.ofSeconds(20))
            .build();
        client.sendAsync(request, BodyHandlers.fromSubscriber(watchSubscriber));
            .thenAccept(response -> {
                InputStream in = response.body();

            });
    }

    private Subscriber<List<ByteBuffer>> watchSubscriber = new Subscriber<List<ByteBuffer>>() {

		@Override
		public void onSubscribe(Subscription subscription) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onNext(List<ByteBuffer> item) {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (char c = in.read()) {
                    if (c != '\r' && c != '\n') {
                        out.write(c);   
                    }
                }
                byte[] bytesRead = out.toByteArray();
                WatchEvent watchEvent = WatchEvent.parseFrom(bytesRead);
		}

		@Override
		public void onError(Throwable throwable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onComplete() {
			// TODO Auto-generated method stub
			
		}

    };

}
