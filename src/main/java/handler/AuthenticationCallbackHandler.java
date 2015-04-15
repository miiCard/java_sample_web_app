package handler;

import com.microsoft.aad.adal4j.AuthenticationCallback;

public interface AuthenticationCallbackHandler extends AuthenticationCallback {
	public String getToken();
}
