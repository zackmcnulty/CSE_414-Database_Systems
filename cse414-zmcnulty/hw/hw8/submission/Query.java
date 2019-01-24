import java.io.FileInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Runs queries against a back-end database
 */
public class Query
{
  private String configFilename;
  private Properties configProps = new Properties();

  private String jSQLDriver;
  private String jSQLUrl;
  private String jSQLUser;
  private String jSQLPassword;

  // DB Connection
  private Connection conn;

  // Logged In User
  private String username; // customer username is unique
  
  private List<Itinerary> prevItineraries;


  // Canned queries

  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  // preare statements added by me
  
  private static final String DIRECT_SEARCH =
              "SELECT TOP (?) fid, day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price FROM Flights "
                      + "WHERE canceled != 1 AND origin_city = ? AND dest_city = ? AND day_of_month = ?  "
                      + "ORDER BY actual_time ASC";
  private PreparedStatement directSearch;

  private static final String INDIRECT_SEARCH = 
		      "SELECT TOP (?) F1.fid as fid1, F1.day_of_month as day_of_month1, F1.carrier_id as carrier_id1, F1.flight_num as flight_num1, F1.origin_city as origin_city1, F1.dest_city as dest_city1, F1.actual_time as actual_time1, F1.capacity as capacity1, F1.price as price1,"
		      	      + "F2.fid as fid2, F2.day_of_month as day_of_month2, F2.carrier_id as carrier_id2, F2.flight_num as flight_num2, F2.origin_city as origin_city2, F2.dest_city as dest_city2, F2.actual_time as actual_time2, F2.capacity as capacity2, F2.price as price2 "
			      + "FROM Flights F1, Flights F2 "
			      + "WHERE F1.canceled != 1 AND F2.canceled != 1 AND F1.origin_city = ? AND F2.dest_city = ? " 
			      + " AND F1.dest_city = F2.origin_city AND F1.day_of_month = F2.day_of_month AND F1.day_of_month = ? "
			      + "ORDER BY (F1.actual_time + F2.actual_time) ASC";
  private PreparedStatement indirectSearch;

  private static final String SPOTS_TAKEN = "SELECT COUNT(*) as spots_taken FROM Reservations WHERE fid1 = ? OR fid2 = ?";
  private PreparedStatement spotsTaken;
	

  private static final String CHECK_USER_EXISTS = "SELECT username FROM Users WHERE username = ? AND password = ?";
  private PreparedStatement checkUserExists;

  private static final String GET_BALANCE = "SELECT balance FROM Users WHERE username = ?";
  private PreparedStatement getBalance;

  private static final String GET_PREV_RID = "SELECT rid FROM PrevRID";
  private PreparedStatement getPrevRID;

  private static final String UPDATE_RID = "DELETE FROM PrevRID; INSERT INTO PrevRID Values(?)";
  private PreparedStatement updateRID;

  private static final String ADD_USER = "INSERT INTO Users Values(?,?,?)";
  private PreparedStatement addUser;

  private static final String ADD_RESERVATION = "INSERT INTO Reservations Values(?,?,?,?,?,?,?)";
  private PreparedStatement addReservation;

  private static final String CHECK_RESERVATION_EXISTS = "SELECT day FROM Reservations WHERE username = ? AND day = ?";
  private PreparedStatement checkReservationExists;

  private static final String CHECK_UNPAID_RESERVATION_EXISTS = "SELECT * FROM Reservations WHERE username = ? AND rid = ? AND paid = 0";
  private PreparedStatement checkUnpaidReservationExists;

  private static final String UPDATE_BALANCE = "UPDATE Users SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalance;

  private static final String UPDATE_PAID = "UPDATE Reservations SET paid = 1 WHERE rid = ?";
  private PreparedStatement updatePaid;

  private static final String GET_RESERVATIONS = "SELECT * FROM Reservations WHERE username = ?";
  private PreparedStatement getReservations;

  private static final String GET_FLIGHT = "SELECT * FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement getFlight;

  private static final String GET_RID = "SELECT * FROM Reservations WHERE rid = ? AND username = ?";
  private PreparedStatement getRID;

  private static final String CANCEL_RESERVATION = "DELETE FROM RESERVATIONS WHERE rid = ?";
  private PreparedStatement cancelReservation;


  // transactions
  private static final String BEGIN_TRANSACTION_SQL = "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;";
  private PreparedStatement beginTransactionStatement;

  private static final String COMMIT_SQL = "COMMIT TRANSACTION";
  private PreparedStatement commitTransactionStatement;

  private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
  private PreparedStatement rollbackTransactionStatement;

  class Flight
  {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public int flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    public Flight(int fid, int dayOfMonth, String carrierId, int flightNum, String originCity, String destCity, int time, int capacity, int price)
    {
    	this.fid = fid;
	this.dayOfMonth = dayOfMonth;
	this.carrierId = carrierId;
	this.flightNum = flightNum;
	this.originCity = originCity;
	this.destCity = destCity;
	this.time = time;
	this.capacity = capacity;
	this.price = price;
    
    }

    @Override
    public String toString()
    {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId +
              " Number: " + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time +
              " Capacity: " + capacity + " Price: " + price;
    }
  }

  class Itinerary implements Comparable<Itinerary>
  {
  	public Flight f1; // flight 2
	public Flight f2; // flight 1
	public int iid; // itinerary id
	public int actual_time;

	public Itinerary(Flight f1) 
	{
		this(f1, null);
	}
	
	public Itinerary(Flight f1, Flight f2) 
	{
		this.f1 = f1;
		this.f2 = f2;
		if (f2 != null) {
			this.actual_time = f1.time + f2.time;
		} else {
			this.actual_time = f1.time;
		}
	}
 	
	@Override
	public String toString() {
		String result =  "Itinerary " + iid + ": " + (f2 != null ? 2:1) + " flight(s), " + actual_time + " minutes\n";
		result += f1.toString() + "\n";
		if (f2 != null) {
			result += f2.toString() + "\n";
		}
		return result;
	}
	
	@Override
	public int compareTo(Itinerary other)
	{
		if (this.actual_time != other.actual_time) {
			return this.actual_time - other.actual_time;
		} else if (this.f1.fid != other.f1.fid) {
			return this.f1.fid - other.f1.fid;
		} else {
			// We won't ever get a null pointer exception here because
			// the only way to enter this case is comparing one hop flights
			// We will never have this.f1.fid == other.f1.fid for two
			// direct flights, and a direct flight and one hop flight
			// cannot have the same destination if they both
			try {
				return this.f2.fid - other.f2.fid;
			} catch (Exception e) {
				return 0;
			}
		}
	}
  
  }

  public Query(String configFilename) throws SQLException
  {
    this.configFilename = configFilename;
  }

  /* Connection code to SQL Azure.  */
  public void openConnection() throws Exception
  {
    configProps.load(new FileInputStream(configFilename));

    jSQLDriver = configProps.getProperty("flightservice.jdbc_driver");
    jSQLUrl = configProps.getProperty("flightservice.url");
    jSQLUser = configProps.getProperty("flightservice.sqlazure_username");
    jSQLPassword = configProps.getProperty("flightservice.sqlazure_password");

    /* load jdbc drivers */
    Class.forName(jSQLDriver).newInstance();

    /* open connections to the flights database */
    conn = DriverManager.getConnection(jSQLUrl, // database
            jSQLUser, // user
            jSQLPassword); // password

    conn.setAutoCommit(true); //by default automatically commit after each statement

    /* You will also want to appropriately set the transaction's isolation level through:
       conn.setTransactionIsolation(...)
       See Connection class' JavaDoc for details.
    */

    // Ryan mentioned we should use this level (i.e. strictest)
    conn.setTransactionIsolation(conn.TRANSACTION_SERIALIZABLE);

  }

  public void closeConnection() throws Exception
  {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created. Do not drop any tables and do not
   * clear the flights table. You should clear any tables you use to store reservations
   * and reset the next reservation ID to be 1.
   */
  public void clearTables() throws SQLException
  {
      	Statement searchStatement = conn.createStatement();
	searchStatement.execute("DELETE FROM Reservations");
      	searchStatement.execute("DELETE FROM Users");
	searchStatement.execute("DELETE FROM PrevRID");
  }

  /**
   * prepare all the SQL statements in this method.
   * "preparing" a statement is almost like compiling it.
   * Note that the parameters (with ?) are still not filled in
   */
  public void prepareStatements() throws Exception
  {
    beginTransactionStatement = conn.prepareStatement(BEGIN_TRANSACTION_SQL);
    commitTransactionStatement = conn.prepareStatement(COMMIT_SQL);
    rollbackTransactionStatement = conn.prepareStatement(ROLLBACK_SQL);

    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);

    /* add here more prepare statements for all the other queries you need */
    /* . . . . . . */

    checkUserExists = conn.prepareStatement(CHECK_USER_EXISTS);
    addUser = conn.prepareStatement(ADD_USER);
    addReservation = conn.prepareStatement(ADD_RESERVATION);
    checkReservationExists = conn.prepareStatement(CHECK_RESERVATION_EXISTS);
    getPrevRID = conn.prepareStatement(GET_PREV_RID);
    updateRID = conn.prepareStatement(UPDATE_RID);
    checkUnpaidReservationExists = conn.prepareStatement(CHECK_UNPAID_RESERVATION_EXISTS);
    getBalance = conn.prepareStatement(GET_BALANCE);
    updatePaid = conn.prepareStatement(UPDATE_PAID);
    updateBalance = conn.prepareStatement(UPDATE_BALANCE);
    directSearch = conn.prepareStatement(DIRECT_SEARCH);
    indirectSearch = conn.prepareStatement(INDIRECT_SEARCH);
    spotsTaken = conn.prepareStatement(SPOTS_TAKEN);
    getReservations = conn.prepareStatement(GET_RESERVATIONS);
    getFlight = conn.prepareStatement(GET_FLIGHT);
    getRID = conn.prepareStatement(GET_RID);
    cancelReservation = conn.prepareStatement(CANCEL_RESERVATION);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username
   * @param password
   *
   * @return If someone has already logged in, then return "User already logged in\n"
   * For all other errors, return "Login failed\n".
   *
   * Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password)
  {
	if (this.username != null) {
		return "User already logged in\n";
	}

	try {
		checkUserExists.clearParameters();
		checkUserExists.setString(1, username.toLowerCase());
		checkUserExists.setString(2, password.toLowerCase());
		ResultSet result = checkUserExists.executeQuery();

		// if no matching username or password found
		if (!result.next()) {
			return "Login failed\n";
		}
		result.close();

	} catch (SQLException e) {
    		return "Login failed\n";
	}

	this.username = username;
	return "Logged in as " + username + "\n";
  }

  /**
   * Implement the create user function.
   *
   * @param username new user's username. User names are unique the system.
   * @param password new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer (String username, String password, int initAmount) {
	
	if (initAmount < 0) {
		return "Failed to create user\n";
	}   

	try {
		addUser.clearParameters();
		addUser.setString(1, username);
		addUser.setString(2, password);
		addUser.setInt(3, initAmount);
		addUser.executeUpdate();
	} catch (SQLException e) {
		//e.printStackTrace();
		// i.e. will fail if the user already exists in the system
		return "Failed to create user\n";
	}
  	
	return "Created user " +  username + "\n";
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights
   * and flights with two "hops." Only searches for up to the number of
   * itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight if true, then only search for direct flights, otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n".
   * If an error occurs, then return "Failed to search\n".
   *
   * Otherwise, the sorted itineraries printed in the following format:
   *
   * Itinerary [itinerary number]: [number of flights] flight(s), [total flight time] minutes\n
   * [first flight in itinerary]\n
   * ...
   * [last flight in itinerary]\n
   *
   * Each flight should be printed using the same format as in the {@code Flight} class. Itinerary numbers
   * in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) 
  {
	
    StringBuffer sb = new StringBuffer();
    List<Itinerary> itineraryList = new ArrayList<Itinerary>(); 

    if (numberOfItineraries < 1) {
    	return "Failed to search\n";
    }

    try {

      // find all possible direct flights (ignoring canceled flights)
      directSearch.clearParameters();
      directSearch.setInt(1, numberOfItineraries);
      directSearch.setString(2, originCity);
      directSearch.setString(3, destinationCity);
      directSearch.setInt(4, dayOfMonth);

      ResultSet directResults = directSearch.executeQuery();

      int currentItineraries = 0; 

      // while I still have flights to look for
      while (directResults.next())
      {
	Flight f1 = new Flight(directResults.getInt("fid"), directResults.getInt("day_of_month"), directResults.getString("carrier_id"), 
				directResults.getInt("flight_num"), directResults.getString("origin_city"), directResults.getString("dest_city"),
				directResults.getInt("actual_time"), directResults.getInt("capacity"), directResults.getInt("price"));

	Itinerary i1 = new Itinerary(f1);
	itineraryList.add(i1);

	currentItineraries += 1;
      }	

      directResults.close();

      if (!directFlight)
      {
      // find all possible indirect flights
      
	      int itinerariesLeft = numberOfItineraries - currentItineraries;
	
	      indirectSearch.clearParameters();
	      indirectSearch.setInt(1, itinerariesLeft);
	      indirectSearch.setString(2, originCity);
	      indirectSearch.setString(3, destinationCity);
	      indirectSearch.setInt(4, dayOfMonth);
	      ResultSet indirectResults = indirectSearch.executeQuery();

	      while (indirectResults.next()) 
	      {
			
		Flight f1 = new Flight(indirectResults.getInt("fid1"), indirectResults.getInt("day_of_month1"), indirectResults.getString("carrier_id1"), 
				indirectResults.getInt("flight_num1"), indirectResults.getString("origin_city1"), indirectResults.getString("dest_city1"),
				indirectResults.getInt("actual_time1"), indirectResults.getInt("capacity1"), indirectResults.getInt("price1"));

		Flight f2 = new Flight(indirectResults.getInt("fid2"), indirectResults.getInt("day_of_month2"), indirectResults.getString("carrier_id2"), 
				indirectResults.getInt("flight_num2"), indirectResults.getString("origin_city2"), indirectResults.getString("dest_city2"),
				indirectResults.getInt("actual_time2"), indirectResults.getInt("capacity2"), indirectResults.getInt("price2"));

		Itinerary it = new Itinerary(f1,f2);
		itineraryList.add(it);
	      }

	      indirectResults.close();

      }
    } catch (SQLException e) {
	//e.printStackTrace();
    	return "Failed to search\n";
    }
	
    if (itineraryList.size() == 0)
    {
	return "No flights match your selection\n"; 
    }

    Collections.sort(itineraryList);


    int curr_iid = 0;
    for (Itinerary it : itineraryList) {
	    it.iid = curr_iid;
	    sb.append(it.toString());
	    curr_iid += 1;
    }

    // clears previous itineraries and updates to hold latest query.
    prevItineraries = itineraryList;
    return sb.toString();
  }

  /**
   * Same as {@code transaction_search} except that it only performs single hop search and
   * do it in an unsafe manner.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight
   * @param dayOfMonth
   * @param numberOfItineraries
   *
   * @return The search results. Note that this implementation *does not conform* to the format required by
   * {@code transaction_search}.
   */
  private String transaction_search_unsafe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries) throws SQLException
  {
    StringBuffer sb = new StringBuffer();

    try
    {
      // one hop itineraries
      String unsafeSearchSQL =
              "SELECT TOP (" + numberOfItineraries + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
                      + "FROM Flights "
                      + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'" + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
                      + "ORDER BY actual_time ASC";

	    
      Statement searchStatement = conn.createStatement();
      ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

      while (oneHopResults.next())
      {
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");

        sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: " + result_flightNum + " Origin: " + result_originCity + " Destination: " + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
      }
      oneHopResults.close();
    } catch (SQLException e) { e.printStackTrace(); }

    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   * If try to book an itinerary with invalid ID, then return "No such itinerary {@code itineraryId}\n".
   * If the user already has a reservation on the same day as the one that they are trying to book now, then return
   * "You cannot book two flights in the same day\n".
   * For all other errors, return "Booking failed\n".
   *
   * And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n" where
   * reservationId is a unique number in the reservation system that starts from 1 and increments by 1 each time a
   * successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId)
  {
	int nextReservationID;

	if (username == null) {
		return "Cannot book reservations, not logged in\n";
	}
	if (itineraryId < 0 || prevItineraries == null || itineraryId >= prevItineraries.size()) {
		return "No such itinerary " + itineraryId + "\n";
	}

	try {
		Itinerary it = prevItineraries.get(itineraryId);
	
		// need transaction here to make sure user does not reserve two flights on same day using
		// different instances, and to make sure the capacity does not have a dirty read across
		// multiple instances
		beginTransaction();

		checkReservationExists.clearParameters();
		checkReservationExists.setString(1, this.username);
		checkReservationExists.setInt(2, it.f1.dayOfMonth);
		ResultSet results = checkReservationExists.executeQuery();
		
		if (results.next()) {
			results.close();
			rollbackTransaction();
			return "You cannot book two flights in the same day\n";

		}
		results.close();

		ResultSet ridTable = getPrevRID.executeQuery();
		if (ridTable.next()) {
			nextReservationID = 1 + ridTable.getInt("rid");
		} else {
			nextReservationID = 1;
		}

		ridTable.close();
		
		// check if flight is full; first check how many people have a spot already on that flight
		spotsTaken.clearParameters();
		spotsTaken.setInt(1, it.f1.fid);
		spotsTaken.setInt(2, it.f1.fid);
		ResultSet spotResults = spotsTaken.executeQuery();
		spotResults.next();
		int taken1 = spotResults.getInt("spots_taken");
		spotResults.close();
		
		int taken2 = 0;

		if (it.f2 != null) {
			spotsTaken.clearParameters();
			spotsTaken.setInt(1, it.f2.fid);
			spotsTaken.setInt(2, it.f2.fid);
			spotResults = spotsTaken.executeQuery();
			spotResults.next();
			taken2 = spotResults.getInt("spots_taken");
			spotResults.close();
		}

		// flights are full
		if (taken1 >= it.f1.capacity || (it.f2 != null && taken2 >= it.f2.capacity)) {
			rollbackTransaction();
			return "Booking failed\n";
		}

		//update reservation ID
		updateRID.clearParameters();
		updateRID.setInt(1, nextReservationID);
		updateRID.executeUpdate();

		// add reservation to reservation table
		addReservation.clearParameters();
		addReservation.setInt(1, nextReservationID);
		addReservation.setInt(2, 0);
		addReservation.setString(3, this.username);
		addReservation.setInt(4, it.f1.fid);
		int price = it.f1.price;

		if (it.f2 == null) {
			addReservation.setNull(5, 0); // 0 is code for NULL in sql
		} else {
			addReservation.setInt(5, it.f2.fid);
			price += it.f2.price;
		}
		addReservation.setInt(6, it.f1.dayOfMonth);
		addReservation.setInt(7, price);
		addReservation.executeUpdate();

		commitTransaction();


		return "Booked flight(s), reservation ID: " + nextReservationID + "\n";

	} catch (SQLException e) {
		//e.printStackTrace();

		// recommended by Ryan
		try {
			rollbackTransaction();
	  		return "Booking failed\n";
		} catch (SQLException e2) {
			return transaction_book(itineraryId);	
		}
	}
	
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n"
   * If the user has no reservations, then return "No reservations found\n"
   * For all other errors, return "Failed to retrieve reservations\n"
   *
   * Otherwise return the reservations in the following format:
   *
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * ...
   *
   * Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations()
  {
    if (this.username == null) {
    	return "Cannot view reservations, not logged in\n";
    }

    StringBuffer sb = new StringBuffer();
    try {
	    getReservations.clearParameters();
	    getReservations.setString(1, this.username);

	    ResultSet reservations = getReservations.executeQuery();

	    while (reservations.next()) {
		
		sb.append("Reservation " + reservations.getInt("rid") + " paid: " + ((reservations.getInt("paid") == 1) ? "true":"false") + ":\n");

		getFlight.clearParameters();
		getFlight.setInt(1, reservations.getInt("fid1"));
		ResultSet flightData = getFlight.executeQuery();
		flightData.next();
		Flight f1 = new Flight(flightData.getInt("fid"), flightData.getInt("day_of_month"), flightData.getString("carrier_id"), 
				flightData.getInt("flight_num"), flightData.getString("origin_city"), flightData.getString("dest_city"),
				flightData.getInt("actual_time"), flightData.getInt("capacity"), flightData.getInt("price"));
		sb.append(f1.toString() + "\n");
		
		flightData.close();

		// fid2 may be null
		getFlight.clearParameters();
		getFlight.setInt(1, reservations.getInt("fid2")); // .getInt returns 0 when fid2 is null, so it wont fail here
		flightData = getFlight.executeQuery();
		
		if(flightData.next()) {
			Flight f2 = new Flight(flightData.getInt("fid"), flightData.getInt("day_of_month"), flightData.getString("carrier_id"), 
					flightData.getInt("flight_num"), flightData.getString("origin_city"), flightData.getString("dest_city"),
					flightData.getInt("actual_time"), flightData.getInt("capacity"), flightData.getInt("price"));

			sb.append(f2.toString() + "\n");
		}

		flightData.close();

	    }

	    reservations.close();

    } catch (SQLException e) {
    	//e.printStackTrace();
	return "Failed to retrieve reservations\n";
    }

    if (sb.length() == 0) {
    	return "No reservations found\n";
    } else {
    	return sb.toString();
    }

  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n"
   * For all other errors, return "Failed to cancel reservation [reservationId]"
   *
   * If successful, return "Canceled reservation [reservationId]"
   *
   * Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId)
  {
    if (this.username == null) {
    	return "Cannot cancel reservations, not logged in\n";
    }

    try {

	beginTransaction();

	// check that the given reservationID is reserved by current user
	getRID.clearParameters();
	getRID.setInt(1, reservationId);
	getRID.setString(2, this.username);

	ResultSet toCancel = getRID.executeQuery();
	if (!toCancel.next()) {
		rollbackTransaction();
		return "Failed to cancel reservation " + reservationId +"\n";
	}
	// check if flight has been paid for already
	// update user's balance to include refund if required
	if (toCancel.getInt("paid") == 1) {

		getBalance.clearParameters();
		getBalance.setString(1, this.username);
		ResultSet balanceResult = getBalance.executeQuery();
		balanceResult.next();
		int balance = balanceResult.getInt("balance");
		balanceResult.close();

		balance += toCancel.getInt("price");

		updateBalance.clearParameters();
		updateBalance.setInt(1, balance);
		updateBalance.setString(2, this.username);
		updateBalance.executeUpdate();
	}
	toCancel.close();
	
	// remove reservation from reservation table.
	cancelReservation.clearParameters();
	cancelReservation.setInt(1,reservationId);
	cancelReservation.executeUpdate();

	commitTransaction();
	return "Canceled reservation " + reservationId + "\n";
    } catch (SQLException e) {
	    //e.printStackTrace();

	    // recommended by Ryan
	    try {
	    	rollbackTransaction();
    	    	return "Failed to cancel reservation " + reservationId + "\n";
	    } catch (SQLException e2) {
	    	return transaction_cancel(reservationId);
	    }
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   * If the reservation is not found / not under the logged in user's name, then return
   * "Cannot find unpaid reservation [reservationId] under user: [username]\n"
   * If the user does not have enough money in their account, then return
   * "User has only [balance] in account but itinerary costs [cost]\n"
   * For all other errors, return "Failed to pay for reservation [reservationId]\n"
   *
   * If successful, return "Paid reservation: [reservationId] remaining balance: [balance]\n"
   * where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay (int reservationId)
  {
      	if (this.username == null) {
		return "Cannot pay, not logged in\n";
	}
	try {
		// Here we need a transaction to avoid having a user pay for the same reservation twice!
		beginTransaction();

		checkUnpaidReservationExists.clearParameters();
		checkUnpaidReservationExists.setString(1, this.username);
		checkUnpaidReservationExists.setInt(2, reservationId);
		ResultSet results = checkUnpaidReservationExists.executeQuery();
		
		if (!results.next()) {
			rollbackTransaction();
			return "Cannot find unpaid reservation " + reservationId + " under user: " + this.username + "\n";
		}
		
		getBalance.clearParameters();
		getBalance.setString(1, this.username);
		ResultSet balanceResults = getBalance.executeQuery();
		balanceResults.next();
		int userBalance = balanceResults.getInt("balance");
		int price = results.getInt("price");

		if (price > userBalance) {
			rollbackTransaction();
			return "User has only " + userBalance + " in account but itinerary costs " + price + "\n";
		}

		// update user balance
		int remainingBalance = userBalance - price;
		updateBalance.clearParameters();
		updateBalance.setInt(1, remainingBalance);
		updateBalance.setString(2, this.username);
		updateBalance.executeUpdate();

		// update reservation to paid
		updatePaid.clearParameters();
		updatePaid.setInt(1, reservationId);
		updatePaid.executeUpdate();
	
		commitTransaction();
		return "Paid reservation: " + reservationId + " remaining balance: " + remainingBalance + "\n";

	} catch (SQLException e) {
		//e.printStackTrace();
		try {
			rollbackTransaction();
			return "Failed to pay for reservation " + reservationId + "\n";
		} catch (SQLException e2) {
			return transaction_pay(reservationId);
		}
	}
  }

  /* some utility functions below */

  public void beginTransaction() throws SQLException
  {
    conn.setAutoCommit(false);
    beginTransactionStatement.executeUpdate();
  }

  public void commitTransaction() throws SQLException
  {
    commitTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  public void rollbackTransaction() throws SQLException
  {
    rollbackTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  /**
   * Shows an example of using PreparedStatements after setting arguments. You don't need to
   * use this method if you don't want to.
   */
  private int checkFlightCapacity(int fid) throws SQLException
  {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }
}
