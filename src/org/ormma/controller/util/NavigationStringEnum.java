/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller.util;

/**
 * The Enum NavigationStringEnum.
 */
public enum NavigationStringEnum {
	NONE("none"), CLOSE("close"), BACK("back"), FORWARD("forward"), REFRESH("refresh");

	private String text;

	/**
	 * Instantiates a new navigation string enum.
	 * 
	 * @param text
	 *            the text
	 */
	NavigationStringEnum(String text) {
		this.text = text;
	}

	/**
	 * Gets the text.
	 * 
	 * @return the text
	 */
	public String getText() {
		return this.text;
	}

	/**
	 * From string.
	 * 
	 * @param text
	 *            the text
	 * @return the navigation string enum
	 */
	public static NavigationStringEnum fromString(String text) {
		if (text != null) {
			for (NavigationStringEnum b : NavigationStringEnum.values()) {
				if (text.equalsIgnoreCase(b.text)) {
					return b;
				}
			}
		}
		return null;
	}
}
