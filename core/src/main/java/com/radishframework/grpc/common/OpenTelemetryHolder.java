package com.radishframework.grpc.common;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class OpenTelemetryHolder {
    private static volatile OpenTelemetry openTelemetry = null;

    public static OpenTelemetry getOpenTelemetry() {
        if (openTelemetry == null) {
            synchronized (OpenTelemetryHolder.class) {
                if (openTelemetry == null) {
                    // install the W3C Trace Context propagator
                    // Get the tracer management instance
                    TracerSdkProvider sdkTracerProvider = TracerSdkProvider.builder().build();
                    // Set to process the the spans by the LogExporter
                    LoggingSpanExporter exporter = new LoggingSpanExporter();
                    sdkTracerProvider.addSpanProcessor(SimpleSpanProcessor.builder(exporter).build());

                    openTelemetry = OpenTelemetrySdk.builder()
                            .setTracerProvider(sdkTracerProvider)
                            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                            .build();
                }
            }
        }

        return openTelemetry;
    }
}
