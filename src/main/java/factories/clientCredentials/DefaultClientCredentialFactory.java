package factories.clientCredentials;

import com.microsoft.aad.adal4j.ClientCredential;

public class DefaultClientCredentialFactory implements ClientCredentialFactory {

	@Override
	public ClientCredential generateCredentials(String clientId,
			String clientSecret) {
		return new ClientCredential(clientId, clientSecret);
	}

}
