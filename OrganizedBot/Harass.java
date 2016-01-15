package Battlecode2016.OrganizedBot;

import Battlecode2016.team061.MessageEncode;
import Battlecode2016.team061.Nav;
import Battlecode2016.team061.NavSafetyPolicy;
import Battlecode2016.team061.SafetyPolicyAvoidAllUnits;
import Battlecode2016.team061.Util;
import battlecode.common.*;

public class Harass extends Bot { // NEW read up bot types for what they call in Harass. Implement those and the rest of these methods are helper methods for the big ones. Once again Optimization.
	static RobotInfo[] enemies; 
	static RobotInfo[] enemiesICanShoot;
	static MapLocation archonLoc;
	static MapLocation targetLoc;
	static boolean targetUpdated;
	static boolean archonUpdated;
	static int archonID;
    private static boolean canWin1v1(RobotInfo enemy) {
        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        turnsTillWeCanAttack = (int) rc.getWeaponDelay();
        effectiveAttackDelay = (int)rc.getType().attackDelay;        
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
        turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
        effectiveEnemyAttackDelay = (int)enemy.type.attackDelay;
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
        int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
        int turnsTillWeCanAttack;
        int effectiveAttackDelay;
        
            double weaponDelayAfterMoving = rc.getWeaponDelay()- 1.0;
            turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
            effectiveAttackDelay = (int)rc.getType().attackDelay;
       
        int turnsToKillEnemy = turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy;

        int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
        int turnsTillEnemyCanAttack;
        int effectiveEnemyAttackDelay;
            turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
            effectiveEnemyAttackDelay = (int)enemy.type.attackDelay;
       
        int turnsForEnemyToKillUs = turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs;

        return turnsToKillEnemy <= turnsForEnemyToKillUs;
    }

    public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] enemies) {

        RobotInfo loneAttacker = null;
        int numAttackers = 0;
        for (RobotInfo enemy : enemies) {
            switch (enemy.type) {
                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                        numAttackers++;
                        if (numAttackers >= 2) return false;
                        loneAttacker = enemy;
                    }
                    break;
            }
        }

        if (numAttackers == 0) return true;

        return Harass.canWin1v1AfterMovingTo(loc, loneAttacker);
    }

    private static boolean tryToRetreat(RobotInfo[] enemies) throws GameActionException {
        Direction bestRetreatDir = null;
        RobotInfo currentClosestEnemy = Util.closest(enemies, here);

        boolean mustMoveOrthogonally = false;
       // if (rc.getType() == RobotType.DRONE && rc.getCoreDelay() >= 0.6 && currentClosestEnemy.type == RobotType.MISSILE) mustMoveOrthogonally = true;

        int bestDistSq = here.distanceSquaredTo(currentClosestEnemy.location);
        for (Direction dir : Direction.values()) {
            if (!rc.canMove(dir)) continue;
            if (mustMoveOrthogonally && dir.isDiagonal()) continue;

            MapLocation retreatLoc = here.add(dir);

            RobotInfo closestEnemy = Util.closest(enemies, retreatLoc);
            int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
            if (distSq > bestDistSq) {
                bestDistSq = distSq;
                bestRetreatDir = dir;
            }
        }

        if (bestRetreatDir != null) {
            rc.move(bestRetreatDir);
            return true;
        }
        return false;
    }

    // currently our micro looks like this:
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

    // it's definitely good to take 1v1s if there are no nearby enemies. however we maybe
    // should avoid initiating 1v1s if there are enemies nearby that can support.

    private static boolean doMicro(RobotInfo[] enemiesInSight,RobotInfo[] enemiesICanShoot,boolean targetUpdated,boolean archonUpdated) throws GameActionException {
        if (enemies.length == 0) {
            RobotInfo[] moreEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, them);
            if (moreEnemies.length == 0) {
                // Debug.indicate("micro", 0, "no enemies, no micro");
                return false;
            } else {
                RobotInfo closestEnemy = Util.closest(moreEnemies, here);
                if (closestEnemy != null && isHarasser(closestEnemy.type) && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
                    // Debug.indicate("micro", 0, "no nearby enemies, shadowing an enemy at long range");
                    if (rc.isCoreReady()) {
                        shadowHarasser(closestEnemy, enemies);
                    }
                    return true;
                }
            }
        }


        int numEnemiesAttackingUs = 0;
        RobotInfo[] enemiesAttackingUs = new RobotInfo[99];
        for (RobotInfo enemy : enemies) {
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
                        // check if we actually have some allied support. if so we can keep fighting
                        boolean haveSupport = false;
                        for (int i = 0; i < numEnemiesAttackingUs; i++) {
                            if (numOtherAlliesInAttackRange(enemiesAttackingUs[i].location) > 0) {
                                haveSupport = true;
                                break;
                            }
                        }
                        if (haveSupport) {
                            // an ally is helping us, so keep fighting the lone enemy
                            // Debug.indicate("micro", 0, "losing 1v1 but we have support");
                            attackIfReady(loneAttacker.location);
                            return true;
                        } else {
                            // we can't win the 1v1.
                            if (rc.getType().cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2
                                    && rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
                                // we can get a shot off and retreat before the enemy can fire at us again, so do that
                                // Debug.indicate("micro", 0, "firing one last shot before leaving losing 1v1");
                                attackIfReady(loneAttacker.location);
                                return true;
                            } else {
                                // we can't get another shot off. run away!
                                if (rc.isCoreReady()) {
                                    // we can move this turn
                                    if (tryToRetreat(enemies)) {
                                        // we moved away
                                        // Debug.indicate("micro", 0, "retreated");
                                        return true;
                                    } else {
                                        // we couldn't find anywhere to retreat to. fire a desperate shot if possible
                                        // Debug.indicate("micro", 0, "couldn't find anywhere to retreat! trying to shoot");
                                        attackIfReady(loneAttacker.location);
                                        return true;
                                    }
                                } else {
                                    // we can't move this turn. if it won't delay retreating, shoot instead
                                    // Debug.indicate("micro", 0, "want to retreat but core isn't ready; trying to shoot if cooldown <= 1");
                                    if (rc.getType().cooldownDelay <= 1) {
                                        attackIfReady(loneAttacker.location);
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                } else {
                    // we are getting shot by someone who outranges us. run away!
                    // Debug.indicate("micro", 0, "trying to retreat from a 1v1 where we are outranged");
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
                    if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy) maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
                    if (rc.getType().attackRadiusSquared >= here.distanceSquaredTo(enemy.location)) {
                        double targetingMetric = numAlliesAttackingEnemy / enemy.health;
                        if (targetingMetric > bestTargetingMetric) {
                            bestTargetingMetric = targetingMetric;
                            bestTarget = enemy;
                        }
                    }
                }

                // multiple enemies are attacking us. stay in the fight iff enough allies are also engaged
                if (maxAlliesAttackingAnEnemy >= numEnemiesAttackingUs) {
                    // enough allies are in the fight.
                    // Debug.indicate("micro", 0, "attacking because numEnemiesAttackingUs = " + numEnemiesAttackingUs + ", maxAlliesAttackingEnemy = "
                    // + maxAlliesAttackingAnEnemy);
                    attackIfReady(bestTarget.location);
                    return true;
                } else {
                    // not enough allies are in the fight. we need to retreat
                    if (rc.isCoreReady()) {
                        // we can move this turn
                        if (tryToRetreat(enemies)) {
                            // we moved away
                            // Debug.indicate("micro", 0, "retreated because numEnemiesAttackingUs = " + numEnemiesAttackingUs + ", maxAlliesAttackingEnemy = "
                            // + maxAlliesAttackingAnEnemy);
                            return true;
                        } else {
                            // we couldn't find anywhere to retreat to. fire a desperate shot if possible
                            // Debug.indicate("micro", 0, "no retreat square :( numEnemiesAttackingUs = " + numEnemiesAttackingUs +
                            // ", maxAlliesAttackingEnemy = "
                            // + maxAlliesAttackingAnEnemy);
                            attackIfReady(bestTarget.location);
                            return true;
                        }
                    } else {
                        // we can't move this turn. if it won't delay retreating, shoot instead
                        // Debug.indicate("micro", 0, "want to retreat but core on cooldown :( numEnemiesAttackingUs = " + numEnemiesAttackingUs
                        // + ", maxAlliesAttackingEnemy = " + maxAlliesAttackingAnEnemy);
                        if (rc.getType().cooldownDelay <= 1) {
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
                // Debug.indicate("micro", 0, "shooting an enemy while no one can shoot us");
                attackIfReady(bestTarget.location);
                return true;
            }

            // we can't shoot anyone

            if (rc.isCoreReady()) { // all remaining possible actions are movements
                // check if we can move to help an ally who has already engaged a nearby enemy
                RobotInfo closestEnemy = Util.closest(enemies, here);
                // we can only think about engage enemies with equal or shorter range, and we shouldn't try to engage missiles
                if (closestEnemy != null && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
                    int numAlliesFightingEnemy = numOtherAlliesInAttackRange(closestEnemy.location);

                    if (numAlliesFightingEnemy > 0) {
                        // see if we can assist our ally(s)
                        int maxEnemyExposure = numAlliesFightingEnemy;
                        if (tryMoveTowardLocationWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, enemies)) {
                            // Debug.indicate("micro", 0, "moved to assist allies against " + closestEnemy.location.toString() + "; maxEnemyExposure = "
                            // + maxEnemyExposure);
                            return true;
                        }
                    } else {
                        // no one is fighting this enemy, but we can try to engage them if we can win the 1v1
                        if (canWin1v1AfterMovingTo(here.add(here.directionTo(closestEnemy.location)), closestEnemy)) {
                            int maxEnemyExposure = 1;
                            if (tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, enemies)) {
                                // Debug.indicate("micro", 0, "moved to engage enemy we can 1v1");
                                return true;
                            }
                        }
                    }
                }

                // try to move toward and kill an enemy worker
              
                // no required actions
                // Debug.indicate("micro", 0, "no micro action though core is ready and there are nearby enemies");
                return false;
            }

            // return true here because core is not ready, so it's as if we took a required action
            // in the sense that we can't do anything else
            // Debug.indicate("micro", 0, "no micro action; core isn't ready");
            return true;
        }
    }

   

    private static boolean tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(MapLocation loc, int maxEnemyExposure, RobotInfo[] enemies)
            throws GameActionException {
        Direction toLoc = here.directionTo(loc);
        Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
        for (Direction dir : tryDirs) {
            if (!rc.canMove(dir)) continue;
            MapLocation moveLoc = here.add(dir);
            if (rc.getType().attackRadiusSquared < moveLoc.distanceSquaredTo(loc)) continue; // must engage in one turn

            int enemyExposure = numEnemiesAttackingLocation(moveLoc, enemies);
            if (enemyExposure <= maxEnemyExposure) {
                rc.move(dir);
                return true;
            }
        }

        return false;
    }

    private static boolean tryMoveTowardLocationWithMaxEnemyExposure(MapLocation loc, int maxEnemyExposure, RobotInfo[] enemies)
            throws GameActionException {
        Direction toLoc = here.directionTo(loc);
        Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
        for (Direction dir : tryDirs) {
            if (!rc.canMove(dir)) continue;
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
        Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(), toEnemy.rotateRight().rotateRight(),
                toEnemy.rotateLeft().rotateLeft() };
        for (Direction dir : dirs) {
            if (!rc.canMove(dir)) continue;

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
            if (enemies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(enemies[i].location)) ret++;
            
        }
        return ret;
    }

    private static int numOtherAlliesInAttackRange(MapLocation loc) {
        int ret = 0;
        RobotInfo[] allies = rc.senseNearbyRobots(loc, 15, us);
        for (RobotInfo ally : allies) {
            if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location)) ret++;
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

    private static boolean updateTargetLoc() {
		Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals) {
			if (signal.getTeam() == us && signal.getID() == archonID) {
				rc.setIndicatorString(1, "updating from message");
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderloc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DIRECT_MOBILE_ARCHON) {
						int[] data = purpose.decode(senderloc, message);
						targetLoc = new MapLocation(data[0], data[1]);
						return true;
					}
					if (purpose == MessageEncode.MOBILE_ARCHON_LOCATION) {
						int[] data = purpose.decode(senderloc, message);
						archonLoc = new MapLocation(data[0], data[1]);
						return true;
					}
				}
			}
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
    private static boolean updateArchonLoc() {
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

    static boolean firstTurn = true;

    public static void doHarass() throws GameActionException {
    	enemies = rc.senseHostileRobots(here, RobotType.SOLDIER.sensorRadiusSquared);
		enemiesICanShoot = rc.senseHostileRobots(here, RobotType.SOLDIER.attackRadiusSquared);
		boolean targetUpdated = updateTargetLoc();
		boolean archonUpdated = updateArchonLoc();
        if (doMicro(enemies,enemiesICanShoot,targetUpdated,archonUpdated)) {
            return;
        }

        if (rc.isCoreReady()) {
    		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
            Nav.goTo(targetLoc, theSafety);
        }
    }
}