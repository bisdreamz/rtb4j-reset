package org.reset;

import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class TelemetryProvider {

    private static final Logger log = LogManager.getLogger(TelemetryProvider.class);

    public static void registerGlobal() {
        Resource resource = Resource.getDefault()
                .merge(Resource.builder()
                        .put(ResourceAttributes.SERVICE_NAME, "rtb4j-reset")
                        .put(ResourceAttributes.K8S_POD_NAME, System.getenv("HOSTNAME"))
                        .build());

        String endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (endpoint == null || endpoint.isEmpty()) {
            log.warn("Env OTEL_EXPORTER_OTLP_ENDPOINT not set, not exporting otel metrics!");
            return;
        }

        // Configure the OTLP Span Exporter for tracing
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)  // SigNoz collector endpoint for tracing
                .build();

        // Configure the OTLP Metric Exporter
        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(endpoint)  // SigNoz collector endpoint for metrics
                .build();

        // Create the TracerProvider with the Span Exporter
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(Sampler.traceIdRatioBased(0.01))
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        // Create the MeterProvider with the Metric Exporter
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setInterval(Duration.ofSeconds(15))  // Export metrics every 10 seconds
                        .build())
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();
    }

}
