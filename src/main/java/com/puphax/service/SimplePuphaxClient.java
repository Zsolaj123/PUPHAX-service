package com.puphax.service;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Simplified PUPHAX client using Java 11 HTTP client.
 * Directly calls PUPHAX without complex Spring WS setup.
 */
@Service
public class SimplePuphaxClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplePuphaxClient.class);
    private static final String PUPHAX_ENDPOINT = "https://puphax.neak.gov.hu/PUPHAXWS";
    private static final String USERNAME = "PUPHAX";
    private static final String PASSWORD = "puphax";
    
    private final CloseableHttpClient httpClient;
    
    // Cache for company names to avoid repeated lookups
    private final Map<String, String> companyNameCache = new ConcurrentHashMap<>();
    
    @Autowired
    public SimplePuphaxClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        logger.info("SimplePuphaxClient initialized with connection pooling HTTP client");
    }
    
    /**
     * Search drugs using direct HTTP call.
     */
    public String searchDrugsSimple(String searchTerm) {
        try {
            String soapRequest = buildTermeklistaRequest(searchTerm, LocalDate.now());
            
            logger.info("Making direct HTTP call to PUPHAX for search term: {}", searchTerm);
            logger.debug("SOAP Request: {}", soapRequest);
            
            return executeSoapCall(soapRequest, "TERMEKLISTA");
            
        } catch (Exception e) {
            logger.error("Direct PUPHAX call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call PUPHAX", e);
        }
    }
    
    private String getBasicAuth() {
        String auth = USERNAME + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
    
    private String retryWithBasicAuth(String soapRequest, String soapAction) throws Exception {
        HttpPost request = new HttpPost(PUPHAX_ENDPOINT);
        request.setHeader("Content-Type", "text/xml; charset=UTF-8");
        request.setHeader("SOAPAction", soapAction);
        request.setHeader("Authorization", getBasicAuth());
        request.setEntity(new StringEntity(soapRequest, StandardCharsets.UTF_8));
        
        try (ClassicHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, Charset.forName("ISO-8859-2"));
            
            if (statusCode == 200) {
                return fixCharacterEncoding(responseBody);
            } else {
                throw new RuntimeException("Basic auth also failed: " + statusCode);
            }
        }
    }
    
    private String createDigestAuthHeader(String wwwAuthenticate, String method, String uri, String requestBody) {
        try {
            // Parse the challenge
            Pattern realmPattern = Pattern.compile("realm=\"([^\"]+)\"");
            Pattern noncePattern = Pattern.compile("nonce=\"([^\"]+)\"");
            Pattern opaquePattern = Pattern.compile("opaque=\"([^\"]+)\"");
            Pattern qopPattern = Pattern.compile("qop=\"([^\"]+)\"");
            
            Matcher realmMatcher = realmPattern.matcher(wwwAuthenticate);
            Matcher nonceMatcher = noncePattern.matcher(wwwAuthenticate);
            Matcher opaqueMatcher = opaquePattern.matcher(wwwAuthenticate);
            Matcher qopMatcher = qopPattern.matcher(wwwAuthenticate);
            
            String realm = realmMatcher.find() ? realmMatcher.group(1) : "";
            String nonce = nonceMatcher.find() ? nonceMatcher.group(1) : "";
            String opaque = opaqueMatcher.find() ? opaqueMatcher.group(1) : "";
            String qop = qopMatcher.find() ? qopMatcher.group(1) : "auth";
            
            // Generate client nonce
            String cnonce = UUID.randomUUID().toString().replace("-", "");
            String nc = "00000001";
            
            // Calculate response
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // HA1 = MD5(username:realm:password)
            String ha1Input = USERNAME + ":" + realm + ":" + PASSWORD;
            byte[] ha1Bytes = md.digest(ha1Input.getBytes("UTF-8"));
            String ha1 = bytesToHex(ha1Bytes);
            
            // HA2 = MD5(method:uri)
            String ha2Input = method + ":" + uri;
            md.reset();
            byte[] ha2Bytes = md.digest(ha2Input.getBytes("UTF-8"));
            String ha2 = bytesToHex(ha2Bytes);
            
            // response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
            String responseInput = ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2;
            md.reset();
            byte[] responseBytes = md.digest(responseInput.getBytes("UTF-8"));
            String response = bytesToHex(responseBytes);
            
            // Build Authorization header
            StringBuilder authHeader = new StringBuilder("Digest ");
            authHeader.append("username=\"").append(USERNAME).append("\", ");
            authHeader.append("realm=\"").append(realm).append("\", ");
            authHeader.append("nonce=\"").append(nonce).append("\", ");
            authHeader.append("uri=\"").append(uri).append("\", ");
            authHeader.append("qop=").append(qop).append(", ");
            authHeader.append("nc=").append(nc).append(", ");
            authHeader.append("cnonce=\"").append(cnonce).append("\", ");
            authHeader.append("response=\"").append(response).append("\"");
            
            if (!opaque.isEmpty()) {
                authHeader.append(", opaque=\"").append(opaque).append("\"");
            }
            
            return authHeader.toString();
            
        } catch (Exception e) {
            logger.error("Failed to create digest auth header", e);
            throw new RuntimeException("Digest auth creation failed", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private String buildTermeklistaRequest(String searchTerm, LocalDate searchDate) {
        String dateStr = searchDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String filterXml = "";
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Format the filter XML with proper indentation like in the documentation
            // Add wildcard % for partial matches
            String termWithWildcard = searchTerm.toUpperCase() + "%";
            filterXml = String.format("""
                <alapfilter>
                    <TNEV>%s</TNEV>
                </alapfilter>""", escapeXml(termWithWildcard));
        }
        
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:COBJIDLISTA-TERMEKLISTAInput>
                     <pup:DSP-DATE-IN>%s</pup:DSP-DATE-IN>
                     <pup:SXFILTER-VARCHAR2-IN>
                        <![CDATA[
%s
                        ]]>
                     </pup:SXFILTER-VARCHAR2-IN>
                  </pup:COBJIDLISTA-TERMEKLISTAInput>
               </soapenv:Body>
            </soapenv:Envelope>""", dateStr, filterXml);
    }
    
    /**
     * Get product basic data using TERMEKADAT operation.
     * This returns product name, ATC code, manufacturer etc.
     */
    public String getProductData(String productId, LocalDate searchDate) {
        try {
            String soapRequest = buildTermekadatRequest(productId, searchDate);
            
            logger.info("Making direct HTTP call to PUPHAX TERMEKADAT for product ID: {}", productId);
            logger.debug("TERMEKADAT SOAP Request: {}", soapRequest);
            
            return executeSoapCall(soapRequest, "TERMEKADAT");
            
        } catch (Exception e) {
            logger.error("TERMEKADAT call failed for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Failed to get product data", e);
        }
    }
    
    /**
     * Get product support data using TAMOGATADAT operation.
     */
    public String getProductSupportData(String productId, LocalDate searchDate) {
        try {
            String soapRequest = buildTamogatadatRequest(productId, searchDate);
            
            logger.info("Making direct HTTP call to PUPHAX TAMOGATADAT for product ID: {}", productId);
            logger.debug("TAMOGATADAT SOAP Request: {}", soapRequest);
            
            return executeSoapCall(soapRequest, "TAMOGATADAT");
            
        } catch (Exception e) {
            logger.error("TAMOGATADAT call failed for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Failed to get product support data", e);
        }
    }
    
    private String buildTermekadatRequest(String productId, LocalDate searchDate) {
        // Based on the sample document, TERMEKADAT only needs the product ID
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:COBJTERMEKADAT-TERMEKADATInput>
                     <pup:NID-NUMBER-IN>%s</pup:NID-NUMBER-IN>
                  </pup:COBJTERMEKADAT-TERMEKADATInput>
               </soapenv:Body>
            </soapenv:Envelope>""", productId);
    }
    
    private String buildTamogatadatRequest(String productId, LocalDate searchDate) {
        String dateStr = searchDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:COBJTAMOGAT-TAMOGATADATInput>
                     <pup:DSP-DATE-IN>%s</pup:DSP-DATE-IN>
                     <pup:NID-NUMBER-IN>%s</pup:NID-NUMBER-IN>
                  </pup:COBJTAMOGAT-TAMOGATADATInput>
               </soapenv:Body>
            </soapenv:Envelope>""", dateStr, productId);
    }
    
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    /**
     * Execute a SOAP call to PUPHAX service with digest authentication.
     * This is a generic method for making SOAP calls to any PUPHAX endpoint.
     */
    private String executeSoapCall(String soapRequest, String soapAction) throws Exception {
        // Create HTTP POST request using Apache HttpClient
        HttpPost initialRequest = new HttpPost(PUPHAX_ENDPOINT);
        initialRequest.setHeader("Content-Type", "text/xml; charset=UTF-8");
        initialRequest.setHeader("SOAPAction", soapAction);
        initialRequest.setEntity(new StringEntity(soapRequest, StandardCharsets.UTF_8));
        
        try (ClassicHttpResponse initialResponse = httpClient.execute(initialRequest)) {
            int statusCode = initialResponse.getCode();
            HttpEntity entity = initialResponse.getEntity();
            String responseBody = EntityUtils.toString(entity, Charset.forName("ISO-8859-2"));
            
            if (statusCode == 200) {
                // If we get 200 immediately, PUPHAX might not require auth for this call
                String fixedResponseBody = fixCharacterEncoding(responseBody);
                logger.debug("PUPHAX response without auth (first 1000 chars): {}", 
                    fixedResponseBody.length() > 1000 ? fixedResponseBody.substring(0, 1000) : fixedResponseBody);
                return fixedResponseBody;
            } else if (statusCode == 401) {
                // Extract WWW-Authenticate header for digest challenge
                String authHeader = initialResponse.getFirstHeader("WWW-Authenticate") != null 
                    ? initialResponse.getFirstHeader("WWW-Authenticate").getValue() : "";
                logger.debug("Received digest challenge: {}", authHeader);
                
                if (authHeader.startsWith("Digest")) {
                    // Parse digest challenge and create response
                    String digestAuth = createDigestAuthHeader(authHeader, "POST", "/PUPHAXWS", soapRequest);
                    
                    // Retry with digest auth
                    HttpPost authRequest = new HttpPost(PUPHAX_ENDPOINT);
                    authRequest.setHeader("Content-Type", "text/xml; charset=UTF-8");
                    authRequest.setHeader("SOAPAction", soapAction);
                    authRequest.setHeader("Authorization", digestAuth);
                    authRequest.setEntity(new StringEntity(soapRequest, StandardCharsets.UTF_8));
                    
                    try (ClassicHttpResponse authResponse = httpClient.execute(authRequest)) {
                        int authStatusCode = authResponse.getCode();
                        HttpEntity authEntity = authResponse.getEntity();
                        String authResponseBody = EntityUtils.toString(authEntity, Charset.forName("ISO-8859-2"));
                        
                        if (authStatusCode == 200) {
                            String fixedAuthResponseBody = fixCharacterEncoding(authResponseBody);
                            logger.debug("PUPHAX authenticated response (first 1000 chars): {}", 
                                fixedAuthResponseBody.length() > 1000 ? fixedAuthResponseBody.substring(0, 1000) : fixedAuthResponseBody);
                            return fixedAuthResponseBody;
                        } else {
                            throw new RuntimeException("PUPHAX authentication failed. Status: " + authStatusCode);
                        }
                    }
                } else {
                    // Fallback to basic auth
                    return retryWithBasicAuth(soapRequest, soapAction);
                }
            } else {
                throw new RuntimeException("Unexpected response from PUPHAX. Status: " + statusCode);
            }
        }
    }

    /**
     * Fix character encoding issues in PUPHAX responses.
     * Since we're now reading responses as ISO-8859-2, this method just ensures
     * the XML declaration matches the actual encoding.
     */
    private String fixCharacterEncoding(String response) {
        // Since we're now correctly reading as ISO-8859-2, just fix the XML declaration
        if (response.contains("<?xml") && response.contains("UTF-8")) {
            response = response.replace("UTF-8", "ISO-8859-2");
        }
        return response;
    }
    
    /**
     * Get company name by ID from PUPHAX CEGEK table.
     * Results are cached to improve performance.
     */
    public String getCompanyName(String companyId) {
        if (companyId == null || companyId.isEmpty()) {
            return null;
        }
        
        // Check cache first
        if (companyNameCache.containsKey(companyId)) {
            return companyNameCache.get(companyId);
        }
        
        try {
            String soapRequest = buildCegekRequest(companyId);
            String response = executeSoapCall(soapRequest, "COBJALAP.TABCEGEK");
            
            // Extract company name from response
            String companyName = extractCompanyName(response);
            
            if (companyName != null && !companyName.isEmpty()) {
                companyNameCache.put(companyId, companyName);
                logger.info("Fetched company name for ID {}: {}", companyId, companyName);
            }
            
            return companyName;
            
        } catch (Exception e) {
            logger.error("Failed to get company name for ID {}: {}", companyId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Build SOAP request for CEGEK (companies) query.
     */
    private String buildCegekRequest(String companyId) {
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:COBJALAP-TABCEGEKInput>
                     <pup:SXFILTER-VARCHAR2-IN>
                       <![CDATA[
                            <alapfilter>
                                <CEGID>%s</CEGID>
                            </alapfilter>
                       ]]>
                     </pup:SXFILTER-VARCHAR2-IN>
                  </pup:COBJALAP-TABCEGEKInput>
               </soapenv:Body>
            </soapenv:Envelope>""", companyId);
    }
    
    /**
     * Extract company name from CEGEK response.
     */
    private String extractCompanyName(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // Look for ELNEVEZ tag which contains the company name
        Pattern pattern = Pattern.compile("<ELNEVEZ>([^<]+)</ELNEVEZ>");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
}