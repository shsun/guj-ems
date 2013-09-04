/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.ormma.controller.util;

/**
 * 
 * Interface class to receive call backs from Player
 *
 */
public interface OrmmaPlayerListener {	
	
	/**
	 * On completion
	 */
	public void onComplete();
	
	/**
	 * On loading complete
	 */
	public void onPrepared();
	
	/**
	 * On Error
	 */
	public void onError();
}

