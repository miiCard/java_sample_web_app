package model;

public class WidgetModel {

	private String version;
	private String apiToken;

	public WidgetModel(String version, String apiToken) {
		this.version = version;
		this.apiToken = apiToken;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}
}
