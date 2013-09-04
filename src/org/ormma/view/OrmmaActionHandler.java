/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */


package org.ormma.view;

import java.util.HashMap;
import java.util.Map;

import org.ormma.controller.OrmmaController.Dimensions;
import org.ormma.controller.OrmmaController.PlayerProperties;
import org.ormma.controller.util.OrmmaPlayer;
import org.ormma.controller.util.OrmmaPlayerListener;
import org.ormma.controller.util.OrmmaUtils;
import org.ormma.view.OrmmaView.ACTION;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/**
 * Activity class to handle full screen audio/video
 * @author Roshan
 *
 */
public class OrmmaActionHandler extends Activity {

	private HashMap<ACTION, Object> actionData = new HashMap<ACTION, Object>();
	private RelativeLayout layout;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle data = getIntent().getExtras();
		
		layout = new RelativeLayout(OrmmaActionHandler.this);
		layout.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setContentView(layout);
		
		doAction(data);
		
	}

	/**
	 * Perform action - Play audio/video
	 * @param data - Action data
	 */
	private void doAction(Bundle data) {

		String actionData = data.getString(OrmmaView.ACTION_KEY);
				
		if(actionData == null)
			return;
		
		OrmmaView.ACTION actionType = OrmmaView.ACTION.valueOf(actionData); 
		
		switch (actionType) {
		case PLAY_AUDIO: {
			OrmmaPlayer player = initPlayer(data,actionType);			
			player.playAudio();
		}
			break;
		case PLAY_VIDEO: {
			OrmmaPlayer player = initPlayer(data,actionType);
			player.playVideo();
		}
			break;
		default:
			break;
		}
	}
	
	/**
	 * Create and initialize player
	 * @param playData - Play data
	 * @param actionType - type of action
	 * @return
	 */
	OrmmaPlayer initPlayer(Bundle playData,ACTION actionType){				

		PlayerProperties properties = (PlayerProperties) playData.getParcelable(OrmmaView.PLAYER_PROPERTIES);

		Dimensions playDimensions = (Dimensions)playData.getParcelable(OrmmaView.DIMENSIONS);		
				
		OrmmaPlayer player = new OrmmaPlayer(getApplicationContext());
		player.setPlayData(properties,OrmmaUtils.getData(OrmmaView.EXPAND_URL, playData));
		
		RelativeLayout.LayoutParams lp;
		if(playDimensions == null) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
			lp.addRule(RelativeLayout.CENTER_IN_PARENT);				
		}
		else {
			// Play video in dimensions given
			lp = new RelativeLayout.LayoutParams(playDimensions.width, playDimensions.height);
			lp.topMargin = playDimensions.y;
			lp.leftMargin = playDimensions.x;		

		}
		player.setLayoutParams(lp);
		layout.addView(player);
		
		this.actionData.put(actionType, player);
		setPlayerListener(player);
		
		return player;
	}
	
	/**
	 * Set listener
	 * @param player - player instance
	 */
	private void setPlayerListener(OrmmaPlayer player){
		player.setListener(new OrmmaPlayerListener() {
			
			@Override
			public void onPrepared() {
				
				
			}
			
			@Override
			public void onError() {				
				finish();
			}
			
			@Override
			public void onComplete() {
				finish();
			}
		});
	}

	@Override
	protected void onStop() {
		
		for(Map.Entry<ACTION, Object> entry: actionData.entrySet()){
			switch(entry.getKey()){
			case PLAY_AUDIO : 
			case PLAY_VIDEO : {
				OrmmaPlayer player = (OrmmaPlayer)entry.getValue();
				player.releasePlayer();
			}			
			break;
			default : break;
		}	
	}
		super.onStop();
	}	
	
}
