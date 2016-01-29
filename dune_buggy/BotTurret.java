package dune_buggy;

import battlecode.common.*;

public class BotTurret extends Bot {
	//protected static MapLocation alpha;
	//static MapLocation[] lastScoutNotifiedArray;
	//static int currentIndexOfLastArray = 0;
	//static int lastTimeTargetChanged;
	//protected static int range; // NEW not necessary for mobile
	//protected static int turretType; // NEW 0 = turtling; 1 = offensive; 2 = map control?
	protected static boolean isTTM;

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
		// TODO have bot choose what type of turret it is
		// if it is a mobile turret it needs to have a target loc
		//chooseTurretType();
		isTTM = false;
		// MessageEncode.getMobileArchonLocation(); //NEW This should be a
		// method
//		if (turretType == 0) { // WE ARE TURTLING
//			range = 2;
//			Signal[] signals = rc.emptySignalQueue();
//			for (int i = 0; i < signals.length; i++) {
//				if (signals[i].getTeam() == them) {
//					continue;
//				}
//				int[] message = signals[i].getMessage();
//				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
//				if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
//					int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(),
//							message);
//					alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
//					break;
//				}
//			}
//		}
		//if (turretType == 1) { // OFFENSIVE
			// MessageEncode.readMessagesAndUpdateInfo();
			targetLoc = null; // for now
			//boolean canSeeHostiles = rc.senseHostileRobots(here, type.sensorRadiusSquared).length > 0;
			//Harass.updateTargetLocWithoutSignals();
			// TODO: UPDATE THINGS FROM SIGNALS (see the big Harass.updateInfoFromSignals method)
			// Harass.updateTargetLoc();
			// this should set its target
//		}
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] enemies = rc.senseHostileRobots(here, type.sensorRadiusSquared);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
		Signal[] signals = rc.emptySignalQueue();
		Harass.updateInfoFromSignals(signals);
		//Harass.updateTargetLocWithoutSignals();
		// TODO: UPDATE THINGS FROM SIGNALS (see the big Harass.updateInfoFromSignals method)
//		for(Signal s: signals){
//			Harass.updateTargetLoc(s, canSeeHostiles);
//		}
		//rc.setIndicatorString(1, "target at " + targetLoc.x + ", " + targetLoc.y);

		// MessageEncode.updateRange(); //NEW update the range and get list of
		// possible targets in same loop to conserve bytecode

//		if (turretType == 1 && targetLoc != null) {
		if(targetLoc == null){
			Harass.updateTargetLocWithoutSignals();
			System.out.println("just updated");
		}
		if (!isTTM) {
			// shoot anything in range and use scout
			//attackIfApplicable(signals);
			if(targetLoc != null){
				if(rc.isWeaponReady() && type.attackRadiusSquared >= here.distanceSquaredTo(targetLoc))
					Combat.turretAttack(enemies, targetLoc);
				else if (enemies.length == 0 && type.attackRadiusSquared < here.distanceSquaredTo(targetLoc)) {
					rc.pack();
					isTTM = true;
					System.out.println("pack");
				}
			} else {
				rc.pack();
				isTTM = true;
			}
		} else {
			if (rc.isCoreReady()) {
				if (targetLoc != null && RobotType.TURRET.attackRadiusSquared >= here.distanceSquaredTo(targetLoc)) {
					rc.unpack();
					isTTM = false;
					System.out.println("unpack");
				} else if(targetLoc != null) {
					rc.setIndicatorString(2, "moving my butt");
					Nav.goTo(targetLoc, theSafety);
				} else
					Nav.goTo(center, theSafety);
			}
		}
		String dens = "";
		if(targetDenSize > 0){
			for(int i = 0; i < targetDenSize; i++){
				MapLocation den = targetDens[i];
				if(den != null)
					dens += den.toString() + ", ";
			}
		}
		rc.setIndicatorString(0, dens);
		if(targetLoc != null)
			rc.setIndicatorString(1, "targetLoc = " + targetLoc.toString());
	}
}
