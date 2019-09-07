package com.s2.spring.cloud.zuul.server.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

@Service
public class AuthRequestHeaderFilter extends ZuulFilter {


  @Override
  public String filterType() {
    return "pre";
  }

  public int filterOrder() {
    return 1;
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  public Object run() {
    RequestContext ctx = RequestContext.getCurrentContext();
    if(ctx.getRequest().getUserPrincipal()!=null){
    	OAuth2Authentication authentication = (OAuth2Authentication) ctx.getRequest().getUserPrincipal();
    	String userName = (String) authentication.getPrincipal();
    	List<String> roles = new ArrayList<String>();
    	if(authentication.getAuthorities()!=null){
    		authentication.getAuthorities().forEach(authority->{
    			roles.add(StringUtils.replaceEach(authority.getAuthority(), new String[]{"{authority=","}"}, new String[]{"",""}));
    		});
    	}
        ctx.addZuulRequestHeader("userId", userName);
        ctx.addZuulRequestHeader("roles", StringUtils.join(roles,','));
    }
    return null;
  }

}
