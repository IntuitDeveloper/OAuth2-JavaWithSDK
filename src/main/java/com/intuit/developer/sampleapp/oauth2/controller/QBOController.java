package com.intuit.developer.sampleapp.oauth2.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.sampleapp.oauth2.client.OAuth2PlatformClientFactory;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.CompanyInfo;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;

/**
 * @author dderose
 *
 */
@RestController
public class QBOController {
	
	@Autowired
	OAuth2PlatformClientFactory factory;

	
	private static final Logger logger = Logger.getLogger(QBOController.class);
	
    /**
     * Sample QBO API call using OAuth2 tokens
     * 
     * @param session
     * @return
     */
	@ResponseBody
    @RequestMapping("/getCompanyInfo")
    public String callQBOCompanyInfo(HttpSession session) {

    	String realmId = (String)session.getAttribute("realmId");
    	if (StringUtils.isEmpty(realmId)) {
    		return new JSONObject().put("response","No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
    	}
    	String accessToken = (String)session.getAttribute("access_token");
    	String failureMsg="Failed";
    	String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";
        try {
        	
        	// set custom config
        	Config.setProperty(Config.BASE_URL_QBO, url);

    		//get DataService
    		DataService service = getDataService(realmId, accessToken);
			
			// get all companyinfo
			String sql = "select * from companyinfo";
			QueryResult queryResult = service.executeQuery(sql);
			return processResponse(failureMsg, queryResult);
			
		}
	        /*
	         * Handle 401 status code - 
	         * If a 401 response is received, refresh tokens should be used to get a new access token,
	         * and the API call should be tried again.
	         */
	        catch (InvalidTokenException e) {			
				logger.error("Error while calling executeQuery :: " + e.getMessage());
				
				//refresh tokens
	        	logger.info("received 401 during companyinfo call, refreshing tokens now");
	        	OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
	        	String refreshToken = (String)session.getAttribute("refresh_token");
	        	
				try {
					BearerTokenResponse bearerTokenResponse = client.refreshToken(refreshToken);
					session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
		            session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
		            
		            //call company info again using new tokens
		            logger.info("calling companyinfo using new tokens");
		            DataService service = getDataService(realmId, accessToken);
					
					// get all companyinfo
					String sql = "select * from companyinfo";
					QueryResult queryResult = service.executeQuery(sql);
					return processResponse(failureMsg, queryResult);
					
				} catch (OAuthException e1) {
					logger.error("Error while calling bearer token :: " + e.getMessage());
					return new JSONObject().put("response",failureMsg).toString();
				} catch (FMSException e1) {
					logger.error("Error while calling company currency :: " + e.getMessage());
					return new JSONObject().put("response",failureMsg).toString();
				}
	            
			} catch (FMSException e) {
				List<Error> list = e.getErrorList();
				list.forEach(error -> logger.error("Error while calling executeQuery :: " + error.getMessage()));
				return new JSONObject().put("response",failureMsg).toString();
			}
		
    }

	private String processResponse(String failureMsg, QueryResult queryResult) {
		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			CompanyInfo companyInfo = (CompanyInfo) queryResult.getEntities().get(0);
			logger.info("Companyinfo -> CompanyName: " + companyInfo.getCompanyName());
			ObjectMapper mapper = new ObjectMapper();
			try {
				String jsonInString = mapper.writeValueAsString(companyInfo);
				return jsonInString;
			} catch (JsonProcessingException e) {
				logger.error("Exception while getting company info ", e);
				return new JSONObject().put("response",failureMsg).toString();
			}
			
		}
		return failureMsg;
	}

	private DataService getDataService(String realmId, String accessToken) throws FMSException {
		
		//create oauth object
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		//create context
		Context context = new Context(oauth, ServiceType.QBO, realmId);

		// create dataservice
		return new DataService(context);
	}

}
