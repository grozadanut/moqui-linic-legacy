package ro.flexbiz.billing.pojo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class InvoiceLine {
	/**
	 * Invoice line identifier
	 * A unique identifier for the individual line within the Invoice.
	 * Usually database persistence id.
	 * Example value: 12
	 */
	private Long id;
	/**
	 * Item Seller's identifier
	 * An identifier, assigned by the Seller, for the item.
	 * Example value: 9873242
	 */
	private String sellersItemIdentification;
	/**
	 * Item Buyer's identifier
	 * An identifier, assigned by the Buyer, for the item.
	 * Example value: 123455
	 */
	private String buyersItemIdentification;
	/**
	 * Invoice line note
	 * A textual note that gives unstructured information that is relevant to the Invoice line.
	 * Example value: New article number 12345
	 */
	private String note;
	/**
	 * Item description
	 * A description for an item.The item description allows for describing the item and its features in more detail than the Item name.
	 * Example value: Long description of the item on the invoice line
	 */
	private String description;
	/**
	 * Item name
	 * A name for an item.
	 * Example value: Item name
	 */
	private String name;
	
	/**
	 * Invoiced quantity
	 * The quantity of items (goods or services) that is charged in the Invoice line.
	 * Example value: 100
	 */
	private BigDecimal quantity;
	/**
	 * Invoiced quantity unit of measure
	 * The unit of measure that applies to the invoiced quantity. Codes for unit of packaging from UNECE Recommendation 
	 * No. 21 can be used in accordance with the descriptions in the "Intro" section of UN/ECE Recommendation 20, 
	 * Revision 11 (2015): The 2 character alphanumeric code values in UNECE Recommendation 21 shall be used. 
	 * To avoid duplication with existing code values in UNECE Recommendation No. 20, each code value from 
	 * UNECE Recommendation 21 shall be prefixed with an “X”, resulting in a 3 alphanumeric code when used as a unit of measure.
	 * Example value: C62
	 * <a href='https://docs.peppol.eu/poacc/billing/3.0/codelist/UNECERec20/'>https://docs.peppol.eu/poacc/billing/3.0/codelist/UNECERec20/<a>
	 */
	private String uom;
	
	/**
	 * A group of business terms providing information about the VAT applicable for the goods and services invoiced on the Invoice line.
	 */
	private TaxCategory classifiedTaxCategory;
	/**
	 * Item net price
	 * The price of an item, exclusive of VAT, after subtracting item price discount. 
	 * The Item net price has to be equal with the Item gross price less the Item price discount, 
	 * if they are both provided. Item price can not be negative.
	 * Example value: 23.45
	 */
	private BigDecimal price;
	
	/**
	 * Item price base quantity
	 * The number of item units to which the price applies.
	 * Example value: 1
	 */
	private BigDecimal baseQuantity;
	
	/**
	 * INVOICE LINE ALLOWANCES OR CHARGES
	 * A group of business terms providing information about allowances or charges applicable to the individual Invoice line.
	 */
	private List<AllowanceCharge> allowanceCharges;
	
	/**
	 * Invoice line net amount
	 * The total amount of the Invoice line. The amount is "net" without VAT, i.e. inclusive of line level allowances and 
	 * charges as well as other relevant taxes. Must be rounded to maximum 2 decimals.
	 * Example value: 2145.00
	 */
	private BigDecimal lineExtensionAmount;
	/**
	 * The total tax amount for a particular taxation scheme, e.g., VAT; the sum of the tax subtotals 
	 * for each tax category within the taxation scheme.
	 */
	private BigDecimal taxAmount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSellersItemIdentification() {
		return sellersItemIdentification;
	}

	public void setSellersItemIdentification(String sellersItemIdentification) {
		this.sellersItemIdentification = sellersItemIdentification;
	}

	public String getBuyersItemIdentification() {
		return buyersItemIdentification;
	}

	public void setBuyersItemIdentification(String buyersItemIdentification) {
		this.buyersItemIdentification = buyersItemIdentification;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}

	public TaxCategory getClassifiedTaxCategory() {
		return classifiedTaxCategory;
	}

	public void setClassifiedTaxCategory(TaxCategory classifiedTaxCategory) {
		this.classifiedTaxCategory = classifiedTaxCategory;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getBaseQuantity() {
		return baseQuantity;
	}

	public void setBaseQuantity(BigDecimal baseQuantity) {
		this.baseQuantity = baseQuantity;
	}

	public List<AllowanceCharge> getAllowanceCharges() {
		return allowanceCharges;
	}

	public void setAllowanceCharges(List<AllowanceCharge> allowanceCharges) {
		this.allowanceCharges = allowanceCharges;
	}

	public BigDecimal getLineExtensionAmount() {
		return lineExtensionAmount;
	}

	public void setLineExtensionAmount(BigDecimal lineExtensionAmount) {
		this.lineExtensionAmount = lineExtensionAmount;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		InvoiceLine that = (InvoiceLine) o;
		return Objects.equals(id, that.id) && Objects.equals(sellersItemIdentification, that.sellersItemIdentification) && Objects.equals(buyersItemIdentification, that.buyersItemIdentification) && Objects.equals(note, that.note) && Objects.equals(description, that.description) && Objects.equals(name, that.name) && Objects.equals(quantity, that.quantity) && Objects.equals(uom, that.uom) && Objects.equals(classifiedTaxCategory, that.classifiedTaxCategory) && Objects.equals(price, that.price) && Objects.equals(baseQuantity, that.baseQuantity) && Objects.equals(allowanceCharges, that.allowanceCharges) && Objects.equals(lineExtensionAmount, that.lineExtensionAmount) && Objects.equals(taxAmount, that.taxAmount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sellersItemIdentification, buyersItemIdentification, note, description, name, quantity, uom, classifiedTaxCategory, price, baseQuantity, allowanceCharges, lineExtensionAmount, taxAmount);
	}

	@Override
	public String toString() {
		return "InvoiceLine{" +
				"id=" + id +
				", sellersItemIdentification='" + sellersItemIdentification + '\'' +
				", buyersItemIdentification='" + buyersItemIdentification + '\'' +
				", note='" + note + '\'' +
				", description='" + description + '\'' +
				", name='" + name + '\'' +
				", quantity=" + quantity +
				", uom='" + uom + '\'' +
				", classifiedTaxCategory=" + classifiedTaxCategory +
				", price=" + price +
				", baseQuantity=" + baseQuantity +
				", allowanceCharges=" + allowanceCharges +
				", lineExtensionAmount=" + lineExtensionAmount +
				", taxAmount=" + taxAmount +
				'}';
	}
}
