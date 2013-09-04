package com.MASTAdView.core;


import java.io.IOException;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

public class InternalBrowser extends Dialog {
	
	int ID_MAIN = 1;
	int ID_WEB = 2;
	int ID_BOTTOM = 3; 
	
	Context _context;
	
	Button buttonBack;
	Button buttonForward;
	Button buttonRefresh;
	Button buttonStopRefresh;
	Button buttonOpen;
	
	WebView webView;
	Dialog thisDialog;
	
	public InternalBrowser(Context context, String url) {
		super(context);
		
		thisDialog = this;
		
		this._context = context;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);		

		LinearLayout mailLayout = new LinearLayout(context);
		mailLayout.setId(ID_MAIN);
		mailLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		mailLayout.setOrientation(LinearLayout.VERTICAL);
		
		webView = new WebView(context);
		webView.setId(ID_WEB);
		webView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT,1f));
		webView.loadUrl(url);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true);
		
		LinearLayout bottomLayout = new LinearLayout(context);
		mailLayout.setOrientation(LinearLayout.VERTICAL);
		bottomLayout.setId(ID_BOTTOM);
		bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT,0f));
		bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
		bottomLayout.setBackgroundDrawable(GetDrawable(_context,"ib_bg_down.png"));
		
		buttonBack = AddButton(bottomLayout,"ib_arrow_left_regular.png","ib_arrow_left_press.png","ib_arrow_left_disabled.png");
		buttonForward = AddButton(bottomLayout,"ib_arrow_right_regular.png","ib_arrow_right_press.png","ib_arrow_right_disabled.png");
		buttonRefresh = AddButton(bottomLayout,"ib_apdate_regular.png","ib_apdate_press.png",null,true);
		buttonOpen = AddButton(bottomLayout,"ib_window_regular.png","ib_window_press.png",null);
		
		mailLayout.addView(webView);
		mailLayout.addView(bottomLayout);
		
		setContentView(mailLayout);
		
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		getWindow().setBackgroundDrawable(null);
		
		buttonBack.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				webView.goBack();
				UpdateButtons();
			}
		});
		
		buttonForward.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				webView.goForward();
				UpdateButtons();
			}
		});
		
		buttonRefresh.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				webView.reload();
			}
		});
		
		buttonOpen.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				try
				{
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
					_context.startActivity(intent);
				} catch (Exception e) {					
				}
				
				thisDialog.dismiss();
			}
		});
		
		buttonStopRefresh.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				webView.stopLoading();
			}
		});
		
		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);				
				return true;
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				buttonRefresh.setVisibility(View.VISIBLE);
				buttonStopRefresh.setVisibility(View.GONE);
				UpdateButtons();
			}
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				buttonRefresh.setVisibility(View.GONE);
				buttonStopRefresh.setVisibility(View.VISIBLE);
			}
		});
		UpdateButtons();
	}
	
	void UpdateButtons()
	{
		buttonBack.setEnabled(webView.canGoBack());
		buttonForward.setEnabled(webView.canGoForward());		
	}
	
	Button AddButton(LinearLayout bottomLayout,String normal,String pressed, String disable)
	{
		return AddButton(bottomLayout, normal, pressed, disable,false);
	}
	
	Button AddButton(LinearLayout bottomLayout,String normal,String pressed, String disable,boolean isStop)
	{
		Button button = new Button(_context);
		button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		button.setBackgroundDrawable(GetSelector(_context,normal, pressed, disable));		
		
		LinearLayout ll = new LinearLayout(_context);
		ll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT,1f));
		ll.setGravity(Gravity.CENTER);
		ll.addView(button);
		
		if(isStop)
		{		
			buttonStopRefresh = new Button(_context);
			buttonStopRefresh.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
			buttonStopRefresh.setBackgroundDrawable(GetSelector(_context,"ib_close_regular.png", "ib_close_press.png", null));
			ll.addView(buttonStopRefresh);
			buttonStopRefresh.setVisibility(View.GONE);
		}
		bottomLayout.addView(ll);
		return button;
	}
	
	
	public static StateListDrawable GetSelector(Context context, String normal,String pressed, String disable)
	{
		StateListDrawable result = new StateListDrawable();
		
		result.addState((new int[] {-android.R.attr.state_pressed,
				android.R.attr.state_enabled}),GetDrawable(context,normal)); 
		
		if(pressed!=null) result.addState((new int[] {android.R.attr.state_pressed,
				android.R.attr.state_enabled}),GetDrawable(context,pressed)); 
		if(disable!=null) result.addState((new int[] {-android.R.attr.state_enabled}),GetDrawable(context,disable)); 
			else result.addState((new int[] {-android.R.attr.state_enabled}),GetDrawable(context,normal));
		
		return result;
	}
	
	public static Drawable GetDrawable(Context context, String fileName)
	{
		try {
			return Drawable.createFromStream(context.getAssets().open(fileName), null);
		} catch (IOException e) {
			return null;
		}
	}
	
	/* private Bitmap getBitmapFromAsset(String strName) throws IOException    
	 {         
		 AssetManager assetManager = context.getAssets();          
		 InputStream istr = assetManager.open(strName);         
		 Bitmap bitmap = BitmapFactory.decodeStream(istr);          
		 return bitmap;     
	} 
	
	Drawable GetImage(String imageName)
	{
		try {
			Bitmap bitmap = getBitmapFromAsset(imageName);
		} catch (IOException e) {
			return null;
		}
		return null;
	}*/

}
