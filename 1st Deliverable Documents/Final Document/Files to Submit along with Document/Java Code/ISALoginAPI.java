public interface ISALoginAPI {

	/**
	 * 
	 * @param username
	 * @param password
	 */
	boolean login(string username, string password);

	/**
	 * 
	 * @param merchantId
	 */
	boolean isAuthenticated(string merchantId);

	/**
	 * 
	 * @param userId
	 */
	void logout(string userId);

}