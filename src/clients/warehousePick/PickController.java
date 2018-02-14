package clients.warehousePick;

import middle.StockException;

/**
 * The BackDoor Controller
 * @author M A Smith (c) June 2014
 */

public class PickController
{
  private PickModel model = null;
  private PickView  view  = null;
  /**
   * Constructor
   * @param model The model 
   * @param view  The view from which the interaction came
   */
  public PickController( PickModel model, PickView view )
  {
    this.view  = view;
    this.model = model;
  }

  /**
   * Picked interaction from view
 * @throws StockException 
   */
  public void doPick() throws StockException
  {
    model.doPick();
  }
  
}

