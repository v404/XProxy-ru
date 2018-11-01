package org.spacehq.mc.protocol.data.game.values.window;


public enum ShiftClickItemParam implements WindowActionParam {
    LEFT_CLICK(0),
    RIGHT_CLICK(1);

	private int id;
	
	private ShiftClickItemParam(int id) {
		this.id = id;
	}
	
	public Integer getClick() {
		return id;
	}
}
