package com.intuit.developer.sampleapp.oauth2.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import com.intuit.developer.sampleapp.oauth2.client.OAuth2PlatformClientFactory;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.config.Scope;
import com.intuit.oauth2.exception.InvalidRequestException;

/**
 * @author dderose
 *
 */
@Controller
public class OAuth2Controller {
	
	private static final Logger logger = Logger.getLogger(OAuth2Controller.class);
	
	@Autowired
	OAuth2PlatformClientFactory factory;
	    
	@RequestMapping("/")
	public String home() {
		return "home";
	}
	
	@RequestMapping("/connected")
	public String connected() {
		return "connected";
	}
	
	/**
	 * Controller mapping for connectToQuickbooks button
	 * @return
	 */
	@RequestMapping("/connectToQuickbooks")
	public View connectToQuickbooks(HttpSession session) {
		logger.info("inside connectToQuickbooks ");
		OAuth2Config oauth2Config = factory.getOAuth2Config();
		
		String redirectUri = factory.getPropertyValue("OAuth2AppRedirectUri"); 
		
		String csrf = oauth2Config.generateCSRFToken();
		session.setAttribute("csrfToken", csrf);
		try {
			List<Scope> scopes = new ArrayList<Scope>();
			scopes.add(Scope.Accounting);
			return new RedirectView(oauth2Config.prepareUrl(scopes, redirectUri, csrf), true, true, false);
		} catch (InvalidRequestException e) {
			logger.error("Exception calling connectToQuickbooks ", e);
			logger.error("intuit_tid: " + e.getIntuit_tid());
			logger.error("More info: " + e.getResponseContent());
		}
		return null;
	}
	
	/**
	 * Controller mapping for signInWithIntuit button
	 * @return
	 */
	@RequestMapping("/signInWithIntuit")
	public View signInWithIntuit(HttpSession session) {
		logger.info("inside signInWithIntuit ");
		OAuth2Config oauth2Config = factory.getOAuth2Config();
		
		String csrf = oauth2Config.generateCSRFToken();
		session.setAttribute("csrfToken", csrf);
		
		String redirectUri = factory.getPropertyValue("OAuth2AppRedirectUri"); 
		try {
			List<Scope> scopes = new ArrayList<Scope>();
			scopes.add(Scope.OpenIdAll);
			return new RedirectView(oauth2Config.prepareUrl(scopes, redirectUri, csrf), true, true, false);
		} catch (InvalidRequestException e) {
			logger.error("Exception calling signInWithIntuit ", e);
			logger.error("intuit_tid: " + e.getIntuit_tid());
			logger.error("More info: " + e.getResponseContent());
		}
		return null;
		
	}
	
	/**
	 * Controller mapping for getAppNow button
	 * @return
	 */
	@RequestMapping("/getAppNow")
	public View getAppNow(HttpSession session) {
		logger.info("inside getAppNow "  );
		OAuth2Config oauth2Config = factory.getOAuth2Config();
		
		String csrf = oauth2Config.generateCSRFToken();
		session.setAttribute("csrfToken", csrf);

		String redirectUri = factory.getPropertyValue("OAuth2AppRedirectUri"); 
		try {
			List<Scope> scopes = new ArrayList<Scope>();
			scopes.add(Scope.OpenIdAll);
			scopes.add(Scope.Accounting);
			return new RedirectView(oauth2Config.prepareUrl(scopes, redirectUri, csrf), true, true, false);
		} catch (InvalidRequestException e) {
			logger.error("Exception calling getAppNow ", e);
			logger.error("intuit_tid: " + e.getIntuit_tid());
			logger.error("More info: " + e.getResponseContent());
		}
		return null;
	}

}
