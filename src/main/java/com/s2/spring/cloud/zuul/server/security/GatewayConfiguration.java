package com.s2.spring.cloud.zuul.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
@EnableResourceServer
public class GatewayConfiguration extends ResourceServerConfigurerAdapter {
	@Override
	public void configure(final HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests().antMatchers("/oauth/**").permitAll().antMatchers("/**").authenticated();
	}

	@Primary
	@Bean
	public RemoteTokenServices tokenServices() {
		final RemoteTokenServices tokenService = new RemoteTokenServices();
		tokenService.setCheckTokenEndpointUrl("http://localhost:8764/oauth/check_token");
		tokenService.setClientId("s2-client");
		tokenService.setClientSecret("secret");
		return tokenService;
	}
}