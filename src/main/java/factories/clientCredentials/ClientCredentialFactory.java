package factories.clientCredentials;

import com.microsoft.aad.adal4j.ClientCredential;

public interface ClientCredentialFactory {
	public ClientCredential generateCredentials(String clientId, String clientSecret);
}
