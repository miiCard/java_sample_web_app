package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.CredentialsModel;
import model.TokenResult;
import model.WidgetModel;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.ClientCredential;

@Controller
public class HomeController {

	/**
	 * The default index controller, creates a new model to pass to the view
	 * @param model 
	 * @return link to the Index view
	 */
	@RequestMapping("/")
	public String index(Model model) {
		model.addAttribute("setup", new CredentialsModel());
		return "Index";
	}

	/**
	 * This method connects to Direct ID after going via the OAuth2 provider to get a valid token
	 * @param form
	 * @return
	 * @throws MalformedURLException
	 * @throws InterruptedException
	 */
	@RequestMapping(value = "/Connect", method = RequestMethod.POST)
	public ModelAndView connect(CredentialsModel form) throws MalformedURLException, InterruptedException {
		trimFormDetails(form);
		String token = acquireUserSessionToken(new DefaultHttpClient(), form.getApi(), acquireOAuthAccessToken(form.getClientId(), form.getSecretKey(), form.getResourceId(), form.getAuthority()));
		ModelAndView modelAndView = new ModelAndView("Widget");
		modelAndView.addObject("widgetmodel", new WidgetModel(form.getFullCDNPath(), token));
		return modelAndView;
	}
	
	
	private void trimFormDetails(CredentialsModel form) {
		form.setApi(form.getApi().trim());
		form.setAuthority(form.getAuthority().trim());
		form.setClientId(form.getClientId().trim());
		form.setFullCDNPath(form.getFullCDNPath().trim());
		form.setResourceId(form.getResourceId().trim());
		form.setSecretKey(form.getSecretKey().trim());	
	}
	
	
	/**
	 * Obtains an OAuth access token which can then be used to make authorized calls
	 *	to the Direct ID API.
	 * @param clientId
	 * @param clientSecret
	 * @param resourceId
	 * @param authority
	 * @return The returned value is expected to be included in the authentication header of subsequent API requests
	 * As the returned value authenticates the application, API calls made using
     * this value should only be made using server-side code
	 * @throws MalformedURLException
	 * @throws InterruptedException 
	 */
	public String acquireOAuthAccessToken(String clientId, String clientSecret, String resourceId, String authority) throws MalformedURLException, InterruptedException {
		DefaultAuthenticationCallbackHandler callback = new DefaultAuthenticationCallbackHandler();
		ExecutorService service = Executors.newFixedThreadPool(1);
		AuthenticationContext context = new AuthenticationContext(authority, true, service);
		context.acquireToken(resourceId, new ClientCredential(clientId, clientSecret), callback);
		waitForResultFor30Seconds(callback);
		return callback.getToken();
	}

	/**
	 * This is a simple sleep/wait method so the code is simple
	 * @param callback
	 * @throws InterruptedException 
	 */
	private void waitForResultFor30Seconds(DefaultAuthenticationCallbackHandler callback) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		boolean flag = false;
		while (!flag) {
			Thread.sleep(100);	
			if (callback.getToken() != null) {
				break;
			}
			if (System.currentTimeMillis() - startTime > 30000) {
				throw new RuntimeException("we have not been able to obtain a token within 3 seconds of trying");
			}
		}
	}

	/**
	 * Queries <paramref name="apiEndpoint"/> with an http request authorized with <paramref name="authenticationToken"/>.
	 * @param client
	 * @param api
	 * @param acquiredToken
	 * @return
	 */
	public String acquireUserSessionToken(DefaultHttpClient client, String api, String acquiredToken) {
		HttpGet request = new HttpGet(api);
		request.setHeader("Authorization", String.format("Bearer %s", acquiredToken));
		try {
			return extractTokenFromResponse(client.execute(request).getEntity().getContent());
		} catch (Exception e) {
			throw new RuntimeException("Unable to get the Api Token");
		}
	}

	/**
	 * Simple method to extract the api key from the response  using jackson
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ClientProtocolException
	 */
	private String extractTokenFromResponse(InputStream stream) throws IOException, JsonParseException, JsonMappingException, ClientProtocolException {
		ObjectMapper mapper = new ObjectMapper();
		TokenResult result = mapper.readValue(stream, TokenResult.class);
		return result.getToken();
	}
}