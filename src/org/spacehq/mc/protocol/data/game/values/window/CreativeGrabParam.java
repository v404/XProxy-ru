package org.spacehq.mc.protocol.data.game.values.window;


public enum CreativeGrabParam implements WindowActionParam {
    GRAB(2);

	private int id;
	private CreativeGrabParam(int id) {
		this.id = id;
	}
	
	public Integer getGrab() {
		return id;
	}
}
