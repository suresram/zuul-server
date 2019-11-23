package com.s2.spring.cloud.zuul.server;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.netflix.hystrix.exception.HystrixTimeoutException;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = ZuulServerApplication.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
public class ZuulServerApplicationTest {

	@Autowired
	private WebApplicationContext wac;
	private MockMvc mockMvc;

	private static final String ROUTE = "*";
	private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

	@Before
	public void setup() {
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

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	public void testZuulFallback() throws Exception {
		ResultActions result = mockMvc.perform(post("/any").accept(CONTENT_TYPE)).andExpect(status().isOk());
	}
}
