package faubiguy.cityclaims;

public class CityLoadingException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public final String cityname;
	
	public CityLoadingException(String message, String cityname) {
		super(message);
		this.cityname = cityname;
	}

}
