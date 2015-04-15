package manager.acquiredTokenManager;

import handler.AuthenticationCallbackHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;

import factories.authenticationContext.AuthenticationContextFactory;
import factories.clientCredentials.ClientCredentialFactory;

public class DefaultAcquiredTokenManager implements AcquiredTokenManager {

	private AuthenticationContextFactory authenticationContextManager;

	private ClientCredentialFactory clientCredentialManager;

	private AuthenticationCallbackHandler callback;

	private ExecutorService service;

	public DefaultAcquiredTokenManager(AuthenticationContextFactory manager, ClientCredentialFactory credentials, AuthenticationCallbackHandler callback, ExecutorService service) {
		this.authenticationContextManager = manager;
		this.service = service;
		this.clientCredentialManager = credentials;
		this.callback = callback;
	}

	private void sleep() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException("Sleep was interupted");
		}

	}

	@Override
	public String returnOAuthTokenFromResource(String clientId, String clientSecret, String resourceId, String authority) {
		AuthenticationContext context = authenticationContextManager.generateAuthenticationContext(authority, service);
		context.acquireToken(resourceId, clientCredentialManager.generateCredentials(clientId, clientSecret), callback);
		waitForResultFor30Seconds();
		return callback.getToken();
	}

	private void waitForResultFor30Seconds() {
		long startTime = System.currentTimeMillis();
		boolean flag = false;
		while (!flag) {
			sleep();
			if (callback.getToken() != null) {
				break;
			}
			if (System.currentTimeMillis() - startTime > 30000) {
				throw new RuntimeException("we have not been able to obtain a token within 3 seconds of trying");
			}
		}
	}

	public AuthenticationContextFactory getAuthenticationContextManager() {
		return authenticationContextManager;
	}

	public void setAuthenticationContextManager(AuthenticationContextFactory authenticationContextManager) {
		this.authenticationContextManager = authenticationContextManager;
	}

	public ClientCredentialFactory getClientCredentialManager() {
		return clientCredentialManager;
	}

	public void setClientCredentialManager(ClientCredentialFactory clientCredentialManager) {
		this.clientCredentialManager = clientCredentialManager;
	}

}
