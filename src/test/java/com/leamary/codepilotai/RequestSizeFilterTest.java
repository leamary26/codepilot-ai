
package com.leamary.codepilotai;

import com.leamary.codepilotai.security.RequestSizeFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestSizeFilterTest {

    private RequestSizeFilter filter;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {

        filter = new RequestSizeFilter();

        response = new MockHttpServletResponse();

        filterChain = mock(FilterChain.class);
    }

    // Helper method to create HTTP requests

    private MockHttpServletRequest createRequest(
            String method,
            String path,
            String body) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        path
                );

        request.setServletPath(path);

        request.setContentType(
                "application/json"
        );

        request.setCharacterEncoding("UTF-8");

        request.setContent(
                body.getBytes(StandardCharsets.UTF_8)
        );

        return request;
    }

    // Test 1: Allow normal review requests

    @Test
    void shouldAllowNormalReviewRequest()
            throws Exception {

        String body =
                """
                {
                    "code": "print(123)",
                    "language": "Python"
                }
                """;

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/review",
                        body
                );

        // Verify that the body is preserved
        // after passing through the filter

        doAnswer(invocation -> {

            HttpServletRequest wrappedRequest =
                    invocation.getArgument(0);

            String receivedBody = new String(
                    wrappedRequest.getInputStream()
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );

            assertEquals(body, receivedBody);

            return null;

        }).when(filterChain)
                .doFilter(any(), eq(response));

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(200, response.getStatus());

        verify(filterChain)
                .doFilter(any(), eq(response));
    }

    // Test 2: Reject oversized review requests

    @Test
    void shouldRejectOversizedReviewRequest()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/review",
                        "a".repeat(300000)
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(413, response.getStatus());

        assertTrue(
                response.getContentAsString()
                        .contains("256 KB")
        );

        verifyNoInteractions(filterChain);
    }

    // Test 3: Reject oversized login requests

    @Test
    void shouldRejectOversizedLoginRequest()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/auth/login",
                        "a".repeat(20000)
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(413, response.getStatus());

        assertTrue(
                response.getContentAsString()
                        .contains("16 KB")
        );

        verifyNoInteractions(filterChain);
    }

    // Test 4: Reject oversized registration requests

    @Test
    void shouldRejectOversizedRegisterRequest()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/auth/register",
                        "a".repeat(20000)
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(413, response.getStatus());

        assertTrue(
                response.getContentAsString()
                        .contains("16 KB")
        );

        verifyNoInteractions(filterChain);
    }

    // Test 5: Allow normal login requests

    @Test
    void shouldAllowNormalLoginRequest()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/auth/login",
                        """
                        {
                            "email": "demo@codepilot.test",
                            "password": "ExamplePass123"
                        }
                        """
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(200, response.getStatus());

        verify(filterChain)
                .doFilter(any(), eq(response));
    }

    // Test 6: Allow GET requests without
    // applying the POST request size limit

    @Test
    void shouldAllowGetRequests()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "GET",
                        "/api/history",
                        ""
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(200, response.getStatus());

        verify(filterChain)
                .doFilter(request, response);
    }

    // Test 7: Reject oversized requests even
    // when Content-Length is unknown

    @Test
    void shouldRejectOversizedUnknownLengthRequest()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/review"
                ) {

                    @Override
                    public int getContentLength() {
                        return -1;
                    }

                    @Override
                    public long getContentLengthLong() {
                        return -1;
                    }
                };

        request.setServletPath("/api/review");

        request.setContentType("application/json");

        request.setContent(
                "a".repeat(300000)
                        .getBytes(StandardCharsets.UTF_8)
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(413, response.getStatus());

        verifyNoInteractions(filterChain);
    }

    // Test 8: Allow a request exactly at
    // the maximum review body size

    @Test
    void shouldAllowExactMaximumReviewSize()
            throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "POST",
                        "/api/review",
                        "a".repeat(256 * 1024)
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(200, response.getStatus());

        verify(filterChain)
                .doFilter(any(), eq(response));
    }
}

