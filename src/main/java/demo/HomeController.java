package demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.*;
import model.*;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.ClientCredential;

@Controller
public class HomeController {

	private String individualSummaryEndpoint;
	private String authenticationToken;

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
	public ModelAndView connect(CredentialsModel form) throws Exception {
		trimFormDetails(form);
		individualSummaryEndpoint = form.getIndividualSummaryEndpoint();
		authenticationToken = acquireOAuthAccessToken(form.getClientId(), form.getSecretKey(), form.getResourceId(), form.getAuthority());
		String token = acquireUserSessionToken(new DefaultHttpClient(), form.getUserSessionEndpoint(), authenticationToken);

		ModelAndView modelAndView = new ModelAndView("Widget");
		modelAndView.addObject("widgetmodel", new WidgetModel(form.getFullCDNPath(), token, form.getIndividualSummaryEndpoint()));
		return modelAndView;
	}

	@RequestMapping("/IndividualsSummary")
	public ModelAndView individualsSummary() throws Exception {
		ModelAndView modelAndView = new ModelAndView("IndividualsSummary");
		modelAndView.addObject("individualModel", populateIndividualsSummary(getJson(individualSummaryEndpoint)));
		return modelAndView;
	}

	@RequestMapping("/IndividualDetails")
	public ModelAndView individualDetails(String reference) throws Exception {		
		ModelAndView modelAndView = new ModelAndView("IndividualDetails");
		modelAndView.addObject("individualModel", populateIndividualDetails(getJson(removeTrailingS(individualSummaryEndpoint) + "/" + reference)));
		return modelAndView;
	}
	
	
	private String removeTrailingS(String endpoint) {
		return endpoint.substring(0, endpoint.length() -1);
	}

	private void trimFormDetails(CredentialsModel form) {
		form.setUserSessionEndpoint(form.getUserSessionEndpoint().trim());
		form.setAuthority(form.getAuthority().trim());
		form.setClientId(form.getClientId().trim());
		form.setFullCDNPath(trimSlash(form.getFullCDNPath().trim()));
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
	 * Queries <paramref name="apiEndpoint"/> with an http request
	 * authorized with <paramref name="authenticationToken"/>.
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
	private String extractTokenFromResponse(InputStream stream) throws IOException, JsonParseException {
		ObjectMapper mapper = new ObjectMapper();
		TokenResult result = mapper.readValue(stream, TokenResult.class);
		return result.getToken();
	}

	/**
	 *	Getting Json using authorization token
	 */
	private String getJson(String urlString) throws Exception {

		try {
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", String.format("Bearer %s", authenticationToken));

			InputStreamReader streamReader = new InputStreamReader(urlConnection.getInputStream(), "UTF-8");

			int read;
			char[] chars = new char[1024];
			StringBuffer stringBuffer = new StringBuffer();
			while ((read = streamReader.read(chars)) > 0)
			{
				stringBuffer.append(chars, 0, read);
			}
			return stringBuffer.toString();
		}	catch (Exception e) {
			throw new RuntimeException("Unable to get the Json string");
		}
	}

	/**
	* Parsing JSON string and populating a IndividualsSummary class using Gson
	*/
	private List<IndividualSummaryModel> populateIndividualsSummary(String json) throws ParseException {
		JsonElement jsonElement = new JsonParser().parse(json);
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		JsonArray jsonArray = jsonObject.getAsJsonArray("Individuals");

		List<IndividualSummaryModel> individuals = new ArrayList<>();

		for(int i = 0; i < jsonArray.size(); i++)
		{
			jsonObject = jsonArray.get(i).getAsJsonObject();
			String reference = jsonObject.get("Reference").getAsString();
			String timestamp = getDate(jsonObject.get("Timestamp").getAsString());
			String name = jsonObject.get("Name").getAsString();
			String emailAddress = jsonObject.get("EmailAddress").getAsString();
			String userID = jsonObject.get("UserID").getAsString();

			IndividualSummaryModel individual = new IndividualSummaryModel(reference, timestamp, name, emailAddress, userID);
			individuals.add(individual);
		}

		return individuals;
	}

	/**
	 * Parsing JSON string and populating a IndividualDetails class using Gson
	 */
	private IndividualDetailsModel populateIndividualDetails(String json) throws ParseException {
		JsonElement jsonElement = new JsonParser().parse(json);
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		String reference = jsonObject.getAsJsonObject("Individual").get("Reference").getAsString();
		jsonObject = jsonObject.getAsJsonObject("Individual").getAsJsonObject("Global").getAsJsonObject("Bank").getAsJsonArray("Providers").get(0).getAsJsonObject();
		String provider = jsonObject.get("Provider").getAsString();

		JsonArray jsonArray = jsonObject.getAsJsonArray("Accounts");
		List<AccountDetails> accounts = new ArrayList<>();
		getAccounts(jsonArray, accounts);

		return new IndividualDetailsModel(reference, provider, accounts);
	}

	/**
	 * Populating a AccountDetails class using Gson
	 */
	private void getAccounts(JsonArray jsonArray, List<AccountDetails> accounts) throws ParseException {
		JsonObject jsonObject;
		for(int i = 0; i < jsonArray.size(); i++)
		{
			jsonObject = jsonArray.get(i).getAsJsonObject();
			String accountName = jsonObject.get("AccountName").getAsString();
			String accountHolder = jsonObject.get("AccountHolder").getAsString();
			String accountType = jsonObject.get("AccountType").getAsString();
			String activityAvailableFrom = getDate(jsonObject.get("ActivityAvailableFrom").getAsString());
			String accountNumber = jsonObject.get("AccountNumber").getAsString();
			String sortCode = jsonObject.get("SortCode").getAsString();
			String balance = jsonObject.get("Balance").getAsString();
			String balanceFormatted = jsonObject.get("BalanceFormatted").getAsString();
			String currencyCode = jsonObject.get("CurrencyCode").getAsString();
			String verifiedOn = getDate(jsonObject.get("VerifiedOn").getAsString());

			JsonArray array = jsonObject.getAsJsonArray("Transactions");
			List<Transaction> transactions = new ArrayList<>();

			for (int j = 0; j < array.size(); j++)
			{
				JsonObject object = array.get(j).getAsJsonObject();
				String date = getDate(object.get("Date").getAsString());

				String description = object.get("Description").getAsString();
				String amount = object.get("Amount").getAsString();
				String type = object.get("Type").getAsString();
				transactions.add(new Transaction(date, description, amount, type));
			}

			AccountDetails accountDetails = new AccountDetails(accountName, accountHolder, accountType, activityAvailableFrom, accountNumber, sortCode, balance, balanceFormatted, transactions, currencyCode, verifiedOn);
			accounts.add(accountDetails);
		}
	}

	private String getDate(String rawDate) throws ParseException {
		Date date = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")).parse(rawDate);
		return (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).format(date);
	}

	private String trimSlash(String url){
		if(url.substring(url.length()-1, url.length()).equals("/")) {
			return url.substring(0, url.length()-1);
		}
		return url;
	}
}