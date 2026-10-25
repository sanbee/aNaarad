package naarad.client.tabhost;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
// import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.text.Spanned;
import android.text.Html;
import android.graphics.Color;
//import java.lang.Integer;
/**
 * ---------------------------------------------------------------------------------------Freely inspired by: 
 * --------------- http://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe-able-tabs-ftw/
 */

public class MainActivity extends FragmentActivity implements
						       OnTabChangeListener, OnPageChangeListener,
						       NaaradPlotFragment.nPlotDataArrivalListener 
{
    
    MyPageAdapter pageAdapter;
    private MyViewPager mViewPager=null;
    private TabHost mTabHost;
    private NaaradControlFragment gControlFragment;
    private TextView b1=null;
    private TextView tab1Label;
    private Handler gUIHandler;
    private mRunnable gRunnableBlinker;

    // This class depends on private variables b1 nd tab1Label.
    class mRunnable implements Runnable
    {
	String theText=null;
	int theColor;
	    
	public void setText(String thisText, int thisColor) 
	{theText=thisText; theColor=thisColor;}
	
	public void run()
	{b1.setTextColor(theColor);tab1Label.setText(theText);}
    }

    public void onDataArrival(String json)
    {
	//System.err.println("MainActivity::onDataArrival()");
	//
	// Give a visual feedback for the arrival of a RF packet.
	// Blink a dot in the title of the sensor data tab.  
	//
	// This is done by (1) creating a Handler attached to the
	// looper in the UI thread, (2) making a Runnable object with
	// the code to change the text of the tab label, (3) setting
	// the Runnable with the label text, posting it and again
	// setting up the Runnable with a "." in the label and posting
	// it with a delay of 200 ms.  These posts via the Handler are
	// queued and executed in the UI thread.
	//
	// Hold the TextView that this JSON string referrs to.
	b1 = (TextView) gControlFragment.setSensorValues(json);

	gRunnableBlinker.setText("Sensors .",Color.GREEN); gUIHandler.post(gRunnableBlinker);
	gRunnableBlinker.setText("Sensors  ",Color.WHITE); gUIHandler.postDelayed(gRunnableBlinker, 250);
    }

    // @Override public void onPause()
    // {
    // 	super.onPause();
    // 	System.err.println("MainActivity::onPause()");
    // 	b1=null;
    // 	gBlinker=null;
    // 	gUIHandler=null;
    // 	gControlFragment=null;
    // 	mTabHost=null;
    // 	mViewPager=null;
    // }

    @Override protected void onResume()
    {
	super.onResume();
	if (this.mViewPager == null) this.mViewPager = (MyViewPager) findViewById(R.id.viewpager);
    }
    @Override protected void onCreate(Bundle savedInstanceState) 
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	
	if (this.mViewPager == null)
	    this.mViewPager = (MyViewPager) findViewById(R.id.viewpager);
	
	// Tab Initialization
	initialiseTabHost();
	
	// Fragments and ViewPager Initialization
	List<Fragment> fragments = getFragments();
	pageAdapter = new MyPageAdapter(getSupportFragmentManager(), fragments);
	this.mViewPager.setAdapter(pageAdapter);
	this.mViewPager.setOnPageChangeListener(MainActivity.this);

	// ((NaaradControlFragment)fragments.get(0)).setViewPager(this.mViewPager);
	// ((NaaradControlFragment)fragments.get(0)).setActivity(this);

	gUIHandler = new Handler(Looper.getMainLooper());
	gRunnableBlinker = new mRunnable();
    }
    
    // Method to add a TabHost
    private static void AddTab(MainActivity activity, TabHost tabHost,
			       TabHost.TabSpec tabSpec) 
    {
	tabSpec.setContent(new MyTabFactory(activity));
	tabHost.addTab(tabSpec);
    }
    
    // Manages the Tab changes, synchronizing it with Pages
    public void onTabChanged(String tag) 
    {
	int pos = this.mTabHost.getCurrentTab();
	this.mViewPager.setCurrentItem(pos);
	// TextView label = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title); 
	// defTextColor1 = label.getCurrentTextColor();
    }
    
    // Manages the Page changes, synchronizing it with Tabs
    @Override public void onPageScrolled(int arg0, float arg1, int arg2) 
    {
	int pos = this.mViewPager.getCurrentItem();
	this.mTabHost.setCurrentTab(pos);

	// Log.i("Main: ","Tab no. "+Integer.toString(pos));

	// fragmentTransaction.setCustomAnimations(animEnter, animExit, animPopEnter, animPopExit);
	// fragmentTransaction.add(android.R.id.content, fragment,"MyStringIdentifierTag");
	// fragmentTransaction.addToBackStack(null);
	// fragmentTransaction.commit();
    }
    
    
    private List<Fragment> getFragments() 
    {
	List<Fragment> fList = new ArrayList<Fragment>();
	
	// TODO Put here your Fragments
	//MySampleFragment f1 = MySampleFragment.newInstance("Sample Fragment 1");
	NaaradControlFragment f1 = NaaradControlFragment.newInstance("Naarad Control Fragment");
        // Capture the article fragment from the activity layout
        gControlFragment = f1;
	tab1Label = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title);

	//MySampleFragment f2 = MySampleFragment.newInstance("Sample Fragment 2");
	NaaradPlotFragment    f2 = NaaradPlotFragment.newInstanceNPF("Naarad Plot Fragment");

	
	//MySampleFragment f3 = MySampleFragment.newInstance("Sample Fragment 3");
	NaaradSettingFragment f3 = NaaradSettingFragment.newInstanceNSF("Naarad Setting Fragment");
	
	
	fList.add(f1);
	fList.add(f2);
	fList.add(f3);
	
	return fList;
    }
    
    // Tabs Creation
    private void initialiseTabHost() 
    {
	mTabHost = (TabHost) findViewById(android.R.id.tabhost);
	mTabHost.setup();
	
	// TODO Put here your Tabs
	MainActivity.AddTab(this, this.mTabHost,
			    this.mTabHost.newTabSpec("Controls").setIndicator("Controls"));
	MainActivity.AddTab(this, this.mTabHost,
			    this.mTabHost.newTabSpec("Sensors  ").setIndicator("Sensors  "));
	MainActivity.AddTab(this, this.mTabHost,
			    this.mTabHost.newTabSpec("Settings").setIndicator("Settings"));
	mTabHost.getTabWidget().getChildAt(0).getLayoutParams().height =40;
	mTabHost.getTabWidget().getChildAt(1).getLayoutParams().height =40;
	mTabHost.getTabWidget().getChildAt(2).getLayoutParams().height =40;
	mTabHost.setOnTabChangedListener(this);
    }
    //
    // MainActivity is an abstract class and the following methods
    // needs an implementation.
    //
    @Override public void onPageScrollStateChanged(int arg0) {}
    @Override public void onPageSelected(int arg0) {}
}
