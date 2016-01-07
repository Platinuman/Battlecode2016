package team061;

import battlecode.common.*;
import java.lang.Math.*;

public enum MessageEncode {
	TURRET_TARGET(0, new int[]{3, 9, 1, 2}, 2),// 30 slots in int 1, less in 2
	PROXIMITY_NOTIFICATION(1, new int[]{4}, 1);
	//note: don't forget to add to the whichStruct method when you add more encode keys

	private final int reasonNumber;
	/* 0 - tell turret where to shoot
	 * 1 - tell units how close to stay to the alpha archon
	 * 
	 */

	/* data number values
	 * 0: reason (current max of 7)
	 * 1: loc.x (will have to offset by 16000)
	 * 2: loc.y (will have to offset by 16000)
	 * 3: health (max 5000, dens)
	 * 4: some radius squared (max 2^7) //if this changes change howMuchSpaceDataNeeds
	 * 
	 * 7: robotType (max of 11, so 4 bits)
	 * (if you increase the max number, make sure the space below matches)
	 */
	private final int[] whichDataToInclude;
	private final int whereToSplitData; // index in whichDataToInclude that gets bumped to 2nd int
	private static final int[] howMuchSpaceDataNeeds = {3, 15, 15, 13, 7, 0, 0, 4, 0, 0};
	//get 30 slots total per int

	private MessageEncode(int reason, int[] data, int split) {
		reasonNumber = reason;
		whichDataToInclude = data;
		whereToSplitData = split;
	}
	/**
	 * Returns the two ints to send in your message. 
	 * 
	 * Use by saying (for example):
	 * 		MessageEncode.TURRET_TARGET.encode(new int[]{87, targettype.ordinal(), 1453, 1234);
	 * 								   health of the unit ^			^			x loc^	   ^ y loc
	 * 													  			|		
	 * 				the ordinal is really important whenever you send RobotTypes to this
	 * 
	 * @param data should be in order specified at top (found here: TURRET_TARGET(0, {_this_order_},...))
	 */
	public int[] encode(int[] data) {
		int[] mess = new int[2];
		mess[0] = reasonNumber;
		int powerOfTwo = howMuchSpaceDataNeeds[0] - 1;
		for ( int i = 0; i < whereToSplitData; i++){
			powerOfTwo += howMuchSpaceDataNeeds[whichDataToInclude[i]];
			if(whichDataToInclude[i] == 1 || whichDataToInclude[i] == 2)	
				data[i] += 16000;
			mess[0] += data[i]*(int)(Math.pow(2, powerOfTwo)+.01);
		}
		powerOfTwo = -1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			powerOfTwo += howMuchSpaceDataNeeds[whichDataToInclude[i]];
			if(whichDataToInclude[i] == 1 || whichDataToInclude[i] == 2)	
				data[i] += 16000;
			mess[1] += data[i]*(int)(Math.pow(2, powerOfTwo)+.01);
		}
		return mess;
	}
	/**
	 * Translates the two ints back into data.
	 * Data is returned in the order specified at the top.
	 *  
	 * @param mess the two int array contained in the message
	 */
	public int[] decode(int[] mess){
		int[] data = new int[whichDataToInclude.length];
		int powerOfTwo = howMuchSpaceDataNeeds[0] - 1;
		for ( int i = 0; i < whereToSplitData; i++){
			powerOfTwo += howMuchSpaceDataNeeds[whichDataToInclude[i]];
			data[i] = mess[0]/(int)(Math.pow(2, powerOfTwo)+.01) % howMuchSpaceDataNeeds[whichDataToInclude[i]];
			if(whichDataToInclude[i] == 1 || whichDataToInclude[i] == 2)	
				data[i] -= 16000;
		}
		powerOfTwo = -1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			powerOfTwo += howMuchSpaceDataNeeds[whichDataToInclude[i]];
			data[i] = mess[1]/(int)(Math.pow(2, powerOfTwo)+.01) % howMuchSpaceDataNeeds[whichDataToInclude[i]];
			if(whichDataToInclude[i] == 1 || whichDataToInclude[i] == 2)	
				data[i] -= 16000;
		}
		return data;
	}
	/**
	 * Tells you the point of a message from our team.
	 * See reason key at top.
	 * <p>
	 * Should call this method to know which decode to use (TURRET_TARGET, for example).
	 * 
	 * @param firstInt first int of the message to be decoded
	 */
	public static MessageEncode whichStruct(int firstInt){
		switch(firstInt%(int)(Math.pow(2,howMuchSpaceDataNeeds[0])+.01)){
		case 0: return TURRET_TARGET;
		case 1: return PROXIMITY_NOTIFICATION;
		
		default: return null;
		}
	}
}