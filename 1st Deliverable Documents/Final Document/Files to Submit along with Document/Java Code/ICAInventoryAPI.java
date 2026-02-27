public interface ICAInventoryAPI {

	/**
	 * 
	 * @param item
	 * @param amount
	 */
	boolean withdrawInventory(string item, int amount);

	/**
	 * 
	 * @param productId
	 */
	string getInventory(string productId);

	/**
	 * 
	 * @param orderId
	 * @param orderDetails
	 */
	void createNewOrder(string orderId, string orderDetails);

}