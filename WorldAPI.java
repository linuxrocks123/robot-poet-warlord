/**
 * WorldAPI Interface:<br>
 * Provides callbacks into Simulator so your Robot can take actions in the
 * virtual world.<br>
 * The underlying class type is an inner class defined inside the simulator
 * which is opaque to your robot.
 */
public interface WorldAPI
{
     /*/**********************************************
      * Movement Methods
      ***********************************************/

     /**
      * Melee attack: attack an adjacent grid cell
      * @param power Power to use for attack (may not exceed attack skill)
      * @param adjacent_cell Adjacent GridCell to attack
      * @return AttackResult indicating whether attack succeeded and/or
      *         destroyed target of attack.
      */
     Robot.AttackResult meleeAttack(int power, Robot.GridCell adjacent_cell);

     /**
      * Ranged attack: attack nonadjacent grid cell within certain range
      * @param power Power to use for attack (may not exceed attack skill)
      * @param nonadjacent_cell non-adjacent GridCell to attack
      * @return AttackResult indicating whether attack succeeded and/or
      *         destroyed target of attack.
      */
     Robot.AttackResult rangedAttack(int power, Robot.GridCell nonadjacent_cell);

     /**
      * Capsule attack: attack with a capsule
      * @param power_of_capsule power of the capsule to use in the attack
      * @param cell GridCell (may be adjacent or nonadjacent) to attack
      */
     Robot.AttackResult capsuleAttack(int power_of_capsule, Robot.GridCell cell);

     /**
      * Defend: increase defense
      * @param power power to use for defense (may not exceed defense skill)
      */
     void defend(int power);

     /**
      * move: move robot
      * @param steps how far to move
      * @param way which way to move
      */
     void move(int steps, Robot.Direction way);

     /**
      * pick_up_capsule: pick up a capsule
      * @param adjacent_cell GridCell where capsule is that you want to pick
      *                      up (must be adjacent)
      */
     void pick_up_capsule(Robot.GridCell adjacent_cell);

     /**
      * drop_capsule: drop a capsule (for an ally to pick up, presumably)
      * @param adjacent_cell where to drop capsule (must be adjacent)
      * @param power_of_capsule how powerful a capsule to drop
      */
     void drop_capsule(Robot.GridCell adjacent_cell, int power_of_capsule);

     /*/**********************************************
      * Movement Methods
      ***********************************************/

     /**
      * Tells us what we're in the middle of building.
      * @return BuildStatus object indicating what we're building.
      *         Is null if we're not building anything.
      */
     Robot.BuildStatus getBuildStatus();

     /**@return GridCell indicating the target of our building efforts.
      *         Is null if we're not building anything.
      */
     Robot.GridCell getBuildTarget();

     /**@return how much we've invested in our current build target.
      */
     int getInvestedBuildPower();

     /**
      * Tells the simulator the robot is beginning to build something in an
      * adjacent cell (or, for capsules, inside itself).<br>
      * In order to mark a capsule or robot as "done", call this method with
      * both parameters null.  This <i>will</i> destroy any in-progress
      * build.<br><br>
      * Note that you must not move from your current cell while in the
      * process of building anything other than a capsule.  If you do, you
      * will lose any in-progress work on a wall or fort, and a robot will
      * be automatically finalized with however many skill points you've
      * invested up to that point.
      * @param status what we're going to start building (or, in the case of
      *               an energy capsule, resume building)
      * @param location where to direct our building efforts.  Must be an
      *                 adjacent, empty location.
      */
     void setBuildTarget(Robot.BuildStatus status, Robot.GridCell location);

     /**@param power how much power to apply to building the current target.
      *              Must not be more than remaining power needed to finish
      *              building target.
      */
     void build(int power);

     /**
      * Sends a message to a newly created robot.<br>
      * Call this method immediately after finishing building a new robot to
      * give it a message about the current status of the world or direct it
      * to allocate its skill points in a specific way.  If you do not call
      * this method before ending your turn on a turn where you've finished
      * constructing a robot, the robot's creation message will be all 0s.
      * @param message 8-byte array containing the message to send
      */
     void sendCreationNotice(byte[] message);

     /**
      * Spend power to repair yourself.  2 power restores 1 health.
      * @param power amount of power to spend on repairs.  Should be even.
      */
     void repair(int power);

     /**
      * Spend power to charge an adjacent ally robot.  1-for-1 efficiency.
      * @param power amount of power to use for charging ally
      * @param ally cell containing ally to charge.  Must be adjacent.
      */
     void charge(int power, Robot.GridCell ally);

     /*/**********************************************
      * Radio Methods
      ***********************************************/

     /**
      * Spend additional power to get radio messages unavailable because of
      * jamming.
      * @param power amount of power to spend to attempt to overcome jamming
      * @return messages received after additional power spent.  Will
      *         include all messages included in simulator's call to act().
      *         May be null if no messages were received.
      */
     byte[][] getMessages(int power);

     /**
      * Sends a message to an ally or allies.
      * @param message 64-byte array containing message to transmit
      * @param power amount of power to use for sending message
      */
     void sendMessage(byte[] message, int power);

     /**
      * Gets a copy of the portion of the world visible to the robot.
      * Range is equal to defense skill.  Does not cost any power.
      * @return a 2-dimensional array containing a GridCell for each cell
      *         visible to the robot.
      */
     Robot.GridCell[][] getVisibleNeighborhood();

     /**
      * Gets a copy of the entire world.  Takes 3 power, plus additional if
      * jamming is taking place.
      * @param power to spend attempting to get the world
      * @return a 2-dimensional array containing a GridCell for each cell in
      *         the world.  Will be null if jamming has prevented the world
      *         from being retrieved.
      */
     Robot.GridCell[][] getWorld(int power);

     /**
      * Scans an enemy (or ally), retrieving information about the robot.
      * The cell scanned must be visible (within defense cells from us).<br>
      * Takes 1 power.
      * @param enemySpecs empty Robot_Specs object to be filled in
      * @param enemyStatus empty Robot_Status object to be filled in
      * @param toScan cell containing robot we want to scan
      */
     void scanEnemy(Robot.Robot_Specs enemySpecs, Robot.Robot_Status enemyStatus, Robot.GridCell toScan);

     /**
      * Jams radio.  Affects both allies and enemies.  Also affects self, so
      * it's best to call this method only after we've attempted to get
      * everything we can out of and send everything we want into the radio.
      * @param power power to use for jamming.  Must not be more than 5.
      */
     void jamRadio(int power);
}
