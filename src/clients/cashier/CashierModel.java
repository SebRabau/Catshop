package clients.cashier;

import catalogue.Basket;
import catalogue.BetterBasket;
import catalogue.Product;
import debug.DEBUG;
import middle.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Observable;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Implements the Model of the cashier client
 * @originalauthor  Mike Smith University of Brighton
 * @author Sebastian Rabau - University of Brighton
 * @version 2.0
 */
public class CashierModel extends Observable
{
	private enum State { process, checked }

	private State       theState   = State.process;   // Current state
	private Product     theProduct = null;            // Current product
	private Product     Previous   = null;            // Previous bought product
	private Basket      theBasket  = null;            // Bought items

	private String      pn = "";                      // Product being processed

	private StockReadWriter theStock     = null;
	private OrderProcessing theOrder     = null;

	/**
	 * Construct the model of the Cashier
	 * @param mf The factory to create the connection objects
	 */

	public CashierModel(MiddleFactory mf)
	{
		try                                           // 
		{      
			theStock = mf.makeStockReadWriter();        // Database access
			theOrder = mf.makeOrderProcessing();        // Process order
		} catch ( Exception e )
		{
			DEBUG.error("CashierModel.constructor\n%s", e.getMessage() );
		}
		theState   = State.process;                  // Current state
	}

	/**
	 * Get the Basket of products
	 * @return basket
	 */
	public Basket getBasket()
	{
		return theBasket;
	}

	/**
	 * Check if the product is in Stock
	 * @param productNum The product number
	 */
	public void doCheck(String productNum )
	{
		String theAction = "";
		theState  = State.process;                  // State process
		pn  = productNum.trim();                    // Product no.
		int    amount  = 1;                         //  & quantity
		try
		{
			if ( theStock.exists( pn ) )              // Stock Exists?
			{                                         // T
				Product pr = theStock.getDetails(pn);   //  Get details
				if ( pr.getQuantity() >= amount )       //  In stock?
				{                                       //  T
					theAction =                           //   Display 
							String.format( "%s : %7.2f (%2d) ", //
									pr.getDescription(),              //    description
									pr.getPrice(),                    //    price
									pr.getQuantity() );               //    quantity     
					theProduct = pr;                      //   Remember prod.
					theProduct.setQuantity( amount );     //    & quantity
					theState = State.checked;             //   OK await BUY 
				} else {                                //  F
					theAction =                           //   Not in Stock
							pr.getDescription() +" not in stock";
				}
			} else {                                  // F Stock exists
				theAction =                             //  Unknown
						"Unknown product number " + pn;       //  product no.
			}
		} catch( StockException e )
		{
			DEBUG.error( "%s\n%s", 
					"CashierModel.doCheck", e.getMessage() );
			theAction = e.getMessage();
		}
		setChanged(); notifyObservers(theAction);
	}

	/**
	 * Buy the product
	 */
	public void doBuy()
	{
		Previous = theProduct;
		String theAction = "";
		int    amount  = 1;                         //  & quantity
		try
		{
			if ( theState != State.checked )          // Not checked
			{                                         //  with customer
				theAction = "Check if OK with customer first";
			} else {
				boolean stockBought =                   // Buy
						theStock.buyStock(                    //  however
								theProduct.getProductNum(),         //  may fail              
								theProduct.getQuantity() );         //
				if ( stockBought )                      // Stock bought
				{                                       // T
					makeBasketIfReq();                    //  new Basket ?
					theBasket.add( theProduct );          //  Add to bought
					theAction = "Purchased " +            //    details
							theProduct.getDescription();  //
				} else {                                // F
					theAction = "!!! Not in stock";       //  Now no stock
				}
			}
		} catch( StockException e )
		{
			DEBUG.error( "%s\n%s", 
					"CashierModel.doBuy", e.getMessage() );
			theAction = e.getMessage();
		}
		theState = State.process;                   // All Done
		setChanged(); notifyObservers(theAction);
	}


	/**
	 * Delete the last item in the basket
	 * @throws StockException 
	 */
	public void doDelete() throws StockException {
		if(Previous != null) { //Delete Previous if not null
			for(Product a: theBasket) { //Find product in basket
				if(a.getProductNum().equals(Previous.getProductNum())) {
					a.setQuantity(a.getQuantity() - 1);
					if(a.getQuantity() < 0) {
						a.setQuantity(0);
					}
					theStock.addStock(a.getProductNum(), 1);
					break;
				}
			}
			Previous = null;			
			setChanged(); notifyObservers("Deleted");
		} else { //if null, delete based on user input
			JPanel panel = new JPanel();
			panel.add(new JLabel("Enter product number to delete:"));
			JTextField delete = new JTextField(6);
			panel.add(delete);

			Object[] options = {"Submit", "Cancel"};

			int reply = JOptionPane.showOptionDialog(null, panel, "Delete", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
			if(reply == 0 && delete.getText() != null) {
				boolean found = false;
				for(Product a: theBasket) { //Find product in basket
					if(a.getProductNum().equals(delete.getText())) {
						if(a.getQuantity() > 0) {
							theStock.addStock(a.getProductNum(), 1);
							a.setQuantity(a.getQuantity() - 1);
						}						
						if(a.getQuantity() < 0) {
							a.setQuantity(0);
						}
						found = true; //found product in basket
						break;
					}
				}					
				if(found) { //only notify deletion if product was found
					setChanged(); notifyObservers("Deleted");
				} else {
					JOptionPane.showMessageDialog(null, "Product not in Basket");
				}
			} 
		}
	}


	/**
	 * Customer pays for the contents of the basket
	 */
	public void doBought()
	{
		if(handlePayment()) {
			//Save Order to file
			try {
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");  
				LocalDateTime now = LocalDateTime.now();
				File file = new File("OrderInfo/Orders/Order"+theBasket.getOrderNum()+"["+dtf.format(now)+"]"+".txt");
				file.createNewFile();			

				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw);
				out.println(theBasket.getDetails());
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String theAction = "";
			int    amount  = 1;                       //  & quantity
			try
			{
				if ( theBasket != null &&
						theBasket.size() >= 1 )            // items > 1
				{                                       // T
					theOrder.newOrder( theBasket );       //  Process order
					theBasket = null;                     //  reset
				}                                       //
				theAction = "Next customer";            // New Customer
				theState = State.process;               // All Done
				theBasket = null;
			} catch( OrderException e )
			{
				DEBUG.error( "%s\n%s", 
						"CashierModel.doCancel", e.getMessage() );
				theAction = e.getMessage();
			}			
			
			theBasket = null;
			setChanged(); notifyObservers(theAction); // Notify
		}
	}
	/**
	 * ask for update of view called at start of day
	 * or after system reset
	 */
	public void askForUpdate()
	{
		setChanged(); notifyObservers("Welcome");
	}

	/**
	 * make a Basket when required
	 */
	private void makeBasketIfReq()
	{
		if ( theBasket == null )
		{
			try
			{
				int uon   = theOrder.uniqueNumber();     // Unique order num.
				theBasket = makeBasket();                //  basket list
				theBasket.setOrderNum( uon );            // Add an order number
			} catch ( OrderException e )
			{
				DEBUG.error( "Comms failure\n" +
						"CashierModel.makeBasket()\n%s", e.getMessage() );
			}
		}
	}

	/**
	 * return an instance of a new Basket
	 * @return an instance of a new Basket
	 */
	protected Basket makeBasket()
	{
		return new Basket();
	}

	/*
	 * Payment System
	 */
	private boolean handlePayment() {
		Object[] options = {"Card", "Exact Cash", "Cash", "Cancel" };

		int reply = JOptionPane.showOptionDialog(null, "How would you like to pay?", "Transaction Type",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
		if (reply == 0 || reply == 1) {			
			return true;
		} else if(reply == 2) {			
			if(handleCash()) {
				return true;
			}
			return false;
		} else {
			return false;
		}
	}

	/*
	 * Cash System
	 */
	private boolean handleCash() {
		double total = 0.0;
		for(Product a: theBasket) {
			total += a.getPrice() * a.getQuantity();
		}

		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter cash amount:"));
		JTextField cash = new JTextField(6);
		panel.add(cash);

		int reply = JOptionPane.showConfirmDialog(null, cash, "Process Cash", JOptionPane.YES_NO_OPTION);
		if(reply == JOptionPane.YES_OPTION) {
			if(total - Double.parseDouble(cash.getText()) > 0) {
				JOptionPane.showMessageDialog(null, "Not Enough Cash! "+(total - Double.parseDouble(cash.getText()))+" Required");
				//handleCash();
				return false;
			} 
			JOptionPane.showMessageDialog(null, "Change Due: "+(total - Double.parseDouble(cash.getText())));
			return true;
			
		} else {
			return false;
		}
	}
}

