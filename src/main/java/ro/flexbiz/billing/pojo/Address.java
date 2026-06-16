package ro.flexbiz.billing.pojo;

import java.util.Objects;

public class Address {
	/**
	 * ISO 3166-1:Alpha2 Country codes
	 * Example value: GB
	 */
	private String country;
	/**
	 * The subdivision of a country.
	 * Example value: RO-BH
	 * <a href="https://ro.wikipedia.org/wiki/ISO_3166-2:RO">https://ro.wikipedia.org/wiki/ISO_3166-2:RO<a>
	 */
	private String countrySubentity;
	private String city;
	private String postalZone;
	private String primaryLine;
	private String secondaryLine;

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountrySubentity() {
		return countrySubentity;
	}

	public void setCountrySubentity(String countrySubentity) {
		this.countrySubentity = countrySubentity;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalZone() {
		return postalZone;
	}

	public void setPostalZone(String postalZone) {
		this.postalZone = postalZone;
	}

	public String getPrimaryLine() {
		return primaryLine;
	}

	public void setPrimaryLine(String primaryLine) {
		this.primaryLine = primaryLine;
	}

	public String getSecondaryLine() {
		return secondaryLine;
	}

	public void setSecondaryLine(String secondaryLine) {
		this.secondaryLine = secondaryLine;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Address address = (Address) o;
		return Objects.equals(country, address.country) && Objects.equals(countrySubentity, address.countrySubentity) && Objects.equals(city, address.city) && Objects.equals(postalZone, address.postalZone) && Objects.equals(primaryLine, address.primaryLine) && Objects.equals(secondaryLine, address.secondaryLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(country, countrySubentity, city, postalZone, primaryLine, secondaryLine);
	}

	@Override
	public String toString() {
		return "Address{" +
				"country='" + country + '\'' +
				", countrySubentity='" + countrySubentity + '\'' +
				", city='" + city + '\'' +
				", postalZone='" + postalZone + '\'' +
				", primaryLine='" + primaryLine + '\'' +
				", secondaryLine='" + secondaryLine + '\'' +
				'}';
	}
}
