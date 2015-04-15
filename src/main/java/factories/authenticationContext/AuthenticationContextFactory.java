package factories.authenticationContext;

import java.util.concurrent.ExecutorService;

import com.microsoft.aad.adal4j.AuthenticationContext;

public interface AuthenticationContextFactory {
	public AuthenticationContext generateAuthenticationContext(String authority, ExecutorService service);
}
