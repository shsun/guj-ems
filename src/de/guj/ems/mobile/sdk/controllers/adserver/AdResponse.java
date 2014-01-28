package de.guj.ems.mobile.sdk.controllers.adserver;


public abstract class AdResponse implements IAdResponse {

	private String response;

	private boolean isRich;

	private boolean isTest;

	private boolean isEmpty;

	private String htmlResponse;

	private AdResponseParser parser;

	public AdResponse(String response) {
		this.response = response;
	}

	@Override
	public String getResponse() {
		return response;
	}

	@Override
	public boolean isImageAd() {
		return !isRich && !isEmpty;
	}

	@Override
	public boolean isRichAd() {
		return isRich && !isEmpty;
	}

	@Override
	public boolean isEmpty() {
		return isEmpty;
	}

	@Override
	public boolean isTest() {
		return isTest;
	}

	@Override
	public AdResponseParser getParser() {
		return parser;
	}

	protected void setIsRich(boolean rich) {
		this.isRich = rich;
	}

	protected void setParser(AdResponseParser parser) {
		this.parser = parser;
	}

	protected void setEmpty(boolean empty) {
		isEmpty = empty;
	}

	@Override
	public String getResponseAsHTML() {
		if (htmlResponse == null) {
			htmlResponse = "<div style=\"width: 100%; margin: 0; padding: 0;\" id=\"ems_ad_container\">"
					+ "<a href=\""
					+ getParser().getClickUrl()
					+ "\">"
					+ "<img onload=\"document.getElementById('ems_ad_container').style.height=this.height+'px'\" src=\""
					+ getParser().getImageUrl()
					+ "\"></a>"
					+ (getParser().getTrackingImageUrl() != null ? "<img src=\""
							+ getParser().getTrackingImageUrl()
							+ "\" style=\"width: 0px; height: 0px; display: none;\">"
							: "") + "</div>";
		}
		return htmlResponse;
	}
}
