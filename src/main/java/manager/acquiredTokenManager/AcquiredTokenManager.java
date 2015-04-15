package manager.acquiredTokenManager;

public interface AcquiredTokenManager {
	
	public String returnOAuthTokenFromResource(String clientId, String clientSecret, String resourceId, String authority); 
}
