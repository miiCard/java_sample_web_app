package factories.authenticationContext;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import com.microsoft.aad.adal4j.AuthenticationContext;

public class DefaultAuthenticationContextFactory implements AuthenticationContextFactory {

	@Override
	public AuthenticationContext generateAuthenticationContext(String authority, ExecutorService service) {
		try {
			return new AuthenticationContext(authority, true, service);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unable to authenticate against the authority provided", e);
		}

	}
}
