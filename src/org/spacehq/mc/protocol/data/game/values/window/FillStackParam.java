package org.spacehq.mc.protocol.data.game.values.window;


public enum FillStackParam implements WindowActionParam {
    FILL(0);

	private int id;
	private FillStackParam(int id) {
		this.id = id;
	}
	
	public Integer getAction() {
		return id;
	}
}
