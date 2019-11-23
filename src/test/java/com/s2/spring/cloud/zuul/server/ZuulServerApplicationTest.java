package com.s2.spring.cloud.zuul.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.netflix.hystrix.exception.HystrixTimeoutException;
import com.netflix.zuul.context.RequestContext;
import com.s2.spring.cloud.zuul.server.filter.AuthRequestHeaderFilter;
import com.s2.spring.cloud.zuul.server.filter.CorsFilter;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = ZuulServerApplication.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
public class ZuulServerApplicationTest {

	private AuthRequestHeaderFilter authFilter;

	@Autowired
	private WebApplicationContext wac;
	private MockMvc mockMvc;

	private static final String ROUTE = "*";
	private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

	@Before
	public void setup() {
		authFilter = new AuthRequestHeaderFilter();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Autowired
	private ZuulFallbackProvider fallback;

	@Test
	public void testWhenGetRouteThenReturnWeatherServiceRoute() {
		assertEquals(ROUTE, fallback.getRoute());

	}

	@Test
	public void testFallbackResponse_whenHystrixException_thenGatewayTimeout() throws Exception {
		HystrixTimeoutException exception = new HystrixTimeoutException();
		ClientHttpResponse response = fallback.fallbackResponse(ROUTE, exception);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	public void testAuthFilter() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		OAuth2Authentication principal = new OAuth2Authentication(getOauth2Request(), getAuthentication());
		request.setUserPrincipal(principal);
		RequestContext context = Mockito.mock(RequestContext.class);
		Mockito.when(context.getRequest()).thenReturn(request);
		RequestContext.testSetCurrentContext(context);
		authFilter.run();
	}

	@Test
	public void testCORSFilter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = new MockFilterChain();

		CorsFilter corsFilter = new CorsFilter();
		corsFilter.init(new MockFilterConfig());
		request.addHeader("Origin", "Test");
		request.addHeader("Access-Control-Request-Method", "GET");
		request.setMethod("OPTIONS");

		corsFilter.doFilter(request, response, filterChain);
		assertEquals(response.getHeader("Access-Control-Allow-Origin"),"Test");
		
		request.setMethod("OTHER");
		corsFilter.doFilter(request, response, filterChain);
		corsFilter.destroy();
	}

	private OAuth2Request getOauth2Request() {
		String clientId = "oauth-client-id";
		List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("Everything");
		OAuth2Request oAuth2Request = new OAuth2Request(Collections.emptyMap(), clientId, authorities, true,
				Collections.emptySet(), Collections.emptySet(), "http://redirect.com", Collections.emptySet(),
				Collections.emptyMap());
		return oAuth2Request;
	}

	private Authentication getAuthentication() {
		List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("Everything");
		HashMap<String, String> details = new HashMap<String, String>();
		TestingAuthenticationToken token = new TestingAuthenticationToken("test", null, authorities);
		token.setAuthenticated(true);
		token.setDetails(details);
		return token;
	}
}
