package team061;

import battlecode.common.*;

public enum MessageEncode {
	// TODO: OPTIMIZE THIS IF YOU CAN, ALSO LOOK IN BOT CLASSES FOR NEW METHODS TO IMPLEMENT, THIS SHOULD BE DOING ALL THE WORK RATHER THAN THE BOTS
	TURRET_TARGET(0, new int[]{3, 6, 1, 2}, 2),	// health, robotType, xloc, yloc
	PROXIMITY_NOTIFICATION(1, new int[]{4}, 0),	// radius squared
	ALPHA_ARCHON_LOCATION (2, new int[]{1,2},0),// xloc , yloc
	MOBILE_ARCHON_LOCATION(3, new int[]{1,2},0),// xloc , yloc
	DEN_NOTIF			  (4, new int[]{1,2,9},0),// den location, dead = 0 , alive = 1
	BE_MY_GUARD	          (5, new int[]{},0),   // no data 
	MULTIPLE_TARGETS	  (6, new int[]{7,8,7,8,7,8,7,8,7,8}, 5),// 5 map locations (as ints) **x and y offset from sender must be <16
						// **NOTE** only can be used by bot that sees the turrets (because of distance restriction)
	WARN_ABOUT_TURRETS    (7, new int[]{7,8}, 0),// map location of enemy turrets -- (here.x,here.y) if fewer than 5
						// **NOTE** only can be used by bot that sees the turrets (because of distance restriction)
	PART_OR_NEUTRAL_NOTIF (8, new int[]{1,2,9},0),// map location of parts/neutral thing, is it an archon
	ENEMY_ARMY_NOTIF	  (9, new int[]{1,2,9},0),// map location of centroid or archon if there is one, boolean that says if there is an enemy archon
	ENEMY_TURRET_DEATH	  (10,new int[]{7,8},0),// map location where there is no longer a turret
						// **NOTE** only can be used by bot that sees the turrets (because of distance restriction)
	RELAY_TURRET_INFO	  (11,new int[]{1,2,1,2,1,2},3),// so archons can tell new things where all the turrets are
	CRUNCH_TIME			  (12,new int[]{1,2,4},1),// approximate center of turret, number of turrets visible 
	NEUTRAL_ARCHON		  (13,new int[]{1,2},0);// xloc, yloc
	//ZOMBIE_HORDE_NOTIF
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
	 * 4 - scouts telling soldiers about dens / den deaths
	 * 5 - tell the soldier i just made to be my guard
	 * 6 - give turrets more than one target
	 * 7 - warn soldiers to avoid turrets they can't see -- (here.x,here.y) if fewer than 5
	 * 8 - scouts to tell archons about parts or neutrals to be interested in
	 * 9 - for when scouts see a lot of enemies that aren't turrets, also enemy archons
	 * 10- if it sees a loc where there used to be a turret
	 * 11- when archons create/activate units they need to know where the turrets are
	 * 12- coordinate crunching
	 * 13- scouts yell when they find a neutral archon
	 * 
	 * (if you increase the max number (15), make sure the space below matches)
	 */

	/* data number values
	 * 0: reason (current max of 7)
	 * 1: loc.x (sent as an offset from sender's loc, offset by 80)
	 * 2: loc.y (sent as an offset from sender's loc, offset by 80)
	 * 3: health (max 2000, dens)
	 * 4: some radius squared (max 2^7) ....... also how many turrets in CRUNCH_TIME
	 * 5: number of parts
	 * 6: robotType (max of 11, so 4 bits)
	 * 7: special loc.x for multiple targets (restricted to 15 away from sender or less)
	 * 8: special loc.x for multiple targets (restricted to 15 away from sender or less)
	 * 9: boolean
	 * 
	 * (make sure to update the how much space data needs array)
	 */
	private final int[] whichDataToInclude;
	private final int whereToSplitData; // index in whichDataToInclude that gets bumped to 2nd int
	private static final int[] howMuchSpaceDataNeeds = {4, 8, 8, 11, 7, 10, 4, 5, 5, 1};
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
			switch (whichDataToInclude[i]) {
			case 1:
				data[i] = data[i] - Bot.center.x + 80;
				break;
			case 2:
				data[i] = data[i] - Bot.center.y + 80;
				break;
			case 7:
				data[i] = data[i] - myloc.x + 15;
				break;
			case 8:
				data[i] = data[i] - myloc.y + 15;
			}
			mess[0] += data[i]*powerOfTwo;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			switch (whichDataToInclude[i]) {
			case 1:
				data[i] = data[i] - Bot.center.x + 80;
				break;
			case 2:
				data[i] = data[i] - Bot.center.y + 80;
				break;
			case 7:
				data[i] = data[i] - myloc.x + 15;
				break;
			case 8:
				data[i] = data[i] - myloc.y + 15;
			}
			mess[1] += data[i]*powerOfTwo;
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		//Bot.rc.setIndicatorString(2,"just encoded " + toString());
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
			switch (whichDataToInclude[i]) {
			case 1:
				data[i] = data[i] + Bot.center.x - 80;
				break;
			case 2:
				data[i] = data[i] + Bot.center.y - 80;
				break;
			case 7:
				data[i] = data[i] + senderloc.x - 15;
				break;
			case 8:
				data[i] = data[i] + senderloc.y - 15;
			}
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
		powerOfTwo = 1;
		for ( int i = whereToSplitData ; i < whichDataToInclude.length ; i++){
			data[i] = mess[1]/powerOfTwo % multiplyByTwo(1, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
			switch (whichDataToInclude[i]) {
			case 1:
				data[i] = data[i] + Bot.center.x - 80;
				break;
			case 2:
				data[i] = data[i] + Bot.center.y - 80;
				break;
			case 7:
				data[i] = data[i] + senderloc.x - 15;
				break;
			case 8:
				data[i] = data[i] + senderloc.y - 15;
			}
			powerOfTwo = multiplyByTwo(powerOfTwo, howMuchSpaceDataNeeds[whichDataToInclude[i]]);
		}
	//	Bot.rc.setIndicatorString(2,"just decoded " + toString());
		return data;
	}
	private static int multiplyByTwo(int num, int times) {
		return num * (int) (0.5 + Math.pow(2, times));
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
		case 4: return DEN_NOTIF;
		case 5: return BE_MY_GUARD;
		case 6: return MULTIPLE_TARGETS;
		case 7: return WARN_ABOUT_TURRETS;
		case 8: return PART_OR_NEUTRAL_NOTIF;
		case 9: return ENEMY_ARMY_NOTIF;
		case 10:return ENEMY_TURRET_DEATH;
		case 11:return RELAY_TURRET_INFO;
		case 12:return CRUNCH_TIME;
		case 13:return NEUTRAL_ARCHON;

		default: return null;
		}
	}
	public String toString(){
		switch(reasonNumber){
		case 0: return "TURRET_TARGET";
		case 1: return "PROXIMITY_NOTIFICATION";
		case 2: return "ALPHA_ARCHON_LOCATION";
		case 3: return "MOBILE_ARCHON_LOCATION";
		case 4: return "DEN_NOTIF";
		case 5: return "BE_MY_GUARD";
		case 6: return "MULTIPLE_TARGETS";
		case 7: return "WARN_ABOUT_TURRETS";
		case 8: return "PART_OR_NEUTRAL_NOTIF";
		case 9: return "ENEMY_ARMY_NOTIF";
		case 10:return "ENEMY_TURRET_DEATH";
		case 11:return "RELAY_TURRET_INFO";
		case 12:return "CRUNCH_TIME";
		case 13:return "NEUTRAL_ARCHON";

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
