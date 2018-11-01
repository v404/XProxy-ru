package org.spacehq.mc.protocol.data.game.values.window;


public enum MoveToHotbarParam implements WindowActionParam {
    SLOT_1(0),
    SLOT_2(1),
    SLOT_3(2),
    SLOT_4(3),
    SLOT_5(4),
    SLOT_6(5),
    SLOT_7(6),
    SLOT_8(7),
    SLOT_9(8);

	private int id;
	private MoveToHotbarParam(int id) {
		this.id = id;
	}
	
	public Integer getSlot() {
		return id;
	}
}
