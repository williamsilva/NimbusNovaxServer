package com.nimbusnovax.common.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import org.junit.jupiter.api.Test;

class RateLimitFilterTest {

  @Test
  void allowsRequestsWithinCapacity() throws Exception {
    RateLimitProperties props = properties(3, 60);
    RateLimitFilter filter = new RateLimitFilter(props);

    for (int i = 0; i < 3; i++) {
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(request("/bff/v1/works", "10.0.0.1"), response(), chain);
      verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
  }

  @Test
  void blocksWithTooManyRequestsOnceCapacityIsExceeded() throws Exception {
    RateLimitProperties props = properties(2, 60);
    RateLimitFilter filter = new RateLimitFilter(props);
    String ip = "10.0.0.2";

    filter.doFilter(request("/bff/v1/works", ip), response(), mock(FilterChain.class));
    filter.doFilter(request("/bff/v1/works", ip), response(), mock(FilterChain.class));

    FilterChain thirdChain = mock(FilterChain.class);
    HttpServletResponse thirdResponse = response();
    filter.doFilter(request("/bff/v1/works", ip), thirdResponse, thirdChain);

    verify(thirdChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(thirdResponse).setStatus(429);
  }

  @Test
  void tracksEachIpIndependently() throws Exception {
    RateLimitProperties props = properties(1, 60);
    RateLimitFilter filter = new RateLimitFilter(props);

    FilterChain chainForIpOne = mock(FilterChain.class);
    filter.doFilter(request("/bff/v1/works", "10.0.0.3"), response(), chainForIpOne);
    verify(chainForIpOne).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    // IP diferente não é afetado pela janela do primeiro, mesmo com capacity=1.
    FilterChain chainForIpTwo = mock(FilterChain.class);
    filter.doFilter(request("/bff/v1/works", "10.0.0.4"), response(), chainForIpTwo);
    verify(chainForIpTwo).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void ignoresPathsOutsideBffAndApi() throws Exception {
    RateLimitProperties props = properties(1, 60);
    RateLimitFilter filter = new RateLimitFilter(props);
    String ip = "10.0.0.5";

    for (int i = 0; i < 5; i++) {
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(request("/actuator/health", ip), response(), chain);
      verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
  }

  @Test
  void doesNothingWhenDisabled() throws Exception {
    RateLimitProperties props = properties(1, 60);
    props.setEnabled(false);
    RateLimitFilter filter = new RateLimitFilter(props);
    String ip = "10.0.0.6";

    for (int i = 0; i < 5; i++) {
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(request("/bff/v1/works", ip), response(), chain);
      verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
  }

  private RateLimitProperties properties(int capacity, int windowSeconds) {
    RateLimitProperties props = new RateLimitProperties();
    props.setCapacity(capacity);
    props.setWindowSeconds(windowSeconds);
    return props;
  }

  private HttpServletRequest request(String path, String remoteAddr) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getRemoteAddr()).thenReturn(remoteAddr);
    return request;
  }

  private HttpServletResponse response() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(mock(PrintWriter.class));
    return response;
  }
}
