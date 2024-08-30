package it.gov.acn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.MeterRegistry;
import it.gov.acn.etc.TestUtils;
import it.gov.acn.model.Constituency;
import it.gov.acn.outbox.core.observability.OutboxMetricsCollector;
import it.gov.acn.service.ConstituencyService;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "acn.outbox.scheduler.enabled=true",
        "acn.outbox.scheduler.fixed-delay=3000",
        "acn.outbox.scheduler.backoff-base=1"
    }
)
public class PrometheusEndpointTest extends PostgresTestContainerConfiguration {

  @LocalServerPort
  private int port;

  private OutboxMetricsCollector outboxMetricsCollector = OutboxMetricsCollector.getInstance();

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private ConstituencyService constituencyService;

  @BeforeEach
  public void setUp() {
    this.outboxMetricsCollector.reset();
  }

  @Test
  public void contextLoads() {
    Assertions.assertNotNull(meterRegistry);
  }

  @Test
  public void test_prometheus_endpoint_correctly_exposed() {
    String baseUrl = "http://localhost:" + port + "/actuator/prometheus";
    ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  @Test
  public void when_multiple_saveConstituency_then_prometheus_counter_increased() {

    int numOfConstituency = 10;

    Metrics initialMetrics = getForMetrics();

    for (int i = 0; i < numOfConstituency; i++) {
      Constituency constituency = TestUtils.createTestConstituency();
      this.constituencyService.saveConstituency(constituency);
    }

    Metrics metrics = getForMetrics();
    assertEquals(initialMetrics.outboxQueuedTotal + numOfConstituency, metrics.outboxQueuedTotal());

    Awaitility.await()
        .atMost(Duration.ofMillis(3500))
        .pollDelay(Duration.ofMillis(500))
        .untilAsserted(() -> {
          Metrics metrics1 = getForMetrics();
          assertEquals(initialMetrics.outboxSuccessesTotal + numOfConstituency, metrics1.outboxSuccessesTotal());
        });

    metrics = getForMetrics();

    assertEquals(initialMetrics.outboxQueuedTotal + numOfConstituency, metrics.outboxQueuedTotal());
    assertEquals(initialMetrics.outboxSuccessesTotal + numOfConstituency, metrics.outboxSuccessesTotal());
    assertEquals(0, metrics.outboxFailuresTotal());
    assertEquals(0, metrics.outboxDlqTotal());
  }


  private Metrics getForMetrics() {
    String baseUrl = "http://localhost:" + port + "/actuator/prometheus";
    ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());

    return extractCustomMetrics(response.getBody());
  }

  private Metrics extractCustomMetrics(String metricsString) {
    Long outboxQueuedTotal = extractMetricValue(metricsString, "outbox_queued_total");
    Long outboxSuccessesTotal = extractMetricValue(metricsString, "outbox_successes_total");
    Long outboxFailuresTotal = extractMetricValue(metricsString, "outbox_failures_total");
    Long outboxDlqTotal = extractMetricValue(metricsString, "outbox_dlq_total");
    Long outboxLastObservation = extractMetricValue(metricsString, "outbox_last_observation");
    Long outboxObservationStart = extractMetricValue(metricsString, "outbox_observation_start");

    return new Metrics(outboxQueuedTotal, outboxSuccessesTotal, outboxFailuresTotal, outboxDlqTotal,
        outboxLastObservation, outboxObservationStart);
  }

  public static Long extractMetricValue(String metricsString, String metricName) {
    String patternString = metricName + "\\{.*?\\}\\s+([\\d\\.eE+-]+)";
    Pattern pattern = Pattern.compile(patternString);
    Matcher matcher = pattern.matcher(metricsString);

    if (matcher.find()) {
      return Long.parseLong(matcher.group(1)
          .split("\\.")[0]); // Handle possible floating point values by splitting and taking the integer part
    } else {
      return null;
    }
  }

  public record Metrics(
      Long outboxQueuedTotal,
      Long outboxSuccessesTotal,
      Long outboxFailuresTotal,
      Long outboxDlqTotal,
      Long outboxLastObservation,
      Long outboxObservationStart
  ) {

  }
}
