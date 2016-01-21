package TurretBot;

import battlecode.common.*;

public enum MessageEncode { // NEW OPTIMIZE THIS IF YOU CAN, ALSO LOOK IN BOT CLASSES FOR NEW METHODS TO IMPLEMENT, THIS SHOULD BE DOING ALL THE WORK RATHER THAN THE BOTS
	TURRET_TARGET(0, new int[]{1, 2}, 2),	// health, robotType, xloc, yloc
	PROXIMITY_NOTIFICATION(1, new int[]{4}, 0),	// radius squared
	ALPHA_ARCHON_LOCATION (2, new int[]{1,2},0),// xloc , yloc
	MOBILE_ARCHON_LOCATION(3, new int[]{1,2},0),// xloc , yloc
	DIRECT_MOBILE_ARCHON  (4, new int[]{1,2},0),
	STOP_BEING_MOBILE	  (5, new int[]{1,2},0),
	MULTIPLE_TARGETS	  (6, new int[]{7,8,7,8,7,8,7,8,7,8}, 5),// 5 map locations (as ints) **x and y offset from sender must be <16
	WARN_ABOUT_TURRETS    (7, new int[]{7,8,7,8,7,8,7,8,7,8}, 5),// 5 map locations of enemy turrets -- (-1,-1) if fewer than 5
	PART_OR_NEUTRAL_NOTIF (8, new int[]{1,2},0),// map location of parts/neutral thing
	ENEMY_ARMY_NOTIF	  (9, new int[]{1,2},0),// map location of centroid
	ENEMY_TURRET_DEATH	  (10,new int[]{7,8},0);// map location where there is no longer a turret
	//SCOUT_CHECKIN(4, new int[]{    }, 2),
	//FOUND_PARTS(4, new int[]{5, 1, 2}, 1),		// num parts, xloc, yloc
	//FOUND_DEN(5, new int[]{1,2},0),				// xloc, ylo
	//FOUND_NEUTRAL(6,new int[]{1, 2, 7}, 2);		// type.ordinal(), xloc, yloc
	//TODO Scout notifies units of a turtle
	/* 4 - scout notifies mobile archon that it found parts
	 * 5 - scout notifies mobile archon of zombie den
	 * 6 - scout notifies mobile archon of neutral bots*/

	//note: don't forget to add to the whichStruct method when you add more encode keys

	private final int reasonNumber;
	/* 0 - tell turret where to shoot
	 * 1 - tell units how close to stay to the alpha archon
	 * 2 - notify a unit of the alpha archon location
	 * 3 - notify a unit of the hunter archon loc
	 * 4 - tell the archon where to go
	 * 5 - tell the mobile archon to turtle (and where to do so)
	 * 6 - give turrets more than one target
	 * 7 - warn soldiers to avoid turrets they can't see -- (-1,-1) if fewer than 5
	 * 8 - scouts to tell archons about parts or neutrals to be interested in
	 * 9 - for when scouts see a lot of enemies that aren't turrets
	 * 10- if it sees a loc where there used to be a turret
	 * 
	 * (if you increase the max number (15), make sure the space below matches)
	 */

	/* data number values
	 * 0: reason (current max of 7)
	 * 1: loc.x (sent as an offset from sender's loc, offset by 80)
	 * 2: loc.y (sent as an offset from sender's loc, offset by 80)
	 * 3: health (max 2000, dens)
	 * 4: some radius squared (max 2^7) //if this changes change howMuchSpaceDataNeeds
	 * 5: number of parts
	 * 6: robotType (max of 11, so 4 bits)
	 * 7: special loc.x for multiple targets (restricted to 15 away from sender or less)
	 * 8: special loc.x for multiple targets (restricted to 15 away from sender or less)
	 * 
	 * (make sure to update the how much space data needs array)
	 */
	private final int[] whichDataToInclude;
	private final int whereToSplitData; // index in whichDataToInclude that gets bumped to 2nd int
	private static final int[] howMuchSpaceDataNeeds = {4, 7, 7, 11, 7, 10, 4, 5, 5};
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
		MapLocation myloc = Bot.here;
		int powerOfTwo = multiplyByTwo(1,howMuchSpaceDataNeeds[0]);
		for ( int i = 0; i < whereToSplitData; i++){
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] - Bot.center.x + 40;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] - Bot.center.y + 40;
			else if (whichDataToInclude[i] == 7)
				data[i] = data[i] - myloc.x + 15;
			else if (whichDataToInclude[i] == 8)
				data[i] = data[i] - myloc.y + 15;
			mess[0] += data[i]*powerOfTwo;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] - Bot.center.x + 40;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] - Bot.center.y + 40;
			else if (whichDataToInclude[i] == 7)
				data[i] = data[i] - myloc.x + 15;
			else if (whichDataToInclude[i] == 8)
				data[i] = data[i] - myloc.y + 15;
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
				data[i] = data[i] + Bot.center.x - 40;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] + Bot.center.y - 40;
			else if (whichDataToInclude[i] == 7)
				data[i] = data[i] + senderloc.x - 15;
			else if (whichDataToInclude[i] == 8)
				data[i] = data[i] + senderloc.y - 15;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			data[i] = mess[1]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			if(whichDataToInclude[i] == 1)	
				data[i] = data[i] + Bot.center.x - 40;
			else if (whichDataToInclude[i] == 2)
				data[i] = data[i] + Bot.center.y - 40;
			else if (whichDataToInclude[i] == 7)
				data[i] = data[i] + senderloc.x - 15;
			else if (whichDataToInclude[i] == 8)
				data[i] = data[i] + senderloc.y - 15;
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
		case 4: return DIRECT_MOBILE_ARCHON;
		case 5: return STOP_BEING_MOBILE;
		case 6: return MULTIPLE_TARGETS;
		case 7: return WARN_ABOUT_TURRETS;
		case 8: return PART_OR_NEUTRAL_NOTIF;
		case 9: return ENEMY_ARMY_NOTIF;
		case 10:return ENEMY_TURRET_DEATH;

		default: return null;
		}
	}
	private static int multiplyByTwo(int num, int times){
		return num*(int)(0.5 + Math.pow(2, times));
	}
	public String toString(){
		switch(reasonNumber){
		case 0: return "TURRET_TARGET";
		case 1: return "PROXIMITY_NOTIFICATION";
		case 2: return "ALPHA_ARCHON_LOCATION";
		case 3: return "MOBILE_ARCHON_LOCATION";
		case 4: return "DIRECT_MOBILE_ARCHON";
		case 5: return "STOP_BEING_MOBILE";
		case 6: return "MULTIPLE_TARGETS";
		case 7: return "WARN_ABOUT_TURRETS";
		case 8: return "PART_OR_NEUTRAL_NOTIF";
		case 9: return "ENEMY_ARMY_NOTIF";
		case 10:return "ENEMY_TURRET_DEATH";

		default: return "@Nate update the toString you idiot";
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
	 *
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
	}*/
}