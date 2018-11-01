package org.spacehq.mc.protocol.data.game.values.window;


public enum DropItemParam implements WindowActionParam {
    LEFT_CLICK_OUTSIDE_NOT_HOLDING(0),
    RIGHT_CLICK_OUTSIDE_NOT_HOLDING(1),
    DROP_FROM_SELECTED(2),
    DROP_SELECTED_STACK(3);

	private int id;
	private DropItemParam(int id) {
		this.id = id;
	}
	
	public Integer getAction() {
		return id;
	}
}
