package ro.flexbiz.billing.pojo;

import java.math.BigDecimal;
import java.util.Objects;

public class TaxSubtotal {
	/**
	 * VAT category taxable amount
	 * Sum of all taxable amounts subject to a specific VAT category code and VAT category rate (if the VAT category rate is applicable). 
	 * Must be rounded to maximum 2 decimals.
	 * Example value: 1945.00
	 */
	private BigDecimal taxableAmount;
	/**
	 * VAT category tax amount
	 * The total VAT amount for a given VAT category. Must be rounded to maximum 2 decimals.
	 * Example value: 486.25
	 */
	private BigDecimal taxAmount;
	private TaxCategory taxCategory;

	public BigDecimal getTaxableAmount() {
		return taxableAmount;
	}

	public void setTaxableAmount(BigDecimal taxableAmount) {
		this.taxableAmount = taxableAmount;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	public TaxCategory getTaxCategory() {
		return taxCategory;
	}

	public void setTaxCategory(TaxCategory taxCategory) {
		this.taxCategory = taxCategory;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		TaxSubtotal that = (TaxSubtotal) o;
		return Objects.equals(taxableAmount, that.taxableAmount) && Objects.equals(taxAmount, that.taxAmount) && Objects.equals(taxCategory, that.taxCategory);
	}

	@Override
	public int hashCode() {
		return Objects.hash(taxableAmount, taxAmount, taxCategory);
	}

	@Override
	public String toString() {
		return "TaxSubtotal{" +
				"taxableAmount=" + taxableAmount +
				", taxAmount=" + taxAmount +
				", taxCategory=" + taxCategory +
				'}';
	}
}
