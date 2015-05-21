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
		String token = returnApiKey(new DefaultHttpClient(), form.getApi(), returnOAuthTokenFromResource(form.getClientId(), form.getSecretKey(), form.getResourceId(), form.getAuthority()));
		ModelAndView modelAndView = new ModelAndView("Widget");
		modelAndView.addObject("widgetmodel", new WidgetModel(form.getVersion(), token));
		return modelAndView;
	}
	
	
	/**
	 * Goes and fetches the oAuth2 token from the3 oAuth2 provider and returns the oAuth access token
	 * @param clientId
	 * @param clientSecret
	 * @param resourceId
	 * @param authority
	 * @return
	 * @throws MalformedURLException
	 * @throws InterruptedException 
	 */
	public String returnOAuthTokenFromResource(String clientId, String clientSecret, String resourceId, String authority) throws MalformedURLException, InterruptedException {
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
	 * Uses the oAuth2 token to connect to DirectID and get a valid API key
	 * @param client
	 * @param api
	 * @param acquiredToken
	 * @return
	 */
	public String returnApiKey(DefaultHttpClient client, String api, String acquiredToken) {
		HttpGet request = new HttpGet(api);
		request.setHeader("Authorization", String.format("Bearer %s", acquiredToken));
		try {
			return extractTokenFromResponse(client.execute(request).getEntity().getContent());
		} catch (Exception e) {
			throw new RuntimeException("Unable to get the Api Token");
		}
	}

	/**
	 * Simple method to extract the api key from the response
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

	/**
	 * Convert stream into a String for easy processing
	 * @param is
	 * @return
	 */
	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}
}