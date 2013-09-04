/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller.util;

public enum TransitionStringEnum {

	DEFAULT("default"), DISSOLVE("dissolve"), FADE("fade"), ROLL("roll"), SLIDE("slide"), ZOOM("zoom"), NONE("none");

	private String text;

	TransitionStringEnum(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}

	public static TransitionStringEnum fromString(String text) {
		if (text != null) {
			for (TransitionStringEnum b : TransitionStringEnum.values()) {
				if (text.equalsIgnoreCase(b.text)) {
					return b;
				}
			}
		}
		return null;
	}
}
