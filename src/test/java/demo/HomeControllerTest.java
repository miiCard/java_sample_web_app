package demo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;

import manager.acquiredTokenManager.AcquiredTokenManager;
import manager.apiConnection.ApiConnectionManager;
import model.CredentialsModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.ModelAndView;

public class HomeControllerTest {

	private HomeController underTest;

	@Mock
	private AcquiredTokenManager tokenManager;

	@Mock
	private ApiConnectionManager connectionManager;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		underTest = new HomeController(tokenManager, connectionManager);
	}

	@Test
	public void test_connect_returns_data_expoected() throws MalformedURLException, InterruptedException {
		when(tokenManager.returnOAuthTokenFromResource("clientId", "secretKey", "resourceId", "authority")).thenReturn("token");
		when(connectionManager.returnApiKey("api", "token")).thenReturn("mytoken");
		ModelAndView view = underTest.connect(buildDefaultCredentailsModel());
		verify(tokenManager).returnOAuthTokenFromResource("clientId", "secretKey", "resourceId", "authority");
	}

	public CredentialsModel buildDefaultCredentailsModel() {
		CredentialsModel model = new CredentialsModel();
		model.setApi("api");
		model.setAuthority("authority");
		model.setClientId("clientId");
		model.setVersion("version");
		model.setResourceId("resourceId");
		model.setSecretKey("secretKey");
		return model;
	}

	@Test
	public void make_sure_index_returns_with_empty_credentials_model() {
		ExtendedModelMap model = new ExtendedModelMap();
		underTest.index(model);
		CredentialsModel creds = (CredentialsModel) model.asMap().get("setup");

		Assert.assertNull(creds.getApi());
		Assert.assertNull(creds.getAuthority());
		Assert.assertNull(creds.getClientId());
		Assert.assertNull(creds.getClientId());
		Assert.assertNull(creds.getResourceId());
		Assert.assertNull(creds.getSecretKey());
		Assert.assertNull(creds.getVersion());
	}

}
