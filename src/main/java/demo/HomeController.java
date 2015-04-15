package demo;

import handler.DefaultAuthenticationCallbackHandler;

import java.net.MalformedURLException;
import java.util.concurrent.Executors;

import manager.acquiredTokenManager.AcquiredTokenManager;
import manager.acquiredTokenManager.DefaultAcquiredTokenManager;
import manager.apiConnection.ApiConnectionManager;
import manager.apiConnection.DefaultApiConnectionManager;
import model.CredentialsModel;
import model.WidgetModel;

import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import factories.authenticationContext.DefaultAuthenticationContextFactory;
import factories.clientCredentials.DefaultClientCredentialFactory;

@Controller
public class HomeController {

	private AcquiredTokenManager acquiredTokenManager;
	private ApiConnectionManager apiConnectionManager;

	public HomeController() {
		acquiredTokenManager = new DefaultAcquiredTokenManager(new DefaultAuthenticationContextFactory(), new DefaultClientCredentialFactory(), new DefaultAuthenticationCallbackHandler(), Executors.newFixedThreadPool(1));
		apiConnectionManager = new DefaultApiConnectionManager(new DefaultHttpClient());
	}

	public HomeController(AcquiredTokenManager tokenManager, ApiConnectionManager apiConnectionManager) {
		this.acquiredTokenManager = tokenManager;
		this.apiConnectionManager = apiConnectionManager;

	}

	@RequestMapping("/")
	public String index(Model model) {
		model.addAttribute("setup", new CredentialsModel());
		return "Index";
	}

	@RequestMapping(value = "/Connect", method = RequestMethod.POST)
	public ModelAndView connect(CredentialsModel form) throws MalformedURLException, InterruptedException {
		String token = apiConnectionManager.returnApiKey(form.getApi(), acquiredTokenManager.returnOAuthTokenFromResource(form.getClientId(), form.getSecretKey(), form.getResourceId(), form.getAuthority()));
		ModelAndView modelAndView = new ModelAndView("Widget");
		modelAndView.addObject("widgetmodel", new WidgetModel(form.getVersion(), token));
		return modelAndView;
	}
}