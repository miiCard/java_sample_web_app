package model;

public class WidgetModel {

	private String fullCDNPath;
	private String apiToken;
	private String individualSummaryEndpoint;

	public WidgetModel() {
		
	}
	
	public WidgetModel(String version, String apiToken, String individualSummaryEndpoint) {
		this.fullCDNPath = version;
		this.apiToken = apiToken;
		this.individualSummaryEndpoint = individualSummaryEndpoint;
	}

	public String getIndividualSummaryEndpoint() {
		return individualSummaryEndpoint;
	}

	public void setIndividualSummaryEndpoint(String individualSummaryEndpoint) {
		this.individualSummaryEndpoint = individualSummaryEndpoint;
	}

	public String getFullCDNPath() {
		return fullCDNPath;
	}

	public void setFullCDNPath(String version) {
		this.fullCDNPath = version;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}
}
