package com.juntao.commons.spring.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by major on 2017/1/5.
 */
public class HttpMethodOverrideHeaderFilter extends OncePerRequestFilter {
    private static final String X_HTTP_METHOD_OVERRIDE_HEADER = "X-HTTP-Method-Override";
    private static final Set<String> acceptedRequestMethodNameSet = Stream.of(RequestMethod.DELETE, RequestMethod.PUT)
            .map(RequestMethod::toString).collect(Collectors.toSet());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String headerValue = StringUtils.stripToEmpty(request.getHeader(X_HTTP_METHOD_OVERRIDE_HEADER));
        if (RequestMethod.POST.name().equals(request.getMethod()) && acceptedRequestMethodNameSet.contains(headerValue)) {
            String method = headerValue.toUpperCase(Locale.ENGLISH);
            HttpServletRequest wrapper = new HttpMethodRequestWrapper(request, method);
            filterChain.doFilter(wrapper, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {
        private final String method;

        public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
            super(request);
            this.method = method;
        }

        @Override
        public String getMethod() {
            return this.method;
        }
    }
}
