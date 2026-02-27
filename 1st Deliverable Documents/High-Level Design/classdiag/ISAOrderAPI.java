public interface ISAOrderAPI {

	/**
	 * 
	 * @param merchantId
	 * @param orderDetails
	 */
	string PlaceOrder(int merchantId, string orderDetails);

	/**
	 * 
	 * @param orderID
	 */
	string getOrder(string orderID);

	/**
	 * 
	 * @param orderId
	 */
	string getInvoice(string orderId);

	/**
	 * 
	 * @param orderId
	 */
	string getOrderStatus(string orderId);

	/**
	 * 
	 * @param orderId
	 * @param dispatchInfo
	 */
	void updateDispathInfo(string orderId, string dispatchInfo);

}