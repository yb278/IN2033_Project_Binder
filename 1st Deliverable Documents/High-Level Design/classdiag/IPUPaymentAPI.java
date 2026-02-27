public interface IPUPaymentAPI {

	/**
	 * 
	 * @param cardNumber
	 * @param name
	 * @param expiryDate
	 * @param cardType
	 * @param amount
	 */
	boolean recordPayment(int cardNumber, string name, string expiryDate, string cardType, int amount);

	/**
	 * 
	 * @param paymentId
	 */
	boolean refundPayment(int paymentId);

	/**
	 * 
	 * @param paymentId
	 */
	string getPaymentStatus(string paymentId);

}