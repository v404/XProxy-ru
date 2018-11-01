package org.spacehq.mc.protocol.data.game.values.window;


public enum SpreadItemParam implements WindowActionParam {
    LEFT_MOUSE_BEGIN_DRAG(0),
    LEFT_MOUSE_ADD_SLOT(1),
    LEFT_MOUSE_END_DRAG(2),
    RIGHT_MOUSE_BEGIN_DRAG(4),
    RIGHT_MOUSE_ADD_SLOT(5),
    RIGHT_MOUSE_END_DRAG(6);

	private int id;
	private SpreadItemParam(int id) {
		this.id = id;
	}
	
	public Integer getDrag() {
		return id;
	}
}
