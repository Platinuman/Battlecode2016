package team061;

import battlecode.common.*;

public class Harass extends Bot {
	// NEW read up bot types for what they call in Harass.
	// Implement those and the rest of these methods are helper methods for the
	// big ones.
	// Once again Optimization.
	static MapLocation turretLoc, targetLoc, archonLoc;
	static boolean targetUpdated, archonUpdated, huntingDen, crunching, wantToMove;
	static int archonUpdate;
	static boolean swarmingArchon;
	static boolean isGuard;

	private static boolean canWin1v1(RobotInfo enemy) {
		if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.ZOMBIEDEN)
			return true;
		// TODO: check viper infection statuses, also if you are a viper
		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / type.attackPower);
		int turnsTillWeCanAttack;
		int effectiveAttackDelay;
		turnsTillWeCanAttack = (int) rc.getWeaponDelay();
		effectiveAttackDelay = (int) type.attackDelay;
		int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

		int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
		int turnsTillEnemyCanAttack;
		int effectiveEnemyAttackDelay;
		turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
		effectiveEnemyAttackDelay = (int) enemy.type.attackDelay;
		int turnsForEnemyToKillUs = turnsTillEnemyCanAttack
				+ effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

		return turnsToKillEnemy <= turnsForEnemyToKillUs;
	}

	public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
		// TODO:!!! take range difference into account! soldiers can kite basically everything
		if (enemy.type == RobotType.ARCHON ||enemy.type == RobotType.ZOMBIEDEN)
			return true;
		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / type.attackPower);
		int turnsTillWeCanAttack;
		int effectiveAttackDelay;

		double weaponDelayAfterMoving = rc.getWeaponDelay() - 1.0;
		turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
		effectiveAttackDelay = (int) type.attackDelay;

		int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

		int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
		int turnsTillEnemyCanAttack;
		int effectiveEnemyAttackDelay;
		turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
		effectiveEnemyAttackDelay = (int) enemy.type.attackDelay;

		int turnsForEnemyToKillUs = turnsTillEnemyCanAttack
				+ effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

		return turnsToKillEnemy <= turnsForEnemyToKillUs;
	}

	public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] enemies) {

		RobotInfo loneAttacker = null;
		int numAttackers = 0;
		for (RobotInfo enemy : enemies) {
			switch (enemy.type) {
			case ARCHON:
				break;
			default:
				if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
					numAttackers++;
					if (numAttackers > 1)
						return false;
					loneAttacker = enemy;
				}
				break;
			}
		}

		if (numAttackers == 0)
			return true;

		return Harass.canWin1v1AfterMovingTo(loc, loneAttacker);
	}

	private static boolean tryToRetreat(RobotInfo[] enemies) throws GameActionException {
		wantToMove = false;
		int bytecodechecker = Clock.getBytecodeNum();
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(enemies, here);

		double bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
		for (Direction dir : Direction.values()) {
			if (!rc.canMove(dir))
				continue;

			MapLocation retreatLoc = here.add(dir);
			if(Util.isInRangeOfTurrets(retreatLoc)){
				continue;
			}
			RobotInfo closestEnemy = Util.closest(enemies, retreatLoc); // TODO: put this back in, maybe only if there aren't tons of enemies?
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			double rubble = rc.senseRubble(retreatLoc);
			double rubbleMod = rubble<GameConstants.RUBBLE_SLOW_THRESH?0:rubble*2.5/GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			if (distSq-rubbleMod > bestDistSq) {
				bestDistSq = distSq-rubbleMod;
				bestRetreatDir = dir;
			}
		}

		if (bestRetreatDir != null && rc.isCoreReady()&&rc.canMove(bestRetreatDir)) {
			rc.move(bestRetreatDir);
			//rc.setIndicatorString(2, "retreating cost us " + (Clock.getBytecodeNum() - bytecodechecker));
			return true;
		}
		//rc.setIndicatorString(2, "trying to retreat and failing cost us " + (Clock.getBytecodeNum() - bytecodechecker));
		return false;
	}

	// if we are getting owned, and we have core delay, try to retreat
	// if we can hit an enemy, attack if our weapon delay is up. otherwise sit still
	// try to stick to enemy harassers, engaging them if we can win the 1v1
	// try to move toward undefended workers, engaging them if we can win the 1v1

	// here's a better micro:
	// if we are getting hit:
	// - if we are getting owned and have core delay and can retreat, do so
	// - otherwise hit an enemy if we can
	// if we are not getting hit:
	// - if we can assist an ally who is engaged, do so
	// - if we can move to engage a worker, do so
	// - if there is an enemy harasser nearby, stick to them
	// - - optionally, engage if we can win the 1v1 or if there is a lot of allied support

	// it's definitely good to take 1v1s if there are no nearby enemies. however
	// we maybe
	// should avoid initiating 1v1s if there are enemies nearby that can
	// support.

	private static boolean doMicro(RobotInfo[] enemiesInSight, RobotInfo[] hostilesICanSee, RobotInfo[] enemiesICanShoot, RobotInfo[] allies, RobotInfo[] enemiesWithoutZombies, RobotInfo[] enemiesAttackingUs, int numEnemiesAttackingUs) throws GameActionException {
		if (enemiesInSight.length == 0 || !(rc.isCoreReady() || rc.isWeaponReady())) {
			return false;
		}
		boolean willDieFromViper = (rc.isInfected() && rc.getHealth() - rc.getViperInfectedTurns() * GameConstants.VIPER_INFECTION_DAMAGE < 0);
		/*
		if(rc.isCoreReady() && rc.getViperInfectedTurns() > 0 && !willDieFromViper && archonLoc != null && rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE).length == 0){
			tryToRetreat(enemiesInSight);
		}
		*/
		if (willDieFromViper && rc.isCoreReady()) {
			// CHARGE blindly
			if(enemiesWithoutZombies.length > 0)
				Nav.goTo(Util.closest(enemiesWithoutZombies, here).location, new SafetyPolicyAvoidAllUnits(new RobotInfo[]{}));
		}
		if (hostilesICanSee.length == 0 || !(rc.isCoreReady() || rc.isWeaponReady())) {
			return false;
		}

		if (numEnemiesAttackingUs > 0) {
			// we are in combat
			if (numEnemiesAttackingUs == 1) {
				// we are in a 1v1
				RobotInfo loneAttacker = enemiesAttackingUs[0];
				if (type.attackRadiusSquared >= here.distanceSquaredTo(loneAttacker.location)&&rc.isLocationOccupied(loneAttacker.location)) {
					// we can actually shoot at the enemy we are 1v1ing
					if (loneAttacker.type == RobotType.ARCHON || canWin1v1(loneAttacker)) {
						// we can beat the other guy 1v1. fire away!
						//rc.setIndicatorString(1, "can win 1v1");
						attackIfReady(loneAttacker.location);
						if (loneAttacker.type == RobotType.ARCHON && rc.isCoreReady())
							shadowArchon(loneAttacker, enemiesInSight);
						if(rc.isCoreReady())
							tryToRetreat(enemiesInSight);
					} else {
						// check if we actually have some allied support. if so, we can keep fighting
						if (numOtherAlliesInAttackRange(loneAttacker.location, allies) > 0) {
							// an ally is helping us, so keep fighting the lone enemy
							//rc.setIndicatorString(1, "can't win 1v1 but have " + allies.length + " allied support");
							// TODO: archon test shooting zombies first instead (comment out two lines below)
							if(rc.isCoreReady() && loneAttacker.team == Team.ZOMBIE)
								tryToRetreat(enemiesInSight);
							attackIfReady(loneAttacker.location);
							if(rc.isCoreReady())
								tryToRetreat(enemiesInSight);
						} else {
							/*
							if(loneAttacker.type != RobotType.VIPER && !inRangeOfZombieDen(rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE)))
								rc.broadcastSignal((int)(type.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
							*/
							// we can't win the 1v1.
							if (type.cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2
									&& rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
								// we can get a shot off and retreat before the enemy can fire at us again, so do that
								//rc.setIndicatorString(1, "can't win 1v1, but can shoot and run");
								attackIfReady(loneAttacker.location);
								if(rc.isCoreReady())
									tryToRetreat(enemiesInSight);
							} else {
								// we can't get another shot off. run away!
								if (rc.isCoreReady()) {
									//rc.setIndicatorString(1, "can't win 1v1, trying to retreat");
									// we can move this turn
									if (tryToRetreat(enemiesInSight)) {
										// we moved away
										return true;
									} else{
										// we couldn't find anywhere to retreat to. fire a desperate shot if possible
										attackIfReady(loneAttacker.location);
										return true;
									}
								} else{
									// we can't move this turn. if it won't delay retreating, shoot instead
									if (type.cooldownDelay <= 1) {
										//rc.setIndicatorString(1, "can't win 1v1 or move, trying to shoot");
										attackIfReady(loneAttacker.location);
									}
									return true;
								}
							}
						}
					}
				} else {
					// we are getting shot by someone who outranges us, CRUNCH!
					//rc.setIndicatorString(1, "outranged!");
					if(rc.isCoreReady())
						Nav.goTo(loneAttacker.location, new SafetyPolicyAvoidAllUnits(new RobotInfo[]{}));
					return true;
				}
			} else { // more than one enemy
				RobotInfo bestTarget = null;
				double bestTargetingMetric = 0;
				int maxAlliesAttackingAnEnemy = 0;
				for (int i = 0; i < numEnemiesAttackingUs; i++) {
					RobotInfo enemy = enemiesAttackingUs[i];
					int numAlliesAttackingEnemy = allies.length > numEnemiesAttackingUs*3?allies.length / 2 + 1 : 1 + numOtherAlliesInAttackRange(enemy.location, allies);
					if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy)
						maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
					if (type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
						double targetingMetric = numAlliesAttackingEnemy / enemy.health
								+ (enemy.team == Team.ZOMBIE?0:0.1) // shoot zombies last
								+  enemy.attackPower/300
								+  enemy.type.attackRadiusSquared/2000
								-  enemy.type.movementDelay/300
								- ((type == RobotType.VIPER && enemy.type == RobotType.ARCHON && enemiesWithoutZombies.length < allies.length && enemy.health < 200)?(rc.getRoundNum()/500):0)
								+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0 && enemy.team!=Team.ZOMBIE && enemy.type != RobotType.ARCHON)?50:0);// shoot non-infected first if viper
						if (targetingMetric > bestTargetingMetric) {
							bestTargetingMetric = targetingMetric;
							bestTarget = enemy;
						}
					}
				}
				// multiple enemies are attacking us. stay in the fight iff enough allies are also engaged
				if (maxAlliesAttackingAnEnemy >= numEnemiesAttackingUs && bestTarget != null) {
					// enough allies are in the fight.
					//rc.setIndicatorString(1, "more than one enemy, but have enough allies");
					attackIfReady(bestTarget.location);
					if(rc.isCoreReady())
						tryToRetreat(enemiesInSight);
					return true;
				} else {
					// not enough allies are in the fight. we need to retreat
					/*
					if(rc.getViperInfectedTurns() == 0 && !inRangeOfZombieDen(rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE)))
						rc.broadcastSignal((int)(type.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
					*/
					//rc.setIndicatorString(1, "more than one enemy, don't have enough allies");
					if (rc.isCoreReady()) {
						// we can move this turn
						if (tryToRetreat(enemiesInSight)) {
							// we moved away :)
							//rc.setIndicatorString(1, "more than one enemy, retreated!");
							return true;
						} else if (bestTarget != null) {
							// we couldn't find anywhere to retreat to. fire a desperate shot if possible
							//rc.setIndicatorString(1, "more than one enemy, couldn't retreat, desprate shot");
							attackIfReady(bestTarget.location);
							return true;
						}
					} else {
						// we can't move this turn. if it won't delay retreating, shoot instead
						if (type.cooldownDelay <= 1 && bestTarget != null) {
							//rc.setIndicatorString(1, "more than one enemy, couldn't move, shot instead");
							attackIfReady(bestTarget.location);
						}
						return true;
					}
				}
			}
		} else {
			// no one is shooting at us. if we can shoot at someone, do so
			RobotInfo bestTarget = null;
			if(enemiesICanShoot.length == 1)
				bestTarget = enemiesICanShoot[0];
			else{
				double bestTargetingMetric = 0;
				int maxAlliesAttackingAnEnemy = 0;
				for (RobotInfo enemy : enemiesICanShoot) {
					int numAlliesAttackingEnemy = allies.length > enemiesICanShoot.length*3?allies.length / 2 + 1 : 1 + numOtherAlliesInAttackRange(enemy.location, allies);
					if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy)
						maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
					if (type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
						double targetingMetric = numAlliesAttackingEnemy / enemy.health
								+ (enemy.team == Team.ZOMBIE?0:0.1) // shoot zombies last
								+  enemy.attackPower/300
								+  enemy.type.attackRadiusSquared/2000
								-  enemy.type.movementDelay/300
								- ((type == RobotType.VIPER && enemy.type == RobotType.ARCHON && enemiesWithoutZombies.length < allies.length && enemy.health < 200)?(rc.getRoundNum()/500):0)
								+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0 && enemy.team!=Team.ZOMBIE && enemy.type != RobotType.ARCHON)?50:0);// shoot non-infected first if viper
						if (targetingMetric > bestTargetingMetric) {
							bestTargetingMetric = targetingMetric;
							bestTarget = enemy;
						}
					}
				}
			}
			// shoot someone if there is someone to shoot
			if (bestTarget != null) {
				//rc.setIndicatorString(1, "no enemies shooting me, shot someone else");
				attackIfReady(bestTarget.location);
				return true;
			}
			// we can't shoot anyone or there was weapon delay
			if (rc.isCoreReady()) { // all remaining possible actions are movements
				// check if we can move to help an ally who has already engaged a nearby enemy
				//rc.setIndicatorString(2, "No core delay, we can see but not shoot");
				RobotInfo closestEnemy = Util.closest(enemiesInSight, here);
				// we can only think about engaging enemies with equal or shorter range
				if (closestEnemy != null
						&& (type.attackRadiusSquared >= closestEnemy.type.attackRadiusSquared
							|| closestEnemy.type == RobotType.ARCHON)) {
					//we outrange them
					//rc.setIndicatorString(2, "No core delay, we can see but not shoot, move to engage closest enemy");
					int numAlliesFightingEnemy = numOtherAlliesInAttackRange(closestEnemy.location, allies);
					if (numAlliesFightingEnemy > 0) {
						//rc.setIndicatorString(2, "No core delay, we can see but not shoot, trying to move to help allies");
						// see if we can assist our ally(s)
						int maxEnemyExposure = numAlliesFightingEnemy;
						if (tryMoveTowardLocationWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, enemiesInSight)) {
							return true;
						}
						// TODO: what if that didn't work?
					} else {
						//System.out.println("no allies");
						// no one is fighting this enemy, but we can try to engage them if we can win the 1v1
						if (canWin1v1AfterMovingTo(here.add(here.directionTo(closestEnemy.location)), closestEnemy)) {
							int maxEnemyExposure = 1;
							//rc.setIndicatorString(2, "No core delay, we can see but not shoot, going for the 1v1 win");
							if (tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(closestEnemy.location,
									maxEnemyExposure, enemiesInSight)) {
								return true;
							}
							// TODO: what if it didn't work?
							//rc.setIndicatorString(2, "no allies to help, no action :(");
						}
					}
				}
				return false;
			}
			// return true here because core is not ready, so it's as if
			// we took a required action in the sense that we can't do anything else
			return true;
		}
		return false;
	}

	private static boolean inRangeOfZombieDen(RobotInfo[] zombies) {
		for(RobotInfo zombie: zombies){
			if(zombie.type == RobotType.ZOMBIEDEN)
				return true;
		}
		return false;
	}

	private static boolean tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(MapLocation loc,
			int maxEnemyExposure, RobotInfo[] enemies) throws GameActionException {
		Direction toLoc = here.directionTo(loc);
		Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
		for (Direction dir : tryDirs) {
			if (!rc.canMove(dir))
				continue;
			MapLocation moveLoc = here.add(dir);
			if (type.attackRadiusSquared < moveLoc.distanceSquaredTo(loc))
				continue; // must engage in one turn

			int enemyExposure = numEnemiesAttackingLocation(moveLoc, enemies);
			if (enemyExposure <= maxEnemyExposure) {
				rc.move(dir);
				return true;
			}
		}

		return false;
	}

	private static boolean tryMoveTowardLocationWithMaxEnemyExposure(MapLocation loc, int maxEnemyExposure,
			RobotInfo[] enemies) throws GameActionException {
		Direction toLoc = here.directionTo(loc);
		Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
		for (Direction dir : tryDirs) {
			if (!rc.canMove(dir))
				continue;
			MapLocation moveLoc = here.add(dir);

			int enemyExposure = numEnemiesAttackingLocation(moveLoc, enemies);
			if (enemyExposure <= maxEnemyExposure&&rc.canMove(dir)) {
				//rc.move(dir); 
				Nav.goTo(moveLoc, new SafetyPolicyAvoidAllUnits(enemies));
				return true;
			}
		}
		return false;
	}

	private static void shadowArchon(RobotInfo enemyToShadow, RobotInfo[] enemies) throws GameActionException {
		Direction toEnemy = here.directionTo(enemyToShadow.location);
		Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(),
				toEnemy.rotateRight().rotateRight(), toEnemy.rotateLeft().rotateLeft() };
		for (Direction dir : dirs) {
			if (!rc.canMove(dir))
				continue;

			MapLocation loc = here.add(dir);

			boolean locIsSafe = true;

			for (RobotInfo enemy : enemies) {
				if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)
						&& enemy.type != RobotType.ARCHON) {
					locIsSafe = false;
					break;
				}
			}

			if (locIsSafe) {
				rc.move(dir);
				break;
			}
		}

		if (rc.isCoreReady()) {
			Util.checkRubbleAndClear(toEnemy, true);
		}
	}

	private static int numEnemiesAttackingLocation(MapLocation loc, RobotInfo[] enemies) {
		int ret = 0;
		for (int i = enemies.length; i-- > 0;) {
			if (enemies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(enemies[i].location)
					&& enemies[i].type != RobotType.ARCHON)
				ret++;

		}
		return ret;
	}

	public static int numOtherAlliesInAttackRange(MapLocation loc, RobotInfo[] allies) {
		int ret = 0;
		for (RobotInfo ally : allies) {
			if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location) && ally.type!=RobotType.ARCHON)
				ret++;
		}
		return ret;
	}

	private static boolean isHarasser(RobotType rt) {
		switch (rt) {
		case SOLDIER:
		case VIPER:
		case GUARD:
			return true;

		default:
			return false;
		}
	}

	public static void updateTargetLocWithoutSignals() throws GameActionException {
		if (type == RobotType.VIPER) {
			updateViperTargetLocWithoutSignals();
			return;
		}
		else if (swarmingArchon && archonLoc != null){
			targetLoc = archonLoc;
			return;
		}
		if(targetLoc == null){
			RobotInfo[] zombies = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE);
			for (RobotInfo zombie : zombies) {
				if (zombie.type == RobotType.ZOMBIEDEN) {
					targetLoc = zombie.location;
				}
			}
		}
		if(targetLoc == null && archonLoc != null && turretLoc == null){
			targetLoc = archonLoc;
			swarmingArchon = true;
			huntingDen = false;
		}
		else if (huntingDen && rc.canSenseLocation(targetLoc) && (rc.senseRobotAtLocation(targetLoc) == null || rc.senseRobotAtLocation(targetLoc).type != RobotType.ZOMBIEDEN)) {
			// tell people a den has been killed
			if (targetLoc != null) {
				rc.broadcastSignal(12800);
				killedDens[killedDenSize] = targetDens[bestIndex];
				targetDens[bestIndex] = null;
				killedDenSize++;
				numDensToHunt--;
			}
			targetLoc = null;
			huntingDen = false;
			if (numDensToHunt > 0) {
				huntingDen = true;
				bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
				targetLoc = targetDens[bestIndex];
			}
		}
		if (targetLoc == null || swarmingArchon && !isGuard && turretLoc != null){
			targetLoc = turretLoc;
			swarmingArchon = false;
		}
	}

	private static void updateViperTargetLocWithoutSignals() {
		if (targetLoc == null){
			targetLoc = turretLoc;
		}
		if (targetLoc == null) {
			MapLocation[] enemyArchonLocations = initialEnemyArchonLocs;
			do {
				int locIndex = Util.closestLocation(enemyArchonLocations, here, enemyArchonLocations.length);
				if (locIndex == -1){
					targetLoc = null;
					break;
				}
				targetLoc = enemyArchonLocations[locIndex];
				enemyArchonLocations[locIndex] = null;
			} while (here.distanceSquaredTo(targetLoc) < 5);
		}
	}

	private static boolean nearEnemies(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		if (closestEnemy != null
				&& here.distanceSquaredTo(closestEnemy.location) < closestEnemy.type.attackRadiusSquared)
			return true;
		return false;
	}

	private static boolean couldMoveOut(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		int range = closestEnemy.type.attackRadiusSquared - here.distanceSquaredTo(closestEnemy.location);
		if (range > -1)
			return true;
		return false;
	}

	private static boolean updateArchonLoc() {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		for (RobotInfo ally : allies) {
			if (ally.type == RobotType.ARCHON) {
				archonLoc = ally.location;
				archonUpdate = rc.getRoundNum();
				return true;
			}
		}
		if(rc.getRoundNum() - archonUpdate > 50)
			archonLoc = null;
		return false;
	}

	private static void attackIfReady(MapLocation loc) throws GameActionException {
		if (rc.isWeaponReady()) {
			rc.attackLocation(loc);
		}
	}

	public static boolean updateTurretStuff(RobotInfo[] enemies) throws GameActionException {
		// then set turretTarget to closest one
		turretLoc = null;
		for(RobotInfo enemy:enemies){
			if(enemy.type == RobotType.ARCHON){
				turretLoc = enemy.location;
				return true;
			}
		}
		for (RobotInfo e : enemies)
			if (e.type == RobotType.TURRET)
				if(!isLocationInTurretArray(e.location)){
					enemyTurrets[turretSize] = e;
					turretSize++;
				}
		if (turretSize > 0) {
			int min = 999999;
			int dist;
			MapLocation t;
			for (int i = 0; i < turretSize; i++) {
				t = enemyTurrets[i].location;
				if (rc.canSenseLocation(t)) {
					RobotInfo bot = rc.senseRobotAtLocation(t);
					if (bot == null || bot.type != RobotType.TURRET) {
						removeLocFromTurretArray(t);
						if(t.equals(targetLoc)) targetLoc = null;
						i--;
						continue;
					}
				}
				dist = here.distanceSquaredTo(t);
				if (dist < min) {
					turretLoc = t;
					min = dist;
				}
			}
			return true;
		}
		return false;
	}

	public static void crunch(RobotInfo[] enemies,RobotInfo[] allies) throws GameActionException {
		if (turretLoc != null && rc.isCoreReady()) {
			NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(new RobotInfo[0]);
			Nav.goTo(turretLoc, theSafety);
		}
		if (rc.isWeaponReady()) {
			crunchShoot(rc.senseNearbyRobots(type.sensorRadiusSquared,them),allies);
			return;
		}
	}

	public static void crunchShoot(RobotInfo [] enemies,RobotInfo[] allies) throws GameActionException{
		RobotInfo bestTarget = null;
		double bestTargetingMetric = 0;
		for (RobotInfo enemy : enemies) {
			if (type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
				double targetingMetric = allies.length/ 3 / enemy.health
						+ (enemy.team == Team.ZOMBIE?0:0.1) // shoot zombies last
						+  enemy.attackPower/300
						+  enemy.type.attackRadiusSquared/2000
						-  enemy.type.movementDelay/300
						+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0 && enemy.team!=Team.ZOMBIE)?50:0);// shoot non-infected first if viper
				if (targetingMetric > bestTargetingMetric) {
					bestTargetingMetric = targetingMetric;
					bestTarget = enemy;
				}
			}
		}
		if(bestTarget!=null && rc.isWeaponReady()){
			rc.attackLocation(bestTarget.location);
		}
	}
	
	public static void stayOutOfRange(RobotInfo[] enemies) throws GameActionException {
		//rc.setIndicatorString(2, "staying out of range");
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(
				Util.combineTwoRIArrays(enemyTurrets, turretSize, enemies));
		if (here.distanceSquaredTo(turretLoc) < 64) {
			Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
		} else {
			Nav.goTo(turretLoc, theSafety);
		}
	}

	public static void updateInfoFromSignals(Signal[] signals) throws GameActionException{
		prepTargetLoc();
		for(Signal signal: signals){
			if(signal.getTeam() == us){
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					int[] data;
					MapLocation senderloc, loc;
					switch(purpose){
					case BE_MY_GUARD:
						if(rc.getRoundNum() - turnCreated < 10){
							isGuard = true;
							swarmingArchon = true;
							huntingDen = false;
						}
						break;
					case MOBILE_ARCHON_LOCATION:
						data = purpose.decode(signal.getLocation(), message);
						MapLocation newArchonLoc = new MapLocation(data[0], data[1]);
						if(archonLoc == null || here.distanceSquaredTo(newArchonLoc) < here.distanceSquaredTo(archonLoc) || archonUpdate != rc.getRoundNum()){
							archonLoc = newArchonLoc;
							if(swarmingArchon)
								targetLoc = archonLoc;
							/*
							if(targetLoc == null)
								isGuard = true;
							*/
						}
						archonUpdate = rc.getRoundNum();
						break;
					case ENEMY_TURRET_DEATH:
						data = purpose.decode(signal.getLocation(), message);
						loc = new MapLocation(data[0],data[1]);
						removeLocFromTurretArray(loc);
						if(loc.equals(targetLoc)) targetLoc = null;
						break;
					case WARN_ABOUT_TURRETS:
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						loc = new MapLocation(data[0], data[1]);
						if(!isLocationInTurretArray(loc)){
							enemyTurrets[turretSize]= new RobotInfo(0, them, RobotType.TURRET, loc,0,0,0,0,0,0,0);
							turretSize++;
						}
						break;
					case RELAY_TURRET_INFO:
						if(rc.getRoundNum()-turnCreated > 10) break;
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						for(int i = 0; i< data.length; i +=2){
							loc = new MapLocation(data[i], data[i+1]);
							if(loc.equals(senderloc)){
								break;
							}
							if(!isLocationInTurretArray(loc)){
								enemyTurrets[turretSize]= new RobotInfo(0, them, RobotType.TURRET, loc,0,0,0,0,0,0,0);
								turretSize++;
							}
						}
						break;
					case CRUNCH_TIME:
						data = purpose.decode(signal.getLocation(), message);
						if(here.distanceSquaredTo(new MapLocation(data[0], data[1])) <= 400){
							crunching = true;
						    huntingDen = false;
						}
						break;
					case DEN_NOTIF:
						if(type == RobotType.VIPER) break;
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if(data[2] == 1){
							//System.out.println("got a den notif");
							if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)
									&& !Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
								targetDens[targetDenSize] = denLoc;
								targetDenSize++;
								numDensToHunt++;
								if (!isGuard && (!huntingDen //test this
										|| here.distanceSquaredTo(denLoc) < here.distanceSquaredTo(targetLoc))) {
									targetLoc = denLoc;
									bestIndex = targetDenSize - 1;
									huntingDen = true;
									swarmingArchon = false;
								}
							}
						} else {
							//System.out.println("got a den death notif");
							//rc.setIndicatorString(0, "not going for den at loc " + targetDens[closestIndex] + " on round " + rc.getRoundNum());
							killedDens[killedDenSize] = denLoc;
							killedDenSize++;
							int deadDenIndex = Util.indexOfLocation(targetDens, targetDenSize, denLoc);
							if(deadDenIndex != -1){
								targetDens[deadDenIndex] = null;
								numDensToHunt--;
								if(huntingDen && targetLoc.equals(denLoc)){
									//rc.setIndicatorString(0, "here"); 
									huntingDen = false;
									targetLoc = null;
									if (numDensToHunt > 0) {
										huntingDen = true;
										swarmingArchon = false;
										bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
										targetLoc = targetDens[bestIndex];
									}
								}
							}
						}
						break;
					case ENEMY_ARMY_NOTIF:
						if(type == RobotType.SOLDIER && huntingDen) break;
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						MapLocation enemyLoc = new MapLocation(data[0], data[1]);
						if (swarmingArchon && !isGuard || targetLoc == null
								|| (double) here.distanceSquaredTo(enemyLoc) < 1.5 * (here.distanceSquaredTo(targetLoc))) {
							targetLoc = enemyLoc;
							swarmingArchon = false;
						}
						break;
					default:
					}
				} else { // our team, no message
					MapLocation signalLoc = signal.getLocation();
					int distToSignal = here.distanceSquaredTo(signalLoc);
					// if (type.sensorRadiusSquared *
					// GameConstants.BROADCAST_RANGE_MULTIPLIER >= distToSignal
					// && (targetLoc == null || distToSignal <
					// here.distanceSquaredTo(targetLoc))) {// call
					// // for
					// // help
					// targetLoc = signalLoc;
					// huntingDen = false;
					// return true;
					// } else {// if a den has been killed don't go for it
					// anymore
					int closestIndex = Util.closestLocation(targetDens, signalLoc, targetDenSize);
					boolean wasAboutDen = closestIndex != -1 && signalLoc.distanceSquaredTo(targetDens[closestIndex]) <= type.sensorRadiusSquared;
					if (wasAboutDen && type != RobotType.VIPER){
						//rc.setIndicatorString(0, "not going for den at loc " + targetDens[closestIndex] + " on round " + rc.getRoundNum());
						MapLocation killedDen = targetDens[closestIndex];
						targetDens[closestIndex] = null;
						killedDens[killedDenSize] = killedDen;
						killedDenSize++;
						numDensToHunt--;
						if(huntingDen && targetLoc.equals(killedDen)){
							//rc.setIndicatorString(0, "here"); 
							huntingDen = false;
							targetLoc = null;
							if (numDensToHunt > 0) {
								huntingDen = true;
								swarmingArchon = false;
								bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
								targetLoc = targetDens[bestIndex];
							}
						}
					}
				}
				//			} else { //we don't use this yet according to eli
				//				// heard a message from the other team! quack
				//				if(canSeeHostiles)
				//					return;
				//				MapLocation enemyLoc = signal.getLocation();
				//				if (targetLoc == null || !huntingDen && (double) here.distanceSquaredTo(enemyLoc) < 0.5
				//						* (here.distanceSquaredTo(targetLoc))) {
				//					targetLoc = enemyLoc;
				//					if(type != RobotType.VIPER) huntingDen = false;
				//				}
			}
		}
	}

	public static void prepTargetLoc() {
		updateArchonLoc();
		if (!huntingDen && targetLoc != null && here.distanceSquaredTo(targetLoc) < 5
			 && !swarmingArchon) {
			targetLoc = null;
			huntingDen = false;
			if (numDensToHunt > 0 && type != RobotType.VIPER) {
				huntingDen = true;
				bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
				targetLoc = targetDens[bestIndex];
			}
		}
	}

	public static void doHarass() throws GameActionException {
		wantToMove = true;
		String bytecodeIndicator = "";
		RobotInfo[] friends = rc.senseNearbyRobots(here, type.sensorRadiusSquared, us);
		RobotInfo[] enemiesWithoutZombies = rc.senseNearbyRobots(here, type.sensorRadiusSquared, them);
		RobotInfo[] hostilesICanSee = rc.senseHostileRobots(here, type.sensorRadiusSquared);
		RobotInfo[] enemies = Util.combineTwoRIArrays(enemyTurrets, turretSize, hostilesICanSee);
		RobotInfo[] enemiesICanShoot = rc.senseHostileRobots(here, type.attackRadiusSquared);
		int numEnemiesAttackingUs = 0;
		RobotInfo[] enemiesAttackingUs = new RobotInfo[enemies.length];
		for (RobotInfo enemy : enemies) {
			if (enemy.type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
				enemiesAttackingUs[numEnemiesAttackingUs++] = enemy;
			}
		}
		// rc.setIndicatorString(0, "" + signals.length);
		boolean turretUpdated = updateTurretStuff(enemies);
		if (crunching && (turretLoc == null || enemiesWithoutZombies.length == 0 && here.distanceSquaredTo(turretLoc) < type.sensorRadiusSquared || here.distanceSquaredTo(turretLoc) > 150)) {
			crunching = false;
			targetLoc = null;
			huntingDen = false;
		}
		//updateMoveIn(signals, enemies);
		//boolean targetUpdated = updateTargetLoc(signals);
		int startB = Clock.getBytecodeNum();
		if(!crunching && numEnemiesAttackingUs == 0){
			Signal[] signals = rc.emptySignalQueue();
			updateInfoFromSignals(signals);
			updateTargetLocWithoutSignals();
		}
		int signalBytecode = Clock.getBytecodeNum() - startB;
		bytecodeIndicator += "Signal Reading: " + signalBytecode;
		//if(signalBytecode > 2000 && rc.getRoundNum() - turnCreated > 30) //System.out.println("signal used " + signalBytecode);
		// starts here
		if (crunching) {
			crunch(enemies,friends);
		} else if (hostilesICanSee.length > 0) {
			startB = Clock.getBytecodeNum();
			doMicro(enemies, hostilesICanSee, enemiesICanShoot, friends, enemiesWithoutZombies, enemiesAttackingUs, numEnemiesAttackingUs);
			int microBytecode = Clock.getBytecodeNum() - startB;
			bytecodeIndicator += " Micro: " + microBytecode;
			//if(microBytecode > 2000) System.out.println("micro used " + microBytecode);
		}
		if(wantToMove && rc.isCoreReady()){ // no enemies
			// maybe uncomment this but only do it if we can't see a scout
			NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostilesICanSee));
			if (turretLoc != null && here.distanceSquaredTo(turretLoc) < RobotType.TURRET.attackRadiusSquared + 4)
				Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
			if (targetLoc != null) {
				if(targetLoc == archonLoc && here.distanceSquaredTo(archonLoc) + 4 < type.sensorRadiusSquared && Util.isSurrounded(archonLoc) &&rc.isCoreReady())
					Nav.goAwayFrom(archonLoc, theSafety);
				else if(rc.isCoreReady()){
					startB = Clock.getBytecodeNum();
					Nav.goTo(targetLoc, new SafetyPolicyAvoidAllUnits(enemies));
					int navBytecode = Clock.getBytecodeNum() - startB;
					bytecodeIndicator += " Nav: " + navBytecode;
				}
				//if(navBytecode > 2000) System.out.println("nav used " + navBytecode);
			}
			else if(!Util.checkRubbleAndClear(here.directionTo(center), true)  && rc.isCoreReady())
				Nav.followFriends(friends, enemies);
		}
		//rc.setIndicatorString(0, "targetLoc = " + targetLoc);
		//rc.setIndicatorString(1, "isGuard = " + isGuard);
//		String s = "";
//		for(int i = 0; i < targetDenSize; i++){
//			if(targetDens[i] != null)
//			s += "[" + targetDens[i].x + ", " + targetDens[i].y +"], "; 
//		}
//		rc.setIndicatorString(1, s + " " + targetDenSize);
//		rc.setIndicatorString(2, "swarming archon = " + swarmingArchon);
	}
}
