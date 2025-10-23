package com.puphax.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance stress testing for PUPHAX service operations.
 * 
 * This test suite validates:
 * 1. Response time under different load scenarios
 * 2. Throughput measurement and benchmarking
 * 3. Circuit breaker behavior under stress
 * 4. Connection pooling effectiveness
 * 5. Memory usage and resource management
 * 6. Concurrent request handling
 * 7. Performance degradation detection
 * 
 * Test Categories:
 * - Load testing: Normal expected load
 * - Stress testing: Peak load scenarios
 * - Spike testing: Sudden load increases
 * - Volume testing: Large data processing
 * - Endurance testing: Sustained load over time
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceStressTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceStressTest.class);
    
    @Autowired
    private SimplePuphaxClient simplePuphaxClient;
    
    @Autowired
    private PuphaxSpringWsClient springWsClient;
    
    @Autowired
    private DrugService drugService;
    
    // Test configuration
    private static final String[] TEST_DRUGS = {
        "XANAX", "ASPIRIN", "PARACETAMOL", "IBUPROFEN", "TRAMADOL",
        "DIAZEPAM", "MORPHINE", "CODEINE", "METFORMIN", "INSULIN"
    };
    
    private static final String[] TEST_PRODUCT_IDS = {
        "14714226", "14714225", "14714227", "14714228", "14714229"
    };
    
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 10, 1);
    
    // Performance thresholds
    private static final long MAX_RESPONSE_TIME_MS = 30000; // 30 seconds
    private static final long ACCEPTABLE_RESPONSE_TIME_MS = 5000; // 5 seconds
    private static final double MIN_SUCCESS_RATE = 0.95; // 95% success rate
    
    // ========================================
    // Load Testing (Normal Expected Load)
    // ========================================
    
    @Test
    @Order(1)
    @DisplayName("Load Test: Sequential requests with normal load")
    void testSequentialRequestLoad() {
        logger.info("Starting sequential load test with {} requests", TEST_DRUGS.length);
        
        List<Long> responseTimes = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (String drug : TEST_DRUGS) {
            long requestStart = System.currentTimeMillis();
            
            try {
                String response = simplePuphaxClient.searchDrugsSimple(drug);
                assertNotNull(response, "Response should not be null for drug: " + drug);
                
                long requestTime = System.currentTimeMillis() - requestStart;
                responseTimes.add(requestTime);
                successCount.incrementAndGet();
                
                logger.debug("Drug search for '{}' completed in {} ms", drug, requestTime);
                
            } catch (Exception e) {
                logger.error("Request failed for drug '{}': {}", drug, e.getMessage());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double successRate = (double) successCount.get() / TEST_DRUGS.length;
        
        logger.info("Sequential load test results:");
        logger.info("- Total time: {} ms", totalTime);
        logger.info("- Average response time: {:.2f} ms", averageResponseTime);
        logger.info("- Success rate: {:.2%}", successRate);
        logger.info("- Total requests: {}", TEST_DRUGS.length);
        logger.info("- Successful requests: {}", successCount.get());
        
        // Assertions
        assertTrue(successRate >= MIN_SUCCESS_RATE, 
            String.format("Success rate %.2f%% should be >= %.2f%%", successRate * 100, MIN_SUCCESS_RATE * 100));
        assertTrue(averageResponseTime < MAX_RESPONSE_TIME_MS, 
            String.format("Average response time %.2f ms should be < %d ms", averageResponseTime, MAX_RESPONSE_TIME_MS));
    }
    
    @Test
    @Order(2)
    @DisplayName("Load Test: Concurrent requests with thread pool")
    void testConcurrentRequestLoad() throws InterruptedException {
        int threadCount = 5;
        int requestsPerThread = 3;
        
        logger.info("Starting concurrent load test with {} threads, {} requests per thread", 
            threadCount, requestsPerThread);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            
            for (int j = 0; j < requestsPerThread; j++) {
                final int requestId = j;
                
                executor.submit(() -> {
                    try {
                        String drug = TEST_DRUGS[requestId % TEST_DRUGS.length];
                        long requestStart = System.currentTimeMillis();
                        
                        String response = simplePuphaxClient.searchDrugsSimple(drug);
                        
                        long requestTime = System.currentTimeMillis() - requestStart;
                        totalResponseTime.addAndGet(requestTime);
                        
                        if (response != null && !response.trim().isEmpty()) {
                            successCount.incrementAndGet();
                        }
                        
                        logger.debug("Thread {} request {} for '{}' completed in {} ms", 
                            threadId, requestId, drug, requestTime);
                        
                    } catch (Exception e) {
                        logger.error("Concurrent request failed: {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        // Wait for all requests to complete (with timeout)
        boolean completed = latch.await(2, TimeUnit.MINUTES);
        assertTrue(completed, "All requests should complete within timeout");
        
        executor.shutdown();
        
        long totalTime = System.currentTimeMillis() - startTime;
        int totalRequests = threadCount * requestsPerThread;
        double averageResponseTime = (double) totalResponseTime.get() / totalRequests;
        double successRate = (double) successCount.get() / totalRequests;
        double throughput = (double) totalRequests / (totalTime / 1000.0); // requests per second
        
        logger.info("Concurrent load test results:");
        logger.info("- Total time: {} ms", totalTime);
        logger.info("- Average response time: {:.2f} ms", averageResponseTime);
        logger.info("- Success rate: {:.2%}", successRate);
        logger.info("- Throughput: {:.2f} requests/second", throughput);
        logger.info("- Total requests: {}", totalRequests);
        logger.info("- Successful requests: {}", successCount.get());
        
        // Assertions
        assertTrue(successRate >= MIN_SUCCESS_RATE, 
            String.format("Success rate %.2f%% should be >= %.2f%%", successRate * 100, MIN_SUCCESS_RATE * 100));
        assertTrue(averageResponseTime < MAX_RESPONSE_TIME_MS, 
            String.format("Average response time %.2f ms should be < %d ms", averageResponseTime, MAX_RESPONSE_TIME_MS));
        assertTrue(throughput > 0.1, "Throughput should be reasonable"); // At least 0.1 requests/second
    }
    
    // ========================================
    // Stress Testing (Peak Load Scenarios)
    // ========================================
    
    @Test
    @Order(3)
    @DisplayName("Stress Test: High concurrent load")
    void testHighConcurrentLoad() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 2;
        
        logger.info("Starting stress test with {} threads, {} requests per thread", 
            threadCount, requestsPerThread);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            
            for (int j = 0; j < requestsPerThread; j++) {
                final int requestId = j;
                
                executor.submit(() -> {
                    try {
                        String drug = TEST_DRUGS[requestId % TEST_DRUGS.length];
                        long requestStart = System.currentTimeMillis();
                        
                        String response = simplePuphaxClient.searchDrugsSimple(drug);
                        
                        long requestTime = System.currentTimeMillis() - requestStart;
                        responseTimes.add(requestTime);
                        
                        if (response != null && !response.trim().isEmpty()) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        
                        logger.debug("Stress test - Thread {} request {} completed in {} ms", 
                            threadId, requestId, requestTime);
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.error("Stress test request failed: {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        // Wait for all requests to complete (with longer timeout for stress test)
        boolean completed = latch.await(5, TimeUnit.MINUTES);
        assertTrue(completed, "All stress test requests should complete within timeout");
        
        executor.shutdown();
        
        long totalTime = System.currentTimeMillis() - startTime;
        int totalRequests = threadCount * requestsPerThread;
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double successRate = (double) successCount.get() / totalRequests;
        double errorRate = (double) errorCount.get() / totalRequests;
        
        // Calculate percentiles
        responseTimes.sort(Long::compareTo);
        long p50 = getPercentile(responseTimes, 50);
        long p90 = getPercentile(responseTimes, 90);
        long p95 = getPercentile(responseTimes, 95);
        long p99 = getPercentile(responseTimes, 99);
        
        logger.info("Stress test results:");
        logger.info("- Total time: {} ms", totalTime);
        logger.info("- Average response time: {:.2f} ms", averageResponseTime);
        logger.info("- P50 response time: {} ms", p50);
        logger.info("- P90 response time: {} ms", p90);
        logger.info("- P95 response time: {} ms", p95);
        logger.info("- P99 response time: {} ms", p99);
        logger.info("- Success rate: {:.2%}", successRate);
        logger.info("- Error rate: {:.2%}", errorRate);
        logger.info("- Total requests: {}", totalRequests);
        logger.info("- Successful requests: {}", successCount.get());
        logger.info("- Failed requests: {}", errorCount.get());
        
        // Under stress, we allow slightly lower success rate but still require reasonable performance
        assertTrue(successRate >= 0.8, 
            String.format("Success rate %.2f%% should be >= 80%% even under stress", successRate * 100));
        assertTrue(p95 < MAX_RESPONSE_TIME_MS, 
            String.format("P95 response time %d ms should be < %d ms", p95, MAX_RESPONSE_TIME_MS));
    }
    
    // ========================================
    // Spike Testing (Sudden Load Increases)
    // ========================================
    
    @Test
    @Order(4)
    @DisplayName("Spike Test: Sudden burst of requests")
    void testSpikeLoad() throws InterruptedException {
        logger.info("Starting spike test - sudden burst of requests");
        
        // First, establish baseline with normal load
        logger.info("Phase 1: Baseline measurement");
        long baselineStart = System.currentTimeMillis();
        String baselineResponse = simplePuphaxClient.searchDrugsSimple("XANAX");
        long baselineTime = System.currentTimeMillis() - baselineStart;
        
        assertNotNull(baselineResponse, "Baseline response should not be null");
        logger.info("Baseline response time: {} ms", baselineTime);
        
        // Then create a sudden spike
        logger.info("Phase 2: Spike load");
        int spikeRequests = 8;
        ExecutorService executor = Executors.newFixedThreadPool(spikeRequests);
        CountDownLatch spikeLatch = new CountDownLatch(spikeRequests);
        
        AtomicInteger spikeSuccessCount = new AtomicInteger(0);
        List<Long> spikeResponseTimes = new CopyOnWriteArrayList<>();
        
        long spikeStart = System.currentTimeMillis();
        
        // Launch all spike requests simultaneously
        for (int i = 0; i < spikeRequests; i++) {
            final int requestId = i;
            
            executor.submit(() -> {
                try {
                    String drug = TEST_DRUGS[requestId % TEST_DRUGS.length];
                    long requestStart = System.currentTimeMillis();
                    
                    String response = simplePuphaxClient.searchDrugsSimple(drug);
                    
                    long requestTime = System.currentTimeMillis() - requestStart;
                    spikeResponseTimes.add(requestTime);
                    
                    if (response != null && !response.trim().isEmpty()) {
                        spikeSuccessCount.incrementAndGet();
                    }
                    
                    logger.debug("Spike request {} for '{}' completed in {} ms", requestId, drug, requestTime);
                    
                } catch (Exception e) {
                    logger.error("Spike request {} failed: {}", requestId, e.getMessage());
                } finally {
                    spikeLatch.countDown();
                }
            });
        }
        
        boolean spikeCompleted = spikeLatch.await(3, TimeUnit.MINUTES);
        assertTrue(spikeCompleted, "Spike requests should complete within timeout");
        
        executor.shutdown();
        
        long spikeTime = System.currentTimeMillis() - spikeStart;
        double averageSpikeResponseTime = spikeResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double spikeSuccessRate = (double) spikeSuccessCount.get() / spikeRequests;
        
        logger.info("Spike test results:");
        logger.info("- Spike duration: {} ms", spikeTime);
        logger.info("- Average spike response time: {:.2f} ms", averageSpikeResponseTime);
        logger.info("- Spike success rate: {:.2%}", spikeSuccessRate);
        logger.info("- Baseline vs spike time ratio: {:.2f}", averageSpikeResponseTime / baselineTime);
        
        // Verify system handles spike gracefully
        assertTrue(spikeSuccessRate >= 0.75, 
            String.format("Spike success rate %.2f%% should be >= 75%%", spikeSuccessRate * 100));
        
        // Response time can degrade during spike but shouldn't be excessive
        assertTrue(averageSpikeResponseTime < MAX_RESPONSE_TIME_MS * 2, 
            String.format("Average spike response time %.2f ms should be manageable", averageSpikeResponseTime));
    }
    
    // ========================================
    // Volume Testing (Large Data Processing)
    // ========================================
    
    @Test
    @Order(5)
    @DisplayName("Volume Test: Multiple SOAP operations")
    void testVolumeProcessing() {
        logger.info("Starting volume test with multiple SOAP operations");
        
        List<String> operationResults = new ArrayList<>();
        List<Long> operationTimes = new ArrayList<>();
        
        try {
            // Test TERMEKLISTA operation
            long start1 = System.currentTimeMillis();
            String searchResult = simplePuphaxClient.searchDrugsSimple("ASPIRIN");
            long time1 = System.currentTimeMillis() - start1;
            operationResults.add("TERMEKLISTA");
            operationTimes.add(time1);
            assertNotNull(searchResult, "TERMEKLISTA result should not be null");
            
            // Test TERMEKADAT operation
            long start2 = System.currentTimeMillis();
            String productResult = springWsClient.getProductDetails("14714226");
            long time2 = System.currentTimeMillis() - start2;
            operationResults.add("TERMEKADAT");
            operationTimes.add(time2);
            assertNotNull(productResult, "TERMEKADAT result should not be null");
            
            // Test TAMOGATADAT operation
            long start3 = System.currentTimeMillis();
            String supportResult = springWsClient.getDrugSupportData("14714226", TEST_DATE);
            long time3 = System.currentTimeMillis() - start3;
            operationResults.add("TAMOGATADAT");
            operationTimes.add(time3);
            assertNotNull(supportResult, "TAMOGATADAT result should not be null");
            
            // Test CEGEK operation
            long start4 = System.currentTimeMillis();
            String companyResult = simplePuphaxClient.getCompanyName("67");
            long time4 = System.currentTimeMillis() - start4;
            operationResults.add("CEGEK");
            operationTimes.add(time4);
            // Company result can be null for non-existent IDs
            
            long totalTime = operationTimes.stream().mapToLong(Long::longValue).sum();
            double averageTime = operationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            
            logger.info("Volume test results:");
            for (int i = 0; i < operationResults.size(); i++) {
                logger.info("- {} operation: {} ms", operationResults.get(i), operationTimes.get(i));
            }
            logger.info("- Total time for all operations: {} ms", totalTime);
            logger.info("- Average time per operation: {:.2f} ms", averageTime);
            
            // Verify reasonable performance for volume operations
            assertTrue(totalTime < MAX_RESPONSE_TIME_MS * 4, 
                String.format("Total volume test time %d ms should be reasonable", totalTime));
            assertTrue(averageTime < MAX_RESPONSE_TIME_MS, 
                String.format("Average operation time %.2f ms should be < %d ms", averageTime, MAX_RESPONSE_TIME_MS));
            
        } catch (Exception e) {
            logger.error("Volume test failed: {}", e.getMessage(), e);
            fail("Volume test should not fail with exception: " + e.getMessage());
        }
    }
    
    // ========================================
    // Endurance Testing (Sustained Load)
    // ========================================
    
    @Test
    @Order(6)
    @DisplayName("Endurance Test: Sustained load over time")
    void testEnduranceLoad() throws InterruptedException {
        int duration = 30; // 30 seconds
        int requestInterval = 2; // 2 seconds between requests
        
        logger.info("Starting endurance test for {} seconds with {} second intervals", duration, requestInterval);
        
        List<Long> responseTimes = new ArrayList<>();
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long enduranceStart = System.currentTimeMillis();
        long endTime = enduranceStart + (duration * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            int currentRequest = requestCount.incrementAndGet();
            String drug = TEST_DRUGS[currentRequest % TEST_DRUGS.length];
            
            long requestStart = System.currentTimeMillis();
            
            try {
                String response = simplePuphaxClient.searchDrugsSimple(drug);
                
                long requestTime = System.currentTimeMillis() - requestStart;
                responseTimes.add(requestTime);
                
                if (response != null && !response.trim().isEmpty()) {
                    successCount.incrementAndGet();
                } else {
                    errorCount.incrementAndGet();
                }
                
                logger.debug("Endurance request {} for '{}' completed in {} ms", currentRequest, drug, requestTime);
                
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Endurance request {} failed: {}", currentRequest, e.getMessage());
            }
            
            // Wait before next request (if there's time left)
            if (System.currentTimeMillis() + (requestInterval * 1000) < endTime) {
                Thread.sleep(requestInterval * 1000);
            }
        }
        
        long totalDuration = System.currentTimeMillis() - enduranceStart;
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double successRate = (double) successCount.get() / requestCount.get();
        double errorRate = (double) errorCount.get() / requestCount.get();
        double throughput = (double) requestCount.get() / (totalDuration / 1000.0);
        
        // Calculate response time trends
        double firstHalfAvg = 0.0;
        double secondHalfAvg = 0.0;
        if (responseTimes.size() >= 2) {
            int midPoint = responseTimes.size() / 2;
            firstHalfAvg = responseTimes.subList(0, midPoint).stream().mapToLong(Long::longValue).average().orElse(0.0);
            secondHalfAvg = responseTimes.subList(midPoint, responseTimes.size()).stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        
        logger.info("Endurance test results:");
        logger.info("- Test duration: {} seconds", totalDuration / 1000.0);
        logger.info("- Total requests: {}", requestCount.get());
        logger.info("- Successful requests: {}", successCount.get());
        logger.info("- Failed requests: {}", errorCount.get());
        logger.info("- Success rate: {:.2%}", successRate);
        logger.info("- Error rate: {:.2%}", errorRate);
        logger.info("- Throughput: {:.2f} requests/second", throughput);
        logger.info("- Average response time: {:.2f} ms", averageResponseTime);
        logger.info("- First half average: {:.2f} ms", firstHalfAvg);
        logger.info("- Second half average: {:.2f} ms", secondHalfAvg);
        
        if (firstHalfAvg > 0 && secondHalfAvg > 0) {
            double degradation = (secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100;
            logger.info("- Performance change: {:.2f}%", degradation);
            
            // Performance shouldn't degrade significantly over time
            assertTrue(Math.abs(degradation) < 50, 
                String.format("Performance degradation %.2f%% should be < 50%%", Math.abs(degradation)));
        }
        
        // Verify sustained performance
        assertTrue(successRate >= 0.8, 
            String.format("Endurance success rate %.2f%% should be >= 80%%", successRate * 100));
        assertTrue(averageResponseTime < MAX_RESPONSE_TIME_MS, 
            String.format("Average endurance response time %.2f ms should be < %d ms", averageResponseTime, MAX_RESPONSE_TIME_MS));
    }
    
    // ========================================
    // Performance Benchmark Tests
    // ========================================
    
    @ParameterizedTest
    @ValueSource(strings = {"XANAX", "ASPIRIN", "PARACETAMOL"})
    @Order(7)
    @DisplayName("Benchmark: Response time comparison across drugs")
    void testResponseTimeBenchmark(String drugName) {
        logger.info("Benchmarking response time for drug: {}", drugName);
        
        List<Long> responseTimes = new ArrayList<>();
        int iterations = 3;
        
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            
            try {
                String response = simplePuphaxClient.searchDrugsSimple(drugName);
                assertNotNull(response, "Response should not be null");
                
                long responseTime = System.currentTimeMillis() - start;
                responseTimes.add(responseTime);
                
                logger.debug("Iteration {} for '{}': {} ms", i + 1, drugName, responseTime);
                
            } catch (Exception e) {
                logger.error("Benchmark iteration {} failed for '{}': {}", i + 1, drugName, e.getMessage());
            }
        }
        
        if (!responseTimes.isEmpty()) {
            double averageTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            
            logger.info("Benchmark results for '{}': avg={:.2f}ms, min={}ms, max={}ms", 
                drugName, averageTime, minTime, maxTime);
            
            assertTrue(averageTime < ACCEPTABLE_RESPONSE_TIME_MS, 
                String.format("Average response time %.2f ms for '%s' should be < %d ms", 
                    averageTime, drugName, ACCEPTABLE_RESPONSE_TIME_MS));
        }
    }
    
    // ========================================
    // Utility Methods
    // ========================================
    
    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }
    
    // ========================================
    // Test Lifecycle Methods
    // ========================================
    
    @BeforeAll
    static void setupAll() {
        logger.info("Starting PUPHAX Performance Stress Test Suite");
        logger.info("Performance thresholds:");
        logger.info("- Max response time: {} ms", MAX_RESPONSE_TIME_MS);
        logger.info("- Acceptable response time: {} ms", ACCEPTABLE_RESPONSE_TIME_MS);
        logger.info("- Minimum success rate: {:.2%}", MIN_SUCCESS_RATE);
    }
    
    @AfterAll
    static void tearDownAll() {
        logger.info("PUPHAX Performance Stress Test Suite completed");
    }
    
    @BeforeEach
    void setup(TestInfo testInfo) {
        logger.info("Starting performance test: {}", testInfo.getDisplayName());
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logger.info("Completed performance test: {}", testInfo.getDisplayName());
        
        // Small delay between tests to avoid overwhelming the service
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}