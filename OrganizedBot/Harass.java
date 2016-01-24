package OrganizedBot;

import battlecode.common.*;

public class Harass extends Bot {
	// NEW read up bot types for what they call in Harass.
	// Implement those and the rest of these methods are helper methods for the
	// big ones.
	// Once again Optimization.
	static MapLocation turretLoc;
	static MapLocation targetLoc;
	static MapLocation archonLoc;
	static boolean targetUpdated;
	static boolean archonUpdated;
	static boolean huntingDen;
	static boolean crunching;
	static int archonID;

	private static boolean canWin1v1(RobotInfo enemy) {

		if (enemy.type == RobotType.ARCHON)
			return true;

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
		if (enemy.type == RobotType.ARCHON)
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
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(enemies, here);

		boolean mustMoveOrthogonally = false;
		double bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
		for (Direction dir : Direction.values()) {
			if (!rc.canMove(dir))
				continue;
			if (mustMoveOrthogonally && dir.isDiagonal())
				continue;

			MapLocation retreatLoc = here.add(dir);

			RobotInfo closestEnemy = Util.closest(enemies, retreatLoc);
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			double rubble = rc.senseRubble(retreatLoc);
			double rubbleMod = rubble<GameConstants.RUBBLE_SLOW_THRESH?0:rubble*2/GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			if (distSq-rubbleMod > bestDistSq) {
				bestDistSq = distSq-rubbleMod;
				bestRetreatDir = dir;
			}
		}

		if (bestRetreatDir != null && rc.isCoreReady()&&rc.canMove(bestRetreatDir)) {
			rc.move(bestRetreatDir);
			return true;
		}
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

	private static boolean doMicro(RobotInfo[] enemiesInSight, RobotInfo[] enemiesICanShoot, RobotInfo[] allies) throws GameActionException {
		if (enemiesInSight.length == 0) {
			return false;
		}
		boolean willDieFromViper = (rc.isInfected() && rc.getHealth() - rc.getViperInfectedTurns() * GameConstants.VIPER_INFECTION_DAMAGE < 0);
		if (willDieFromViper && rc.isCoreReady()) {
			// CHARGE blindly
			Nav.goTo(Util.closest(enemiesInSight, here).location, new SafetyPolicyAvoidAllUnits(new RobotInfo[]{}));
		}

		int numEnemiesAttackingUs = 0;
		RobotInfo[] enemiesAttackingUs = new RobotInfo[enemiesInSight.length];
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
				if (type.attackRadiusSquared >= here.distanceSquaredTo(loneAttacker.location)) {
					// we can actually shoot at the enemy we are 1v1ing
					if (canWin1v1(loneAttacker) || loneAttacker.type == RobotType.ARCHON) {
						// we can beat the other guy 1v1. fire away!
						attackIfReady(loneAttacker.location);
						if (loneAttacker.type == RobotType.ARCHON && rc.isCoreReady())
							shadowHarasser(loneAttacker, enemiesInSight);
						if(rc.isCoreReady())
							tryToRetreat(enemiesInSight);
					} else {
						// check if we actually have some allied support. if so, we can keep fighting
						if (numOtherAlliesInAttackRange(loneAttacker.location, allies) > 0) {
							// an ally is helping us, so keep fighting the lone enemy
							if(rc.isCoreReady() && loneAttacker.team == Team.ZOMBIE)
								tryToRetreat(enemiesInSight);
							attackIfReady(loneAttacker.location);
						} else {
							// we can't win the 1v1.
							if (type.cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2
									&& rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
								// we can get a shot off and retreat before the enemy can fire at us again, so do that
								attackIfReady(loneAttacker.location);
								if(rc.isCoreReady())
									tryToRetreat(enemiesInSight);
							} else {
								// we can't get another shot off. run away!
								if (rc.isCoreReady()) {
									// we can move this turn
									if (tryToRetreat(enemiesInSight)) {
										// we moved away
										return true;
									} else {
										// we couldn't find anywhere to retreat to. fire a desperate shot if possible
										attackIfReady(loneAttacker.location);
										return true;
									}
								} else {
									// we can't move this turn. if it won't delay retreating, shoot instead
									if (type.cooldownDelay <= 1) {
										attackIfReady(loneAttacker.location);
									}
									return true;
								}
							}
						}
					}
				} else {
					// we are getting shot by someone who outranges us, CRUNCH!
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
					int numAlliesAttackingEnemy = 1 + numOtherAlliesInAttackRange(enemy.location, allies);
					if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy)
						maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
					if (type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
						double targetingMetric = numAlliesAttackingEnemy / enemy.health
								+ enemy.attackPower / 2.0 // TODO: optimize
								+ enemy.type.attackRadiusSquared / 2.0 // ranged things are annoying TODO: optimize
								+ (enemy.team == Team.ZOMBIE?0:200) // shoot zombies last
								+ (enemy.type == RobotType.FASTZOMBIE?5:0)
								+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0)?50:0);// shoot non-infected first if viper
						if (targetingMetric > bestTargetingMetric) {
							bestTargetingMetric = targetingMetric;
							bestTarget = enemy;
						}
					}

				}
				// multiple enemies are attacking us. stay in the fight iff enough allies are also engaged
				if (maxAlliesAttackingAnEnemy >= numEnemiesAttackingUs && bestTarget != null) {
					// enough allies are in the fight.
					attackIfReady(bestTarget.location);
					if(rc.isCoreReady())
						tryToRetreat(enemiesInSight);
					return true;
				} else {
					// not enough allies are in the fight. we need to retreat
					if (rc.isCoreReady()) {
						// we can move this turn
						if (tryToRetreat(enemiesInSight)) {
							// we moved away :)
							return true;
						} else if (bestTarget != null) {
							// we couldn't find anywhere to retreat to. fire a desperate shot if possible
							attackIfReady(bestTarget.location);
							return true;
						}
					} else {
						// we can't move this turn. if it won't delay retreating, shoot instead
						if (type.cooldownDelay <= 1 && bestTarget != null) {
							attackIfReady(bestTarget.location);
						}
						return true;
					}
				}
			}
		} else {
			// no one is shooting at us. if we can shoot at someone, do so
			RobotInfo bestTarget = null;
			double bestTargetingMetric = 0;
			int maxAlliesAttackingAnEnemy = 0;
			for (RobotInfo enemy : enemiesInSight) {
				int numAlliesAttackingEnemy = 1 + numOtherAlliesInAttackRange(enemy.location, allies);
				if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy)
					maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
				if (type.attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
					double targetingMetric = numAlliesAttackingEnemy / enemy.health
							+ enemy.attackPower // TODO: optimize
							+ (enemy.type == RobotType.FASTZOMBIE?10:0)
							+ (enemy.team == Team.ZOMBIE?0:100) // shoot zombies last
							+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0)?50:0);// shoot non-infected first if viper
					if (targetingMetric > bestTargetingMetric) {
						bestTargetingMetric = targetingMetric;
						bestTarget = enemy;
					}
				}
			}
			// shoot someone if there is someone to shoot
			if (bestTarget != null) {
				attackIfReady(bestTarget.location);
				return true;
			}
			// we can't shoot anyone or there was weapon delay
			if (rc.isCoreReady()) { // all remaining possible actions are movements
				// check if we can move to help an ally who has already engaged a nearby enemy
				RobotInfo closestEnemy = Util.closest(enemiesInSight, here);
				// we can only think about engage enemies with equal or shorter range
				if (closestEnemy != null
						&& (type.attackRadiusSquared >= closestEnemy.type.attackRadiusSquared
							|| closestEnemy.type == RobotType.ARCHON)) {
					//we outrange them
					int numAlliesFightingEnemy = numOtherAlliesInAttackRange(closestEnemy.location, allies);
					if (numAlliesFightingEnemy > 0) {
						// see if we can assist our ally(s)
						int maxEnemyExposure = numAlliesFightingEnemy;
						if (tryMoveTowardLocationWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, enemiesInSight)) {
							return true;
						}
						// TODO: what if that didn't work?
					} else {
						// no one is fighting this enemy, but we can try to engage them if we can win the 1v1
						if (canWin1v1AfterMovingTo(here.add(here.directionTo(closestEnemy.location)), closestEnemy)) {
							int maxEnemyExposure = 1;
							if (tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(closestEnemy.location,
									maxEnemyExposure, enemiesInSight)) {
								return true;
							}
							// TODO: what if it didn't work?
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

	private static int numOtherAlliesInAttackRange(MapLocation loc, RobotInfo[] allies) {
		int ret = 0;
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
		boolean updated = false;
		boolean canSeeHostiles = rc.senseHostileRobots(here, type.sensorRadiusSquared).length > 0;
		if (type == RobotType.VIPER) {
			return updateViperTargetLoc(signals);
		}
		//int startB = Clock.getBytecodeNum();
		if(targetLoc == null){
			RobotInfo[] zombies = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE);
			for (RobotInfo zombie : zombies) {
				if (zombie.type == RobotType.ZOMBIEDEN) {
					targetLoc = zombie.location;
					updated = true;
				}
			}
		}
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
							if (!huntingDen//test this
									|| here.distanceSquaredTo(denLoc) < here.distanceSquaredTo(targetLoc)) {
								targetLoc = denLoc;
								bestIndex = targetDenSize;
								huntingDen = true;
							}
						}
						updated = true;
					}
//					if (purpose == MessageEncode.MOBILE_ARCHON_LOCATION) {
//						int[] data = purpose.decode(senderloc, message);
//						archonLoc = new MapLocation(data[0], data[1]);
//						return true;
//					}
					if (purpose == MessageEncode.ENEMY_ARMY_NOTIF && numDensToHunt == 0) {
						int[] data = purpose.decode(senderloc, message);
						MapLocation enemyLoc = new MapLocation(data[0], data[1]);
						if (!huntingDen && (targetLoc == null || (double) here.distanceSquaredTo(enemyLoc) < 1.5
								* (here.distanceSquaredTo(targetLoc)))) {
							targetLoc = enemyLoc;
						}
					}
				} else {
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
					if (closestIndex != -1 && signalLoc.distanceSquaredTo(targetDens[closestIndex]) <= type.sensorRadiusSquared){
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
								bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
								targetLoc = targetDens[bestIndex];
							}
							updated = true;
						}
						// }
					}
				}
			}
			else{
				if(canSeeHostiles)
					continue;
				MapLocation enemyLoc = signal.getLocation();
				if (targetLoc == null || !huntingDen && (double) here.distanceSquaredTo(enemyLoc) < 0.5
						* (here.distanceSquaredTo(targetLoc))) {
					targetLoc = enemyLoc;
					updated = true;
					huntingDen = false;
				}
			}
		}
		if (huntingDen && rc.canSenseLocation(targetLoc) && rc.senseRobotAtLocation(targetLoc) == null) {
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
			updated = true;
		}
		else if (!huntingDen && targetLoc != null && here.distanceSquaredTo(targetLoc) < 5
				&& rc.senseHostileRobots(here, type.sensorRadiusSquared).length == 0) {
			targetLoc = null;
			huntingDen = false;
			if (numDensToHunt > 0) {
				huntingDen = true;
				bestIndex = Util.closestLocation(targetDens, here, targetDenSize);
				targetLoc = targetDens[bestIndex];
			}
			updated = true;
		}
		/*
		 * RobotInfo[] allies =
		 * rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		 * for(RobotInfo ally : allies){ if(ally.ID == archonID){ targetLoc =
		 * ally.location; return true; } }
		 */
		//System.out.println(Clock.getBytecodeNum() - startB);
		return updated;
	}

	private static boolean updateViperTargetLoc(Signal[] signals) {
		boolean updated = false;
		if (targetLoc != null && here.distanceSquaredTo(targetLoc) < 5
				&& rc.senseHostileRobots(here, type.sensorRadiusSquared).length == 0) {
			targetLoc = null;
			updated = true;
		}
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderLoc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.ENEMY_ARMY_NOTIF) {
						int[] data = purpose.decode(senderLoc, message);
						MapLocation enemyLoc = new MapLocation(data[0], data[1]);
						if (targetLoc == null || (double) here.distanceSquaredTo(enemyLoc) < 1.5
								* (here.distanceSquaredTo(targetLoc))) {
							targetLoc = enemyLoc;
							updated = true;
						}
					}
				}
			}
			else{
				MapLocation enemyLoc = signal.getLocation();
				if (targetLoc == null || (double) here.distanceSquaredTo(enemyLoc) < 0.5
						* (here.distanceSquaredTo(targetLoc))) {
					targetLoc = enemyLoc;
					updated = true;
				}
			}
		}
		if (targetLoc == null) {
			MapLocation[] enemyArchonLocations = rc.getInitialArchonLocations(them);
			do {
				int locIndex = Util.closestLocation(enemyArchonLocations, here, enemyArchonLocations.length);
				if (locIndex == -1){
					targetLoc = null;
					break;
				}
				targetLoc = enemyArchonLocations[locIndex];
				enemyArchonLocations[locIndex] = null;
			} while (here.distanceSquaredTo(targetLoc) < 5);
			if (targetLoc != null)
				updated = true;
		}
		//rc.setIndicatorString(2, "targetLoc = " + targetLoc);
		return updated;
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
		// then set turretTarget to closest one
		if (turretSize > 0) {
			int min = 999999;
			int dist;
			MapLocation turret;
			for (int i = 0; i < turretSize; i++) {
				turret = enemyTurrets[i].location;
				dist = here.distanceSquaredTo(turret);
				if (dist < min) {
					turretLoc = turret;
					min = dist;
				}
			}
			return true;
		}
		turretLoc = null;
		return false;

	}

	public static void updateMoveIn(Signal[] signals, RobotInfo[] enemies) {
		// if(type == RobotType.VIPER)
		// return false;
		if (turretLoc == null || enemies.length == 0 && here.distanceSquaredTo(turretLoc) < type.sensorRadiusSquared || here.distanceSquaredTo(turretLoc) > 150) {
			crunching = false;
		}
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.CRUNCH_TIME){
						int[] mess = purpose.decode(signal.getLocation(), message);
						if(here.distanceSquaredTo(new MapLocation(mess[0], mess[1])) <= 400)
							crunching = true;
					}
				}
			}
		}
	}

	public static void crunch() throws GameActionException {
		// if (friends.length > 20)
		if (turretLoc != null && rc.isCoreReady()) {
			NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(new RobotInfo[0]);
			Nav.goTo(turretLoc, theSafety);
		}
		if (rc.isWeaponReady()) {
			Combat.shootAtNearbyEnemies();
			return;
		}
	}

	public static void stayOutOfRange(RobotInfo[] enemies) throws GameActionException {
		rc.setIndicatorString(2, "staying out of range");
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(
				Util.combineTwoRIArrays(enemyTurrets, turretSize, enemies));
		if (here.distanceSquaredTo(turretLoc) < 64) {
			Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
		} else {
			Nav.goTo(turretLoc, theSafety);
		}
	}

	public static boolean updateTurretList(Signal[] signals, RobotInfo[] enemies) throws GameActionException{
		boolean updated = updateTurretList(signals);
		for (int i = 0; i < turretSize; i++) {
			MapLocation t = enemyTurrets[i].location;
			if (rc.canSenseLocation(t)) {
				RobotInfo bot = rc.senseRobotAtLocation(t);
				if (bot == null || bot.type != RobotType.TURRET) {
					removeLocFromTurretArray(t);
					i--;
					updated = true;
				}
			}
		}
		for (RobotInfo e : enemies)
			if (e.type == RobotType.TURRET)
				if(!isLocationInTurretArray(e.location)){
					enemyTurrets[turretSize] = e;
					turretSize++;
					updated = true;
				}
		return updated;
	}
	
	public static void doHarass() throws GameActionException {
		RobotInfo[] friends = rc.senseNearbyRobots(here, type.sensorRadiusSquared, us);
		RobotInfo[] enemies = Util.combineTwoRIArrays(enemyTurrets, turretSize, rc.senseHostileRobots(here, type.sensorRadiusSquared));
		RobotInfo[] enemiesICanShoot = rc.senseHostileRobots(here, type.attackRadiusSquared);
		Signal[] signals = rc.emptySignalQueue();
		// rc.setIndicatorString(0, "" + signals.length);
		updateTurretList(signals, enemies);
		boolean turretUpdated = updateTurretLoc();
		updateMoveIn(signals, enemies);
		boolean targetUpdated = updateTargetLoc(signals);
		// TODO Nate, can you take a look at the macro micro please, I'm bad at it
		// starts here
		if (crunching) {
			crunch();
		} else {
			doMicro(enemies, enemiesICanShoot, friends);
		}
		if(rc.isCoreReady()){ // no enemies
			// maybe uncomment this but only do it if we can't see a scout
//			if (turretLoc != null && here.distanceSquaredTo(turretLoc) < type.TURRET.attackRadiusSquared + 4) {
//				Nav.goTo(here.add(turretLoc.directionTo(here)), theSafety);
			if (targetLoc != null) {
				Nav.goTo(targetLoc, new SafetyPolicyAvoidAllUnits(enemies));
			}
			else if(!
					Util.checkRubbleAndClear(here.directionTo(center), true))
				Nav.explore(enemies, friends);
		}
		// ends here
	}
}