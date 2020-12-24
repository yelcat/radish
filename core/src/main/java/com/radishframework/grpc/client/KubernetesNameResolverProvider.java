package com.radishframework.grpc.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Usage: kubernetes:///{namespace}/{service}/{port}
 * E.g.: kubernetes:///default/echo-server/8080
 */
public class KubernetesNameResolverProvider extends NameResolverProvider {
    public static final String SCHEME = "kubernetes";
    public static final String URI_ERROR_MESSAGE = "Must be formatted like kubernetes:///{namespace}/{service}/{port}";

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(final URI targetUri, final NameResolver.Args args) {
        if (!SCHEME.equals(targetUri.getScheme())) {
            return null;
        }

        final String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
        checkArgument(targetPath.startsWith("/"),
                "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);

        final Iterator<String> parts = Splitter.on("/").split(targetPath).iterator();
        checkArgument(parts.hasNext(), URI_ERROR_MESSAGE);
        final String namespace = parts.next();

        checkArgument(parts.hasNext(), URI_ERROR_MESSAGE);
        final String serviceName = parts.next();

        checkArgument(parts.hasNext(), URI_ERROR_MESSAGE);
        final String servicePort = parts.next();

        final int port;
        try {
            port = Integer.parseInt(servicePort);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse port number", e);
        }

        return new KubernetesNameResolver(namespace, serviceName, port, GrpcUtil.TIMER_SERVICE);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }
}