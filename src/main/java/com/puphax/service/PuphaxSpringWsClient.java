package com.puphax.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.StringResult;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PUPHAX SOAP client using Spring WebServiceTemplate.
 * This implementation bypasses JAX-WS to avoid character encoding issues.
 * Based on the Spring Boot integration documentation recommendations.
 */
@Service
public class PuphaxSpringWsClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxSpringWsClient.class);
    
    @Autowired
    private WebServiceTemplate webServiceTemplate;
    
    /**
     * Search for drugs using TERMEKLISTA operation.
     * 
     * @param searchTerm Drug name or partial name to search for
     * @param searchDate The snapshot date for the search
     * @return XML response as string
     */
    public String searchDrugs(String searchTerm, LocalDate searchDate) {
        try {
            String soapRequest = buildTermeklistaRequest(searchTerm, searchDate);
            logger.info("Calling PUPHAX TERMEKLISTA with search term: {}, date: {}", searchTerm, searchDate);
            
            // Use marshalSendAndReceive which handles the request/response properly
            Object response = webServiceTemplate.marshalSendAndReceive(
                new org.springframework.ws.client.core.WebServiceMessageCallback() {
                    @Override
                    public void doWithMessage(org.springframework.ws.WebServiceMessage message) throws java.io.IOException, javax.xml.transform.TransformerException {
                        org.springframework.ws.soap.SoapMessage soapMessage = (org.springframework.ws.soap.SoapMessage) message;
                        soapMessage.setSoapAction("TERMEKLISTA");
                        
                        // Write the SOAP body
                        javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                        transformer.transform(new StringSource(soapRequest), soapMessage.getPayloadResult());
                    }
                }
            );
            
            // Convert response to string
            java.io.StringWriter writer = new java.io.StringWriter();
            javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            transformer.transform(new javax.xml.transform.dom.DOMSource((org.w3c.dom.Node) response), new javax.xml.transform.stream.StreamResult(writer));
            
            String responseStr = writer.toString();
            logger.debug("Received PUPHAX response: {} characters", responseStr.length());
            
            return responseStr;
            
        } catch (Exception e) {
            logger.error("PUPHAX TERMEKLISTA call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search drugs in PUPHAX", e);
        }
    }
    
    /**
     * Get drug support data using TAMOGATADAT operation.
     * 
     * @param productId The internal TERMEK.ID
     * @param searchDate The snapshot date
     * @return XML response as string
     */
    public String getDrugSupportData(String productId, LocalDate searchDate) {
        try {
            String soapRequest = buildTamogatadatRequest(productId, searchDate);
            logger.info("Calling PUPHAX TAMOGATADAT for product ID: {}, date: {}", productId, searchDate);
            
            StringSource requestSource = new StringSource(soapRequest);
            StringResult responseResult = new StringResult();
            
            webServiceTemplate.sendSourceAndReceiveToResult(
                requestSource,
                new SoapActionCallback("TAMOGATADAT"),
                responseResult
            );
            
            String response = responseResult.toString();
            logger.debug("Received PUPHAX support data: {} characters", response.length());
            
            return response;
            
        } catch (Exception e) {
            logger.error("PUPHAX TAMOGATADAT call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get drug support data from PUPHAX", e);
        }
    }
    
    /**
     * Get product details using TERMEKADAT operation.
     * 
     * @param productId The internal TERMEK.ID
     * @return XML response as string
     */
    public String getProductDetails(String productId) {
        try {
            String soapRequest = buildTermekadatRequest(productId);
            logger.info("Calling PUPHAX TERMEKADAT for product ID: {}", productId);
            
            StringSource requestSource = new StringSource(soapRequest);
            StringResult responseResult = new StringResult();
            
            webServiceTemplate.sendSourceAndReceiveToResult(
                requestSource,
                new SoapActionCallback("TERMEKADAT"),
                responseResult
            );
            
            String response = responseResult.toString();
            logger.debug("Received PUPHAX product details: {} characters", response.length());
            
            return response;
            
        } catch (Exception e) {
            logger.error("PUPHAX TERMEKADAT call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get product details from PUPHAX", e);
        }
    }
    
    /**
     * Build SOAP request for TERMEKLISTA operation.
     */
    private String buildTermeklistaRequest(String searchTerm, LocalDate searchDate) {
        String dateStr = searchDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Build the filter XML
        String filterXml = "";
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            filterXml = String.format("<alapfilter><TNEV>%s</TNEV></alapfilter>", escapeXml(searchTerm));
        }
        
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                              xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:C_OBJ_ID_LISTA_TERMEKLISTA-INPUT>
                     <pup:DSP-DATE-IN>%s</pup:DSP-DATE-IN>
                     <pup:SX-FILTER-VARCHAR2-IN>
                        <![CDATA[%s]]>
                     </pup:SX-FILTER-VARCHAR2-IN>
                  </pup:C_OBJ_ID_LISTA_TERMEKLISTA-INPUT>
               </soapenv:Body>
            </soapenv:Envelope>
            """, dateStr, filterXml);
    }
    
    /**
     * Build SOAP request for TAMOGATADAT operation.
     */
    private String buildTamogatadatRequest(String productId, LocalDate searchDate) {
        String dateStr = searchDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                              xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:C_OBJ_TAMOGAT_TAMOGATADAT-INPUT>
                     <pup:DSP-DATE-IN>%s</pup:DSP-DATE-IN>
                     <pup:N_ID-NUMBER-IN>%s</pup:N_ID-NUMBER-IN>
                  </pup:C_OBJ_TAMOGAT_TAMOGATADAT-INPUT>
               </soapenv:Body>
            </soapenv:Envelope>
            """, dateStr, productId);
    }
    
    /**
     * Build SOAP request for TERMEKADAT operation.
     */
    private String buildTermekadatRequest(String productId) {
        return String.format("""
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                              xmlns:pup="http://xmlns.oracle.com/orawsv/PUPHAX/PUPHAXWS">
               <soapenv:Header/>
               <soapenv:Body>
                  <pup:C_OBJ_TERMEKADAT_TERMEKADAT-INPUT>
                     <pup:N_ID-NUMBER-IN>%s</pup:N_ID-NUMBER-IN>
                  </pup:C_OBJ_TERMEKADAT_TERMEKADAT-INPUT>
               </soapenv:Body>
            </soapenv:Envelope>
            """, productId);
    }
    
    /**
     * Escape XML special characters.
     */
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
}