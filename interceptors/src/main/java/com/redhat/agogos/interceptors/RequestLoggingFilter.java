package com.redhat.agogos.interceptors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@PreMatching
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private boolean debugHeaders = false;
    private boolean debugContent = false;

    @Override
    public void filter(ContainerRequestContext crc) {
        // Tekton trigger interceptors do not include Content-Type, which results in a 415 response.
        crc.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);

        if (debugHeaders) {
            LOG.info(crc.getMethod() + " " + crc.getUriInfo().getAbsolutePath());
            for (String key : crc.getHeaders().keySet()) {
                LOG.info("Header => " + key + ": " + crc.getHeaders().get(key));
            }
        }

        // Note that reading the input stream will make the request content unavailable elsewhere.
        if (debugContent) {
            try {
                StringBuilder textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader(crc.getEntityStream(), StandardCharsets.UTF_8))) {
                    int c = 0;
                    while ((c = reader.read()) != -1) {
                        textBuilder.append((char) c);
                    }
                }
                LOG.info("Content => " + textBuilder.toString());
            } catch (Exception e) {
            }
        }
    }
}
