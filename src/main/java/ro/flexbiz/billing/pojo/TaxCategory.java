package ro.flexbiz.billing.pojo;

import java.math.BigDecimal;
import java.util.Objects;

public class TaxCategory {
	/**
	 * VAT category code
	 * Coded identification of a VAT category.
	 * Example value: S
	 * Duty or tax or fee category code (Subset of UNCL5305)
	 */
	private String code;
	/**
	 * VAT category rate
	 * The VAT rate, represented as percentage that applies for the relevant VAT category.
	 * Example value: 0.19
	 */
	private BigDecimal percent;
	/**
	 * VAT exemption reason text
	 * A textual statement of the reason why the amount is exempted from VAT or why no VAT is being charged.
	 * Example value: Exempt
	 */
	private String taxExemptionReason;
	/**
	 * Mandatory element. Use "VAT"
	 * Default value: VAT
	 */
	private String taxScheme;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public BigDecimal getPercent() {
		return percent;
	}

	public void setPercent(BigDecimal percent) {
		this.percent = percent;
	}

	public String getTaxExemptionReason() {
		return taxExemptionReason;
	}

	public void setTaxExemptionReason(String taxExemptionReason) {
		this.taxExemptionReason = taxExemptionReason;
	}

	public String getTaxScheme() {
		return taxScheme;
	}

	public void setTaxScheme(String taxScheme) {
		this.taxScheme = taxScheme;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		TaxCategory that = (TaxCategory) o;
		return Objects.equals(code, that.code) && Objects.equals(percent, that.percent) && Objects.equals(taxExemptionReason, that.taxExemptionReason) && Objects.equals(taxScheme, that.taxScheme);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, percent, taxExemptionReason, taxScheme);
	}

	@Override
	public String toString() {
		return "TaxCategory{" +
				"code='" + code + '\'' +
				", percent=" + percent +
				", taxExemptionReason='" + taxExemptionReason + '\'' +
				", taxScheme='" + taxScheme + '\'' +
				'}';
	}
}
