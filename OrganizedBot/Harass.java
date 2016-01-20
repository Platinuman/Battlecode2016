package OrganizedBot;

import battlecode.common.*;

public class Harass extends Bot {
	// NEW read up bot types for what they call in Harass.
	// Implement those and the rest of these methods are helper methods for the
	// big ones.
	// Once again Optimization.
	static MapLocation turretLoc;
	static MapLocation targetLoc;
	static RobotInfo[] enemies;
	static RobotInfo[] enemiesICanShoot;
	static RobotInfo[] friends;
	static MapLocation archonLoc;
	static boolean targetUpdated;
	static boolean archonUpdated;
	static boolean huntingDen;
	static boolean isPreparingForCrunch;
	static int archonID;

	private static boolean canWin1v1(RobotInfo enemy) {

		if (enemy.type == RobotType.ARCHON)
			return true;

		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
		int turnsTillWeCanAttack;
		int effectiveAttackDelay;
		turnsTillWeCanAttack = (int) rc.getWeaponDelay();
		effectiveAttackDelay = (int) rc.getType().attackDelay;
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
		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
		int turnsTillWeCanAttack;
		int effectiveAttackDelay;

		double weaponDelayAfterMoving = rc.getWeaponDelay() - 1.0;
		turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
		effectiveAttackDelay = (int) rc.getType().attackDelay;

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
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(enemies, here);

		boolean mustMoveOrthogonally = false;
		// if (rc.getType() == RobotType.DRONE && rc.getCoreDelay() >= 0.6 &&
		// currentClosestEnemy.type == RobotType.MISSILE) mustMoveOrthogonally =
		// true;

		int bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
		for (Direction dir : Direction.values()) {
			if (!rc.canMove(dir))
				continue;
			if (mustMoveOrthogonally && dir.isDiagonal())
				continue;

			MapLocation retreatLoc = here.add(dir);

			RobotInfo closestEnemy = Util.closest(enemies, retreatLoc);
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			if (distSq > bestDistSq) {
				bestDistSq = distSq;
				bestRetreatDir = dir;
			}
		}

		if (bestRetreatDir != null && rc.isCoreReady()) {
			rc.move(bestRetreatDir);
			return true;
		}
		return false;
	}

	// currently our micro looks like this:
	// if we are getting owned, and we have core delay, try to retreat
	// if we can hit an enemy, attack if our weapon delay is up. otherwise sit
	// still
	// try to stick to enemy harassers, engaging them if we can win the 1v1
	// try to move toward undefended workers, engaging them if we can win the
	// 1v1

	// here's a better micro:
	// if we are getting hit:
	// - if we are getting owned and have core delay and can retreat, do so
	// - otherwise hit an enemy if we can
	// if we are not getting hit:
	// - if we can assist an ally who is engaged, do so
	// - if we can move to engage a worker, do so
	// - if there is an enemy harasser nearby, stick to them
	// - - optionally, engage if we can win the 1v1 or if there is a lot of
	// allied support

	// it's definitely good to take 1v1s if there are no nearby enemies. however
	// we maybe
	// should avoid initiating 1v1s if there are enemies nearby that can
	// support.

	private static boolean doMicro(RobotInfo[] enemiesInSight, RobotInfo[] enemiesICanShoot, boolean targetUpdated,
			boolean archonUpdated) throws GameActionException {
		if (enemies.length == 0) {
			/*
			 * RobotInfo[] moreEnemies =
			 * rc.senseNearbyRobots(rc.getType().attackRadiusSquared, them); if
			 * (moreEnemies.length == 0) { // Debug.indicate("micro", 0,
			 * "no enemies, no micro"); return false; } else { RobotInfo
			 * closestEnemy = Util.closest(moreEnemies, here); if (closestEnemy
			 * != null && isHarasser(closestEnemy.type) &&
			 * rc.getType().attackRadiusSquared >=
			 * closestEnemy.type.attackRadiusSquared) { //
			 * Debug.indicate("micro", 0,
			 * "no nearby enemies, shadowing an enemy at long range"); if
			 * (rc.isCoreReady()) { shadowHarasser(closestEnemy, enemies); }
			 * return true; } }
			 */
			return false;
		}
		/*
		 * else if (rc.getRoundNum() % 7 == 0){//call for help
		 * rc.broadcastSignal((int)(rc.getType().sensorRadiusSquared *
		 * GameConstants.BROADCAST_RANGE_MULTIPLIER)); }
		 */

		int numEnemiesAttackingUs = 0;
		RobotInfo[] enemiesAttackingUs = new RobotInfo[99];
		for (RobotInfo enemy : enemiesInSight) {
			if (enemy.type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
				enemiesAttackingUs[numEnemiesAttackingUs++] = enemy;
			}
		}

		if (numEnemiesAttackingUs > 0) {
			// we are in combat
			if (numEnemiesAttackingUs == 1) {
				// we are in a 1v1
				RobotInfo loneAttacker = enemiesAttackingUs[0];
				if (rc.getType().attackRadiusSquared >= here.distanceSquaredTo(loneAttacker.location)) {
					// we can actually shoot at the enemy we are 1v1ing
					if (canWin1v1(loneAttacker)) {
						// we can beat the other guy 1v1. fire away!
						// Debug.indicate("micro", 0, "winning 1v1");
						attackIfReady(loneAttacker.location);
						return true;
					} else {
						// check if we actually have some allied support. if so
						// we can keep fighting
						boolean haveSupport = false;
						for (int i = 0; i < numEnemiesAttackingUs; i++) {
							if (numOtherAlliesInAttackRange(enemiesAttackingUs[i].location) > 0) {
								haveSupport = true;
								break;
							}
						}
						if (haveSupport) {
							// an ally is helping us, so keep fighting the lone
							// enemy
							// Debug.indicate("micro", 0, "losing 1v1 but we
							// have support");
							attackIfReady(loneAttacker.location);
							return true;
						} else {
							// we can't win the 1v1.
							if (rc.getType().cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2
									&& rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
								// we can get a shot off and retreat before the
								// enemy can fire at us again, so do that
								// Debug.indicate("micro", 0, "firing one last
								// shot before leaving losing 1v1");
								attackIfReady(loneAttacker.location);
								return true;
							} else {
								// we can't get another shot off. run away!
								if (rc.isCoreReady()) {
									// we can move this turn
									if (tryToRetreat(enemies)) {
										// we moved away
										// Debug.indicate("micro", 0,
										// "retreated");
										return true;
									} else {
										// we couldn't find anywhere to retreat
										// to. fire a desperate shot if possible
										// Debug.indicate("micro", 0, "couldn't
										// find anywhere to retreat! trying to
										// shoot");
										attackIfReady(loneAttacker.location);
										return true;
									}
								} else {
									// we can't move this turn. if it won't
									// delay retreating, shoot instead
									// Debug.indicate("micro", 0, "want to
									// retreat but core isn't ready; trying to
									// shoot if cooldown <= 1");
									if (rc.getType().cooldownDelay <= 1) {
										attackIfReady(loneAttacker.location);
									}
									return true;
								}
							}
						}
					}
				} else {
					// we are getting shot by someone who outranges us. run
					// away!
					// Debug.indicate("micro", 0, "trying to retreat from a 1v1
					// where we are outranged");
					tryToRetreat(enemies);
					return true;
				}
			} else {
				RobotInfo bestTarget = null;
				double bestTargetingMetric = 0;
				int maxAlliesAttackingAnEnemy = 0;
				for (int i = 0; i < numEnemiesAttackingUs; i++) {
					RobotInfo enemy = enemiesAttackingUs[i];
					int numAlliesAttackingEnemy = 1 + numOtherAlliesInAttackRange(enemy.location);
					if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy)
						maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
					if (rc.getType().attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
						double targetingMetric = numAlliesAttackingEnemy / enemy.health + enemy.attackPower;
						if (targetingMetric > bestTargetingMetric) {
							bestTargetingMetric = targetingMetric;
							bestTarget = enemy;
						}
					}
				}

				// multiple enemies are attacking us. stay in the fight iff
				// enough allies are also engaged
				if (maxAlliesAttackingAnEnemy >= numEnemiesAttackingUs && bestTarget != null) {
					// enough allies are in the fight.
					// Debug.indicate("micro", 0, "attacking because
					// numEnemiesAttackingUs = " + numEnemiesAttackingUs + ",
					// maxAlliesAttackingEnemy = "
					// + maxAlliesAttackingAnEnemy);
					attackIfReady(bestTarget.location);
					return true;
				} else {
					// not enough allies are in the fight. we need to retreat
					if (rc.isCoreReady()) {
						// we can move this turn
						if (tryToRetreat(enemies)) {
							// we moved away
							// Debug.indicate("micro", 0, "retreated because
							// numEnemiesAttackingUs = " + numEnemiesAttackingUs
							// + ", maxAlliesAttackingEnemy = "
							// + maxAlliesAttackingAnEnemy);
							return true;

						} else if (bestTarget != null) {
							// we couldn't find anywhere to retreat to. fire a
							// desperate shot if possible
							// Debug.indicate("micro", 0, "no retreat square :(
							// numEnemiesAttackingUs = " + numEnemiesAttackingUs
							// +
							// ", maxAlliesAttackingEnemy = "
							// + maxAlliesAttackingAnEnemy);
							attackIfReady(bestTarget.location);
							return true;
						}
					} else {

						// we can't move this turn. if it won't delay
						// retreating, shoot instead
						// Debug.indicate("micro", 0, "want to retreat but core
						// on cooldown :( numEnemiesAttackingUs = " +
						// numEnemiesAttackingUs
						// + ", maxAlliesAttackingEnemy = " +
						// maxAlliesAttackingAnEnemy);
						if (rc.getType().cooldownDelay <= 1 && bestTarget != null) {
							attackIfReady(bestTarget.location);
						}
						return true;
					}
				}
			}
		} else {
			// no one is shooting at us. if we can shoot at someone, do so
			RobotInfo bestTarget = null;
			double minHealth = 1e99;
			for (RobotInfo enemy : enemies) {
				if (rc.getType().attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
					if (enemy.health < minHealth) {
						minHealth = enemy.health;
						bestTarget = enemy;
					}
				}
			}
			// shoot someone if there is someone to shoot
			if (bestTarget != null) {
				// Debug.indicate("micro", 0, "shooting an enemy while no one
				// can shoot us");
				attackIfReady(bestTarget.location);
				return true;
			}
			// we can't shoot anyone
			if (rc.isCoreReady()) { // all remaining possible actions are
									// movements
				// check if we can move to help an ally who has already engaged
				// a nearby enemy
				RobotInfo closestEnemy = Util.closest(enemies, here);
				// we can only think about engage enemies with equal or shorter
				// range
				if (closestEnemy != null && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
					int numAlliesFightingEnemy = numOtherAlliesInAttackRange(closestEnemy.location);
					if (numAlliesFightingEnemy > 0) {
						// see if we can assist our ally(s)
						int maxEnemyExposure = numAlliesFightingEnemy;
						if (tryMoveTowardLocationWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure,
								enemies)) {
							// Debug.indicate("micro", 0, "moved to assist
							// allies against " +
							// closestEnemy.location.toString() + ";
							// maxEnemyExposure = "
							// + maxEnemyExposure);
							return true;
						}
					} else {
						// no one is fighting this enemy, but we can try to
						// engage them if we can win the 1v1
						if (canWin1v1AfterMovingTo(here.add(here.directionTo(closestEnemy.location)), closestEnemy)) {
							int maxEnemyExposure = 1;
							if (tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(closestEnemy.location,
									maxEnemyExposure, enemies)) {
								// Debug.indicate("micro", 0, "moved to engage
								// enemy we can 1v1");
								return true;
							}
						}
					}
				}
				// Debug.indicate("micro", 0, "no micro action though core is
				// ready and there are nearby enemies");
				return false;
			}
			// return true here because core is not ready, so it's as if we took
			// a required action
			// in the sense that we can't do anything else
			// Debug.indicate("micro", 0, "no micro action; core isn't ready");
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
			if (rc.getType().attackRadiusSquared < moveLoc.distanceSquaredTo(loc))
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
			if (enemyExposure <= maxEnemyExposure) {
				rc.move(dir);
				return true;
			}
		}
		return false;
	}

	private static void shadowHarasser(RobotInfo enemyToShadow, RobotInfo[] enemies) throws GameActionException {
		Direction toEnemy = here.directionTo(enemyToShadow.location);
		Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(),
				toEnemy.rotateRight().rotateRight(), toEnemy.rotateLeft().rotateLeft() };
		for (Direction dir : dirs) {
			if (!rc.canMove(dir))
				continue;

			MapLocation loc = here.add(dir);

			boolean locIsSafe = true;

			for (RobotInfo enemy : enemies) {
				if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
					locIsSafe = false;
					break;
				}
			}

			if (locIsSafe) {
				rc.move(dir);
				break;
			}
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

	private static int numOtherAlliesInAttackRange(MapLocation loc) {
		int ret = 0;
		RobotInfo[] allies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, us);
		for (RobotInfo ally : allies) {
			if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location))
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

	public static boolean updateTargetLoc(Signal[] signals) throws GameActionException {
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderloc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DIRECT_MOBILE_ARCHON) {
						int[] data = purpose.decode(senderloc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)
								&& !Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
							targetDens[targetDenSize] = denLoc;
							targetDenSize++;
							numDensToHunt++;

							if (targetLoc == null
									|| here.distanceSquaredTo(denLoc) < here.distanceSquaredTo(targetLoc)) {
								targetLoc = denLoc;
								bestIndex = targetDenSize;
								huntingDen = true;
							}
						}
						return true;
					}
					if (purpose == MessageEncode.MOBILE_ARCHON_LOCATION) {
						int[] data = purpose.decode(senderloc, message);
						archonLoc = new MapLocation(data[0], data[1]);
						return true;
					}
					if (purpose == MessageEncode.ENEMY_ARMY_NOTIF) {
						if (targetLoc == null) {
							huntingDen = false;
							int[] data = purpose.decode(senderloc, message);
							MapLocation enemyLoc = new MapLocation(data[0], data[1]);
							targetLoc = enemyLoc;
						}
					}
				} else {
					MapLocation signalLoc = signal.getLocation();
					int distToSignal = here.distanceSquaredTo(signalLoc);
					if (rc.getType().sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER >= distToSignal
							&& (targetLoc == null || distToSignal < here.distanceSquaredTo(targetLoc))) {
						targetLoc = signalLoc;
						huntingDen = false;
						return true;
					} else {// if a den has been killed don't go for it anymore
						int closestIndex = Util.closestLocation(targetDens, signalLoc, targetDenSize);
						if (closestIndex != -1 && targetDens[closestIndex]
								.distanceSquaredTo(signalLoc) <= RobotType.SOLDIER.sensorRadiusSquared) {
							rc.setIndicatorString(1, "not gonig for den at loc " + targetDens[closestIndex]
									+ " on round " + rc.getRoundNum());
							killedDens[killedDenSize] = targetDens[closestIndex];
							killedDenSize++;
							targetDens[closestIndex] = null;
							numDensToHunt--;
						}
					}
				}
			}
		}

		if (huntingDen && (targetLoc == null
				|| rc.canSenseLocation(targetLoc) && rc.senseRobotAtLocation(targetLoc) == null)) {
			// tell people a den has been killed
			if (targetLoc != null) {
				rc.broadcastSignal(10000);
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
			return true;
		} else if (!huntingDen && targetLoc != null && here.distanceSquaredTo(targetLoc) < 10
				&& rc.senseHostileRobots(here, rc.getType().sensorRadiusSquared).length == 0) {
			targetLoc = null;
			if (numDensToHunt > 0) {
				huntingDen = true;
				bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
				targetLoc = targetDens[bestIndex];
			}
			return true;
		}
		/*
		 * RobotInfo[] allies =
		 * rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		 * for(RobotInfo ally : allies){ if(ally.ID == archonID){ targetLoc =
		 * ally.location; return true; } }
		 */
		return false;
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

	private static boolean updateArchonLoc(Signal[] signals) {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		for (RobotInfo ally : allies) {
			if (ally.ID == archonID) {
				archonLoc = ally.location;
				return true;
			}
		}
		return false;
	}

	private static void attackIfReady(MapLocation loc) throws GameActionException {
		if (rc.isWeaponReady()) {
			rc.attackLocation(loc);
		}
	}

	public static boolean updateTurretLoc() {
		if(enemyTurrets.size() > 0){
			int min = 999999;
			int dist;
			MapLocation turret;
			for(int i = 0 ; i < enemyTurrets.size(); i++){
				turret = enemyTurrets.get(i).location;
				dist = here.distanceSquaredTo(turret);
				if(dist < min){
					turretLoc = turret;
					min = dist;
				}
			}
			return true;
		}
		turretLoc = null;
		return false;	}

	public static void crunch() throws GameActionException {
		if (friends.length > 20)
			if (rc.isCoreReady() && rc.canMove(here.directionTo(turretLoc))) {
				RobotInfo[] blank = new RobotInfo[1];
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(blank);
				Nav.goTo(turretLoc, theSafety);
				Combat.shootAtNearbyEnemies();
			}
	}

	public static boolean updateMoveIn() {
		if (friends == null || friends.length < 15)
			return false;
		return true;
	}

	public static RobotInfo[] addRobotInfo(RobotInfo[] series, RobotInfo newInt) {
		// create a new array with extra index
		RobotInfo[] newSeries = new RobotInfo[series.length + 1];
		// copy the integers from series to newSeries
		for (int i = 0; i < series.length; i++) {
			newSeries[i] = series[i];
		}
		// add the new integer to the last index
		newSeries[newSeries.length - 1] = newInt;
		return newSeries;
	}

	public static void stayOutOfRange(RobotInfo[] enemies) throws GameActionException {
		rc.setIndicatorString(2, "staying out of range");
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets.toArray(new RobotInfo[0]), enemies));
		if (here.distanceSquaredTo(turretLoc) < 64) {
			Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
		} else {
			Nav.goTo(turretLoc, theSafety);
		}
	}

	public static void doHarass() throws GameActionException {
		friends = rc.senseNearbyRobots(here, RobotType.SOLDIER.sensorRadiusSquared, us);
		enemies = rc.senseHostileRobots(here, RobotType.SOLDIER.sensorRadiusSquared);
		enemiesICanShoot = rc.senseHostileRobots(here, RobotType.SOLDIER.attackRadiusSquared);
		Signal[] signals = rc.emptySignalQueue();
		rc.setIndicatorString(0, "" + signals.length);
		updateTurretList(signals,enemies);
		boolean turretUpdated = updateTurretLoc();
		boolean targetUpdated = updateTargetLoc(signals);
		boolean shouldMoveIn = updateMoveIn();
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets.toArray(new RobotInfo[0]), enemies));
		
			if (here.distanceSquaredTo(turretLoc) < 81 && rc.isCoreReady()) {
				//this is impossible!!
				// TODO: wtf was this supposed to do @theauthor
				Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
				rc.setIndicatorString(2, "turret");
			doMicro(enemies, enemiesICanShoot, targetUpdated, archonUpdated);
		}
		else{
			doMicro(enemies, enemiesICanShoot, targetUpdated, archonUpdated);
			if (rc.isCoreReady() && targetLoc != null) {
				rc.setIndicatorString(0, "I am moving to the target " + targetLoc);
				Nav.goTo(targetLoc, theSafety);
			} else if (rc.isCoreReady()) {
				rc.setIndicatorString(0, "I am exploring.");
				Nav.explore(enemies);

			}

		}
	}
}