package naarad.client.tabhost;
import android.app.Application;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View.OnTouchListener;
import android.support.v4.app.Fragment;
import java.net.UnknownHostException;
import android.widget.RelativeLayout;
import android.content.res.Resources;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.widget.ToggleButton;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.Button;
import android.app.Activity;
import android.os.AsyncTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Integer;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import java.net.Socket;
import android.view.View.MeasureSpec;
import android.util.TypedValue;
import org.json.JSONException;
import android.text.Spanned;
import org.json.JSONObject;
import android.text.Html;
import android.view.Gravity;
import java.util.Set;
import java.lang.Math;


//public class NaaradControlFragment extends Fragment implements View.OnLongClickListener 
public class NaaradControlFragment extends NaaradAbstractFragment //implements OnTouchListener
{
    private static View mView;

    private Socket client;
    private PrintWriter printwriter;
    private ToggleButton lamp0, lamp1, lamp2, currentToggleButton;
    private ToggleButton[] lampArr;
    private ImageView bulb0, bulb1, bulb2;
    private ImageView[] bulbArr;
    private TextView[] tempBubbleArr;
    //    private TextView ttBulbArr;
    private String messsage, serverName;
    private int serverPort=1234;
    private boolean tempInDegC=true;
    final private String ALL_WELL="All well";
    private ImageView iv;

    //public boolean touchFlag=false;
    //private View selected_item=null;
    //private int longPress_x, longPress_y;

    private GestureDetector gGestureDetector, gBubbleGestureListener;
    private View.OnTouchListener gOnTouchListener, gBubbleOnTouchListener;
    private View.OnClickListener gLampHandler;
    //private View.OnClickListener gBulbOnClickListener;
    //private boolean touchFlag_p;
    private Activity mActivity0=null;        
    //private MyViewPager mViewPager=null;
    public NaaradApp myApp;
    public NaaradDnDParameters gDnDParams;
    //
    //-----------------------------------------------------------------------------------------
    //
    private void resizeView(View v, MotionEvent event, int xSizeDP, int ySizeDP)
    {
	int x,y,pX,pY, oX=0, oY=0, iX,iY;
	int xsdp,ysdp;
	xsdp = (int)(xSizeDP*myApp.densityDpi/160.0);
	ysdp = (int)(ySizeDP*myApp.densityDpi/160.0);
	
	pX=(int) event.getX();
	pY=(int) event.getY();

	oX=v.getHeight()*2;
	oY=v.getWidth();
	oX = xsdp*2;
	oY = ysdp;
	iX=v.getRight();
	iY=v.getTop();
	x=pX - oX + iX;
	y=pY - oY + iY;
	// System.err.println("M Display Here X Value-->"+(x)+" "+pX+" "+oX+" "+iX);
	// System.err.println("M Display Here Y Value-->"+(y)+" "+pY+" "+oY+" "+iY);

	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
	    (
	     new ViewGroup.MarginLayoutParams
	     (RelativeLayout.LayoutParams.WRAP_CONTENT,
	      RelativeLayout.LayoutParams.WRAP_CONTENT)
	     );
	lp.setMargins(x, y, 0, 0);                  
	lp.height=xsdp;
	lp.width=ysdp;
	v.setLayoutParams(lp);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public static final NaaradControlFragment newInstance(String sampleText) 
    {
	NaaradControlFragment f = new NaaradControlFragment();
	
	Bundle b = new Bundle();
	b.putString("bString", sampleText);
	f.setArguments(b);
	
	return f;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public TextView setSensorValues(String jsonStr)
    {
	//System.err.println("From NCF: "+jsonStr);
	float temp, svolt, rssi;
	int nodeid;
	try 
	    {
		JSONObject json = new JSONObject(jsonStr);
		temp   = (float)json.getDouble("degc");

		// svolt  = (float)json.getDouble("node_v");
		// rssi   = (float)json.getDouble("node_p");
		nodeid = json.getInt("node_id");
		int bubbleID=mapNodeID2Ndx(nodeid);
		// if (nodeid == 1)      bubbleID = 0;
		// else if (nodeid == 3) bubbleID = 1;
		

		String unit=(String)tempBubbleArr[bubbleID].getTag(R.integer.key1);
		if (unit=="F") temp = temp * 9.0F/5.0F + 32.0F;

		setBubbleValue(nodeid, temp);
		return tempBubbleArr[bubbleID];
	    }
	catch (JSONException e) 
	    {
		//throw new RuntimeException(e);
		System.err.println(e.getMessage());
	    }
	return null;
    }
    public void setBubbleValue(int nodeid, float temp)
    {
	class myRunnable implements Runnable
	{
	    int thisID=0;
	    String thisText;
	    TextView thisTV;
	    Spanned tmp;

	    //tempBubbleArr[thisID].setText(thisText);
	    //myRunnable(int id, String text) 
	    myRunnable(TextView tv, String text) 
	    {
		thisTV=tv; thisText = text;
		tmp = Html.fromHtml("<p><b>"+thisText+"</b><font size =\"50\" color=\"#0066FF\"></font></p>");
	    }
	    myRunnable(TextView tv, Spanned text) 
	    {
		thisTV=tv; 
		tmp = text;
	    }
	    public void run()  
	    {
		//tempBubbleArr[thisID].setText(thisText);
		// tempBubbleArr[thisID].setText(tmp);
		// tempBubbleArr[thisID].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		// tempBubbleArr[0].setTextColor(Color.parseColor("white"));

		thisTV.setText(tmp);
		thisTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		thisTV.setTextColor(Color.parseColor("white"));
	    } 
	};
	int bubbleID=mapNodeID2Ndx(nodeid);
	// int bubbleID=0;
	// if (nodeid == 1)      bubbleID = 0;
	// else if (nodeid == 3) bubbleID = 1;
	
	String text=String.format("%.2f",temp), 
	    unit=(String)tempBubbleArr[bubbleID].getTag(R.integer.key1);
	text = text+unit;

	tempBubbleArr[bubbleID].setTag(R.integer.key0,temp);
	//tempBubbleArr[bubbleID].setTag(R.integer.key1,unit);

	//Spanned tmp=Html.fromHtml("<p>"+text+"<sup><small>"+unit+"</small></sup></p>");
	//myRunnable setTVText=new myRunnable(tempBubbleArr[bubbleID],tmp);
	myRunnable setTVText=new myRunnable(tempBubbleArr[bubbleID],text);

	tempBubbleArr[bubbleID].post(setTVText);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public void setBulbBG(View v,boolean on)
    {
	if (on)
	    {
		((ImageView)(v)).setImageDrawable(mActivity0.getResources().getDrawable(R.drawable.lamp_off));
	    }
	else
	    {
		((ImageView)(v)).setImageDrawable(mActivity0.getResources().getDrawable(R.drawable.lamp_on));
		//v.setBackgroundColor(Color.YELLOW);
	    }
	v.setBackgroundColor(Color.TRANSPARENT);
	
	// if (on) v.setBackgroundColor(Color.TRANSPARENT);
	// else    v.setBackgroundColor(Color.YELLOW);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public boolean setLampBG(View v)
    {
	boolean on = ((ToggleButton)v).isChecked();
	if (on) 
	    {
		//v.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.lamp_on));
		v.getBackground().setAlpha(255);
	    }
	else    
	    {
		//		v.setBackgroundDrawable(R.drawable.lamp_off);
		v.getBackground().setAlpha(128);
	    }
	return on;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public void lampHandler0(View v)
    {
	serverName = getServerName();
	serverPort = getServerPort();
	currentToggleButton = (ToggleButton)(v);
	//Log.i("Ctrl Server: ", serverName+":"+serverPort);
	
	int tag = Integer.parseInt((String)v.getTag());
	boolean on = setLampBG(v);
	setBulbBG(bulbArr[tag],!on);
	messsage="tell "+v.getTag()+" ";
	
	if (on)	messsage += "1";
	else	messsage += "0";
	
	//	Log.i("Cmd: ",messsage);
	
	SendMessage sendMessageTask = new SendMessage();
	sendMessageTask.execute();
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    // @Override public void onSaveInstanceState(Bundle outState)
    // {
    // 	//Log.i("setInstance: ", "Setting 0");
	
    // 	super.onSaveInstanceState(outState);
    // 	ColorDrawable cd=(ColorDrawable)(bulb2.getBackground());
    // 	outState.putInt("bg0", cd.getColor());
    // }
    //
    //-----------------------------------------------------------------------------------------
    //
    GestureDetector.SimpleOnGestureListener bubbleGestureListener
	= new GestureDetector.SimpleOnGestureListener()
	    {
		@Override public boolean onSingleTapUp(MotionEvent e)
		{
		    super.onSingleTapUp(e);
		    tempInDegC = !tempInDegC;
		    // if (tempInDegC) toast("Temp. in degC");
		    // else toast("Temp. in F");
		    float tt    = ((Number)gDnDParams.selected_item.getTag(R.integer.key0)).floatValue();
		    String unit = (String)gDnDParams.selected_item.getTag(R.integer.key1);
		    int nodeid = (Integer)gDnDParams.selected_item.getTag();
		    //System.err.println("T: "+tt+unit+nodeid);
		    if (unit == "C")
			{
			    tt = tt * 9.0F/5.0F + 32.0F;
			    unit = "F";
			}
		    else
			{
			    tt = (tt-32.0f)*5.0f/9.0f;
			    unit = "C";
			}
		    gDnDParams.selected_item.setTag(R.integer.key1,unit);
		    // if (!tempInDegC) 
		    // 	tt = tt * 9.0F/5.0F + 32.0F;
		    //     //temp = temp * 9.0F/5.0F + 32.0F;
		    // else
		    // 	tt = (tt-32.0f)*5.0f/9.0f;
		    setBubbleValue(nodeid,tt);
			
		    return false;
		}
		@Override public void onLongPress(MotionEvent e)
		{
		    // Log.i("Gesture: ","LongPress");
		    super.onLongPress(e);
		    toast("DnD activated",Gravity.TOP|Gravity.RIGHT);

		    gDnDParams.touchFlag_p=true;
		    //((TextView)gDnDParams.selected_item).setAlpha(100);
		    //gDnDParams.selected_item.setBackgroundColor(Color.GREEN);
		    gDnDParams.selected_item.getBackground().setAlpha(80);
		    
		    myApp.setSwipeState(false);

		    gDnDParams.longPress_x = (int)e.getX();
		    gDnDParams.longPress_y = (int)e.getY();

		    // int[] loc = new int[2];
		    // gDnDParams.selected_item.getLocationOnScreen(loc);
		    // System.err.println("On screen (x,y): "
		    // 		       +loc[0]+" "+loc[1]+" "
		    // 		       +gDnDParams.selected_item.getRight()+" "+gDnDParams.selected_item.getTop()+" "
		    // 		       +gDnDParams.longPress_x+" "+gDnDParams.longPress_y);

		return;
		};
	    };
    //
    //-----------------------------------------------------------------------------------------
    // Gesture listener with onSingleTapUp() and onLongPress() overloaded.  
    //
    GestureDetector.SimpleOnGestureListener myGestureListener
	= new GestureDetector.SimpleOnGestureListener()
	{
	    //
	    // onSingleTap() on the lamp icons recreates the same
	    // operation as click on the associated ToggleButtons.
	    //
	    @Override public boolean onSingleTapUp(MotionEvent e)
	    {
	    	super.onSingleTapUp(e);
		//		Log.i("Gesture: ","TapUp");
		int tag=(Integer)(gDnDParams.selected_item.getTag(R.integer.key0));
		//Log.i("Tag: ",Integer.toString(tag));//R.integer.key0);
		boolean on = lampArr[tag].isChecked();
		setBulbBG(gDnDParams.selected_item, on);
		lampArr[tag].setChecked(!on);
		lampHandler0(lampArr[tag]);
	    	return false;
	    }
	    //
	    // onLongPress() sets up the global variables used for the
	    // MOVE operation (touchFlag_p, selected_item and
	    // longPress_{x,y}) and disables the swiping of the
	    // Fragements.
	    //
	    @Override public void onLongPress(MotionEvent e)
	    {
		// Log.i("Gesture: ","LongPress");
		toast("DnD activated",Gravity.TOP|Gravity.RIGHT);
		super.onLongPress(e);
		gDnDParams.touchFlag_p=true;
		// ttbulb -- uncomment the following line when not using ttbulb
		((ImageView)gDnDParams.selected_item).setAlpha(100);
		
		gDnDParams.selected_item.setBackgroundColor(Color.GREEN);
		gDnDParams.selected_item.getBackground().setAlpha(100);

		//resizeView(selected_item, e, 50, 50);
		//mViewPager = ((MyViewPager)mActivity0.findViewById(R.id.viewpager));
		//mViewPager.enableSwipe(false);
		//mViewPager.enableSwipe(false);

		myApp.setSwipeState(false);
		// Location of the long press event on the screen (in
		// the co-ordinate system of the Fragment display area
		// -- i.e. minus any area of the screen occupied by
		// the Tabs etc.).
		//
		// These co-ordinates are relative to the co-ordinate
		// system of the View on which the LongPress was
		// detected.  Therefore these need not be reset in the
		// MOVE event handler.
		gDnDParams.longPress_x = (int)e.getX();
		gDnDParams.longPress_y = (int)e.getY();

		int[] loc = new int[2];
		gDnDParams.selected_item.getLocationOnScreen(loc);
		System.err.println("On screen (x,y): "
				   +loc[0]+" "+loc[1]+" "
				   +gDnDParams.selected_item.getRight()+" "+gDnDParams.selected_item.getTop()+" "
				   +gDnDParams.longPress_x+" "+gDnDParams.longPress_y);

		//containerOnTouch(selected_item, e, touchFlag_p);
		return;
	    }
	};
    //
    //-----------------------------------------------------------------------------------------
    //
    private void makeHandlers(Activity activity)
			      // View.OnClickListener lampHandler, 
			      // View.OnTouchListener onTouchListener, 
			      // GestureDetector gestureDetector)
    {
	// The following handles events on the toggle buttons
	gLampHandler = new View.OnClickListener()
	    {
		public void onClick(View v) 
		{
		    lampHandler0(v);
		}
	    };
	
	// The following two handlers handle gestures (SingleTap,
	// LongPress) on lamp icons.
	// gOnTouchListener = new View.OnTouchListener() 
        //     {
        //         public boolean onTouch(View v, MotionEvent event) 
        //         {
	// 	    return myOnTouchListener_p(v,event);
	// 	}
	//     };

	// Gesture detection for the lamp icons
	gGestureDetector = new GestureDetector(mActivity0, myGestureListener);
	gOnTouchListener = new NaaradDnDOnTouchListener(gGestureDetector, gDnDParams,myApp,1.0F,1.0F,20,20); //Test code
	
	// Gesture detection for the data-bubble icons.  Need for the
	// height,width fudge factors (2.0, 1.5) should fixed.
	gBubbleGestureListener = new GestureDetector(mActivity0, bubbleGestureListener);
	gBubbleOnTouchListener = new NaaradDnDOnTouchListener(gBubbleGestureListener, gDnDParams,myApp,1.0F,1.0F,45,25);//2.3F,1.5F); //Test code
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    private void installHandlers(View.OnClickListener lampHandler, 
				 View.OnTouchListener onTouchListener, 
				 ToggleButton[] lampArr, 
				 ImageView[] bulbArr)
    {
	// Set up the handlers for events on the toggle buttons
	// (Single click).
	for (int i=0; i<lampArr.length; i++)
	    {
		lampArr[i].setTag(Integer.toString(i)); 
		lampArr[i].setOnClickListener(lampHandler);
		setLampBG(lampArr[i]);
	    }

	// Set up the handlers to handle gestures (SingleTap,
	// LongPress) on the icons for the lamps.
	for (int i=0; i<bulbArr.length; i++)
	    {
		// if (i==1) 
		//     {
		// 	ttBulbArr.setTag(R.integer.key0,i);
		// 	ttBulbArr.setTag(R.integer.key1,"0");
		// 	ttBulbArr.setOnTouchListener(onTouchListener);
		//     }
		// else 
		    {
			bulbArr[i].setTag(R.integer.key0,i);
			bulbArr[i].setTag(R.integer.key1,"0");
			bulbArr[i].setOnTouchListener(onTouchListener);
		    }
	    }
    }	
    //
    //-----------------------------------------------------------------------------------------
    //
    public void makeSensorIcons(View thisView)
    {
	//
	// To add more icons/buttons, adding to the following arrays
	// would be sufficient.
	//
	mActivity0 = getActivity();
	lampArr = new ToggleButton[3];
	bulbArr = new ImageView[3];

	int[] keys = getKeysAsArray(nodeID2Ndx);
	// for (int i=0;i<keys.length;i++)
	//     System.err.println("Keys: "+keys[i]);

	int nBubbles = nodeID2Ndx.size();

	tempBubbleArr = new TextView[nBubbles];

	lampArr[0]  = lamp0 = (ToggleButton) thisView.findViewById(R.id.lamp0);
	lampArr[1]  = lamp1 = (ToggleButton) thisView.findViewById(R.id.lamp1);
	lampArr[2]  = lamp2 = (ToggleButton) thisView.findViewById(R.id.lamp2);
	bulbArr[0]  = bulb0 = (ImageView) thisView.findViewById(R.id.iv1); 
	bulbArr[1]  = bulb1 = (ImageView) thisView.findViewById(R.id.iv2); 

	//ttBulbArr  =  (TextView) thisView.findViewById(R.id.iv2); 
	bulbArr[2]  = bulb2 = (ImageView) thisView.findViewById(R.id.iv3); 

	tempBubbleArr[0] = (TextView) thisView.findViewById(R.id.tv1);
	tempBubbleArr[1] = (TextView) thisView.findViewById(R.id.tv2);

	for (int i=0;i<tempBubbleArr.length;i++)
	    {
		tempBubbleArr[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		tempBubbleArr[i].setTag(R.integer.key1,"C");
		//tempBubbleArr[0].setTag(1); // Set the default tag value
		tempBubbleArr[i].setTag(keys[i]); // Set the default tag value
		//setBubbleValue(1,20.63f);
		//tempBubbleArr[0].setText(Html.fromHtml("<p><b>21.63C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
		tempBubbleArr[i].setTextColor(Color.parseColor("white"));
	    }

	// tempBubbleArr[1].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
	// //tempBubbleArr[1].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
	// tempBubbleArr[1].setTag(R.integer.key1,"C");
	// //tempBubbleArr[1].setTag(3); // Set the default tag value
	// tempBubbleArr[1].setTag(keys[1]); // Set the default tag value
	// //setBubbleValue(3,21.45f);
	// tempBubbleArr[1].setTextColor(Color.parseColor("white"));


	//
	// If the position of icons was saved earlier, load the
	// position and re-position the icons.
	//
	int oX,oY,x=-2,y=-2, xMargin=(int)(54*480.0/600.0), yMargin=(int)(24*800.0/1024.0);
	oX=oY=myApp.dpToPixel(20);//(int)(20*myApp.densityDpi);///160.0);
	
	for (int i=0;i<bulbArr.length;i++)
	    {
		//Drawable image = getResources().getDrawable(R.id.iv1);
		//float scale = myApp.metrics.widthPixels / (float) bulbArr[i].getIntrinsicWidth();

		System.err.println("Scale = "+myApp.metrics.widthPixels);

		x=getPreference("bulbX"+String.format("%d",i),-1);
		y=getPreference("bulbY"+String.format("%d",i),-1);
		System.err.println("bulb"+i+" "+x+" "+y);
		if ((x != -1) && (y != -1))
		    {
			// if (i == 1)
			//     gDnDParams.moveView(ttBulbArr,x-xMargin,y-yMargin,oX,oY,1.0F,1.0F);
			// else
			    {
				System.err.println("bulb"+i
						   +" "+((View)bulbArr[i].getParent()).getLeft()
						   // +" "+bulbArr[i].getTotalPaddingRight()
						   // +" "+bulbArr[i].getTotalPaddingLeft()
						   // +" "+bulbArr[i].getTotalPaddingTop()
						   // +" "+bulbArr[i].getTotalPaddingBottom()
						   // +" "+xPadding
						   +" "+myApp.densityDpi
						   );
				gDnDParams.moveView(bulbArr[i],x-xMargin,y-yMargin,oX,oY,1.0F,1.0F);
			    }
		    }			    
	    }
	xMargin = (int)(xMargin *myApp.densityDpi);
	for (int i=0; i<tempBubbleArr.length; i++)
	    {
		tempBubbleArr[i].measure(MeasureSpec.UNSPECIFIED,MeasureSpec.UNSPECIFIED);
		int width = tempBubbleArr[i].getMeasuredWidth();
		int height = tempBubbleArr[i].getMeasuredHeight();
		width=myApp.dpToPixel(45);//(int)(45*myApp.densityDpi);
		height=myApp.dpToPixel(25);//(int)(25*myApp.densityDpi);
		x=getPreference("bubbleX"+String.format("%d",i),-1);
		y=getPreference("bubbleY"+String.format("%d",i),-1);
		//int xPadding = 1+tempBubbleArr[i].getTotalPaddingRight()+tempBubbleArr[i].getTotalPaddingLeft();
		int xPadding = (int)((1+tempBubbleArr[i].getTotalPaddingRight()+tempBubbleArr[i].getTotalPaddingLeft())),//*1.5F/2.0F),
		    yPadding = (int)((1+tempBubbleArr[i].getTotalPaddingTop()+tempBubbleArr[i].getTotalPaddingBottom()));//*1.5F/2.0F);
		System.err.println("bubble"+i
				   +" "+tempBubbleArr[i].getTotalPaddingRight()
				   +" "+tempBubbleArr[i].getTotalPaddingLeft()
				   +" "+tempBubbleArr[i].getTotalPaddingTop()
				   +" "+tempBubbleArr[i].getTotalPaddingBottom()
				   +" "+((View)tempBubbleArr[i].getParent()).getLeft()
				   +" "+xPadding+" "+yPadding
				   +" "+myApp.densityDpi
				   );
		if ((x != -1) && (y != -1))
		    gDnDParams.moveView(tempBubbleArr[i],
					//x,y,
					x-xMargin-xPadding,y-yMargin,//-xPadding,
					width,height,1.0F,1.0F);
	    }

	// Makes the handles accessed via the global variables
	// gLampHandler, gOnTouchListener, gGestureDetector;
	makeHandlers(mActivity0);

	// Install the gLampHandler, gOnTouchListener,
	// gGestureDetector event/gesture handlers.
	installHandlers(gLampHandler, gOnTouchListener, lampArr, bulbArr);
	tempBubbleArr[0].setOnTouchListener(gBubbleOnTouchListener);
	tempBubbleArr[1].setOnTouchListener(gBubbleOnTouchListener);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
				       Bundle savedInstanceState) 
    {
	super.onCreateView(inflater, container, savedInstanceState);
	myApp = (NaaradApp) getActivity().getApplication();
	//Log.i("dpi: ",Integer.toString(myApp.densityDpi));

	setRetainInstance(true);	
	mActivity0 = getActivity();
	gDnDParams = new NaaradDnDParameters();

	if (recreateView(mView)) return mView;	

	mView = inflater.inflate(R.layout.activity_naarad_control, container, false);
	makeSensorIcons(mView);
	// //
	// // To add more icons/buttons, adding to the following arrays
	// // would be sufficient.
	// //
	// lampArr = new ToggleButton[3];
	// bulbArr = new ImageView[3];
	// tempBubbleArr = new TextView[2];

	// lampArr[0]  = lamp0 = (ToggleButton) mView.findViewById(R.id.lamp0);
	// lampArr[1]  = lamp1 = (ToggleButton) mView.findViewById(R.id.lamp1);
	// lampArr[2]  = lamp2 = (ToggleButton) mView.findViewById(R.id.lamp2);
	// bulbArr[0]  = bulb0 = (ImageView) mView.findViewById(R.id.iv1); 
	// bulbArr[1]  = bulb1 = (ImageView) mView.findViewById(R.id.iv2); 
	// bulbArr[2]  = bulb2 = (ImageView) mView.findViewById(R.id.iv3); 
	// tempBubbleArr[0] = (TextView) mView.findViewById(R.id.tv1);
	// tempBubbleArr[0].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
	// tempBubbleArr[0].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
	// tempBubbleArr[0].setTextColor(Color.parseColor("white"));
	// tempBubbleArr[1] = (TextView) mView.findViewById(R.id.tv2);
	// tempBubbleArr[1].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
	// tempBubbleArr[1].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
	// tempBubbleArr[1].setTextColor(Color.parseColor("white"));
	// // Makes the handles accessed via the global variables
	// // gLampHandler, gOnTouchListener, gGestureDetector;
	// makeHandlers(mActivity0);

	// // Install the gLampHandler, gOnTouchListener,
	// // gGestureDetector event/gesture handlers.
	// installHandlers(gLampHandler, gOnTouchListener, lampArr, bulbArr);
	// tempBubbleArr[0].setOnTouchListener(gBubbleOnTouchListener);
	// tempBubbleArr[1].setOnTouchListener(gBubbleOnTouchListener);

	mView.getBackground().setAlpha(100);

	return mView;
    }
    @Override public void onResume()
    {
	super.onResume();
	makeSensorIcons(mView);
	ViewGroup.MarginLayoutParams lp=(MarginLayoutParams)mView.getLayoutParams();
	System.err.println("Offsets: "+
			   lp.leftMargin+" "+
			   lp.rightMargin);
	getRetainInstance();
	System.err.println("From NCF::onResume");

    }
    @Override public void onPause()
    {
	super.onPause();
	setRetainInstance(true);	
	System.err.println("From NCF::onPause");
	double xf=1.0,yf=1.0;//480.0/600.0, yf=800.0/1024.0;
	for(int i=0;i < lampArr.length; i++)
	    {
		setPreference("bulbX"+String.format("%d", i), (int)(bulbArr[i].getRight()*xf));
		setPreference("bulbY"+String.format("%d", i), (int)(bulbArr[i].getTop()*yf));
	    }
	for(int i=0;i < tempBubbleArr.length; i++)
	    {
		setPreference("bubbleX"+String.format("%d", i), tempBubbleArr[i].getRight());
		setPreference("bubbleY"+String.format("%d", i), tempBubbleArr[i].getTop());
	    }
	
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public String sendCommand(String cmd)
    {
	try 
	    {
		if (cmd.length() == 0) return null;
		
		//Log.i("Thread: ",serverName+":"+Integer.toString(serverPort));
		
		client = new Socket(serverName, serverPort);
		printwriter = new PrintWriter(client.getOutputStream(), true);
		
		printwriter.write(mkMessage("open"));
		printwriter.flush();
		SystemClock.sleep(500);
		
		printwriter.write(mkMessage(cmd)); // write the message to output stream
		// printwriter.write((cmd)); // write the message to output stream
		printwriter.flush();
		SystemClock.sleep(500);
		
		printwriter.write(mkMessage("done"));
		printwriter.flush();
		//SystemClock.sleep(500);
		
		printwriter.close();
		client.close(); // closing the connection
		
	    } 
	catch (UnknownHostException e) 
	    {
		String msg="";
		if (!isWifiConnected()) msg = "Wifi is not connected.\n";
		msg = msg + "Unknown host: "+serverName+":"+Integer.toString(serverPort)+"\nCheck settings";
		return msg;
	    } 
	catch (IOException e) 
	    {
		String msg = "Error connecting to "+serverName+":"+Integer.toString(serverPort)+"\nCheck settings";
		return msg;
	    }
	return ALL_WELL;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    private class SendMessage extends AsyncTask<Void, Void, String> 
    {
	//
	//-----------------------------------------------------------------------------------------
	//
	@Override protected String doInBackground(Void... params) 
	    {
		//Log.i("Thread: ",messsage);
		return sendCommand(messsage);
	    }
	@Override protected void onPostExecute(String result) 
	    {
		super.onPostExecute(result);
		if (result != ALL_WELL)		
		    {
			//Toast.makeText(mActivity0, result, Toast.LENGTH_SHORT).show();
			toast(result,Gravity.TOP|Gravity.RIGHT);
			boolean on = currentToggleButton.isChecked();
			int tag = Integer.parseInt((String)currentToggleButton.getTag());
			
			currentToggleButton.setChecked(!on);
			setBulbBG(bulbArr[tag],on);
		    }
	    }
    }
}
//
//=====================TEST CODE===================================
//

// RelativeLayout rl = new RelativeLayout(mView.getContext());
// ImageView iv;
// RelativeLayout.LayoutParams params;

// int yellow_iv_id = 123; // Some arbitrary ID value.

// iv = new ImageView(rl.getContext());
// iv.setId(yellow_iv_id);
// iv.setImageResource(R.drawable.ic_launcher);
// //	iv.setBackgroundColor(Color.YELLOW);
// params = new RelativeLayout.LayoutParams(30, 40);
// params.leftMargin = 50;
// params.topMargin = 60;
// iv.setLayoutParams(params);
// rl.addView(iv, params);
// iv.setVisibility(View.VISIBLE);

// iv = new ImageView(rl.getContext());
// iv.setImageResource(R.drawable.ourhouse2);
// //	iv.setBackgroundColor(Color.RED);
// params = new RelativeLayout.LayoutParams(30, 40);
// params.leftMargin = 80;
// params.topMargin = 90;

// // This line defines how params.leftMargin and params.topMargin are interpreted.
// // In this case, "<80,90>" means <80,90> to the right of the yellow ImageView.
// params.addRule(RelativeLayout.RIGHT_OF, yellow_iv_id);

// iv.setVisibility(View.VISIBLE);
// rl.addView(iv, params);

	//getRetainInstance();
	// final Resources res = getResources();
	// final int k0 = res.getInteger(R.integer.key0);
	// final int k1 = res.getInteger(R.integer.key1);
	//
	// This checks mView and recreates if it is null.  Otherwise
	// returns the existing one.
	//


	// if(savedInstanceState != null)
	//     {
	// 	Log.i("BG: ",Integer.toString(savedInstanceState.getInt("bg0")));
	// 	bulb2.setBackgroundColor(savedInstanceState.getInt("bg0"));//		mEditText.setText(savedInstanceState.getString("textKey"));
	//     }
	

//
//=====================TEST CODE===================================
//

