package clients.collection;

import debug.DEBUG;
import middle.MiddleFactory;
import middle.OrderProcessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Observable;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import clients.warehousePick.PickModel;

/**
 * Implements the Model of the collection client
 * @author  Mike Smith University of Brighton
 * @version 1.0
 */

public class CollectModel extends Observable
{
	private String      theAction   = "";
	private String      theOutput   = "";
	private OrderProcessing theOrder     = null;

	/*
	 * Construct the model of the Collection client
	 * @param mf The factory to create the connection objects
	 */
	public CollectModel(MiddleFactory mf)
	{
		try                                           // 
		{      
			theOrder = mf.makeOrderProcessing();        // Process order
		} catch ( Exception e )
		{
			DEBUG.error("%s\n%s",
					"CollectModel.constructor\n%s", 
					e.getMessage() );
		}
	}

	/**
	 * Check if the product is in Stock
	 * @param orderNumber The order to be collected
	 */
	public void doCollect(String orderNumber )
	{

		int orderNum = 0;
		String on  = orderNumber.trim();         // Product no.		
		try
		{
			orderNum = Integer.parseInt(on);       // Convert
		}
		catch ( Exception err) 
		{
			// Convert invalid order number to 0
		}		
		if(checkID()) { //Check Customer's I.D. before collection
			checkRefund(orderNum);
			try
			{
				boolean ok = 
						theOrder.informOrderCollected( orderNum );
				if ( ok )
				{
					theAction = "";
					theOutput = "Collected order #" + orderNum;
				}
				else
				{
					theAction = "No such order to be collected : " + orderNumber;
					theOutput = "No such order to be collected : " + orderNumber;
				}
			} catch ( Exception e )
			{
				theOutput = String.format( "%s\n%s",
						"Error connection to order processing system",
						e.getMessage() );
				theAction = "!!!Error";
			}

			/*
			 * Write orders to a file, followed by the date and time they were collected. 
			 */

			try {
				File file = new File("OrderInfo/CollectedOrders.txt");

				if(!file.exists()) {
					file.createNewFile();
				}

				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");  
				LocalDateTime now = LocalDateTime.now();

				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw);
				out.println("Order "+orderNumber+" collected at "+dtf.format(now));
				out.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				setChanged(); notifyObservers(theAction);
			} 
		} else {
			theAction = "Invalid Credentials!!!";
			theOutput = "Invalid Credentials!!!";
			setChanged();
			notifyObservers(theAction);
		}
	}


	/**
	 * The output to be displayed
	 * @return The string to be displayed
	 */
	public String getResponce()
	{
		return theOutput;
	}

	/*
	 * JOptionPane to check customer ID versus collection ID
	 */
	private boolean checkID() {
		int reply = JOptionPane.showConfirmDialog(null, "Does the Customer's I.D. match the Customer Name on the order?", 
				"I.D. Verification", JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.YES_OPTION) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/*
	 * JOptionPane to check Refunds TreeMap for a refund on this order
	 */
	private void checkRefund(int on) {
		if(PickModel.Refunds.containsKey(on)) {
			JOptionPane.showMessageDialog(null, "Issue CASH Refund to customer of: "+PickModel.Refunds.get(on), 
					"Issue Refund", JOptionPane.PLAIN_MESSAGE);
			PickModel.Refunds.remove(on);
		}
	}
}
