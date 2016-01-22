package OrganizedBot;

import battlecode.common.*;

public class BotArchon extends Bot {
	static MapLocation alpha, hunter;
	static boolean isAlphaArchon;
	static boolean isMobileArchon;
	static int maxRange;
	static int numScoutsCreated = 0;
	static RobotType typeToBuild;
	// static int numTurretsCreated = 0;

	// mobile archon fields here:
	// NEW darn this is a ton of static initializers, are you sure there isnt a
	// more efficient way to do this?
	static MapLocation targetLocation;// partsLoc, denLoc, neutralLoc;
	// static int cautionLevel = 16; // how close a zombie has to be to run away
	/*
	 * static final int NO_SCOUT = -1000; static int lastTurnSeenScout =
	 * NO_SCOUT; static MapLocation[] densToHunt; static int numDensToHunt;
	 * static int denArraySize; static boolean huntingDen; static int bestIndex;
	 */
	static MapLocation targetDen;
	static boolean scoutCreated;

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
		while (true) {
			try {
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		// maxRange = 2;
		// Signal[] signals = rc.emptySignalQueue();
		isMobileArchon = true;
		targetDen = null;
		typeToBuild = null;
		//// MessageEncode.setArchonTypes(signals); //NEW This should be a
		//// method
		// analyzeMap();
		/*
		 * rc.setIndicatorString(0 ,"no one special"); if
		 * (!signalsFromOurTeam(signals)) { MapLocation myLocation = here; int[]
		 * myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] {
		 * myLocation.x, myLocation.y }); rc.broadcastMessageSignal(myMsg[0],
		 * myMsg[1], 10000); isAlphaArchon = true; rc.setIndicatorString(0
		 * ,"alpha"); alpha = myLocation; } else { for (int i = 0; i <
		 * signals.length; i++) { if(signals[i].getTeam() == us){ int[] message
		 * = signals[i].getMessage(); //can move this outside the if when we
		 * deal with signals from other teams MessageEncode purpose =
		 * MessageEncode.whichStruct(message[0]); int[] decodedMessage =
		 * purpose.decode(signals[i].getLocation(), message); if(purpose ==
		 * MessageEncode.ALPHA_ARCHON_LOCATION){ alpha = new
		 * MapLocation(decodedMessage[0], decodedMessage[1]); } else if( purpose
		 * == MessageEncode.MOBILE_ARCHON_LOCATION){ hunter = new
		 * MapLocation(decodedMessage[0], decodedMessage[1]);break; } } } if
		 * (hunter == null){ MapLocation myLocation = rc.getLocation(); int[]
		 * myMsg = MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] {
		 * myLocation.x, myLocation.y }); rc.broadcastMessageSignal(myMsg[0],
		 * myMsg[1], 10000); isMobileArchon = true; rc.setIndicatorString(0
		 * ,"mobile"); hunter = myLocation; densToHunt = new MapLocation[1000];
		 * denArraySize = 0; numDensToHunt = 0; bestIndex = 0; huntingDen =
		 * false; } }
		 */
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		String s = "";
		for(int i = 0; i < turretSize; i++){
			s += "[" + enemyTurrets[i].location.x + ", " + enemyTurrets[i].location.y +"], "; 
		}
		rc.setIndicatorString(0, s + " " + turretSize);
		repairBotMostInNeed();
		RobotInfo[] enemies = rc.senseNearbyRobots(type.sensorRadiusSquared, them);
		if (rc.isCoreReady()) {
			if (isMobileArchon) {
				beMobileArchon(enemies);
				//rc.setIndicatorString(0, "mobile as of round " + rc.getRoundNum());
			} else if (isAlphaArchon || here.distanceSquaredTo(alpha) <= 2) {
				aarons_shitty_strat();
			} else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(alpha, theSafety);
			}
		}
	}

	private static void beMobileArchon(RobotInfo[] enemies) throws GameActionException {
		// update target den
		if(rc.getRoundNum() % 500 == 0){
			scoutCreated = false;
		}
		RobotInfo[] hostiles = rc.senseHostileRobots(here, RobotType.ARCHON.sensorRadiusSquared);
		updateInfoFromScouts(hostiles);
		rc.setIndicatorString(1,"target loc is " + targetLocation);
		if (rc.isCoreReady()) {
			RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
			RobotInfo[] zombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
			// if i can see enemies run away
			// if(inDanger(allies, enemies, zombies)){
			if (hostiles.length > 0) {
				Nav.flee(hostiles);
				if (rc.getRoundNum() % 5 == 0)// && allies.length <
					// hostiles.length){
					callForHelp();
				return;
			}
			// if i haven't created a scout create one
			if (createScoutIfNecessary(allies)) // isn't running away more
				// important? meh can fix later
				// if necessary
				return;

			// else if i can activate a neutral do it
			if (activateNeutralIfPossible(allies)) {
				return;
			}
			// else if has enough parts for a SOLDIER
			if(typeToBuild == null)
				determineTypeToBuild();
			if (rc.hasBuildRequirements(typeToBuild)) {
				if (targetDen != null)
					buildUnitInDir(here.directionTo(targetDen), typeToBuild, allies);
				else
					buildUnitInDir(here.directionTo(center), typeToBuild, allies);
				typeToBuild = null;
				return;
			}
			// else if targetDen is not null move towards it
			/*
			 * if(targetDen != null){ updateAndMoveTowardTargetDen(); }
			 */
			// if nothing else to do move toward nearest neutral/[part
			updateAndMoveTowardTargetLocation(hostiles);
		}
	}
	/*
	 * rc.setIndicatorString(2, ""); RobotInfo[] allies =
	 * rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
	 * broadcastTargetLocation(allies); if(numDensToHunt > 0 && !huntingDen &&
	 * haveEnoughFighters(allies)){ //System.out.println(densToHunt); bestIndex
	 * = Util.closestLocation(densToHunt, alpha, denArraySize); if(bestIndex >
	 * -1){ targetLocation = densToHunt[bestIndex];
	 * broadcastTargetLocation(allies); huntingDen = true; } }
	 * updateInfoFromScouts(allies); if(targetLocation != null)
	 * rc.setIndicatorString(1,"target is " + targetLocation.x + ", "+
	 * targetLocation.y); RobotInfo[] zombies =
	 * rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
	 * RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL); RobotInfo[]
	 * hostiles = Util.combineTwoRIArrays(enemies, zombies);
	 * //rc.setIndicatorString(0, "target = " + targetLocation); if (!huntingDen
	 * && rc.getRoundNum() >= roundToStopHuntingDens) {// found in Bot class //
	 * TODO: this doesn't work for some reason? int[] msg =
	 * MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y
	 * }); rc.broadcastMessageSignal(msg[0], msg[1], 10000); targetLocation =
	 * alpha; isMobileArchon = false; System.out.println( "this happened"); } if
	 * (neutrals.length > 0) { rc.activate(neutrals[0].location);
	 * if(neutrals[0].type == RobotType.ARCHON){ int[] myMsg =
	 * MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { here.x, here.y });
	 * rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2); myMsg =
	 * MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] { here.x, here.y
	 * }); rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2); } else
	 * sendNewUnitImportantData(allies); if (targetLocation != null &&
	 * neutrals[0].location.equals(targetLocation)) { targetLocation = null;
	 * huntingDen = false; } } else if (rc.isCoreReady() && inDanger(allies,
	 * enemies, zombies)){ flee(allies, enemies, zombies);
	 * rc.setIndicatorString(1, "i am in danger on round " + rc.getRoundNum());
	 * } else if(huntingDen){//behavior is very different
	 * if(rc.canSenseLocation(targetLocation)){//within range of den RobotInfo
	 * ri = rc.senseRobotAtLocation(targetLocation); if(ri == null){//den is
	 * dead targetLocation = null; densToHunt[bestIndex] = null;
	 * numDensToHunt--; huntingDen = false; } else
	 * if(here.distanceSquaredTo(targetLocation) > cautionLevel)
	 * Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(hostiles)); else
	 * if(!haveEnoughFighters(allies) && rc.isCoreReady())
	 * buildUnitInDir(directions[rand.nextInt(8)], RobotType.SOLDIER, allies);
	 * else if (isMobileScoutNeeded(allies)) { if
	 * (buildUnitInDir(directions[rand.nextInt(8)], RobotType.SCOUT, allies)) {
	 * scoutCreated = true; } } } else if(rc.isCoreReady()){ boolean built =
	 * false; if (!haveEnoughFighters(allies)){ built =
	 * buildUnitInDir(directions[rand.nextInt(8)], RobotType.SOLDIER, allies); }
	 * if(!built && rc.hasBuildRequirements(RobotType.SOLDIER))
	 * Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(hostiles)); } }
	 * else { if (targetLocation != null && targetLocation.equals(here)){
	 * targetLocation = null; huntingDen = false; } if (rc.isCoreReady()) { if
	 * (isMobileScoutNeeded(allies)) { if
	 * (buildUnitInDir(directions[rand.nextInt(8)], RobotType.SCOUT, allies)) {
	 * scoutCreated = true; } } if (targetLocation == null) {// no known things
	 * worth pursuing if (!haveEnoughFighters(allies))
	 * buildUnitInDir(directions[rand.nextInt(8)], RobotType.SOLDIER, allies);
	 * else{ if(updateTargetLocationMySelf(allies) && rc.isCoreReady())
	 * Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(hostiles)); else
	 * if(rc.isCoreReady()) explore(allies); } } else { boolean built = false;
	 * if (!haveEnoughFighters(allies)){ built =
	 * buildUnitInDir(directions[rand.nextInt(8)], RobotType.SOLDIER, allies); }
	 * if(!built && rc.hasBuildRequirements(RobotType.SOLDIER))
	 * Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(hostiles)); }
	 * 
	 * } else rc.setIndicatorString(2, "I did nothing this turn"); }
	 */

	private static void determineTypeToBuild() {
		int fate = rand.nextInt(1000);
		if(fate % 10 == 0)
			typeToBuild = RobotType.VIPER;
		else
			typeToBuild = RobotType.SOLDIER;
	}

	private static void callForHelp() throws GameActionException {
		rc.broadcastSignal((int) (RobotType.ARCHON.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
		rc.setIndicatorString(1, "help me pls on round " + rc.getRoundNum());
	}

	private static void updateAndMoveTowardTargetLocation(RobotInfo[] hostiles) throws GameActionException {
		// TODO moves toward closest safe parts or neutral
		if (targetLocation != null && targetLocation.equals(here))
			targetLocation = null;
		/*
		if (targetLocation == null || !Combat.isSafe(targetLocation)) {
			updateTargetLocationMySelf(hostiles);
		}
		*/
		updateTargetLocationMySelf(hostiles);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostiles));
		if (targetLocation != null)
			Nav.goTo(targetLocation, theSafety);
		else
			Nav.explore();
	}
	/*
	 * private static void updateAndMoveTowardTargetDen() { // TODO makes sure
	 * the den it's heading towards still exists and if it doesn't change it
	 * 
	 * }
	 */

	private static boolean activateNeutralIfPossible(RobotInfo[] allies) throws GameActionException {
		RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		if (neutrals.length > 0) {
			rc.activate(neutrals[0].location);
			sendNewUnitImportantData(allies);
			if (targetLocation != null && neutrals[0].location.equals(targetLocation)) {
				targetLocation = null;
			}
			return true;

		}
		else if(targetLocation != null && rc.canSense(targetLocation) && (rc.senseRobotAtLocation(targetLocation) == null || rc.senseRobotAtLocation(targetLocation).team != Team.NEUTRAL)){
			rc.setIndicatorString(2,"hi");
			targetLocation = null;
		}
		return false;
	}

	private static boolean createScoutIfNecessary(RobotInfo[] allies) throws GameActionException {
		boolean built = false;
		if (!scoutCreated) {
			built = buildUnitInDir(directions[rand.nextInt(8)], RobotType.SCOUT, allies);
		}
		if (built) {
			scoutCreated = true;
		}
		return built;
	}

	private static boolean signalsFromOurTeam(Signal[] signals) {// NEW move to
																	// util
		if (signals.length == 0) {
			return false;
		} else {
			for (Signal sig : signals) {
				if (sig.getTeam() == us)
					return true;
			}
		}
		return false;
	}

	private static boolean isSurrounded() throws GameActionException {// NEW
																		// move
																		// to
																		// Util
		Direction dir = Direction.NORTH;
		Boolean surrounded = true;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)) {
				surrounded = false;
				break;
			}
			dir = dir.rotateLeft();
		}
		return surrounded;
	}

	private static void checkNeededUnits(RobotInfo[] ourUnits) {
		// We need to pick unit ratios
		// Then produce more of whatever is needed most to achieve that ratio
	}

	private static void chooseMoveLocAndDir(MapLocation loc) {
		// If enemies are near retreat
		// return opposite dir of nearest enemy
		// If scrap is near take it
		// If scrap && enemies aren't near move towards nearest scrap
		// return dir of nearest scrap
	}

	private static void repairBotMostInNeed() throws GameActionException {// New
																			// Move
																			// to
																			// util
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, us);
		if (allies.length > 0) {
			RobotInfo mostInNeed = Util.leastHealth(allies, 1);
			if (mostInNeed != null) {
				// rc.setIndicatorString(0, "Repairing" +
				// mostInNeed.location.toString());
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void updateInfoFromScouts(RobotInfo[] enemies) throws GameActionException { 
		Signal[] signals = rc.emptySignalQueue();
		updateTurretList(signals);
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderLoc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DIRECT_MOBILE_ARCHON) {
						int[] data = purpose.decode(senderLoc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)
								&& !Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
							targetDens[targetDenSize] = denLoc;
							targetDenSize++;
							numDensToHunt++;

							if (targetDen == null || here.distanceSquaredTo(denLoc) < here.distanceSquaredTo(targetDen)) {
								targetDen = denLoc;
								bestIndex = targetDenSize;
							}
						}
						/*
						 * int[] data = purpose.decode(senderloc, message);
						 * if(rc.getRoundNum() < roundToStopHuntingDens &&
						 * numDensToHunt > 0){ densToHunt[denArraySize] = new
						 * MapLocation(data[0],data[1]); numDensToHunt++;
						 * denArraySize++; } else{ if(rc.getRoundNum() <
						 * roundToStopHuntingDens){ huntingDen = true;
						 * numDensToHunt++; targetLocation = new
						 * MapLocation(data[0],data[1]);
						 * broadcastTargetLocation(allies); } else
						 * if(!huntingDen){ targetLocation = new
						 * MapLocation(data[0],data[1]);
						 * broadcastTargetLocation(allies); } } } else if
						 * (purpose == MessageEncode.STOP_BEING_MOBILE){ int[]
						 * data = purpose.decode(senderloc, message); alpha =
						 * new MapLocation(data[0],data[1]); isMobileArchon =
						 * false;
						 */
					} else if (purpose == MessageEncode.PART_OR_NEUTRAL_NOTIF) {
						int[] data = purpose.decode(senderLoc, message);
						MapLocation targetLoc = new MapLocation(data[0], data[1]);
						if (targetLocation == null) {
							targetLocation = targetLoc;
						}
					}
				} else {
					MapLocation signalLoc = signal.getLocation();
					int distToSignal = here.distanceSquaredTo(signalLoc);
					int closestIndex = Util.closestLocation(targetDens, signalLoc, targetDenSize);
					if (closestIndex != -1 && targetDens[closestIndex].distanceSquaredTo(signalLoc) <= RobotType.SOLDIER.sensorRadiusSquared) {
						rc.setIndicatorString(1, "not gonig for den at loc " + targetDens[closestIndex] + " on round " + rc.getRoundNum());
						killedDens[killedDenSize] = targetDens[closestIndex];
						killedDenSize++;
						targetDens[closestIndex] = null;
						numDensToHunt--;
						targetDen = null;
						if (numDensToHunt > 0) {
							bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
							targetDen = targetDens[bestIndex];
						}
					}
				}
			}
		}
	}

	private static void broadcastTargetDen(RobotInfo[] allies) throws GameActionException { // New
																							// INTO
																							// MESSAGE
																							// ENCODE
		/*if (!haveEnoughFighters(allies))
			return;*/
		int[] msg = MessageEncode.DIRECT_MOBILE_ARCHON.encode(new int[] { targetDen.x, targetDen.y });
		rc.broadcastMessageSignal(msg[0], msg[1], (int) (RobotType.ARCHON.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
	}

	// private static MapLocation chooseNextTarget(RobotInfo[] allies,
	// RobotInfo[] zombies){
	// // TODO: may be keep track of more possible targets, also may want to
	// switch priorities
	// MapLocation closest = partsLoc;
	// if(denLoc != null && (closest == null || here.distanceSquaredTo(denLoc) <
	// here.distanceSquaredTo(closest)))
	// closest = denLoc;
	// if(neutralLoc != null && (closest == null ||
	// here.distanceSquaredTo(neutralLoc) < here.distanceSquaredTo(closest)))
	// closest = neutralLoc;
	// if(closest == denLoc){
	// //actually want to stay kinda far away from denLoc
	// if (here.distanceSquaredTo(denLoc) < cautionLevel)
	// closest = null;
	// }
	// return closest;
	// }

	private static boolean updateTargetLocationMySelf(RobotInfo[] allies) throws GameActionException { // NEW
																										// Harass???
		RobotInfo[] neutrals = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.NEUTRAL);
		MapLocation[] partLocations = rc.sensePartLocations(-1);// gets all the
																// ones we can
																// sense
		MapLocation closestLoc = null;
		int smallestDistance = 1000000;
		for (MapLocation loc : partLocations) {
			int distanceToLoc = here.distanceSquaredTo(loc);
			if (distanceToLoc < smallestDistance && Combat.isSafe(loc) && !rc.isLocationOccupied(loc)) {
				closestLoc = loc;
				smallestDistance = distanceToLoc;
			}
		}
		for (RobotInfo ri : neutrals) {
			int distanceToLoc = here.distanceSquaredTo(ri.location);
			if (distanceToLoc < smallestDistance && Combat.isSafe(ri.location)) {
				closestLoc = ri.location;
				smallestDistance = distanceToLoc;
			}
		}
		if (closestLoc != null) {
			targetLocation = closestLoc;
			/*
			 * broadcastTargetLocation(allies); huntingDen = false;
			 */
			return true;
		}
		return false;
	}

	private static boolean haveEnoughFighters(RobotInfo[] allies) {// NEW COMBAT
		int fighters = 0;
		for (RobotInfo a : allies)
			if (a.type == RobotType.GUARD || a.type == RobotType.SOLDIER)
				fighters++;
		return fighters >= 7;
	}

	// private static boolean inDanger(RobotInfo[] allies, RobotInfo[] enemies,
	// RobotInfo[] zombies) {// NEW
	// // Combat
	// if (enemies.length > 0 || zombies.length > allies.length + 2
	// || !(zombies.length == 1 && zombies[0].type == RobotType.ZOMBIEDEN) &&
	// (zombies.length > 0
	// && here.distanceSquaredTo(Util.closest(zombies, here).location) <
	// cautionLevel))
	// return true;
	// return false;
	// }

	private static void constructNeededUnits(RobotType neededUnit, RobotInfo[] allies) throws GameActionException {
		// Check for sufficient parts
		if (rc.hasBuildRequirements(neededUnit)) {
			// Choose a random direction to try to build in
			Direction dirToBuild = directions[rand.nextInt(8)];
			buildUnitInDir(dirToBuild, neededUnit, allies);
			if(rc.isCoreReady()){//failed... try to clear rubble
				for(int i : directionOrder)
					if (Util.checkRubbleAndClear(dirToBuild)) break;
			}
		}
		if (isAlphaArchon && isSurrounded() && rc.getRoundNum() % 10 == 0) {
			maxRange++;
			int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[] { maxRange });
			rc.broadcastMessageSignal(message[0], message[1], (maxRange + 1) * (maxRange + 1));
		}
	}

	private static boolean buildUnitInDir(Direction dir, RobotType r, RobotInfo[] allies) throws GameActionException {// New Util
		for (int i : directionOrder) {
			if (rc.canBuild(Direction.values()[(dir.ordinal()+i+8)%8], r) && rc.isCoreReady()) {
				rc.build(Direction.values()[(dir.ordinal()+i+8)%8], r);
				sendNewUnitImportantData(allies);
				return true;
			}
		}
		return false;
	}

	private static void sendNewUnitImportantData(RobotInfo[] allies) throws GameActionException {// New																			// Util
		int[] myMsg;
		
		if(alpha != null){
			myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
		}
		
		if (targetDen != null)
			broadcastTargetDen(allies);
		
		// now notify them of turrets
		int[] turretLocs = {here.x, here.y,here.x, here.y,here.x, here.y};
		MapLocation t;
		for(int i = 0; i < turretSize; i++){
			t = enemyTurrets[i].location;
			turretLocs[(i%3)*2] = t.x;
			turretLocs[(i%3)*2+1] = t.y;
			if(i % 3 == 2){
				//send
				myMsg = MessageEncode.RELAY_TURRET_INFO.encode(turretLocs);
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
				turretLocs = new int[]{here.x, here.y,here.x, here.y,here.x, here.y};
			}
		}
		//send
		myMsg = MessageEncode.RELAY_TURRET_INFO.encode(turretLocs);
		rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
	}

	private static boolean checkRubbleAndClear(Direction dir) throws GameActionException {

		if (rc.senseRubble(rc.getLocation().add(dir)) > 0) {
			rc.clearRubble(dir);
			return true;
		}
		return false;
	}

	private static void aarons_shitty_strat() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		RobotType needed = RobotType.TURRET;
		if (isScoutNeeded()) {
			needed = RobotType.SCOUT;
		}
		constructNeededUnits(needed, allies);

		if (rc.isCoreReady()) {
			Direction dir = Direction.NORTH;
			for (int i = 0; i < 8; i++) {
				if (checkRubbleAndClear(dir)) {
					break;
				}
				dir = dir.rotateRight();
			}
		}

	}

	/*
	 * private static boolean isMobileScoutNeeded(RobotInfo[] teammates) throws
	 * GameActionException{ if(!scoutCreated){ return true; }
	 * if(rc.getRoundNum() + 50 < roundToStopHuntingDens){ for (int i = 0; i <
	 * teammates.length; i++) { if (teammates[i].type == RobotType.SCOUT) return
	 * false; } return true; } return false; }
	 */
	private static boolean isScoutNeeded() {
		RobotInfo[] teammates = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		int nearbyScouts = 0;
		int nearbyTurrets = 0;
		for (int i = 0; i < teammates.length; i++) {
			if (teammates[i].type == RobotType.SCOUT) {
				nearbyScouts++;
			} else if (teammates[i].type == RobotType.TURRET) {
				nearbyTurrets++;
			}
		}
		if (nearbyScouts < (int) teammates.length / 7.0 && nearbyTurrets >= 4) {
			return true;
		}
		return false;
	}
}
