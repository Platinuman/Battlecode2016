package team061;

import battlecode.common.*;
import java.lang.Math.*;

public enum MessageEncode {
	TURRET_TARGET(0, new int[]{3, 7, 1, 2}, 2),	// health, robotType, xloc, yloc
	PROXIMITY_NOTIFICATION(1, new int[]{4}, 0),	// radius squared
	ALPHA_ARCHON_LOCATION(2, new int[]{1,2},0),	// xloc , yloc
	MOBILE_ARCHON_LOCATION(3, new int[]{1,2},0);// xloc , yloc
	//note: don't forget to add to the whichStruct method when you add more encode keys

	private final int reasonNumber;
	/* 0 - tell turret where to shoot
	 * 1 - tell units how close to stay to the alpha archon
	 * 2 - notify a unit of the alpha archon location
	 * 3 - notify a unit of the hunter archon loc
	 * 
	 */

	/* data number values
	 * 0: reason (current max of 7)
	 * 1: loc.x (sent as an offset from sender's loc, will have to offset by 100)
	 * 2: loc.y (sent as an offset from sender's loc, will have to offset by 100)
	 * 3: health (max 2000, dens)
	 * 4: some radius squared (max 2^7) //if this changes change howMuchSpaceDataNeeds
	 * 
	 * 7: robotType (max of 11, so 4 bits)
	 * (if you increase the max number, make sure the space below matches)
	 */
	private final int[] whichDataToInclude;
	private final int whereToSplitData; // index in whichDataToInclude that gets bumped to 2nd int
	private static final int[] howMuchSpaceDataNeeds = {3, 9, 9, 11, 7, 0, 0, 4};
	//get 30 slots total per int

	// TODO: make "yell" method to do the actual broadcast too
	// use Bot.rc to access the rc

	private MessageEncode(int reason, int[] data, int split) {
		reasonNumber = reason;
		whichDataToInclude = data;
		whereToSplitData = split;
	}
	/**
	 * Returns the two ints to send in your message. 
	 * <p>
	 * Use by saying (for example):
	 * 		MessageEncode.TURRET_TARGET.encode(new int[]{87, targettype.ordinal(), 1453, 1234});
	 * 								   health of the unit ^			^			x loc^	   ^ y loc
	 * 													  			|		
	 * 			the ordinal is really important whenever you send RobotTypes to this
	 * 
	 * @param data should be in order specified at top (found here: TURRET_TARGET(0, {_this_order_},...))
	 */
	public int[] encode(int[] data) {
		int[] mess = new int[2];
		mess[0] = reasonNumber;
		MapLocation myloc = Bot.rc.getLocation();
		int powerOfTwo = multiplyByTwo(1,howMuchSpaceDataNeeds[0]);
		for ( int i = 0; i < whereToSplitData; i++){
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] - myloc.x + 100;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] - myloc.y + 100;
			mess[0] += data[i]*powerOfTwo;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] - myloc.x + 100;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] - myloc.y + 100;
			mess[1] += data[i]*powerOfTwo;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		return mess;
	}
	/**
	 * ALWAYS USE THIS DECODE.
	 * Data is returned in the order specified at the top.
	 * 
	 * @param senderloc sender's location. used to compress locations more
	 * @param mess the two int array contained in the message
	 */
	public int[] decode(MapLocation senderloc, int[] mess){
		int[] data = new int[whichDataToInclude.length];
		int powerOfTwo = multiplyByTwo(1, howMuchSpaceDataNeeds[0]);
		for ( int i = 0; i < whereToSplitData; i++){
			data[i] = mess[0]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] + senderloc.x - 100;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] + senderloc.y - 100;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			data[i] = mess[1]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] + senderloc.x - 100;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] + senderloc.y - 100;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
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
		switch(firstInt%multiplyByTwo(1,howMuchSpaceDataNeeds[0])){
		case 0: return TURRET_TARGET;
		case 1: return PROXIMITY_NOTIFICATION;
		case 2: return ALPHA_ARCHON_LOCATION;
		case 3: return MOBILE_ARCHON_LOCATION;

		default: return null;
		}
	}
	private static int multiplyByTwo(int num, int times){
		for (int j = 0; j < times; j++)
			num *= 2;
		return num;
	}
	public String toString(){
		switch(reasonNumber){
		case 0: return "TURRET_TARGET";
		case 1: return "PROXIMITY_NOTIFICATION";
		case 2: return "ALPHA_ARCHON_LOCATION";
		case 3: return "MOBILE_ARCHON_LOCATION";

		default: return "@Nate update the toString you idoit";
		}
	}
	/**
	 * ***** DEPRECIATED *****
	 * <p>
	 * DO NOT USE FOR DATA THAT INLCUDES LOCATION(S) such as TURRET_TARGET.
	 * Translates the two ints back into data.
	 * Data is returned in the order specified at the top.
	 *  
	 * @param mess the two int array contained in the message
	 */
	public int[] decode(int[] mess){ // ***** DEPRECIATED *****
		int[] data = new int[whichDataToInclude.length];
		int powerOfTwo = multiplyByTwo(1, howMuchSpaceDataNeeds[0]);
		for ( int i = 0; i < whereToSplitData; i++){
			//System.out.println(Math.log(powerOfTwo)/Math.log(2) + " " + Math.log(multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]))/Math.log(2));
			data[i] = mess[0]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			data[i] = mess[1]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		return data;
	}
}