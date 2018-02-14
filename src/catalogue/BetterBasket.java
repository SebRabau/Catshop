package catalogue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class inherits from Basket, and overrides it's add method, which first cycles through the items in the basket
 * to check if the item is already in the basket. If it is, then we simply increase the quantity, and end the method.
 * 
 * If the item is not in the basket, I call a method called basketSort, which compares the product numbers in the 
 * basket to find the correct place for the product to be positioned to be in ascending order. This includes either
 * placing it at the end of the basket list, or inserting it at the correct index in the list using ArrayList function
 * indexOf(product) and add(index, product).
 * 
 * @author Seb Rabau 16851390
 * @version 1.0
 */
public class BetterBasket extends Basket implements Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean add(Product pr) {
		for (Product a : this) {
			if (a.getProductNum().equals(pr.getProductNum())) { //Is the product already in the basket?
				a.setQuantity(a.getQuantity() + 1);  //If so, quantity++
				return true;
			}
		}
		basketSort(pr, this);  //else, find location through sort
		return true;
	}

	private void basketSort(Product pr, Basket bskt) {
		int aResult;
		int prResult = Integer.parseInt(pr.getProductNum()); //String -> Int
		int aIndex;
		boolean found = false;
		
		for (Product a : bskt) {
			aResult = Integer.parseInt(a.getProductNum());  //String -> Int
			if(prResult < aResult) { //Find index if product number comes before that of an element in the list
				aIndex = bskt.indexOf(a);
				super.add(aIndex, pr);
				found = true;
				break;
			}
		}
		
		if(!found) { //If not to be inserted in the middle of list, add to end
			super.add(pr);
		}
	}
}
