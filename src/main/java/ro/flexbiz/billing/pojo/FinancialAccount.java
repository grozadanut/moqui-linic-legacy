package ro.flexbiz.billing.pojo;

import java.util.Objects;

// alias BankAccount
public class FinancialAccount {
	/**
	 * Payment account identifier
	 * A unique identifier of the financial payment account, at a payment service provider, to which payment should be made. 
	 * Such as IBAN or BBAN.
	 * Example value: NO99991122222
	 */
	private String id;
	/**
	 * Payment account name
	 * The name of the payment account, at a payment service provider, to which payment should be made.
	 * Example value: Company SRL
	 */
	private String name;
	/**
	 * Payment service provider identifier
	 * An identifier for the payment service provider where a payment account is located. 
	 * Such as a BIC or a national clearing code where required. No identification scheme Identifier to be used.
	 * Example value: 9999
	 */
	private String financialInstitutionBranch;
	/**
	 * A code signifying the currency in which this financial account is held.
	 * Example: RON
	 */
	private String currency;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFinancialInstitutionBranch() {
		return financialInstitutionBranch;
	}

	public void setFinancialInstitutionBranch(String financialInstitutionBranch) {
		this.financialInstitutionBranch = financialInstitutionBranch;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		FinancialAccount that = (FinancialAccount) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(financialInstitutionBranch, that.financialInstitutionBranch) && Objects.equals(currency, that.currency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, financialInstitutionBranch, currency);
	}

	@Override
	public String toString() {
		return "FinancialAccount{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", financialInstitutionBranch='" + financialInstitutionBranch + '\'' +
				", currency='" + currency + '\'' +
				'}';
	}
}
