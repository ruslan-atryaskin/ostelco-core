package org.ostelco.ocsgw.metrics;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.ostelco.ocsgw.datasource.grpc.GrpcDataSource;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReply;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class OcsgwMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(OcsgwMetrics.class);

    private static final int KEEP_ALIVE_TIMEOUT_IN_MINUTES = 1;

    private static final int KEEP_ALIVE_TIME_IN_SECONDS = 50;

    private OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceStub ocsgwAnalyticsServiceStub;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture initAnalyticsFuture = null;

    private ScheduledFuture keepAliveFuture = null;

    private ScheduledFuture autoReportAnalyticsFuture = null;

    private OcsgwAnalyticsReport lastActiveSessions = null;

    private StreamObserver<OcsgwAnalyticsReport> ocsgwAnalyticsReport;

    private GrpcDataSource datasource;

    public OcsgwMetrics(String metricsServerHostname, ServiceAccountJwtAccessCredentials credentials, GrpcDataSource grpcDataSource) {

        datasource = grpcDataSource;

        try {
            final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                    .forTarget(metricsServerHostname)
                    .keepAliveWithoutCalls(true)
                    .keepAliveTimeout(KEEP_ALIVE_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)
                    .keepAliveTime(KEEP_ALIVE_TIME_IN_SECONDS, TimeUnit.SECONDS);

            final ManagedChannelBuilder channelBuilder = Files.exists(Paths.get("/cert/metrics.crt"))
                        ? nettyChannelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(new File("/cert/metrics.crt")).build())
                        : nettyChannelBuilder;

            final ManagedChannel channel = channelBuilder
                    .useTransportSecurity()
                    .build();
            if (credentials != null) {
                ocsgwAnalyticsServiceStub  = OcsgwAnalyticsServiceGrpc.newStub(channel)
                        .withCallCredentials(MoreCallCredentials.from(credentials));
            } else {
                ocsgwAnalyticsServiceStub = OcsgwAnalyticsServiceGrpc.newStub(channel);
            }

        } catch (SSLException e) {
            LOG.warn("Failed to setup OcsMetrics", e);
        }
    }

    public void initAnalyticsRequest() {
        ocsgwAnalyticsReport = ocsgwAnalyticsServiceStub.ocsgwAnalyticsEvent(
                new StreamObserver<OcsgwAnalyticsReply>() {

                    @Override
                    public void onNext(OcsgwAnalyticsReply value) {
                        // Ignore reply from Prime
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOG.error("AnalyticsRequestObserver error", t);
                        if (t instanceof StatusRuntimeException) {
                            reconnectAnalyticsReport();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        // Nothing to do here
                    }
                }
        );
        initAutoReportAnalyticsReport();
    }


    private void sendAnalytics(OcsgwAnalyticsReport report) {
        if (report != null) {
            ocsgwAnalyticsReport.onNext(report);
        }
    }

    private void reconnectKeepAlive() {
        LOG.debug("reconnectKeepAlive called");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
    }

    private void reconnectAnalyticsReport() {
        LOG.debug("reconnectAnalyticsReport called");

        if (autoReportAnalyticsFuture != null) {
            autoReportAnalyticsFuture.cancel(true);
        }

        if (initAnalyticsFuture != null) {
            initAnalyticsFuture.cancel(true);
        }

        LOG.debug("Schedule new Callable initAnalyticsRequest");
        initAnalyticsFuture = executorService.schedule((Callable<Object>) () -> {
                    reconnectKeepAlive();
                    LOG.debug("Calling initAnalyticsRequest");
                    initAnalyticsRequest();
                    sendAnalytics(lastActiveSessions);
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
    }

    private void initAutoReportAnalyticsReport() {
        autoReportAnalyticsFuture = executorService.scheduleAtFixedRate(() -> {
                    lastActiveSessions = datasource.getAnalyticsReport();
                    sendAnalytics(lastActiveSessions);
                    LOG.debug("Sent analytics report");
                },
                5,
                5,
                TimeUnit.SECONDS);
    }
}