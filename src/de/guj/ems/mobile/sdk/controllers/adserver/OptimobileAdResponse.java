package de.guj.ems.mobile.sdk.controllers.adserver;

public class OptimobileAdResponse extends AdResponse {

	public OptimobileAdResponse(String response) {
		super(response);
		setIsRich(false);
		setEmpty(response == null || response.length() < 1);
		if (!isEmpty()) {
			setParser(new OptimobileXmlParser(response));
		}
	}

}
