
package com.leamary.codepilotai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RequestSizeFilter extends OncePerRequestFilter {

    // Maximum review request size: 256 KB
    private static final int MAX_REVIEW_SIZE =
            256 * 1024;

    // Maximum login/register request size: 16 KB
    private static final int MAX_AUTH_SIZE =
            16 * 1024;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        int maxSize;

        // Only check POST requests
        if (!"POST".equalsIgnoreCase(
                request.getMethod())) {

            filterChain.doFilter(request, response);
            return;
        }

        // Select size limit based on endpoint
        if ("/api/review".equals(path)) {

            maxSize = MAX_REVIEW_SIZE;

        } else if ("/api/auth/login".equals(path) ||
                "/api/auth/register".equals(path)) {

            maxSize = MAX_AUTH_SIZE;

        } else {

            filterChain.doFilter(request, response);
            return;
        }

        // Reject oversized Content-Length immediately
        long contentLength =
                request.getContentLengthLong();

        if (contentLength > maxSize) {

            rejectRequest(response, maxSize);
            return;
        }

        // Read only up to the maximum size + 1 byte.
        // This also protects requests without Content-Length.
        byte[] body = request.getInputStream()
                .readNBytes(maxSize + 1);

        if (body.length > maxSize) {

            rejectRequest(response, maxSize);
            return;
        }

        // Restore the request body for Spring Boot
        HttpServletRequest wrappedRequest =
                new CachedBodyRequest(request, body);

        filterChain.doFilter(
                wrappedRequest,
                response
        );
    }

    // Return a safe JSON error response
    private void rejectRequest(
            HttpServletResponse response,
            int maxSize) throws IOException {

        response.setStatus(413);

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.getWriter().write(
                """
                {
                    "status": 413,
                    "error": "Payload Too Large",
                    "message": "Request body exceeds the maximum allowed size of %d KB."
                }
                """.formatted(maxSize / 1024)
        );
    }

    // Wrapper that allows Spring Boot to read
    // the request body after our filter checks it
    private static class CachedBodyRequest
            extends HttpServletRequestWrapper {

        private final byte[] body;

        public CachedBodyRequest(
                HttpServletRequest request,
                byte[] body) {

            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(body);

            return new ServletInputStream() {

                @Override
                public int read() {

                    return inputStream.read();
                }

                @Override
                public int read(
                        byte[] bytes,
                        int offset,
                        int length) {

                    return inputStream.read(
                            bytes,
                            offset,
                            length
                    );
                }

                @Override
                public boolean isFinished() {

                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {

                    return true;
                }

                @Override
                public void setReadListener(
                        ReadListener readListener) {

                    throw new UnsupportedOperationException(
                            "Non-blocking reads are not supported."
                    );
                }
            };
        }

        @Override
        public BufferedReader getReader()
                throws IOException {

            String encoding =
                    getCharacterEncoding();

            if (encoding == null) {

                encoding =
                        StandardCharsets.UTF_8.name();
            }

            return new BufferedReader(
                    new InputStreamReader(
                            getInputStream(),
                            encoding
                    )
            );
        }

        @Override
        public int getContentLength() {

            return body.length;
        }

        @Override
        public long getContentLengthLong() {

            return body.length;
        }
    }
}
