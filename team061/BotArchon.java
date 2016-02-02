package team061;

import battlecode.common.*;

public class BotArchon extends Bot {
	static MapLocation alpha, hunter;//dont use currently
	static MapLocation runAwayFromThisLoc;
	static int runAwayRound;
	static boolean isAlphaArchon;//dont use currently
	static boolean isMobileArchon;
	static int maxRange;//dont use currently
	static int numScoutsCreated = 0;
	static int numVipersCreated = 0;
	static int numSoldiersCreated = 0;
	static int numGuardsCreated = 0;
	static int lastEnemyRound;
	static boolean targetIsNeutral;//false if chasing neutral
	static RobotType typeToBuild;
	static int lastSeenHostile;
	static MapLocation lastEnemyLoc;
	// static int numTurretsCreated = 0;

	// -----mobile archon fields here-----
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
	static boolean scoutCreated;//dont use currently

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
//		//test messageencode stuff
//		int b = Clock.getBytecodeNum();
//		MessageEncode.TURRET_TARGET.decode(here, 
//				MessageEncode.TURRET_TARGET.encode(new int[]{149, 6, here.x+5, here.y+5}));
//		MessageEncode.WARN_ABOUT_TURRETS.decode(here, 
//				MessageEncode.WARN_ABOUT_TURRETS.encode(new int[]{here.x+5, here.y+5,here.x+5, here.y+5,here.x+5, here.y+5,here.x+5, here.y+5,here.x+5, here.y+5,}));
//		System.out.println(Clock.getBytecodeNum()-b);
		// maxRange = 2;
		// Signal[] signals = rc.emptySignalQueue();
		isMobileArchon = true;
		targetDen = null;
		typeToBuild = null;
		lastSeenHostile = -100;
		lastEnemyRound = -100;
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
		//System.out.println(type.attackPower);
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		//String s = "";
		//for(int i = 0; i < turretSize; i++){
		//	s += "[" + enemyTurrets[i].location.x + ", " + enemyTurrets[i].location.y +"], "; 
		//}
		repairBotMostInNeed();
		RobotInfo[] enemies = rc.senseNearbyRobots(type.sensorRadiusSquared, them);
		if (rc.isCoreReady()) {
			if (isMobileArchon) {
				beMobileArchon(enemies);
			} else if (isAlphaArchon || here.distanceSquaredTo(alpha) <= 2) {
				aarons_shitty_strat();
			} else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(alpha, theSafety);
			}
		}
	}

	private static void beMobileArchon(RobotInfo[] enemies) throws GameActionException {
		RobotInfo[] hostiles = rc.senseHostileRobots(here, RobotType.ARCHON.sensorRadiusSquared);
		RobotInfo[] allies = rc.senseNearbyRobots(type.sensorRadiusSquared, us);
		updateInfoFromScouts(hostiles);
		hostiles = Util.removeHarmlessUnits(hostiles);
		//rc.setIndicatorString(0, "targetIsNeutral " + targetIsNeutral);
		//rc.setIndicatorString(1,"target loc is " + targetLocation);
		if (rc.isCoreReady()) {
			if (hostiles.length > 0){
				//rc.setIndicatorDot(here, 255, 0, 0);
				Nav.flee(hostiles,allies);
				lastSeenHostile = rc.getRoundNum();
			}
			else if(rc.getRoundNum() > 500 && numDensToHunt == 0 && rc.getRoundNum() - lastSeenHostile > 25 && rc.getRoundNum() % 50 == 0){
				int[] msg = MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[]{here.x, here.y});
				rc.broadcastMessageSignal(msg[0], msg[1], 2000);
			}
			//RobotInfo[] zombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
			// if i can see enemies run away
			// if(inDanger(allies, enemies, zombies)){
			//	if (rc.getRoundNum() % 5 == 0)// && allies.length <
			// hostiles.length){
			//	callForHelp();
			if(!rc.isCoreReady())
				return;
			// if i haven't created a scout create one
			/*
			if (rc.hasBuildRequirements(RobotType.SCOUT) && createScoutIfNecessary(allies)) // isn't running away more
				// important? meh can fix later
				// if necessary
				return;
*/
			// else if i can activate a neutral do it
			if (activateNeutralIfPossible(allies)) {
				return;
			}
			if(typeToBuild == null)
				determineTypeToBuild(hostiles, allies);
			//rc.setIndicatorString(0,"numSoldiersCreated = " + numSoldiersCreated);
			//rc.setIndicatorString(1,"numScoutsCreated = " + numScoutsCreated);
			//rc.setIndicatorString(2,"numVipersCreated = " + numVipersCreated);
			updateTargetLocationMySelf(allies);
			if (rc.hasBuildRequirements(typeToBuild) && (!targetIsNeutral || here.distanceSquaredTo(targetLocation) > 16)) {
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

	private static void determineTypeToBuild(RobotInfo[] hostiles, RobotInfo[] allies) {
		if(!haveEnoughFighters(allies) && (rc.getRoundNum() - lastSeenHostile < 15 || (numGuardsCreated + 1) * 7 < numSoldiersCreated)){
			typeToBuild = RobotType.SOLDIER;
			return;
		}
		if(numScoutsCreated * 8 <= numSoldiersCreated)
			typeToBuild = RobotType.SCOUT;
		else if((numVipersCreated) * (MapAnalysis.mapDifficulty == 0 ?7:10)  <= numSoldiersCreated )//optimize with MapAnalysis and Team Memory
			typeToBuild = RobotType.VIPER;
		else
			typeToBuild = RobotType.SOLDIER;
	}

	private static void callForHelp() throws GameActionException {
		rc.broadcastSignal((int) (RobotType.ARCHON.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
		//rc.setIndicatorString(1, "help me pls on round " + rc.getRoundNum());
	}

	private static void updateAndMoveTowardTargetLocation(RobotInfo[] hostiles) throws GameActionException {
		// TODO moves toward closest safe parts or neutral
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostiles));
		if(runAwayFromThisLoc != null){
			if(rc.getRoundNum() - runAwayRound > 125)
				runAwayFromThisLoc = null;
			else{
				//MapLocation goToThisLoc = here.add(runAwayFromThisLoc.directionTo(here), 5);
				Nav.goAwayFrom(runAwayFromThisLoc, theSafety);
				return;
			}
		}
		if (targetLocation != null && (targetLocation.equals(here) || !Combat.isSafe(targetLocation))){
			targetLocation = null;
			targetIsNeutral = false;
		}
		/*
		if (targetLocation == null || !Combat.isSafe(targetLocation)) {
			updateTargetLocationMySelf(hostiles);
		}
		*/
			
		if (targetLocation != null)
			Nav.goTo(targetLocation, theSafety);
		else{
			int bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
			if(bestIndex != -1)
				Nav.goTo(targetDens[bestIndex], theSafety);
		}
		//Nav.explore();
	}

	private static boolean activateNeutralIfPossible(RobotInfo[] allies) throws GameActionException {
		RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		if (neutrals.length > 0) {
			rc.activate(neutrals[0].location);
			sendNewUnitImportantData(allies);
			if (targetLocation != null && neutrals[0].location.equals(targetLocation)) {
				targetIsNeutral = false;
				targetLocation = null;
			}
			return true;
		}
		if(targetIsNeutral && targetLocation != null && rc.canSense(targetLocation) && (rc.senseRobotAtLocation(targetLocation) == null || rc.senseRobotAtLocation(targetLocation).team != Team.NEUTRAL)){
			targetLocation = null;
			targetIsNeutral = false;
		}
		return false;
	}

	private static void chooseMoveLocAndDir(MapLocation loc) {
		// If enemies are near retreat
		// return opposite dir of nearest enemy
		// If scrap is near take it
		// If scrap && enemies aren't near move towards nearest scrap
		// return dir of nearest scrap
	}

	private static void repairBotMostInNeed() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, us);
		if (allies.length > 0) {
			RobotInfo mostInNeed = Util.leastHealth(allies, 1);
			if (mostInNeed != null) {
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void updateInfoFromScouts(RobotInfo[] enemies) throws GameActionException { 
		Signal[] signals = rc.emptySignalQueue();
		updateTurretList(signals, enemies);
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderLoc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DEN_NOTIF) {
						int[] data = purpose.decode(senderLoc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if(data[2] == 1){
							if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)
									&& !Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
								targetDens[targetDenSize] = denLoc;
								targetDenSize++;
								numDensToHunt++;
							}
						} else {
							if(!Util.containsMapLocation(killedDens, denLoc, targetDenSize)){
								killedDens[killedDenSize] = targetDens[bestIndex];
								killedDenSize++;
							}
							if(Util.containsMapLocation(targetDens, denLoc, targetDenSize)){
								targetDens[bestIndex] = null;
								numDensToHunt--;
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
						if (targetLocation == null || data[2] == 1 || here.distanceSquaredTo(targetLocation) > here.distanceSquaredTo(targetLoc)) {
							targetLocation = targetLoc;
							targetIsNeutral = false;
						}
					}
					else if (purpose == MessageEncode.CRUNCH_TIME){
						int[] data = purpose.decode(senderLoc, message);
						if(data[2] > 3){
							runAwayFromThisLoc = new MapLocation(data[0], data[1]);
							//rc.setIndicatorString(0, "running away from " + runAwayFromThisLoc);
							runAwayRound = rc.getRoundNum();
						}
					}
					else if (purpose == MessageEncode.ENEMY_ARMY_NOTIF){
						int[] data = purpose.decode(senderLoc, message);
						MapLocation enemyLoc = new MapLocation(data[0], data[1]);
						if(lastEnemyLoc == null || here.distanceSquaredTo(enemyLoc) < here.distanceSquaredTo(lastEnemyLoc) * 1.5 || rc.getRoundNum() - lastEnemyRound > 50){
							lastEnemyRound = rc.getRoundNum();
							lastEnemyLoc = enemyLoc;
						}
					}
				} else {
					MapLocation signalLoc = signal.getLocation();
					int distToSignal = here.distanceSquaredTo(signalLoc);
					int closestIndex = Util.closestLocation(targetDens, signalLoc, targetDenSize);
					if (closestIndex != -1 && targetDens[closestIndex].distanceSquaredTo(signalLoc) <= RobotType.SOLDIER.sensorRadiusSquared) {
						//rc.setIndicatorString(1, "not gonig for den at loc " + targetDens[closestIndex] + " on round " + rc.getRoundNum());
						killedDens[killedDenSize] = targetDens[closestIndex];
						killedDenSize++;
						targetDens[closestIndex] = null;
						numDensToHunt--;
					}
				}
			}
		}
	}
	
	public static boolean updateTurretList(Signal[] signals, RobotInfo[] enemies) throws GameActionException {
		boolean updated = Bot.updateTurretList(signals);
		for (int i = 0; i < turretSize; i++) {
			MapLocation t = enemyTurrets[i].location;
			if (rc.canSenseLocation(t)) {
				RobotInfo bot = rc.senseRobotAtLocation(t);
				if (bot == null || bot.type != RobotType.TURRET) {
					removeLocFromTurretArray(t);
					int[] myMsg = MessageEncode.ENEMY_TURRET_DEATH.encode(
							new int[] { t.x, t.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], (int)(type.sensorRadiusSquared*GameConstants.BROADCAST_RANGE_MULTIPLIER));
					i--;
					updated = true;
				}
			}
		}
		for (RobotInfo e : enemies)
			if (e.type == RobotType.TURRET) {
				if (!isLocationInTurretArray(e.location)) {
					enemyTurrets[turretSize] = e;
					turretSize++;
					int[] myMsg = MessageEncode.WARN_ABOUT_TURRETS.encode(new int[] { e.location.x, e.location.y});
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], (int)(type.sensorRadiusSquared*GameConstants.BROADCAST_RANGE_MULTIPLIER));
					updated = true;
				}
			}
		return updated;
	}

	private static void broadcastTargetDen(RobotInfo[] allies) throws GameActionException { // New INTO MESSAGE ENCODE
		/*if (!haveEnoughFighters(allies))
			return;*/
		for(MapLocation den: targetDens){
			if(den == null)
				continue;
			if(rc.getMessageSignalCount() == 19)
				break;
			int[] msg = MessageEncode.DEN_NOTIF.encode(new int[] { den.x, den.y , 1});
			rc.broadcastMessageSignal(msg[0], msg[1], 2);
		}
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
		for (RobotInfo ri : neutrals) {
			int distanceToLoc = here.distanceSquaredTo(ri.location);
			if (distanceToLoc < smallestDistance && Combat.isSafe(ri.location) && Util.rubbleBetweenHereAndThere(here, ri.location) < 400) {
				closestLoc = ri.location;
				smallestDistance = distanceToLoc;
				targetIsNeutral = true;
			}
		}
		if(closestLoc == null){
			for (MapLocation loc : partLocations) {
				int distanceToLoc = here.distanceSquaredTo(loc);
				if (distanceToLoc < smallestDistance && Combat.isSafe(loc) && !rc.isLocationOccupied(loc) && Util.rubbleBetweenHereAndThere(here, loc) < rc.senseParts(loc)*10) {
					closestLoc = loc;
					smallestDistance = distanceToLoc;
					targetIsNeutral = false;
				}
			}
		}
		if (closestLoc != null) {
			targetLocation = closestLoc;
			return true;
		}
		return false;
	}

	private static boolean haveEnoughFighters(RobotInfo[] allies) {// NEW COMBAT
		int fighters = 0;
		for (RobotInfo a : allies)
			if (a.type == RobotType.GUARD || a.type == RobotType.SOLDIER)
				fighters++;
		return fighters >= 5;
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
					Util.checkRubbleAndClear(dirToBuild,true);
			}
		}
//		if (isAlphaArchon && isSurrounded() && rc.getRoundNum() % 10 == 0) {
//			maxRange++;
//			int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[] { maxRange });
//			rc.broadcastMessageSignal(message[0], message[1], (maxRange + 1) * (maxRange + 1));
//		}
	}

	private static boolean buildUnitInDir(Direction dir, RobotType r, RobotInfo[] allies) throws GameActionException {// New Util
		for (int i : directionOrder) {
			if (rc.canBuild(Direction.values()[(dir.ordinal()+i+8)%8], r) && rc.isCoreReady()) {
				rc.build(Direction.values()[(dir.ordinal()+i+8)%8], r);
				sendNewUnitImportantData(allies);
				incrementTypeCount(r, allies);
				typeToBuild = null;
				return true;
			}
		}
		return false;
	}

	private static void incrementTypeCount(RobotType r, RobotInfo[] allies) {
		switch (r){
		case SCOUT:
			numScoutsCreated++;
			break;
		case SOLDIER:
			if(!haveEnoughFighters(allies) && (rc.getRoundNum() - lastSeenHostile < 15 || (numGuardsCreated + 1) * 7 < numSoldiersCreated))
				numGuardsCreated++;
			else
				numSoldiersCreated++;
			break;
		case VIPER:
			numVipersCreated++;
			break;
		default:
			break;
		}
	}

	private static void sendNewUnitImportantData(RobotInfo[] allies) throws GameActionException {// New																			// Util
		int[] myMsg;
		//if(alpha != null){
		//	myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
		//	rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
		//}
		if(!haveEnoughFighters(allies) && (rc.getRoundNum() - lastSeenHostile < 15 || (numGuardsCreated + 1) * 7 < numSoldiersCreated))
			notifySoldierTheyShouldGuard();
		else if (numDensToHunt > 0)
			broadcastTargetDen(allies);
		else if (lastEnemyLoc != null){
			myMsg = MessageEncode.ENEMY_ARMY_NOTIF.encode(new int[] { lastEnemyLoc.x, lastEnemyLoc.y, 0 });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 0);
		}
		// now notify them of turrets
		int[] turretLocs = {here.x, here.y,here.x, here.y,here.x, here.y};
		MapLocation t;
		for(int i = 0; i < turretSize; i++){
			if(rc.getMessageSignalCount() == 19)
				break;
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
		//send the extras
		myMsg = MessageEncode.RELAY_TURRET_INFO.encode(turretLocs);
		rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
	}

	private static void notifySoldierTheyShouldGuard() throws GameActionException{
		int[] msg = MessageEncode.BE_MY_GUARD.encode(new int[]{});
		rc.broadcastMessageSignal(msg[0], msg[1], 2);
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
				if (Util.checkRubbleAndClear(dir,true)) {
					break;
				}
				dir = dir.rotateRight();
			}
		}
	}

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
